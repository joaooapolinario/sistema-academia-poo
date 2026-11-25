package br.ufal.academia.controller;

import br.ufal.academia.dao.InstrutorDAO;
import br.ufal.academia.exception.CpfInvalidoException;
import br.ufal.academia.exception.DadosInvalidosException;
import br.ufal.academia.model.entities.Instrutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstrutorControllerTest {

    private InstrutorDAOEmMemoria instrutorDAO;
    private IInstrutorController controller;

    @BeforeEach
    void setUp() {
        instrutorDAO = new InstrutorDAOEmMemoria();
        controller = new InstrutorController(instrutorDAO);
    }

    @Test
    void deveCadastrarInstrutorValidoComNormalizacaoDeCampos() throws Exception {
        Instrutor cadastrado = controller.cadastrar(
                "  Carlos Lima ",
                "529.982.247-25",
                "(82) 98888-1111",
                " 012345-g/al ",
                "  Musculação ");

        assertTrue(cadastrado.getId() > 0);
        assertEquals("Carlos Lima", cadastrado.getNome());
        assertEquals("52998224725", cadastrado.getCpf());
        assertEquals("82988881111", cadastrado.getTelefone());
        assertEquals("012345-G/AL", cadastrado.getCref());
        assertEquals("Musculação", cadastrado.getEspecialidade());
        assertEquals(1, instrutorDAO.listarTodos().size());
    }

    @Test
    void deveBloquearCadastroComCrefDuplicado() throws Exception {
        controller.cadastrar("Carlos Lima", "52998224725", "82988881111", "012345-G/AL", "Musculação");

        assertThrows(DadosInvalidosException.class, () -> controller.cadastrar(
                "Outro Instrutor", "11144477735", "82977776666", " 012345-g/al ", "Pilates"));
        assertEquals(1, instrutorDAO.listarTodos().size());
    }

    @Test
    void deveBloquearCadastroComCpfDuplicado() throws Exception {
        controller.cadastrar("Carlos Lima", "52998224725", "82988881111", "012345-G/AL", "Musculação");

        assertThrows(DadosInvalidosException.class, () -> controller.cadastrar(
                "Outro Instrutor", "529.982.247-25", "82977776666", "654321-P/PE", "Pilates"));
        assertEquals(1, instrutorDAO.listarTodos().size());
    }

    @Test
    void deveRejeitarDadosInvalidosNoCadastro() {
        assertThrows(CpfInvalidoException.class, () -> controller.cadastrar(
                "Carlos", "000.000.000-00", "82988881111", "012345-G/AL", "Musculação"));
        assertThrows(DadosInvalidosException.class, () -> controller.cadastrar(
                "Carlos", "52998224725", "82988881111", "12345-G/AL", "Musculação"));
        assertThrows(DadosInvalidosException.class, () -> controller.cadastrar(
                "Carlos", "52998224725", "82988881111", "012345-G/AL", "   "));
        assertThrows(DadosInvalidosException.class, () -> controller.cadastrar(
                "Carlos", "52998224725", "123", "012345-G/AL", "Musculação"));
        assertTrue(instrutorDAO.listarTodos().isEmpty());
    }

    @Test
    void deveBuscarPorCrefIgnorandoMaiusculasEEspacos() throws Exception {
        controller.cadastrar("Carlos Lima", "52998224725", "82988881111", "012345-G/AL", "Musculação");

        assertEquals("Carlos Lima", controller.buscarPorCref(" 012345-g/al ").getNome());
        assertEquals("Carlos Lima", controller.buscarPorCref("012345-G/AL").getNome());

        assertThrows(DadosInvalidosException.class, () -> controller.buscarPorCref("999999-P/SP"));
        assertThrows(DadosInvalidosException.class, () -> controller.buscarPorCref(null));
        assertThrows(DadosInvalidosException.class, () -> controller.buscarPorCref("   "));
    }

    @Test
    void deveListarTodosOsInstrutoresEmListaImutavel() throws Exception {
        assertTrue(controller.listar().isEmpty());
        controller.cadastrar("Carlos Lima", "52998224725", "82988881111", "012345-G/AL", "Musculação");
        controller.cadastrar("Ana Costa", "11144477735", "82977776666", "654321-P/PE", "Pilates");

        List<Instrutor> instrutores = controller.listar();

        assertEquals(2, instrutores.size());
        assertThrows(UnsupportedOperationException.class, () -> instrutores.add(instrutores.get(0)));
    }

    @Test
    void deveAtualizarEspecialidadeEPersistir() throws Exception {
        controller.cadastrar("Carlos Lima", "52998224725", "82988881111", "012345-G/AL", "Musculação");

        controller.atualizarEspecialidade(" 012345-g/al ", "  Treinamento Funcional ");

        assertEquals("Treinamento Funcional", controller.buscarPorCref("012345-G/AL").getEspecialidade());
        assertEquals(1, instrutorDAO.atualizacoes);

        assertThrows(DadosInvalidosException.class, () -> controller.atualizarEspecialidade("012345-G/AL", "  "));
        assertThrows(DadosInvalidosException.class, () -> controller.atualizarEspecialidade("012345-G/AL", null));
        assertThrows(DadosInvalidosException.class, () -> controller.atualizarEspecialidade("999999-P/SP", "Pilates"));
        assertEquals(1, instrutorDAO.atualizacoes);
    }

    @Test
    void deveRemoverInstrutorExistenteERejeitarInexistente() throws Exception {
        controller.cadastrar("Carlos Lima", "52998224725", "82988881111", "012345-G/AL", "Musculação");

        controller.remover("012345-g/al");

        assertTrue(controller.listar().isEmpty());
        assertThrows(DadosInvalidosException.class, () -> controller.remover("012345-G/AL"));
    }

    @Test
    void deveRejeitarDaoNuloNoConstrutor() {
        assertThrows(IllegalArgumentException.class, () -> new InstrutorController(null));
    }

    private static class InstrutorDAOEmMemoria implements InstrutorDAO {
        private final List<Instrutor> instrutores = new ArrayList<>();
        private int proximoId = 1;
        private int atualizacoes;

        @Override
        public void salvar(Instrutor instrutor) {
            instrutor.atribuirId(proximoId++);
            instrutores.add(instrutor);
        }

        @Override
        public void atualizar(Instrutor instrutor) {
            atualizacoes++;
        }

        @Override
        public void remover(int id) {
            instrutores.removeIf(i -> i.getId() == id);
        }

        @Override
        public Optional<Instrutor> buscarPorId(int id) {
            return instrutores.stream().filter(i -> i.getId() == id).findFirst();
        }

        @Override
        public List<Instrutor> listarTodos() {
            return List.copyOf(instrutores);
        }

        @Override
        public Optional<Instrutor> buscarPorCref(String cref) {
            return instrutores.stream().filter(i -> i.getCref().equals(cref)).findFirst();
        }
    }
}
