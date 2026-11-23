package br.ufal.academia.dao;

import br.ufal.academia.model.entities.Aluno;
import br.ufal.academia.model.enums.ObjetivoTreino;
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

class AlunoDAOImplTest {
    private ConnectionFactory factory;
    private AlunoDAOImpl dao;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        factory = ConnectionFactory.criarParaTestes("jdbc:sqlite:" + tempDir.resolve("alunos.db").toAbsolutePath());
        dao = new AlunoDAOImpl(factory);
    }

    @Test
    void deveSalvarEAtribuirIdGerado() throws Exception {
        Aluno aluno = novoAluno("Maria Souza", "52998224725");

        dao.salvar(aluno);

        assertTrue(aluno.getId() > 0);
    }

    @Test
    void deveBuscarPorIdPreservandoTodosOsCamposERestricoes() throws Exception {
        Aluno aluno = novoAluno("Maria Souza", "52998224725");
        aluno.adicionarRestricao("Asma");
        aluno.adicionarRestricao("Lesão no joelho");
        dao.salvar(aluno);

        Aluno encontrado = dao.buscarPorId(aluno.getId()).orElseThrow();

        assertEquals(aluno.getId(), encontrado.getId());
        assertEquals("Maria Souza", encontrado.getNome());
        assertEquals("52998224725", encontrado.getCpf());
        assertEquals("82999990000", encontrado.getTelefone());
        assertEquals(LocalDate.of(2000, 1, 10), encontrado.getDataNascimento());
        assertEquals(ObjetivoTreino.HIPERTROFIA, encontrado.getObjetivo());
        assertEquals(List.of("Asma", "Lesão no joelho"), encontrado.getRestricoesMedicas());
    }

    @Test
    void deveBuscarPorCpfComOuSemPontuacao() throws Exception {
        dao.salvar(novoAluno("Maria Souza", "52998224725"));

        assertEquals("Maria Souza", dao.buscarPorCpf("529.982.247-25").orElseThrow().getNome());
        assertEquals("Maria Souza", dao.buscarPorCpf("52998224725").orElseThrow().getNome());
        assertFalse(dao.buscarPorCpf("11144477735").isPresent());
        assertThrows(IllegalArgumentException.class, () -> dao.buscarPorCpf(null));
    }

    @Test
    void deveRetornarVazioQuandoIdNaoExiste() {
        assertFalse(dao.buscarPorId(999).isPresent());
    }

    @Test
    void deveListarOrdenadosPorNome() throws Exception {
        assertTrue(dao.listarTodos().isEmpty());
        dao.salvar(novoAluno("Pedro Lima", "11144477735"));
        dao.salvar(novoAluno("Ana Costa", "52998224725"));

        List<Aluno> todos = dao.listarTodos();

        assertEquals(2, todos.size());
        assertEquals("Ana Costa", todos.get(0).getNome());
        assertEquals("Pedro Lima", todos.get(1).getNome());
    }

    @Test
    void deveAtualizarDadosERestricoes() throws Exception {
        Aluno aluno = novoAluno("Maria Souza", "52998224725");
        aluno.adicionarRestricao("Asma");
        dao.salvar(aluno);

        aluno.atualizarTelefone("(82) 98888-7777");
        aluno.alterarObjetivo(ObjetivoTreino.EMAGRECIMENTO);
        aluno.removerRestricao("Asma");
        aluno.adicionarRestricao("Hipertensão");
        dao.atualizar(aluno);

        Aluno atualizado = dao.buscarPorId(aluno.getId()).orElseThrow();
        assertEquals("82988887777", atualizado.getTelefone());
        assertEquals(ObjetivoTreino.EMAGRECIMENTO, atualizado.getObjetivo());
        assertEquals(List.of("Hipertensão"), atualizado.getRestricoesMedicas());
    }

    @Test
    void naoDeveAtualizarAlunoNaoSalvoOuInexistente() throws Exception {
        Aluno naoSalvo = novoAluno("Maria Souza", "52998224725");
        assertThrows(IllegalArgumentException.class, () -> dao.atualizar(naoSalvo));

        Aluno inexistente = novoAluno("Pedro Lima", "11144477735");
        inexistente.atribuirId(999);
        assertThrows(IllegalArgumentException.class, () -> dao.atualizar(inexistente));
    }

    @Test
    void naoDeveSalvarOMesmoAlunoDuasVezes() throws Exception {
        Aluno aluno = novoAluno("Maria Souza", "52998224725");
        dao.salvar(aluno);

        assertThrows(IllegalArgumentException.class, () -> dao.salvar(aluno));
    }

    @Test
    void naoDeveSalvarCpfDuplicadoNemAtribuirId() throws Exception {
        dao.salvar(novoAluno("Maria Souza", "52998224725"));
        Aluno duplicado = novoAluno("Outra Maria", "529.982.247-25");

        assertThrows(IllegalStateException.class, () -> dao.salvar(duplicado));
        assertEquals(0, duplicado.getId());
        assertEquals(1, dao.listarTodos().size());
    }

    @Test
    void deveRemoverAlunoESuasRestricoesEmCascata() throws Exception {
        Aluno aluno = novoAluno("Maria Souza", "52998224725");
        aluno.adicionarRestricao("Asma");
        dao.salvar(aluno);

        dao.remover(aluno.getId());

        assertFalse(dao.buscarPorId(aluno.getId()).isPresent());
        assertEquals(0, contarRestricoes());
    }

    @Test
    void deveRejeitarArgumentosNulos() {
        assertThrows(IllegalArgumentException.class, () -> dao.salvar(null));
        assertThrows(IllegalArgumentException.class, () -> dao.atualizar(null));
        assertThrows(IllegalArgumentException.class, () -> new AlunoDAOImpl(null));
    }

    private Aluno novoAluno(String nome, String cpf) throws Exception {
        return new Aluno(nome, cpf, "82999990000", LocalDate.of(2000, 1, 10), ObjetivoTreino.HIPERTROFIA);
    }

    private int contarRestricoes() throws Exception {
        try (Connection conn = factory.getConexao();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM restricoes_medicas")) {
            assertTrue(rs.next());
            return rs.getInt(1);
        }
    }
}