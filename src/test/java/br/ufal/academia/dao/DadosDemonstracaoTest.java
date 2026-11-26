package br.ufal.academia.dao;

import br.ufal.academia.DadosDemonstracao;
import br.ufal.academia.Main;
import br.ufal.academia.controller.AlunoController;
import br.ufal.academia.controller.FrequenciaController;
import br.ufal.academia.controller.InstrutorController;
import br.ufal.academia.controller.MatriculaController;
import br.ufal.academia.controller.TreinoController;
import br.ufal.academia.exception.AcessoNegadoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DadosDemonstracaoTest {

    private AlunoController alunoController;
    private InstrutorController instrutorController;
    private MatriculaController matriculaController;
    private TreinoController treinoController;
    private FrequenciaController frequenciaController;
    private DadosDemonstracao dados;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        ConnectionFactory factory =
                ConnectionFactory.criarParaTestes("jdbc:sqlite:" + tempDir.resolve("demo.db").toAbsolutePath());
        AlunoDAOImpl alunoDAO = new AlunoDAOImpl(factory);
        InstrutorDAOImpl instrutorDAO = new InstrutorDAOImpl(factory);
        PlanoDAOImpl planoDAO = new PlanoDAOImpl(factory);
        MatriculaDAOImpl matriculaDAO = new MatriculaDAOImpl(factory, alunoDAO, planoDAO);
        TreinoDAOImpl treinoDAO = new TreinoDAOImpl(factory, alunoDAO, instrutorDAO);
        FrequenciaDAOImpl frequenciaDAO = new FrequenciaDAOImpl(factory, matriculaDAO);
        Main.inicializarPlanosPadrao(planoDAO);

        alunoController = new AlunoController(alunoDAO);
        instrutorController = new InstrutorController(instrutorDAO);
        matriculaController = new MatriculaController(matriculaDAO, alunoDAO, planoDAO, List.of());
        treinoController = new TreinoController(treinoDAO, alunoDAO, instrutorDAO);
        frequenciaController = new FrequenciaController(frequenciaDAO, alunoDAO, matriculaDAO, List.of());
        dados = new DadosDemonstracao(alunoController, instrutorController, matriculaController,
                treinoController, frequenciaController);
    }

    @Test
    void deveCarregarDadosQuandoBancoEstaVazio() throws Exception {
        assertTrue(dados.carregarSeBancoVazio());

        assertEquals(4, alunoController.listar().size());
        assertEquals(2, instrutorController.listar().size());
        assertEquals(List.of("Asma"), alunoController.buscarPorCpf("12345678909").getRestricoesMedicas());
        assertEquals(1, treinoController.listarPorAluno("12345678909").size());
        assertEquals(1, frequenciaController.contarPresencas("12345678909", YearMonth.now()));
    }

    @Test
    void naoDeveDuplicarDadosNaSegundaExecucao() {
        assertTrue(dados.carregarSeBancoVazio());

        assertFalse(dados.carregarSeBancoVazio());
        assertEquals(4, alunoController.listar().size());
    }

    @Test
    void alunoSemMatriculaDeveTerAcessoNegado() {
        dados.carregarSeBancoVazio();

        assertThrows(AcessoNegadoException.class, () -> frequenciaController.registrarEntrada("321.654.987-91"));
    }

    @Test
    void deveRejeitarControllersNulos() {
        assertThrows(IllegalArgumentException.class, () -> new DadosDemonstracao(
                null, instrutorController, matriculaController, treinoController, frequenciaController));
    }
}