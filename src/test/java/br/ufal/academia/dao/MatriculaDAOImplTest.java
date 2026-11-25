package br.ufal.academia.dao;

import br.ufal.academia.model.entities.Aluno;
import br.ufal.academia.model.entities.Matricula;
import br.ufal.academia.model.entities.Plano;
import br.ufal.academia.model.enums.ObjetivoTreino;
import br.ufal.academia.model.enums.StatusMatricula;
import br.ufal.academia.model.enums.TipoPlano;
import br.ufal.academia.model.patterns.strategy.CalculoPadraoStrategy;
import br.ufal.academia.model.patterns.strategy.DescontoEstudanteStrategy;
import br.ufal.academia.model.patterns.strategy.DescontoIdosoStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatriculaDAOImplTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 11, 1);

    private ConnectionFactory factory;
    private PlanoDAOImpl planoDAO;
    private MatriculaDAOImpl dao;
    private Aluno aluno;
    private Plano plano;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        factory = ConnectionFactory.criarParaTestes("jdbc:sqlite:" + tempDir.resolve("matriculas.db").toAbsolutePath());
        planoDAO = new PlanoDAOImpl(factory);
        aluno = inserirAluno(1, "Maria Souza", "52998224725");
        plano = new Plano(TipoPlano.TRIMESTRAL, "Trimestral", 3, 100.0);
        planoDAO.salvar(plano);
        dao = new MatriculaDAOImpl(factory, new AlunoDAOEmMemoria(aluno), planoDAO);
    }

    @Test
    void deveSalvarEAtribuirIdGerado() throws Exception {
        Matricula matricula = new Matricula(aluno, plano, INICIO, new CalculoPadraoStrategy());

        dao.salvar(matricula);

        assertTrue(matricula.getId() > 0);
    }

    @Test
    void deveBuscarPorIdPreservandoCamposEEstrategia() throws Exception {
        Matricula matricula = new Matricula(aluno, plano, INICIO, new DescontoEstudanteStrategy());
        dao.salvar(matricula);

        Matricula lida = dao.buscarPorId(matricula.getId()).orElseThrow();

        assertEquals(matricula.getId(), lida.getId());
        assertEquals(aluno.getId(), lida.getAluno().getId());
        assertEquals(plano.getId(), lida.getPlano().getId());
        assertEquals(INICIO, lida.getDataInicio());
        assertEquals(LocalDate.of(2027, 2, 1), lida.getDataFim());
        assertEquals(StatusMatricula.ATIVA, lida.getStatus());
        assertInstanceOf(DescontoEstudanteStrategy.class, lida.getCalculoMensalidade());
        assertEquals(80.0, lida.calcularMensalidade(), 0.001);
    }

    @Test
    void deveRetornarVazioQuandoNaoEncontrar() {
        assertFalse(dao.buscarPorId(999).isPresent());
        assertFalse(dao.buscarAtivaPorAluno(1).isPresent());
    }

    @Test
    void deveListarTodas() throws Exception {
        assertTrue(dao.listarTodos().isEmpty());
        dao.salvar(new Matricula(aluno, plano, INICIO, new CalculoPadraoStrategy()));
        dao.salvar(new Matricula(aluno, plano, INICIO.minusYears(1), new DescontoIdosoStrategy()));

        assertEquals(2, dao.listarTodos().size());
    }

    @Test
    void deveBuscarMatriculaAtivaDoAluno() throws Exception {
        Matricula antiga = new Matricula(aluno, plano, INICIO.minusYears(1), new CalculoPadraoStrategy());
        antiga.cancelar();
        dao.salvar(antiga);
        Matricula atual = new Matricula(aluno, plano, INICIO, new CalculoPadraoStrategy());
        dao.salvar(atual);

        Optional<Matricula> ativa = dao.buscarAtivaPorAluno(aluno.getId());

        assertTrue(ativa.isPresent());
        assertEquals(atual.getId(), ativa.get().getId());
    }

    @Test
    void deveListarPorStatus() throws Exception {
        Matricula ativa = new Matricula(aluno, plano, INICIO, new CalculoPadraoStrategy());
        Matricula cancelada = new Matricula(aluno, plano, INICIO.minusYears(1), new CalculoPadraoStrategy());
        cancelada.cancelar();
        dao.salvar(ativa);
        dao.salvar(cancelada);

        assertEquals(1, dao.listarPorStatus(StatusMatricula.ATIVA).size());
        assertEquals(cancelada.getId(), dao.listarPorStatus(StatusMatricula.CANCELADA).get(0).getId());
        assertTrue(dao.listarPorStatus(StatusMatricula.VENCIDA).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> dao.listarPorStatus(null));
    }

    @Test
    void deveAtualizarStatusDatasECalculo() throws Exception {
        Matricula matricula = new Matricula(aluno, plano, INICIO, new CalculoPadraoStrategy());
        dao.salvar(matricula);

        matricula.verificarVencimento(LocalDate.of(2027, 6, 1));
        matricula.alterarCalculo(new DescontoIdosoStrategy());
        dao.atualizar(matricula);
        assertEquals(StatusMatricula.VENCIDA, dao.buscarPorId(matricula.getId()).orElseThrow().getStatus());

        matricula.renovar(LocalDate.of(2027, 6, 1));
        dao.atualizar(matricula);
        Matricula lida = dao.buscarPorId(matricula.getId()).orElseThrow();

        assertEquals(StatusMatricula.ATIVA, lida.getStatus());
        assertEquals(LocalDate.of(2027, 6, 1), lida.getDataInicio());
        assertEquals(LocalDate.of(2027, 9, 1), lida.getDataFim());
        assertInstanceOf(DescontoIdosoStrategy.class, lida.getCalculoMensalidade());
    }

    @Test
    void deveFalharAoAtualizarMatriculaInexistente() throws Exception {
        Matricula naoSalva = new Matricula(aluno, plano, INICIO, new CalculoPadraoStrategy());
        assertThrows(IllegalArgumentException.class, () -> dao.atualizar(naoSalva));

        Matricula inexistente = new Matricula(aluno, plano, INICIO, new CalculoPadraoStrategy());
        inexistente.atribuirId(999);
        assertThrows(IllegalArgumentException.class, () -> dao.atualizar(inexistente));
    }

    @Test
    void naoDeveSalvarAMesmaMatriculaDuasVezes() throws Exception {
        Matricula matricula = new Matricula(aluno, plano, INICIO, new CalculoPadraoStrategy());
        dao.salvar(matricula);

        assertThrows(IllegalArgumentException.class, () -> dao.salvar(matricula));
        assertEquals(1, dao.listarTodos().size());
    }

    @Test
    void deveRemoverPorId() throws Exception {
        Matricula matricula = new Matricula(aluno, plano, INICIO, new CalculoPadraoStrategy());
        dao.salvar(matricula);

        dao.remover(matricula.getId());

        assertFalse(dao.buscarPorId(matricula.getId()).isPresent());
        dao.remover(999); // id inexistente não deve falhar
    }

    @Test
    void naoDeveSalvarMatriculaDeAlunoOuPlanoNaoPersistidos() throws Exception {
        Plano planoNaoSalvo = new Plano(TipoPlano.ANUAL, "Anual", 12, 70.0);

        assertThrows(IllegalStateException.class,
                () -> dao.salvar(new Matricula(aluno, planoNaoSalvo, INICIO, new CalculoPadraoStrategy())));
    }

    @Test
    void deveFalharClaramenteQuandoAlunoDaMatriculaNaoExisteMais() throws Exception {
        Matricula matricula = new Matricula(aluno, plano, INICIO, new CalculoPadraoStrategy());
        dao.salvar(matricula);
        MatriculaDAOImpl semAluno = new MatriculaDAOImpl(factory, new AlunoDAOEmMemoria(), planoDAO);

        assertThrows(IllegalStateException.class, () -> semAluno.buscarPorId(matricula.getId()));
    }

    @Test
    void deveRejeitarMatriculaNula() {
        assertThrows(IllegalArgumentException.class, () -> dao.salvar(null));
        assertThrows(IllegalArgumentException.class, () -> dao.atualizar(null));
    }

    @Test
    void removerAlunoDeveApagarMatriculasEmCascata() throws Exception {
        dao.salvar(new Matricula(aluno, plano, INICIO, new CalculoPadraoStrategy()));

        try (Connection conn = factory.getConexao(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM alunos WHERE id = " + aluno.getId());
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM matriculas")) {
                assertTrue(rs.next());
                assertEquals(0, rs.getInt(1));
            }
        }
    }

    // Insere o aluno direto no banco: a T2.2 (AlunoDAOImpl) é independente desta tarefa.
    private Aluno inserirAluno(int id, String nome, String cpf) throws Exception {
        try (Connection conn = factory.getConexao(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("INSERT INTO alunos (id, nome, cpf, telefone, data_nascimento, objetivo) VALUES ("
                    + id + ", '" + nome + "', '" + cpf + "', '82999990000', '2000-01-10', 'HIPERTROFIA')");
        }
        Aluno novo = new Aluno(nome, cpf, "82999990000", LocalDate.of(2000, 1, 10), ObjetivoTreino.HIPERTROFIA);
        novo.atribuirId(id);
        return novo;
    }

    // AlunoDAO mínimo em memória, usado só para resolver o aluno das matrículas.
    private static class AlunoDAOEmMemoria implements AlunoDAO {
        private final List<Aluno> alunos;

        AlunoDAOEmMemoria(Aluno... alunos) {
            this.alunos = List.of(alunos);
        }

        @Override public void salvar(Aluno aluno) { throw new UnsupportedOperationException(); }
        @Override public void atualizar(Aluno aluno) { throw new UnsupportedOperationException(); }
        @Override public void remover(int id) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<Aluno> buscarPorId(int id) {
            return alunos.stream().filter(a -> a.getId() == id).findFirst();
        }

        @Override public List<Aluno> listarTodos() { return alunos; }

        @Override
        public Optional<Aluno> buscarPorCpf(String cpf) {
            return alunos.stream().filter(a -> a.getCpf().equals(cpf)).findFirst();
        }
    }
}
