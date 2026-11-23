package br.ufal.academia.dao;

import br.ufal.academia.model.entities.Instrutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstrutorDAOImplTest {
    private InstrutorDAOImpl dao;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        ConnectionFactory factory =
                ConnectionFactory.criarParaTestes("jdbc:sqlite:" + tempDir.resolve("instrutores.db").toAbsolutePath());
        dao = new InstrutorDAOImpl(factory);
    }

    @Test
    void deveSalvarEAtribuirIdGerado() throws Exception {
        Instrutor instrutor = novoInstrutor("Carlos Lima", "52998224725", "012345-G/AL");

        dao.salvar(instrutor);

        assertTrue(instrutor.getId() > 0);
    }

    @Test
    void deveBuscarPorIdPreservandoTodosOsCampos() throws Exception {
        Instrutor instrutor = novoInstrutor("Carlos Lima", "52998224725", "012345-G/AL");
        dao.salvar(instrutor);

        Instrutor encontrado = dao.buscarPorId(instrutor.getId()).orElseThrow();

        assertEquals(instrutor.getId(), encontrado.getId());
        assertEquals("Carlos Lima", encontrado.getNome());
        assertEquals("52998224725", encontrado.getCpf());
        assertEquals("82988881111", encontrado.getTelefone());
        assertEquals("012345-G/AL", encontrado.getCref());
        assertEquals("Musculação", encontrado.getEspecialidade());
    }

    @Test
    void deveBuscarPorCrefIgnorandoMaiusculasEEspacos() throws Exception {
        dao.salvar(novoInstrutor("Carlos Lima", "52998224725", "012345-G/AL"));

        assertEquals("Carlos Lima", dao.buscarPorCref(" 012345-g/al ").orElseThrow().getNome());
        assertFalse(dao.buscarPorCref("999999-P/SP").isPresent());
        assertThrows(IllegalArgumentException.class, () -> dao.buscarPorCref(null));
    }

    @Test
    void deveRetornarVazioQuandoIdNaoExiste() {
        assertFalse(dao.buscarPorId(999).isPresent());
    }

    @Test
    void deveListarOrdenadosPorNome() throws Exception {
        assertTrue(dao.listarTodos().isEmpty());
        dao.salvar(novoInstrutor("Pedro Lima", "11144477735", "000111-P/PE"));
        dao.salvar(novoInstrutor("Ana Costa", "52998224725", "012345-G/AL"));

        List<Instrutor> todos = dao.listarTodos();

        assertEquals(2, todos.size());
        assertEquals("Ana Costa", todos.get(0).getNome());
        assertEquals("Pedro Lima", todos.get(1).getNome());
    }

    @Test
    void deveAtualizarTelefoneEEspecialidade() throws Exception {
        Instrutor instrutor = novoInstrutor("Carlos Lima", "52998224725", "012345-G/AL");
        dao.salvar(instrutor);

        instrutor.atualizarTelefone("82977776666");
        instrutor.alterarEspecialidade("Pilates");
        dao.atualizar(instrutor);

        Instrutor atualizado = dao.buscarPorId(instrutor.getId()).orElseThrow();
        assertEquals("82977776666", atualizado.getTelefone());
        assertEquals("Pilates", atualizado.getEspecialidade());
    }

    @Test
    void naoDeveAtualizarInstrutorNaoSalvoOuInexistente() throws Exception {
        Instrutor naoSalvo = novoInstrutor("Carlos Lima", "52998224725", "012345-G/AL");
        assertThrows(IllegalArgumentException.class, () -> dao.atualizar(naoSalvo));

        Instrutor inexistente = novoInstrutor("Pedro Lima", "11144477735", "000111-P/PE");
        inexistente.atribuirId(999);
        assertThrows(IllegalArgumentException.class, () -> dao.atualizar(inexistente));
    }

    @Test
    void naoDeveSalvarCpfOuCrefDuplicado() throws Exception {
        dao.salvar(novoInstrutor("Carlos Lima", "52998224725", "012345-G/AL"));

        Instrutor mesmoCref = novoInstrutor("Outro", "11144477735", "012345-G/AL");
        Instrutor mesmoCpf = novoInstrutor("Outro", "52998224725", "000111-P/PE");

        assertThrows(IllegalStateException.class, () -> dao.salvar(mesmoCref));
        assertThrows(IllegalStateException.class, () -> dao.salvar(mesmoCpf));
        assertEquals(1, dao.listarTodos().size());
    }

    @Test
    void deveRemoverInstrutor() throws Exception {
        Instrutor instrutor = novoInstrutor("Carlos Lima", "52998224725", "012345-G/AL");
        dao.salvar(instrutor);

        dao.remover(instrutor.getId());

        assertFalse(dao.buscarPorId(instrutor.getId()).isPresent());
    }

    @Test
    void deveRejeitarArgumentosNulos() {
        assertThrows(IllegalArgumentException.class, () -> dao.salvar(null));
        assertThrows(IllegalArgumentException.class, () -> dao.atualizar(null));
        assertThrows(IllegalArgumentException.class, () -> new InstrutorDAOImpl(null));
    }

    private Instrutor novoInstrutor(String nome, String cpf, String cref) throws Exception {
        return new Instrutor(nome, cpf, "82988881111", cref, "Musculação");
    }
}