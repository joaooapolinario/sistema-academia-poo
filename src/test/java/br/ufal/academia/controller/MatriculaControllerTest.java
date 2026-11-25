package br.ufal.academia.controller;

import br.ufal.academia.dao.AlunoDAO;
import br.ufal.academia.dao.MatriculaDAO;
import br.ufal.academia.dao.PlanoDAO;
import br.ufal.academia.exception.DadosInvalidosException;
import br.ufal.academia.exception.MatriculaAtivaExistenteException;
import br.ufal.academia.model.entities.Aluno;
import br.ufal.academia.model.entities.Matricula;
import br.ufal.academia.model.entities.Plano;
import br.ufal.academia.model.enums.ObjetivoTreino;
import br.ufal.academia.model.enums.StatusMatricula;
import br.ufal.academia.model.enums.TipoPlano;
import br.ufal.academia.model.patterns.observer.AlertaInterfaceObserver;
import br.ufal.academia.model.patterns.observer.LogAuditoriaObserver;
import br.ufal.academia.model.patterns.strategy.CalculoPadraoStrategy;
import br.ufal.academia.model.patterns.strategy.DescontoEstudanteStrategy;
import br.ufal.academia.model.patterns.strategy.DescontoIdosoStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatriculaControllerTest {

    private static final Clock RELOGIO_NOVEMBRO =
            Clock.fixed(Instant.parse("2026-11-15T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate HOJE = LocalDate.of(2026, 11, 15);

    private AlunoDAOEmMemoria alunoDAO;
    private PlanoDAOEmMemoria planoDAO;
    private MatriculaDAOEmMemoria matriculaDAO;
    private AlertaInterfaceObserver alertaObserver;
    private LogAuditoriaObserver logObserver;

    private Aluno maria;
    private Aluno joaoIdoso;
    private Aluno pedro59Anos;
    private Plano mensal;
    private Plano trimestral;

    @BeforeEach
    void setUp() throws Exception {
        alunoDAO = new AlunoDAOEmMemoria();
        planoDAO = new PlanoDAOEmMemoria();
        matriculaDAO = new MatriculaDAOEmMemoria();
        alertaObserver = new AlertaInterfaceObserver(msg -> { });
        logObserver = new LogAuditoriaObserver(msg -> { });

        maria = new Aluno("Maria Souza", "52998224725", "82999990000",
                LocalDate.of(2000, 1, 10), ObjetivoTreino.HIPERTROFIA);
        joaoIdoso = new Aluno("João Silva", "11144477735", "82988887777",
                LocalDate.of(1966, 11, 15), ObjetivoTreino.REABILITACAO); // 60 anos exatos em 2026-11-15
        pedro59Anos = new Aluno("Pedro Lima", "39053344705", "82977776666",
                LocalDate.of(1966, 11, 16), ObjetivoTreino.CONDICIONAMENTO); // 59 anos em 2026-11-15

        alunoDAO.salvar(maria);
        alunoDAO.salvar(joaoIdoso);
        alunoDAO.salvar(pedro59Anos);

        mensal = new Plano(TipoPlano.MENSAL, "Mensal", 1, 100.0);
        trimestral = new Plano(TipoPlano.TRIMESTRAL, "Trimestral", 3, 99.9);
        planoDAO.salvar(mensal);
        planoDAO.salvar(trimestral);
    }

    @Test
    void deveMatricularComCalculoPadraoEAcoplarObservers() throws Exception {
        IMatriculaController controller = criarController(RELOGIO_NOVEMBRO);

        Matricula matricula = controller.matricular("529.982.247-25", TipoPlano.MENSAL, false);

        assertTrue(matricula.getId() > 0);
        assertEquals(StatusMatricula.ATIVA, matricula.getStatus());
        assertEquals(HOJE, matricula.getDataInicio());
        assertEquals(HOJE.plusMonths(1), matricula.getDataFim());
        assertInstanceOf(CalculoPadraoStrategy.class, matricula.getCalculoMensalidade());
        assertEquals(100.0, matricula.calcularMensalidade(), 0.001);
        assertEquals(1, matriculaDAO.listarTodos().size());

        // Os observers acoplados na criação reagem a mudanças de estado na instância retornada.
        matricula.cancelar();
        assertEquals(1, alertaObserver.getAlertas().size());
        assertEquals(1, logObserver.getRegistros().size());
    }

    @Test
    void deveAplicarDescontoEstudanteQuandoEstudanteEMenorDe60Anos() throws Exception {
        IMatriculaController controller = criarController(RELOGIO_NOVEMBRO);

        Matricula matricula = controller.matricular(pedro59Anos.getCpf(), TipoPlano.MENSAL, true);

        assertInstanceOf(DescontoEstudanteStrategy.class, matricula.getCalculoMensalidade());
        assertEquals(80.0, matricula.calcularMensalidade(), 0.001);
    }

    @Test
    void devePriorizarDescontoIdosoParaIdadeMaiorOuIgualA60MesmoSeEstudante() throws Exception {
        IMatriculaController controller = criarController(RELOGIO_NOVEMBRO);

        Matricula matricula = controller.matricular(joaoIdoso.getCpf(), TipoPlano.MENSAL, true);

        assertInstanceOf(DescontoIdosoStrategy.class, matricula.getCalculoMensalidade());
        assertEquals(70.0, matricula.calcularMensalidade(), 0.001);
    }

    @Test
    void deveBloquearMatriculaDuplicadaQuandoJaExisteMatriculaAtivaVigente() throws Exception {
        IMatriculaController controller = criarController(RELOGIO_NOVEMBRO);
        controller.matricular(maria.getCpf(), TipoPlano.MENSAL, false);

        assertThrows(MatriculaAtivaExistenteException.class,
                () -> controller.matricular("529.982.247-25", TipoPlano.TRIMESTRAL, true));

        assertEquals(1, matriculaDAO.listarTodos().size());
        assertEquals(0, matriculaDAO.atualizacoes);
        assertTrue(alertaObserver.getAlertas().isEmpty());
        assertTrue(logObserver.getRegistros().isEmpty());
    }

    @Test
    void deveVencerMatriculaExpiradaNoBancoENotificarObserversAntesDeCriarNovaMatricula() throws Exception {
        // Matrícula antiga ainda marcada como ATIVA no banco, porém vencida em 2026-11-01.
        Matricula antiga = new Matricula(maria, mensal, LocalDate.of(2026, 10, 1), new CalculoPadraoStrategy());
        matriculaDAO.salvar(antiga);
        IMatriculaController controller = criarController(RELOGIO_NOVEMBRO);

        Matricula nova = controller.matricular(maria.getCpf(), TipoPlano.TRIMESTRAL, true);

        assertEquals(StatusMatricula.VENCIDA, antiga.getStatus());
        assertEquals(1, matriculaDAO.atualizacoes);
        assertEquals(StatusMatricula.ATIVA, nova.getStatus());
        assertEquals(HOJE, nova.getDataInicio());
        assertEquals(HOJE.plusMonths(3), nova.getDataFim());
        assertEquals(2, matriculaDAO.listarTodos().size());
        assertEquals(1, alertaObserver.getAlertas().size());
        assertTrue(alertaObserver.getAlertas().get(0).contains("venceu em 2026-11-01"));
        assertEquals(1, logObserver.getRegistros().size());
        assertTrue(logObserver.getRegistros().get(0).contains("VENCIDA"));
    }

    @Test
    void devePermitirNovaMatriculaQuandoAnteriorFoiCancelada() throws Exception {
        IMatriculaController controller = criarController(RELOGIO_NOVEMBRO);
        Matricula primeira = controller.matricular(maria.getCpf(), TipoPlano.MENSAL, false);
        controller.cancelar(primeira.getId());

        Matricula segunda = controller.matricular(maria.getCpf(), TipoPlano.TRIMESTRAL, false);

        assertEquals(StatusMatricula.CANCELADA, primeira.getStatus());
        assertEquals(StatusMatricula.ATIVA, segunda.getStatus());
        assertEquals(2, matriculaDAO.listarTodos().size());
    }

    @Test
    void deveCancelarMatriculaPersistirENotificarObservers() throws Exception {
        IMatriculaController controller = criarController(RELOGIO_NOVEMBRO);
        Matricula matricula = controller.matricular(maria.getCpf(), TipoPlano.MENSAL, false);

        controller.cancelar(matricula.getId());

        assertEquals(StatusMatricula.CANCELADA, matricula.getStatus());
        assertEquals(1, matriculaDAO.atualizacoes);
        assertEquals(1, alertaObserver.getAlertas().size());
        assertTrue(alertaObserver.getAlertas().get(0).contains("foi cancelada"));
        assertEquals(1, logObserver.getRegistros().size());
        assertTrue(logObserver.getRegistros().get(0).contains("CANCELADA"));

        // Não permite cancelar novamente.
        assertThrows(DadosInvalidosException.class, () -> controller.cancelar(matricula.getId()));
        assertEquals(1, matriculaDAO.atualizacoes);
        assertEquals(1, alertaObserver.getAlertas().size());
    }

    @Test
    void deveRenovarMatriculaAtivaVigenteEstendendoAPartirDaDataFim() throws Exception {
        Matricula matricula = new Matricula(maria, mensal, LocalDate.of(2026, 11, 1), new CalculoPadraoStrategy());
        matriculaDAO.salvar(matricula);
        IMatriculaController controller = criarController(RELOGIO_NOVEMBRO);

        controller.renovar(matricula.getId());

        assertEquals(StatusMatricula.ATIVA, matricula.getStatus());
        assertEquals(LocalDate.of(2026, 12, 1), matricula.getDataInicio());
        assertEquals(LocalDate.of(2027, 1, 1), matricula.getDataFim());
        assertEquals(1, matriculaDAO.atualizacoes);
        assertEquals(1, alertaObserver.getAlertas().size());
        assertTrue(alertaObserver.getAlertas().get(0).contains("está ativa até 2027-01-01"));
        assertEquals(1, logObserver.getRegistros().size());
        assertTrue(logObserver.getRegistros().get(0).contains("ATIVA"));
    }

    @Test
    void deveRenovarMatriculaVencidaRecomecandoAPartirDeHoje() throws Exception {
        Matricula matricula = new Matricula(maria, trimestral, LocalDate.of(2026, 6, 1), new CalculoPadraoStrategy());
        matricula.verificarVencimento(HOJE);
        matriculaDAO.salvar(matricula);
        IMatriculaController controller = criarController(RELOGIO_NOVEMBRO);

        controller.renovar(matricula.getId());

        assertEquals(StatusMatricula.ATIVA, matricula.getStatus());
        assertEquals(HOJE, matricula.getDataInicio());
        assertEquals(HOJE.plusMonths(3), matricula.getDataFim());
        assertEquals(1, matriculaDAO.atualizacoes);
        assertEquals(1, alertaObserver.getAlertas().size());
        assertEquals(1, logObserver.getRegistros().size());
    }

    @Test
    void deveRenovarMatriculaAtivaComPrazoExpiradoRecomecandoAPartirDeHoje() throws Exception {
        // Status ainda ATIVA no banco, mas dataFim (2026-10-01) já passou.
        Matricula matricula = new Matricula(maria, mensal, LocalDate.of(2026, 9, 1), new CalculoPadraoStrategy());
        matriculaDAO.salvar(matricula);
        IMatriculaController controller = criarController(RELOGIO_NOVEMBRO);

        controller.renovar(matricula.getId());

        assertEquals(StatusMatricula.ATIVA, matricula.getStatus());
        assertEquals(HOJE, matricula.getDataInicio());
        assertEquals(HOJE.plusMonths(1), matricula.getDataFim());
        assertEquals(1, matriculaDAO.atualizacoes);
        assertEquals(1, alertaObserver.getAlertas().size());
        assertEquals(1, logObserver.getRegistros().size());
    }

    @Test
    void deveRejeitarRenovacaoDeMatriculaCanceladaOuQuandoOutraAtivaExiste() throws Exception {
        IMatriculaController controller = criarController(RELOGIO_NOVEMBRO);
        Matricula cancelada = controller.matricular(maria.getCpf(), TipoPlano.MENSAL, false);
        controller.cancelar(cancelada.getId());
        int atualizacoesAntes = matriculaDAO.atualizacoes;
        int alertasAntes = alertaObserver.getAlertas().size();

        assertThrows(DadosInvalidosException.class, () -> controller.renovar(cancelada.getId()));
        assertEquals(atualizacoesAntes, matriculaDAO.atualizacoes);
        assertEquals(alertasAntes, alertaObserver.getAlertas().size());

        // Aluno com matrícula vencida + nova matrícula ativa vigente não pode reativar a antiga.
        Matricula vencida = new Matricula(joaoIdoso, mensal, LocalDate.of(2026, 9, 1), new DescontoIdosoStrategy());
        vencida.verificarVencimento(HOJE);
        matriculaDAO.salvar(vencida);
        controller.matricular(joaoIdoso.getCpf(), TipoPlano.MENSAL, false);

        assertThrows(DadosInvalidosException.class, () -> controller.renovar(vencida.getId()));
        assertEquals(StatusMatricula.VENCIDA, vencida.getStatus());
    }

    @Test
    void deveAtualizarVencimentosEmLoteNotificandoApenasAsExpiradas() throws Exception {
        Matricula expirada = new Matricula(maria, mensal, LocalDate.of(2026, 10, 14), new CalculoPadraoStrategy());
        Matricula venceHoje = new Matricula(joaoIdoso, mensal, LocalDate.of(2026, 10, 15), new DescontoIdosoStrategy());
        Matricula vigente = new Matricula(pedro59Anos, mensal, LocalDate.of(2026, 11, 1), new CalculoPadraoStrategy());
        matriculaDAO.salvar(expirada);
        matriculaDAO.salvar(venceHoje);
        matriculaDAO.salvar(vigente);
        IMatriculaController controller = criarController(RELOGIO_NOVEMBRO);

        controller.atualizarVencimentos();

        assertEquals(StatusMatricula.VENCIDA, expirada.getStatus());
        assertEquals(StatusMatricula.ATIVA, venceHoje.getStatus());
        assertEquals(StatusMatricula.ATIVA, vigente.getStatus());
        assertEquals(1, matriculaDAO.atualizacoes);
        assertEquals(1, alertaObserver.getAlertas().size());
        assertTrue(alertaObserver.getAlertas().get(0).contains(maria.getNome()));
        assertEquals(1, logObserver.getRegistros().size());
    }

    @Test
    void deveListarTodasAsMatriculasEmListaImutavel() throws Exception {
        IMatriculaController controller = criarController(RELOGIO_NOVEMBRO);
        Matricula ativa = controller.matricular(maria.getCpf(), TipoPlano.MENSAL, false);
        Matricula cancelada = controller.matricular(joaoIdoso.getCpf(), TipoPlano.TRIMESTRAL, false);
        controller.cancelar(cancelada.getId());

        List<Matricula> todas = controller.listar();

        assertEquals(List.of(ativa, cancelada), todas);
        assertThrows(UnsupportedOperationException.class, () -> todas.add(ativa));
    }

    @Test
    void deveListarMatriculasAVencerDentroDaJanelaEmListaImutavelOrdenada() throws Exception {
        // Expirada ontem (2026-11-14) - fora
        Matricula expirada = new Matricula(maria, mensal, LocalDate.of(2026, 10, 14), new CalculoPadraoStrategy());
        // Vence no limite (2026-11-22 = hoje + 7) - dentro
        Matricula venceNoLimite = new Matricula(joaoIdoso, mensal, LocalDate.of(2026, 10, 22), new DescontoIdosoStrategy());
        // Vence hoje (2026-11-15 = hoje + 0) - dentro
        Matricula venceHoje = new Matricula(pedro59Anos, mensal, LocalDate.of(2026, 10, 15), new CalculoPadraoStrategy());
        // Vence depois do limite (2026-11-23 = hoje + 8) - fora
        Aluno ana = new Aluno("Ana Costa", "98765432100", "82966665555",
                LocalDate.of(1995, 5, 5), ObjetivoTreino.EMAGRECIMENTO);
        alunoDAO.salvar(ana);
        Matricula foraDoLimite = new Matricula(ana, mensal, LocalDate.of(2026, 10, 23), new CalculoPadraoStrategy());
        // Cancelada dentro da janela - fora
        Matricula cancelada = new Matricula(ana, mensal, LocalDate.of(2026, 10, 18), new CalculoPadraoStrategy());
        cancelada.cancelar();

        matriculaDAO.salvar(expirada);
        matriculaDAO.salvar(venceNoLimite);
        matriculaDAO.salvar(venceHoje);
        matriculaDAO.salvar(foraDoLimite);
        matriculaDAO.salvar(cancelada);
        IMatriculaController controller = criarController(RELOGIO_NOVEMBRO);

        List<Matricula> aVencer7 = controller.listarAVencer(7);
        List<Matricula> aVencerHoje = controller.listarAVencer(0);

        assertEquals(List.of(venceHoje, venceNoLimite), aVencer7);
        assertEquals(List.of(venceHoje), aVencerHoje);
        assertThrows(UnsupportedOperationException.class, () -> aVencer7.add(venceHoje));
        assertThrows(DadosInvalidosException.class, () -> controller.listarAVencer(-1));
    }

    @Test
    void deveCalcularFaturamentoMensalSomenteDeMatriculasAtivasVigentes() throws Exception {
        IMatriculaController controller = criarController(RELOGIO_NOVEMBRO);
        assertEquals(0.0, controller.calcularFaturamentoMensal(), 0.001);

        // Maria: Trimestral 99.90 padrão -> 99.90
        controller.matricular(maria.getCpf(), TipoPlano.TRIMESTRAL, false);
        // João (60 anos): Trimestral 99.90 idoso (30% off) -> 69.93
        controller.matricular(joaoIdoso.getCpf(), TipoPlano.TRIMESTRAL, false);
        // Pedro (59 anos): Trimestral 99.90 estudante (20% off) -> 79.92
        controller.matricular(pedro59Anos.getCpf(), TipoPlano.TRIMESTRAL, true);

        // Matrícula cancelada e matrícula expirada não devem entrar no faturamento.
        Aluno ana = new Aluno("Ana Costa", "98765432100", "82966665555",
                LocalDate.of(1995, 5, 5), ObjetivoTreino.EMAGRECIMENTO);
        alunoDAO.salvar(ana);
        Matricula cancelada = new Matricula(ana, mensal, HOJE, new CalculoPadraoStrategy());
        cancelada.cancelar();
        Matricula ativaExpirada = new Matricula(ana, mensal, LocalDate.of(2026, 9, 1), new CalculoPadraoStrategy());
        matriculaDAO.salvar(cancelada);
        matriculaDAO.salvar(ativaExpirada);

        // 99.90 + 69.93 + 79.92 = 249.75
        assertEquals(249.75, controller.calcularFaturamentoMensal(), 0.001);
    }

    @Test
    void deveRejeitarParametrosInvalidosEConstrutoresNulos() {
        IMatriculaController controller = criarController(RELOGIO_NOVEMBRO);

        assertThrows(DadosInvalidosException.class, () -> controller.matricular(null, TipoPlano.MENSAL, false));
        assertThrows(DadosInvalidosException.class, () -> controller.matricular("   ", TipoPlano.MENSAL, false));
        assertThrows(DadosInvalidosException.class, () -> controller.matricular("---", TipoPlano.MENSAL, false));
        assertThrows(DadosInvalidosException.class, () -> controller.matricular(maria.getCpf(), null, false));
        assertThrows(DadosInvalidosException.class, () -> controller.matricular("98765432100", TipoPlano.MENSAL, false));
        assertThrows(DadosInvalidosException.class, () -> controller.matricular(maria.getCpf(), TipoPlano.ANUAL, false));

        assertThrows(DadosInvalidosException.class, () -> controller.cancelar(0));
        assertThrows(DadosInvalidosException.class, () -> controller.cancelar(-5));
        assertThrows(DadosInvalidosException.class, () -> controller.cancelar(999));

        assertThrows(DadosInvalidosException.class, () -> controller.renovar(0));
        assertThrows(DadosInvalidosException.class, () -> controller.renovar(-1));
        assertThrows(DadosInvalidosException.class, () -> controller.renovar(999));

        assertThrows(IllegalArgumentException.class, () -> new MatriculaController(null, alunoDAO, planoDAO));
        assertThrows(IllegalArgumentException.class, () -> new MatriculaController(matriculaDAO, null, planoDAO));
        assertThrows(IllegalArgumentException.class, () -> new MatriculaController(matriculaDAO, alunoDAO, null));
        assertThrows(IllegalArgumentException.class,
                () -> new MatriculaController(matriculaDAO, alunoDAO, planoDAO, null));
        assertThrows(IllegalArgumentException.class,
                () -> new MatriculaController(matriculaDAO, alunoDAO, planoDAO, List.of(), null));
    }

    private MatriculaController criarController(Clock clock) {
        return new MatriculaController(
                matriculaDAO, alunoDAO, planoDAO, List.of(alertaObserver, logObserver), clock);
    }

    private static class AlunoDAOEmMemoria implements AlunoDAO {
        private final List<Aluno> alunos = new ArrayList<>();
        private int proximoId = 1;

        @Override
        public void salvar(Aluno aluno) {
            aluno.atribuirId(proximoId++);
            alunos.add(aluno);
        }

        @Override public void atualizar(Aluno aluno) { }
        @Override public void remover(int id) { alunos.removeIf(a -> a.getId() == id); }

        @Override
        public Optional<Aluno> buscarPorId(int id) {
            return alunos.stream().filter(a -> a.getId() == id).findFirst();
        }

        @Override public List<Aluno> listarTodos() { return List.copyOf(alunos); }

        @Override
        public Optional<Aluno> buscarPorCpf(String cpf) {
            return alunos.stream().filter(a -> a.getCpf().equals(cpf)).findFirst();
        }
    }

    private static class PlanoDAOEmMemoria implements PlanoDAO {
        private final List<Plano> planos = new ArrayList<>();
        private int proximoId = 1;

        @Override
        public void salvar(Plano plano) {
            plano.atribuirId(proximoId++);
            planos.add(plano);
        }

        @Override public void atualizar(Plano plano) { }
        @Override public void remover(int id) { planos.removeIf(p -> p.getId() == id); }

        @Override
        public Optional<Plano> buscarPorId(int id) {
            return planos.stream().filter(p -> p.getId() == id).findFirst();
        }

        @Override public List<Plano> listarTodos() { return List.copyOf(planos); }

        @Override
        public Optional<Plano> buscarPorTipo(TipoPlano tipo) {
            return planos.stream().filter(p -> p.getTipo() == tipo).findFirst();
        }
    }

    private static class MatriculaDAOEmMemoria implements MatriculaDAO {
        private final List<Matricula> matriculas = new ArrayList<>();
        private int proximoId = 1;
        private int atualizacoes;

        @Override
        public void salvar(Matricula matricula) {
            matricula.atribuirId(proximoId++);
            matriculas.add(matricula);
        }

        @Override
        public void atualizar(Matricula matricula) {
            atualizacoes++;
        }

        @Override public void remover(int id) { matriculas.removeIf(m -> m.getId() == id); }

        @Override
        public Optional<Matricula> buscarPorId(int id) {
            return matriculas.stream().filter(m -> m.getId() == id).findFirst();
        }

        @Override public List<Matricula> listarTodos() { return List.copyOf(matriculas); }

        @Override
        public Optional<Matricula> buscarAtivaPorAluno(int idAluno) {
            return matriculas.stream()
                    .filter(m -> m.getAluno().getId() == idAluno && m.estaAtiva())
                    .findFirst();
        }

        @Override
        public List<Matricula> listarPorStatus(StatusMatricula status) {
            return matriculas.stream().filter(m -> m.getStatus() == status).toList();
        }
    }
}
