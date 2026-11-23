package br.ufal.academia.controller;

import br.ufal.academia.dao.AlunoDAO;
import br.ufal.academia.dao.FrequenciaDAO;
import br.ufal.academia.dao.MatriculaDAO;
import br.ufal.academia.exception.AcessoNegadoException;
import br.ufal.academia.exception.DadosInvalidosException;
import br.ufal.academia.model.entities.Aluno;
import br.ufal.academia.model.entities.Frequencia;
import br.ufal.academia.model.entities.Matricula;
import br.ufal.academia.model.entities.Plano;
import br.ufal.academia.model.enums.ObjetivoTreino;
import br.ufal.academia.model.enums.StatusMatricula;
import br.ufal.academia.model.enums.TipoPlano;
import br.ufal.academia.model.patterns.observer.AlertaInterfaceObserver;
import br.ufal.academia.model.patterns.observer.LogAuditoriaObserver;
import br.ufal.academia.model.patterns.strategy.CalculoPadraoStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrequenciaControllerTest {

    private static final Clock RELOGIO_NOVEMBRO =
            Clock.fixed(Instant.parse("2026-11-15T10:00:00Z"), ZoneOffset.UTC);

    private AlunoDAOEmMemoria alunoDAO;
    private MatriculaDAOEmMemoria matriculaDAO;
    private FrequenciaDAOEmMemoria frequenciaDAO;
    private AlertaInterfaceObserver alertaObserver;
    private LogAuditoriaObserver logObserver;
    private Aluno maria;
    private Plano mensal;

    @BeforeEach
    void setUp() throws Exception {
        alunoDAO = new AlunoDAOEmMemoria();
        matriculaDAO = new MatriculaDAOEmMemoria();
        frequenciaDAO = new FrequenciaDAOEmMemoria();
        alertaObserver = new AlertaInterfaceObserver(msg -> { });
        logObserver = new LogAuditoriaObserver(msg -> { });

        maria = new Aluno("Maria Souza", "52998224725", "82999990000",
                LocalDate.of(2000, 1, 10), ObjetivoTreino.HIPERTROFIA);
        alunoDAO.salvar(maria);
        mensal = new Plano(TipoPlano.MENSAL, "Mensal", 1, 99.9);
        mensal.atribuirId(1);
    }

    @Test
    void deveRegistrarEntradaParaAlunoComMatriculaAtiva() throws Exception {
        Matricula matricula = new Matricula(maria, mensal, LocalDate.of(2026, 11, 1), new CalculoPadraoStrategy());
        matriculaDAO.salvar(matricula);
        IFrequenciaController controller = criarController(RELOGIO_NOVEMBRO);

        // Aceita CPF formatado com pontuação.
        Frequencia registrada = controller.registrarEntrada("529.982.247-25");

        assertTrue(registrada.getId() > 0);
        assertEquals(matricula.getId(), registrada.getMatricula().getId());
        assertEquals(LocalDateTime.of(2026, 11, 15, 10, 0), registrada.getDataHoraEntrada());
        assertEquals(1, frequenciaDAO.listarTodos().size());
        assertTrue(alertaObserver.getAlertas().isEmpty());
        assertTrue(logObserver.getRegistros().isEmpty());
    }

    @Test
    void deveNegarAcessoQuandoCpfNaoCadastrado() {
        IFrequenciaController controller = criarController(RELOGIO_NOVEMBRO);

        assertThrows(AcessoNegadoException.class, () -> controller.registrarEntrada("11144477735"));
        assertTrue(frequenciaDAO.listarTodos().isEmpty());
    }

    @Test
    void deveNegarAcessoQuandoAlunoNaoPossuiMatriculaAtiva() throws Exception {
        Matricula cancelada = new Matricula(maria, mensal, LocalDate.of(2026, 11, 1), new CalculoPadraoStrategy());
        cancelada.cancelar();
        matriculaDAO.salvar(cancelada);
        IFrequenciaController controller = criarController(RELOGIO_NOVEMBRO);

        assertThrows(AcessoNegadoException.class, () -> controller.registrarEntrada("52998224725"));
        assertTrue(frequenciaDAO.listarTodos().isEmpty());
    }

    @Test
    void deveVencerMatriculaNotificarObservadoresENegarAcessoNaCatraca() throws Exception {
        Matricula matricula = new Matricula(maria, mensal, LocalDate.of(2026, 10, 1), new CalculoPadraoStrategy());
        matriculaDAO.salvar(matricula);
        IFrequenciaController controller = criarController(RELOGIO_NOVEMBRO);

        assertThrows(AcessoNegadoException.class, () -> controller.registrarEntrada("52998224725"));

        assertEquals(StatusMatricula.VENCIDA, matricula.getStatus());
        assertEquals(1, matriculaDAO.atualizacoes);
        assertEquals(1, alertaObserver.getAlertas().size());
        assertEquals(1, logObserver.getRegistros().size());
        assertTrue(frequenciaDAO.listarTodos().isEmpty());
    }

    @Test
    void deveListarEContarPresencasPorMes() throws Exception {
        Matricula matricula = new Matricula(maria, mensal, LocalDate.of(2026, 11, 1), new CalculoPadraoStrategy());
        matriculaDAO.salvar(matricula);
        frequenciaDAO.salvar(new Frequencia(matricula, LocalDateTime.of(2026, 11, 5, 8, 0)));
        frequenciaDAO.salvar(new Frequencia(matricula, LocalDateTime.of(2026, 11, 20, 19, 0)));
        frequenciaDAO.salvar(new Frequencia(matricula, LocalDateTime.of(2026, 12, 1, 7, 0)));
        IFrequenciaController controller = criarController(RELOGIO_NOVEMBRO);

        List<Frequencia> novembro = controller.listarPorAluno("529.982.247-25", YearMonth.of(2026, 11));

        assertEquals(2, novembro.size());
        assertEquals(2, controller.contarPresencas("52998224725", YearMonth.of(2026, 11)));
        assertEquals(1, controller.contarPresencas("52998224725", YearMonth.of(2026, 12)));
        assertEquals(0, controller.contarPresencas("52998224725", YearMonth.of(2026, 10)));
        assertThrows(UnsupportedOperationException.class, () -> novembro.add(novembro.get(0)));
    }

    @Test
    void deveRejeitarParametrosInvalidos() {
        IFrequenciaController controller = criarController(RELOGIO_NOVEMBRO);

        assertThrows(DadosInvalidosException.class, () -> controller.registrarEntrada(null));
        assertThrows(DadosInvalidosException.class, () -> controller.registrarEntrada("   "));
        assertThrows(DadosInvalidosException.class, () -> controller.registrarEntrada("---"));
        assertThrows(DadosInvalidosException.class, () -> controller.listarPorAluno("52998224725", null));
        assertThrows(DadosInvalidosException.class, () -> controller.listarPorAluno("11144477735", YearMonth.of(2026, 11)));
        assertThrows(IllegalArgumentException.class, () -> new FrequenciaController(null, alunoDAO, matriculaDAO));
        assertThrows(IllegalArgumentException.class, () -> new FrequenciaController(frequenciaDAO, null, matriculaDAO));
        assertThrows(IllegalArgumentException.class, () -> new FrequenciaController(frequenciaDAO, alunoDAO, null));
        assertThrows(IllegalArgumentException.class, () -> new FrequenciaController(frequenciaDAO, alunoDAO, matriculaDAO, null));
    }

    private FrequenciaController criarController(Clock clock) {
        return new FrequenciaController(
                frequenciaDAO, alunoDAO, matriculaDAO, List.of(alertaObserver, logObserver), clock);
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

    private static class FrequenciaDAOEmMemoria implements FrequenciaDAO {
        private final List<Frequencia> frequencias = new ArrayList<>();
        private int proximoId = 1;

        @Override
        public void salvar(Frequencia frequencia) {
            frequencia.atribuirId(proximoId++);
            frequencias.add(frequencia);
        }

        @Override public void atualizar(Frequencia frequencia) { }
        @Override public void remover(int id) { frequencias.removeIf(f -> f.getId() == id); }

        @Override
        public Optional<Frequencia> buscarPorId(int id) {
            return frequencias.stream().filter(f -> f.getId() == id).findFirst();
        }

        @Override public List<Frequencia> listarTodos() { return List.copyOf(frequencias); }

        @Override
        public List<Frequencia> listarPorAlunoEPeriodo(int idAluno, LocalDate inicio, LocalDate fim) {
            return frequencias.stream()
                    .filter(f -> f.getMatricula().getAluno().getId() == idAluno)
                    .filter(f -> {
                        LocalDate dia = f.getDataHoraEntrada().toLocalDate();
                        return !dia.isBefore(inicio) && !dia.isAfter(fim);
                    })
                    .toList();
        }
    }
}
