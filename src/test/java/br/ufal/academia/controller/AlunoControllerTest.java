package br.ufal.academia.controller;

import br.ufal.academia.dao.AlunoDAO;
import br.ufal.academia.exception.CpfInvalidoException;
import br.ufal.academia.exception.DadosInvalidosException;
import br.ufal.academia.model.entities.Aluno;
import br.ufal.academia.model.enums.ObjetivoTreino;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlunoControllerTest {

    private AlunoDAOEmMemoria alunoDAO;
    private IAlunoController controller;

    @BeforeEach
    void setUp() {
        alunoDAO = new AlunoDAOEmMemoria();
        controller = new AlunoController(alunoDAO);
    }

    @Test
    void deveCadastrarAlunoValidoComNormalizacaoDeCampos() throws Exception {
        Aluno cadastrado = controller.cadastrar(
                "  Maria Souza ",
                "529.982.247-25",
                "(82) 99999-0000",
                LocalDate.of(2000, 1, 10),
                ObjetivoTreino.HIPERTROFIA);

        assertTrue(cadastrado.getId() > 0);
        assertEquals("Maria Souza", cadastrado.getNome());
        assertEquals("52998224725", cadastrado.getCpf());
        assertEquals("82999990000", cadastrado.getTelefone());
        assertEquals(1, alunoDAO.listarTodos().size());
    }

    @Test
    void deveBloquearCadastroComCpfDuplicado() throws Exception {
        controller.cadastrar("Maria Souza", "52998224725", "82999990000",
                LocalDate.of(2000, 1, 10), ObjetivoTreino.HIPERTROFIA);

        assertThrows(DadosInvalidosException.class, () -> controller.cadastrar(
                "Outra Maria", "529.982.247-25", "82988887777",
                LocalDate.of(1998, 5, 20), ObjetivoTreino.EMAGRECIMENTO));
        assertEquals(1, alunoDAO.listarTodos().size());
    }

    @Test
    void deveRejeitarDadosInvalidosNoCadastro() {
        LocalDate nascimento = LocalDate.of(2000, 1, 10);

        assertThrows(CpfInvalidoException.class, () -> controller.cadastrar(
                "Maria", "111.111.111-11", "82999990000", nascimento, ObjetivoTreino.HIPERTROFIA));
        assertThrows(DadosInvalidosException.class, () -> controller.cadastrar(
                " ", "52998224725", "82999990000", nascimento, ObjetivoTreino.HIPERTROFIA));
        assertThrows(DadosInvalidosException.class, () -> controller.cadastrar(
                "Maria", "52998224725", "123", nascimento, ObjetivoTreino.HIPERTROFIA));
        assertThrows(DadosInvalidosException.class, () -> controller.cadastrar(
                "Maria", "52998224725", "82999990000", LocalDate.now().plusDays(1), ObjetivoTreino.HIPERTROFIA));
        assertThrows(DadosInvalidosException.class, () -> controller.cadastrar(
                "Maria", "52998224725", "82999990000", nascimento, null));
        assertTrue(alunoDAO.listarTodos().isEmpty());
    }

    @Test
    void deveBuscarPorCpfComOuSemPontuacao() throws Exception {
        controller.cadastrar("Maria Souza", "52998224725", "82999990000",
                LocalDate.of(2000, 1, 10), ObjetivoTreino.HIPERTROFIA);

        assertEquals("Maria Souza", controller.buscarPorCpf("529.982.247-25").getNome());
        assertEquals("Maria Souza", controller.buscarPorCpf("52998224725").getNome());

        assertThrows(DadosInvalidosException.class, () -> controller.buscarPorCpf("11144477735"));
        assertThrows(DadosInvalidosException.class, () -> controller.buscarPorCpf(null));
        assertThrows(DadosInvalidosException.class, () -> controller.buscarPorCpf("   "));
        assertThrows(DadosInvalidosException.class, () -> controller.buscarPorCpf("---"));
    }

    @Test
    void deveListarTodosOsAlunosEmListaImutavel() throws Exception {
        assertTrue(controller.listar().isEmpty());
        controller.cadastrar("Maria Souza", "52998224725", "82999990000",
                LocalDate.of(2000, 1, 10), ObjetivoTreino.HIPERTROFIA);
        controller.cadastrar("Pedro Lima", "11144477735", "82988887777",
                LocalDate.of(1995, 3, 20), ObjetivoTreino.CONDICIONAMENTO);

        List<Aluno> alunos = controller.listar();

        assertEquals(2, alunos.size());
        assertThrows(UnsupportedOperationException.class, () -> alunos.add(alunos.get(0)));
    }

    @Test
    void deveAtualizarTelefoneEPersistir() throws Exception {
        controller.cadastrar("Maria Souza", "52998224725", "82999990000",
                LocalDate.of(2000, 1, 10), ObjetivoTreino.HIPERTROFIA);

        controller.atualizarTelefone("529.982.247-25", "(82) 3222-1111");

        assertEquals("8232221111", controller.buscarPorCpf("52998224725").getTelefone());
        assertEquals(1, alunoDAO.atualizacoes);

        assertThrows(DadosInvalidosException.class, () -> controller.atualizarTelefone("52998224725", "999"));
        assertThrows(DadosInvalidosException.class, () -> controller.atualizarTelefone("11144477735", "82988887777"));
        assertEquals(1, alunoDAO.atualizacoes);
    }

    @Test
    void deveAdicionarERemoverRestricoesMedicasEPersistir() throws Exception {
        controller.cadastrar("Maria Souza", "52998224725", "82999990000",
                LocalDate.of(2000, 1, 10), ObjetivoTreino.REABILITACAO);

        controller.adicionarRestricaoMedica("529.982.247-25", "  Lesão no joelho ");
        controller.adicionarRestricaoMedica("52998224725", "Asma");

        Aluno aluno = controller.buscarPorCpf("52998224725");
        assertEquals(List.of("Lesão no joelho", "Asma"), aluno.getRestricoesMedicas());
        assertEquals(2, alunoDAO.atualizacoes);

        assertThrows(DadosInvalidosException.class, () -> controller.adicionarRestricaoMedica("52998224725", "Asma"));
        assertThrows(DadosInvalidosException.class, () -> controller.adicionarRestricaoMedica("52998224725", "  "));
        assertThrows(DadosInvalidosException.class, () -> controller.adicionarRestricaoMedica("11144477735", "Asma"));
        assertEquals(2, alunoDAO.atualizacoes);

        controller.removerRestricaoMedica("529.982.247-25", "Lesão no joelho");

        assertEquals(List.of("Asma"), controller.buscarPorCpf("52998224725").getRestricoesMedicas());
        assertEquals(3, alunoDAO.atualizacoes);

        assertThrows(DadosInvalidosException.class, () -> controller.removerRestricaoMedica("52998224725", "Inexistente"));
        assertThrows(DadosInvalidosException.class, () -> controller.removerRestricaoMedica("11144477735", "Asma"));
        assertEquals(3, alunoDAO.atualizacoes);
    }

    @Test
    void deveRemoverAlunoExistenteERejeitarInexistente() throws Exception {
        controller.cadastrar("Maria Souza", "52998224725", "82999990000",
                LocalDate.of(2000, 1, 10), ObjetivoTreino.HIPERTROFIA);

        controller.remover("529.982.247-25");

        assertTrue(controller.listar().isEmpty());
        assertThrows(DadosInvalidosException.class, () -> controller.remover("52998224725"));
    }

    @Test
    void deveRejeitarDaoNuloNoConstrutor() {
        assertThrows(IllegalArgumentException.class, () -> new AlunoController(null));
    }

    private static class AlunoDAOEmMemoria implements AlunoDAO {
        private final List<Aluno> alunos = new ArrayList<>();
        private int proximoId = 1;
        private int atualizacoes;

        @Override
        public void salvar(Aluno aluno) {
            aluno.atribuirId(proximoId++);
            alunos.add(aluno);
        }

        @Override
        public void atualizar(Aluno aluno) {
            atualizacoes++;
        }

        @Override
        public void remover(int id) {
            alunos.removeIf(a -> a.getId() == id);
        }

        @Override
        public Optional<Aluno> buscarPorId(int id) {
            return alunos.stream().filter(a -> a.getId() == id).findFirst();
        }

        @Override
        public List<Aluno> listarTodos() {
            return List.copyOf(alunos);
        }

        @Override
        public Optional<Aluno> buscarPorCpf(String cpf) {
            return alunos.stream().filter(a -> a.getCpf().equals(cpf)).findFirst();
        }
    }
}
