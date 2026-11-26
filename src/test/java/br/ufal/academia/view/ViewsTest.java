package br.ufal.academia.view;

import br.ufal.academia.controller.IAlunoController;
import br.ufal.academia.controller.IFrequenciaController;
import br.ufal.academia.controller.IInstrutorController;
import br.ufal.academia.controller.IMatriculaController;
import br.ufal.academia.controller.ITreinoController;
import br.ufal.academia.exception.DadosInvalidosException;
import br.ufal.academia.model.entities.Aluno;
import br.ufal.academia.model.entities.Frequencia;
import br.ufal.academia.model.entities.Instrutor;
import br.ufal.academia.model.entities.Matricula;
import br.ufal.academia.model.entities.Plano;
import br.ufal.academia.model.entities.Treino;
import br.ufal.academia.model.enums.ObjetivoTreino;
import br.ufal.academia.model.enums.TipoPlano;
import br.ufal.academia.model.patterns.factory.PlanoFactory;
import br.ufal.academia.model.patterns.observer.MatriculaObserver;
import br.ufal.academia.model.patterns.strategy.CalculoPadraoStrategy;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViewsTest {

    @Test
    void construtoresDevemRejeitarControllersNulos() {
        IAlunoController alunoController = new AlunoControllerStub();
        IMatriculaController matriculaController = new MatriculaControllerStub();

        assertThrows(IllegalArgumentException.class, () -> new AlunoView(null));
        assertThrows(IllegalArgumentException.class, () -> new InstrutorView(null));
        assertThrows(IllegalArgumentException.class, () -> new MatriculaView(null, alunoController));
        assertThrows(IllegalArgumentException.class, () -> new MatriculaView(matriculaController, null));
    }

    @Test
    void alunoViewDeveFormatarCpfETelefone() {
        assertEquals("529.982.247-25", AlunoView.formatarCpf("52998224725"));
        assertEquals("(82) 99999-0000", AlunoView.formatarTelefone("82999990000"));
        assertEquals("(82) 3221-4000", AlunoView.formatarTelefone("8232214000"));
    }

    @Test
    void matriculaViewDeveDelegarSimulacaoDeMensalidadeAoController() throws Exception {
        Aluno aluno = new Aluno("Maria Souza", "52998224725", "82999990000",
                LocalDate.now().minusYears(25), ObjetivoTreino.HIPERTROFIA);
        Plano planoMensal = PlanoFactory.criar(TipoPlano.MENSAL);
        MatriculaControllerStub matriculaController = new MatriculaControllerStub(
                List.of(), 0.0, List.of(planoMensal), 79.92);
        AlunoControllerStub alunoController = new AlunoControllerStub(List.of(aluno));

        AtomicReference<MatriculaView> viewRef = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> viewRef.set(new MatriculaView(matriculaController, alunoController)));
        MatriculaView view = viewRef.get();

        assertEquals(1, matriculaController.chamadasListarPlanos);
        assertEquals("Selecione um plano para calcular a mensalidade.", view.getTextoPreview());
        assertEquals("Selecione um aluno para calcular a mensalidade.",
                view.formatarPreview(null, planoMensal, false));

        String preview = view.formatarPreview(aluno, planoMensal, true);
        assertTrue(preview.contains("79,92"), "Preview inesperado: " + preview);
        assertTrue(preview.contains("1 mês"), "Vigência inesperada: " + preview);
        assertEquals(aluno.getCpf(), matriculaController.ultimoCpfSimulado);
        assertEquals(TipoPlano.MENSAL, matriculaController.ultimoTipoSimulado);
        assertTrue(matriculaController.ultimoEstudanteSimulado);
    }

    @Test
    void matriculaViewDeveImplementarMatriculaObserverEReagirANotificacoes() throws Exception {
        Aluno aluno = new Aluno("Maria Souza", "52998224725", "82999990000",
                LocalDate.now().minusYears(25), ObjetivoTreino.HIPERTROFIA);
        Matricula matricula = new Matricula(aluno, PlanoFactory.criar(TipoPlano.MENSAL),
                LocalDate.now(), new CalculoPadraoStrategy());
        matricula.atribuirId(7);

        AlunoControllerStub alunoController = new AlunoControllerStub(List.of(aluno));
        MatriculaControllerStub matriculaController = new MatriculaControllerStub(List.of(matricula), 99.90);

        AtomicReference<MatriculaView> viewRef = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> viewRef.set(new MatriculaView(matriculaController, alunoController)));
        MatriculaView view = viewRef.get();

        assertInstanceOf(MatriculaObserver.class, view);
        assertThrows(IllegalArgumentException.class, () -> view.atualizar(null));
        assertTrue(view.getTextoFaturamento().contains("99,90"));

        matricula.adicionarObserver(view);
        matricula.cancelar();
        SwingUtilities.invokeAndWait(() -> { });

        assertNotNull(view.getUltimaNotificacao());
        assertTrue(view.getUltimaNotificacao().contains("Maria Souza"));
        assertTrue(view.getUltimaNotificacao().contains("CANCELADA"));
    }

    @Test
    void telaPrincipalDevePlugarTodasAsCincoAbasQuandoAmbienteGraficoDisponivel() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "Teste de JFrame requer display gráfico.");

        AtomicReference<TelaPrincipal> telaRef = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> telaRef.set(new TelaPrincipal(
                new AlunoControllerStub(),
                new InstrutorControllerStub(),
                new MatriculaControllerStub(),
                new TreinoControllerStub(),
                new FrequenciaControllerStub())));

        TelaPrincipal tela = telaRef.get();
        try {
            assertEquals(5, tela.getTabbedPane().getTabCount());
            assertInstanceOf(AlunoView.class, tela.getTabbedPane().getComponentAt(TelaPrincipal.ABA_ALUNOS));
            assertInstanceOf(InstrutorView.class, tela.getTabbedPane().getComponentAt(TelaPrincipal.ABA_INSTRUTORES));
            assertInstanceOf(MatriculaView.class, tela.getTabbedPane().getComponentAt(TelaPrincipal.ABA_MATRICULAS));
            assertInstanceOf(TreinoView.class, tela.getTabbedPane().getComponentAt(TelaPrincipal.ABA_TREINOS));
            assertInstanceOf(FrequenciaView.class, tela.getTabbedPane().getComponentAt(TelaPrincipal.ABA_FREQUENCIA));
        } finally {
            SwingUtilities.invokeAndWait(tela::dispose);
        }
    }

    private static class AlunoControllerStub implements IAlunoController {
        private final List<Aluno> alunos;

        AlunoControllerStub() {
            this(List.of());
        }

        AlunoControllerStub(List<Aluno> alunos) {
            this.alunos = new ArrayList<>(alunos);
        }

        @Override
        public Aluno cadastrar(String nome, String cpf, String telefone, LocalDate dataNascimento,
                               ObjetivoTreino objetivo) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Aluno buscarPorCpf(String cpf) throws DadosInvalidosException {
            throw new DadosInvalidosException("Não encontrado");
        }

        @Override
        public List<Aluno> listar() {
            return List.copyOf(alunos);
        }

        @Override public void atualizarTelefone(String cpf, String telefone) { }
        @Override public void adicionarRestricaoMedica(String cpf, String restricao) { }
        @Override public void removerRestricaoMedica(String cpf, String restricao) { }
        @Override public void remover(String cpf) { }
    }

    private static class InstrutorControllerStub implements IInstrutorController {
        @Override
        public Instrutor cadastrar(String nome, String cpf, String telefone, String cref, String especialidade) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Instrutor buscarPorCref(String cref) throws DadosInvalidosException {
            throw new DadosInvalidosException("Não encontrado");
        }

        @Override
        public List<Instrutor> listar() {
            return List.of();
        }

        @Override public void atualizarEspecialidade(String cref, String especialidade) { }
        @Override public void remover(String cref) { }
    }

    private static class MatriculaControllerStub implements IMatriculaController {
        private final List<Matricula> matriculas;
        private final double faturamento;
        private final List<Plano> planos;
        private final double valorSimulado;
        private int chamadasListarPlanos;
        private String ultimoCpfSimulado;
        private TipoPlano ultimoTipoSimulado;
        private boolean ultimoEstudanteSimulado;

        MatriculaControllerStub() {
            this(List.of(), 0.0, List.of(), 0.0);
        }

        MatriculaControllerStub(List<Matricula> matriculas, double faturamento) {
            this(matriculas, faturamento, List.of(), 0.0);
        }

        MatriculaControllerStub(List<Matricula> matriculas, double faturamento,
                                List<Plano> planos, double valorSimulado) {
            this.matriculas = new ArrayList<>(matriculas);
            this.faturamento = faturamento;
            this.planos = new ArrayList<>(planos);
            this.valorSimulado = valorSimulado;
        }

        @Override
        public Matricula matricular(String cpfAluno, TipoPlano tipo, boolean estudante) {
            throw new UnsupportedOperationException();
        }

        @Override public void cancelar(int idMatricula) { }
        @Override public void renovar(int idMatricula) { }
        @Override public void atualizarVencimentos() { }

        @Override
        public List<Matricula> listar() {
            return List.copyOf(matriculas);
        }

        @Override
        public List<Matricula> listarAVencer(int dias) {
            return List.copyOf(matriculas);
        }

        @Override
        public List<Plano> listarPlanos() {
            chamadasListarPlanos++;
            return List.copyOf(planos);
        }

        @Override
        public double simularMensalidade(String cpfAluno, TipoPlano tipo, boolean estudante) {
            this.ultimoCpfSimulado = cpfAluno;
            this.ultimoTipoSimulado = tipo;
            this.ultimoEstudanteSimulado = estudante;
            return valorSimulado;
        }

        @Override
        public double calcularFaturamentoMensal() {
            return faturamento;
        }
    }

    private static class TreinoControllerStub implements ITreinoController {
        @Override
        public Treino criar(String cpfAluno, String crefInstrutor, ObjetivoTreino objetivo) {
            throw new UnsupportedOperationException();
        }

        @Override public void adicionarExercicio(int idTreino, String descricao) { }
        @Override public void removerExercicio(int idTreino, String descricao) { }
        @Override public List<Treino> listarPorAluno(String cpf) { return List.of(); }
        @Override public void remover(int idTreino) { }
    }

    private static class FrequenciaControllerStub implements IFrequenciaController {
        @Override
        public Frequencia registrarEntrada(String cpf) {
            throw new UnsupportedOperationException();
        }

        @Override public List<Frequencia> listarPorAluno(String cpf, YearMonth mes) { return List.of(); }
        @Override public int contarPresencas(String cpf, YearMonth mes) { return 0; }
    }
}
