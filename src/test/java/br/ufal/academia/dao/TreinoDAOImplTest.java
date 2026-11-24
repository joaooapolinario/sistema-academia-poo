package br.ufal.academia.dao;

import br.ufal.academia.model.entities.Aluno;
import br.ufal.academia.model.entities.Instrutor;
import br.ufal.academia.model.entities.Treino;
import br.ufal.academia.model.enums.ObjetivoTreino;
import br.ufal.academia.model.patterns.factory.TreinoFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreinoDAOImplTest {

    private ConnectionFactory factory;
    private AlunoDAOImpl alunoDAO;
    private InstrutorDAOImpl instrutorDAO;
    private TreinoDAOImpl dao;
    private Aluno maria;
    private Instrutor carlos;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        factory = ConnectionFactory.criarParaTestes("jdbc:sqlite:" + tempDir.resolve("treinos.db").toAbsolutePath());
        alunoDAO = new AlunoDAOImpl(factory);
        instrutorDAO = new InstrutorDAOImpl(factory);
        dao = new TreinoDAOImpl(factory, alunoDAO, instrutorDAO);

        maria = new Aluno("Maria Souza", "52998224725", "82999990000", LocalDate.of(2000, 1, 10),
                ObjetivoTreino.HIPERTROFIA);
        alunoDAO.salvar(maria);
        carlos = new Instrutor("Carlos Lima", "11144477735", "82988881111", "012345-G/AL", "Musculação");
        instrutorDAO.salvar(carlos);
    }

    @Test
    void deveSalvarEAtribuirIdGerado() throws Exception {
        Treino treino = new Treino(maria, carlos, ObjetivoTreino.HIPERTROFIA);

        dao.salvar(treino);

        assertTrue(treino.getId() > 0);
    }

    @Test
    void deveBuscarPorIdPreservandoCamposEExerciciosNaOrdem() throws Exception {
        Treino treino = new Treino(maria, carlos, ObjetivoTreino.HIPERTROFIA);
        treino.adicionarExercicio("Supino reto 4x10");
        treino.adicionarExercicio("Agachamento 4x10");
        dao.salvar(treino);

        Treino encontrado = dao.buscarPorId(treino.getId()).orElseThrow();

        assertEquals(treino.getId(), encontrado.getId());
        assertEquals(maria.getId(), encontrado.getAluno().getId());
        assertEquals(carlos.getId(), encontrado.getInstrutor().getId());
        assertEquals(ObjetivoTreino.HIPERTROFIA, encontrado.getObjetivo());
        assertEquals(LocalDate.now(), encontrado.getDataCriacao());
        assertEquals(List.of("Supino reto 4x10", "Agachamento 4x10"), encontrado.getExercicios());
    }

    @Test
    void deveSalvarTreinoCriadoPelaFactory() throws Exception {
        Treino treino = new TreinoFactory().criar(maria, carlos);
        dao.salvar(treino);

        Treino encontrado = dao.buscarPorId(treino.getId()).orElseThrow();

        assertEquals(treino.getExercicios(), encontrado.getExercicios());
        assertFalse(encontrado.getExercicios().isEmpty());
    }

    @Test
    void deveRetornarVazioQuandoIdNaoExiste() {
        assertFalse(dao.buscarPorId(999).isPresent());
    }

    @Test
    void deveListarTodosEPorAluno() throws Exception {
        Aluno pedro = new Aluno("Pedro Lima", "39053344705", "82977776666", LocalDate.of(1995, 5, 20),
                ObjetivoTreino.EMAGRECIMENTO);
        alunoDAO.salvar(pedro);
        assertTrue(dao.listarTodos().isEmpty());

        dao.salvar(new Treino(maria, carlos, ObjetivoTreino.HIPERTROFIA));
        dao.salvar(new Treino(maria, carlos, ObjetivoTreino.CONDICIONAMENTO));
        dao.salvar(new Treino(pedro, carlos, ObjetivoTreino.EMAGRECIMENTO));

        assertEquals(3, dao.listarTodos().size());
        assertEquals(2, dao.listarPorAluno(maria.getId()).size());
        assertEquals(1, dao.listarPorAluno(pedro.getId()).size());
        assertTrue(dao.listarPorAluno(999).isEmpty());
    }

    @Test
    void deveAtualizarExercicios() throws Exception {
        Treino treino = new Treino(maria, carlos, ObjetivoTreino.HIPERTROFIA);
        treino.adicionarExercicio("Supino reto 4x10");
        dao.salvar(treino);

        treino.removerExercicio("Supino reto 4x10");
        treino.adicionarExercicio("Leg press 4x12");
        treino.adicionarExercicio("Remada baixa 3x12");
        dao.atualizar(treino);

        Treino atualizado = dao.buscarPorId(treino.getId()).orElseThrow();
        assertEquals(List.of("Leg press 4x12", "Remada baixa 3x12"), atualizado.getExercicios());
    }

    @Test
    void naoDeveAtualizarTreinoNaoSalvo() throws Exception {
        Treino naoSalvo = new Treino(maria, carlos, ObjetivoTreino.HIPERTROFIA);

        assertThrows(IllegalArgumentException.class, () -> dao.atualizar(naoSalvo));
    }

    @Test
    void naoDeveSalvarOMesmoTreinoDuasVezes() throws Exception {
        Treino treino = new Treino(maria, carlos, ObjetivoTreino.HIPERTROFIA);
        dao.salvar(treino);

        assertThrows(IllegalArgumentException.class, () -> dao.salvar(treino));
    }

    @Test
    void naoDeveSalvarTreinoDeAlunoNaoSalvoNemAtribuirId() throws Exception {
        Aluno naoSalvo = new Aluno("Ana Costa", "39053344705", "82977776666", LocalDate.of(1995, 5, 20),
                ObjetivoTreino.EMAGRECIMENTO);
        Treino treino = new Treino(naoSalvo, carlos, ObjetivoTreino.EMAGRECIMENTO);
        treino.adicionarExercicio("Esteira 30 min");

        assertThrows(IllegalStateException.class, () -> dao.salvar(treino));
        assertEquals(0, treino.getId());
        assertEquals(0, contarLinhas("treino_exercicios"));
    }

    @Test
    void deveRemoverTreinoESeusExerciciosEmCascata() throws Exception {
        Treino treino = new Treino(maria, carlos, ObjetivoTreino.HIPERTROFIA);
        treino.adicionarExercicio("Supino reto 4x10");
        dao.salvar(treino);

        dao.remover(treino.getId());

        assertFalse(dao.buscarPorId(treino.getId()).isPresent());
        assertEquals(0, contarLinhas("treino_exercicios"));
    }

    @Test
    void removerAlunoDeveApagarSeusTreinos() throws Exception {
        dao.salvar(new Treino(maria, carlos, ObjetivoTreino.HIPERTROFIA));

        alunoDAO.remover(maria.getId());

        assertTrue(dao.listarTodos().isEmpty());
    }

    @Test
    void naoDeveRemoverInstrutorComTreinos() throws Exception {
        dao.salvar(new Treino(maria, carlos, ObjetivoTreino.HIPERTROFIA));

        assertThrows(IllegalStateException.class, () -> instrutorDAO.remover(carlos.getId()));
        assertEquals(1, dao.listarTodos().size());
    }

    @Test
    void deveRejeitarArgumentosNulos() {
        assertThrows(IllegalArgumentException.class, () -> dao.salvar(null));
        assertThrows(IllegalArgumentException.class, () -> dao.atualizar(null));
        assertThrows(IllegalArgumentException.class, () -> new TreinoDAOImpl(null, alunoDAO, instrutorDAO));
        assertThrows(IllegalArgumentException.class, () -> new TreinoDAOImpl(factory, null, instrutorDAO));
        assertThrows(IllegalArgumentException.class, () -> new TreinoDAOImpl(factory, alunoDAO, null));
    }

    private int contarLinhas(String tabela) throws Exception {
        try (Connection conn = factory.getConexao();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tabela)) {
            assertTrue(rs.next());
            return rs.getInt(1);
        }
    }
}