package br.ufal.academia.controller;

import br.ufal.academia.dao.AlunoDAO;
import br.ufal.academia.dao.InstrutorDAO;
import br.ufal.academia.dao.TreinoDAO;
import br.ufal.academia.exception.DadosInvalidosException;
import br.ufal.academia.model.entities.Aluno;
import br.ufal.academia.model.entities.Instrutor;
import br.ufal.academia.model.entities.Treino;
import br.ufal.academia.model.enums.ObjetivoTreino;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreinoControllerTest {

    private AlunoDAOEmMemoria alunoDAO;
    private InstrutorDAOEmMemoria instrutorDAO;
    private TreinoDAOEmMemoria treinoDAO;
    private ITreinoController controller;

    private Aluno maria;
    private Aluno pedro;
    private Instrutor carlos;

    @BeforeEach
    void setUp() throws Exception {
        alunoDAO = new AlunoDAOEmMemoria();
        instrutorDAO = new InstrutorDAOEmMemoria();
        treinoDAO = new TreinoDAOEmMemoria();
        controller = new TreinoController(treinoDAO, alunoDAO, instrutorDAO);

        maria = new Aluno("Maria Souza", "52998224725", "82999990000",
                LocalDate.of(2000, 1, 10), ObjetivoTreino.HIPERTROFIA);
        pedro = new Aluno("Pedro Lima", "11144477735", "82988887777",
                LocalDate.of(1995, 3, 20), ObjetivoTreino.EMAGRECIMENTO);
        carlos = new Instrutor("Carlos Lima", "39053344705", "82977776666",
                "012345-G/AL", "Musculação");

        alunoDAO.salvar(maria);
        alunoDAO.salvar(pedro);
        instrutorDAO.salvar(carlos);
    }

    @Test
    void deveCriarTreinoViaFactoryComExerciciosPadraoENormalizacao() throws Exception {
        Treino treino = controller.criar("529.982.247-25", " 012345-g/al ", ObjetivoTreino.HIPERTROFIA);

        assertTrue(treino.getId() > 0);
        assertEquals(maria.getId(), treino.getAluno().getId());
        assertEquals(carlos.getId(), treino.getInstrutor().getId());
        assertEquals(ObjetivoTreino.HIPERTROFIA, treino.getObjetivo());
        assertFalse(treino.getExercicios().isEmpty());
        assertTrue(treino.getExercicios().contains("Supino reto - 4x8 a 12"));
        assertEquals(1, treinoDAO.listarTodos().size());
    }

    @Test
    void deveCriarFichasPadraoParaTodosOsObjetivos() throws Exception {
        for (ObjetivoTreino objetivo : ObjetivoTreino.values()) {
            Treino treino = controller.criar(maria.getCpf(), carlos.getCref(), objetivo);

            assertEquals(objetivo, treino.getObjetivo());
            assertFalse(treino.getExercicios().isEmpty());
        }
        assertEquals(ObjetivoTreino.values().length, treinoDAO.listarPorAluno(maria.getId()).size());
    }

    @Test
    void deveAdicionarERemoverExerciciosEPersistir() throws Exception {
        Treino treino = controller.criar(maria.getCpf(), carlos.getCref(), ObjetivoTreino.REABILITACAO);
        int quantidadeInicial = treino.getExercicios().size();

        controller.adicionarExercicio(treino.getId(), "  Ponte glútea - 3x15 ");

        assertEquals(quantidadeInicial + 1, treino.getExercicios().size());
        assertTrue(treino.getExercicios().contains("Ponte glútea - 3x15"));
        assertEquals(1, treinoDAO.atualizacoes);

        controller.removerExercicio(treino.getId(), "Ponte glútea - 3x15");

        assertEquals(quantidadeInicial, treino.getExercicios().size());
        assertFalse(treino.getExercicios().contains("Ponte glútea - 3x15"));
        assertEquals(2, treinoDAO.atualizacoes);

        assertThrows(DadosInvalidosException.class, () -> controller.adicionarExercicio(treino.getId(), "  "));
        assertThrows(DadosInvalidosException.class,
                () -> controller.adicionarExercicio(treino.getId(), "Mobilidade articular - 10 min"));
        assertThrows(DadosInvalidosException.class,
                () -> controller.removerExercicio(treino.getId(), "Exercício inexistente"));
        assertEquals(2, treinoDAO.atualizacoes);
    }

    @Test
    void deveListarTreinosPorAlunoEmListaImutavel() throws Exception {
        assertTrue(controller.listarPorAluno(maria.getCpf()).isEmpty());

        Treino treinoMaria1 = controller.criar(maria.getCpf(), carlos.getCref(), ObjetivoTreino.HIPERTROFIA);
        Treino treinoMaria2 = controller.criar(maria.getCpf(), carlos.getCref(), ObjetivoTreino.CONDICIONAMENTO);
        controller.criar(pedro.getCpf(), carlos.getCref(), ObjetivoTreino.EMAGRECIMENTO);

        List<Treino> treinosMaria = controller.listarPorAluno("529.982.247-25");

        assertEquals(List.of(treinoMaria1, treinoMaria2), treinosMaria);
        assertEquals(1, controller.listarPorAluno(pedro.getCpf()).size());
        assertThrows(UnsupportedOperationException.class, () -> treinosMaria.add(treinoMaria1));
    }

    @Test
    void deveRemoverTreinoExistenteERejeitarInexistente() throws Exception {
        Treino treino = controller.criar(maria.getCpf(), carlos.getCref(), ObjetivoTreino.HIPERTROFIA);

        controller.remover(treino.getId());

        assertTrue(controller.listarPorAluno(maria.getCpf()).isEmpty());
        assertThrows(DadosInvalidosException.class, () -> controller.remover(treino.getId()));
    }

    @Test
    void deveRejeitarParametrosInvalidosEConstrutoresNulos() {
        assertThrows(DadosInvalidosException.class,
                () -> controller.criar(null, carlos.getCref(), ObjetivoTreino.HIPERTROFIA));
        assertThrows(DadosInvalidosException.class,
                () -> controller.criar("   ", carlos.getCref(), ObjetivoTreino.HIPERTROFIA));
        assertThrows(DadosInvalidosException.class,
                () -> controller.criar("---", carlos.getCref(), ObjetivoTreino.HIPERTROFIA));
        assertThrows(DadosInvalidosException.class,
                () -> controller.criar(maria.getCpf(), null, ObjetivoTreino.HIPERTROFIA));
        assertThrows(DadosInvalidosException.class,
                () -> controller.criar(maria.getCpf(), "  ", ObjetivoTreino.HIPERTROFIA));
        assertThrows(DadosInvalidosException.class,
                () -> controller.criar(maria.getCpf(), carlos.getCref(), null));
        assertThrows(DadosInvalidosException.class,
                () -> controller.criar("98765432100", carlos.getCref(), ObjetivoTreino.HIPERTROFIA));
        assertThrows(DadosInvalidosException.class,
                () -> controller.criar(maria.getCpf(), "999999-P/SP", ObjetivoTreino.HIPERTROFIA));

        assertThrows(DadosInvalidosException.class, () -> controller.adicionarExercicio(0, "Supino"));
        assertThrows(DadosInvalidosException.class, () -> controller.adicionarExercicio(999, "Supino"));
        assertThrows(DadosInvalidosException.class, () -> controller.removerExercicio(-1, "Supino"));
        assertThrows(DadosInvalidosException.class, () -> controller.removerExercicio(999, "Supino"));
        assertThrows(DadosInvalidosException.class, () -> controller.listarPorAluno("98765432100"));
        assertThrows(DadosInvalidosException.class, () -> controller.listarPorAluno(null));
        assertThrows(DadosInvalidosException.class, () -> controller.remover(0));
        assertThrows(DadosInvalidosException.class, () -> controller.remover(999));

        assertThrows(IllegalArgumentException.class, () -> new TreinoController(null, alunoDAO, instrutorDAO));
        assertThrows(IllegalArgumentException.class, () -> new TreinoController(treinoDAO, null, instrutorDAO));
        assertThrows(IllegalArgumentException.class, () -> new TreinoController(treinoDAO, alunoDAO, null));
        assertThrows(IllegalArgumentException.class,
                () -> new TreinoController(treinoDAO, alunoDAO, instrutorDAO, null));
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

    private static class InstrutorDAOEmMemoria implements InstrutorDAO {
        private final List<Instrutor> instrutores = new ArrayList<>();
        private int proximoId = 1;

        @Override
        public void salvar(Instrutor instrutor) {
            instrutor.atribuirId(proximoId++);
            instrutores.add(instrutor);
        }

        @Override public void atualizar(Instrutor instrutor) { }
        @Override public void remover(int id) { instrutores.removeIf(i -> i.getId() == id); }

        @Override
        public Optional<Instrutor> buscarPorId(int id) {
            return instrutores.stream().filter(i -> i.getId() == id).findFirst();
        }

        @Override public List<Instrutor> listarTodos() { return List.copyOf(instrutores); }

        @Override
        public Optional<Instrutor> buscarPorCref(String cref) {
            return instrutores.stream().filter(i -> i.getCref().equals(cref)).findFirst();
        }
    }

    private static class TreinoDAOEmMemoria implements TreinoDAO {
        private final List<Treino> treinos = new ArrayList<>();
        private int proximoId = 1;
        private int atualizacoes;

        @Override
        public void salvar(Treino treino) {
            treino.atribuirId(proximoId++);
            treinos.add(treino);
        }

        @Override
        public void atualizar(Treino treino) {
            atualizacoes++;
        }

        @Override public void remover(int id) { treinos.removeIf(t -> t.getId() == id); }

        @Override
        public Optional<Treino> buscarPorId(int id) {
            return treinos.stream().filter(t -> t.getId() == id).findFirst();
        }

        @Override public List<Treino> listarTodos() { return List.copyOf(treinos); }

        @Override
        public List<Treino> listarPorAluno(int idAluno) {
            return treinos.stream().filter(t -> t.getAluno().getId() == idAluno).toList();
        }
    }
}
