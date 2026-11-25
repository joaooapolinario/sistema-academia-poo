package br.ufal.academia.dao;

import br.ufal.academia.Main;
import br.ufal.academia.controller.AlunoController;
import br.ufal.academia.controller.FrequenciaController;
import br.ufal.academia.controller.IAlunoController;
import br.ufal.academia.controller.IFrequenciaController;
import br.ufal.academia.controller.IInstrutorController;
import br.ufal.academia.controller.IMatriculaController;
import br.ufal.academia.controller.ITreinoController;
import br.ufal.academia.controller.InstrutorController;
import br.ufal.academia.controller.MatriculaController;
import br.ufal.academia.controller.TreinoController;
import br.ufal.academia.exception.AcessoNegadoException;
import br.ufal.academia.exception.MatriculaAtivaExistenteException;
import br.ufal.academia.model.entities.Aluno;
import br.ufal.academia.model.entities.Frequencia;
import br.ufal.academia.model.entities.Instrutor;
import br.ufal.academia.model.entities.Matricula;
import br.ufal.academia.model.entities.Treino;
import br.ufal.academia.model.enums.ObjetivoTreino;
import br.ufal.academia.model.enums.StatusMatricula;
import br.ufal.academia.model.enums.TipoPlano;
import br.ufal.academia.model.patterns.observer.AlertaInterfaceObserver;
import br.ufal.academia.model.patterns.observer.LogAuditoriaObserver;
import br.ufal.academia.model.patterns.observer.MatriculaObserver;
import br.ufal.academia.view.AlunoView;
import br.ufal.academia.view.FrequenciaView;
import br.ufal.academia.view.InstrutorView;
import br.ufal.academia.view.MatriculaView;
import br.ufal.academia.view.TreinoView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.SwingUtilities;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Teste ponta a ponta: DAOs SQLite + Seed de Planos + Controllers + Observers + todas as 5 Views.
class IntegracaoSistemaSQLiteTest {

    @Test
    void fluxoCompletoDevePersistirNoSqliteEIntegrarTodasAsCincoViews(@TempDir Path tempDir) throws Exception {
        ConnectionFactory factory = ConnectionFactory.criarParaTestes(
                "jdbc:sqlite:" + tempDir.resolve("academia-teste.db").toAbsolutePath());

        AlunoDAO alunoDAO = new AlunoDAOImpl(factory);
        InstrutorDAO instrutorDAO = new InstrutorDAOImpl(factory);
        PlanoDAO planoDAO = new PlanoDAOImpl(factory);
        MatriculaDAO matriculaDAO = new MatriculaDAOImpl(factory, alunoDAO, planoDAO);
        TreinoDAO treinoDAO = new TreinoDAOImpl(factory, alunoDAO, instrutorDAO);
        FrequenciaDAO frequenciaDAO = new FrequenciaDAOImpl(factory, matriculaDAO);

        // 0. Seed idempotente de planos no SQLite via Main.inicializarPlanosPadrao
        Main.inicializarPlanosPadrao(planoDAO);
        Main.inicializarPlanosPadrao(planoDAO);
        assertEquals(TipoPlano.values().length, planoDAO.listarTodos().size());

        AtomicReference<MatriculaView> matriculaViewRef = new AtomicReference<>();
        LogAuditoriaObserver logObserver = new LogAuditoriaObserver(msg -> { });
        AlertaInterfaceObserver alertaObserver = new AlertaInterfaceObserver(msg -> { });
        MatriculaObserver viewObserver = matricula -> {
            MatriculaView view = matriculaViewRef.get();
            if (view != null) {
                view.atualizar(matricula);
            }
        };
        List<MatriculaObserver> observers = List.of(logObserver, alertaObserver, viewObserver);

        IAlunoController alunoController = new AlunoController(alunoDAO);
        IInstrutorController instrutorController = new InstrutorController(instrutorDAO);
        IMatriculaController matriculaController = new MatriculaController(matriculaDAO, alunoDAO, planoDAO, observers);
        ITreinoController treinoController = new TreinoController(treinoDAO, alunoDAO, instrutorDAO);
        IFrequenciaController frequenciaController = new FrequenciaController(
                frequenciaDAO, alunoDAO, matriculaDAO, observers);

        // Instancia todas as 5 views sobre os controllers conectados ao SQLite.
        AtomicReference<AlunoView> alunoViewRef = new AtomicReference<>();
        AtomicReference<InstrutorView> instrutorViewRef = new AtomicReference<>();
        AtomicReference<TreinoView> treinoViewRef = new AtomicReference<>();
        AtomicReference<FrequenciaView> frequenciaViewRef = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            alunoViewRef.set(new AlunoView(alunoController));
            instrutorViewRef.set(new InstrutorView(instrutorController));
            matriculaViewRef.set(new MatriculaView(matriculaController, alunoController));
            treinoViewRef.set(new TreinoView(treinoController));
            frequenciaViewRef.set(new FrequenciaView(frequenciaController));
        });

        // 1. Alunos + restrições médicas
        Aluno aluno = alunoController.cadastrar("Maria Souza", "52998224725", "82999990000",
                LocalDate.now().minusYears(22), ObjetivoTreino.HIPERTROFIA);
        alunoController.adicionarRestricaoMedica(aluno.getCpf(), "Condromalácia patelar");
        alunoController.atualizarTelefone(aluno.getCpf(), "82988881111");

        Aluno alunoPersistido = alunoController.buscarPorCpf("529.982.247-25");
        assertTrue(alunoPersistido.getId() > 0);
        assertEquals("82988881111", alunoPersistido.getTelefone());
        assertEquals(List.of("Condromalácia patelar"), alunoPersistido.getRestricoesMedicas());

        // 2. Instrutores
        Instrutor instrutor = instrutorController.cadastrar("Carlos Lima", "11144477735", "82977776666",
                "123456-G/AL", "Musculação");
        instrutorController.atualizarEspecialidade(instrutor.getCref(), "Hipertrofia e Funcional");
        Instrutor instrutorPersistido = instrutorController.buscarPorCref("123456-g/al");
        assertTrue(instrutorPersistido.getId() > 0);
        assertEquals("Hipertrofia e Funcional", instrutorPersistido.getEspecialidade());

        // 3. Matrículas + Strategy + Faturamento + Bloqueio de matrícula ativa duplicada
        Matricula matricula = matriculaController.matricular(aluno.getCpf(), TipoPlano.MENSAL, true);
        assertTrue(matricula.getId() > 0);
        assertEquals(79.92, matricula.calcularMensalidade(), 0.001);
        assertEquals(79.92, matriculaController.calcularFaturamentoMensal(), 0.001);
        assertEquals(1, matriculaController.listar().size());
        assertThrows(MatriculaAtivaExistenteException.class,
                () -> matriculaController.matricular(aluno.getCpf(), TipoPlano.ANUAL, false));

        // 4. Treinos
        Treino treino = treinoController.criar(aluno.getCpf(), instrutor.getCref(), ObjetivoTreino.HIPERTROFIA);
        assertTrue(treino.getId() > 0);
        treinoController.adicionarExercicio(treino.getId(), "Elevação pélvica - 4x12");
        List<Treino> treinosAluno = treinoController.listarPorAluno(aluno.getCpf());
        assertEquals(1, treinosAluno.size());
        assertTrue(treinosAluno.get(0).getExercicios().contains("Elevação pélvica - 4x12"));

        // 5. Frequência / Catraca (acesso liberado enquanto matrícula ativa)
        Frequencia entrada = frequenciaController.registrarEntrada(aluno.getCpf());
        assertNotNull(entrada);
        assertTrue(entrada.getId() > 0);
        assertEquals(1, frequenciaController.contarPresencas(aluno.getCpf(), YearMonth.now()));

        // Renovação e cancelamento notificam a MatriculaView (Observer) e persistem no SQLite
        matriculaController.renovar(matricula.getId());
        matriculaController.cancelar(matricula.getId());

        // Após cancelamento, a catraca bloqueia nova entrada mas preserva o histórico anterior
        assertThrows(AcessoNegadoException.class, () -> frequenciaController.registrarEntrada(aluno.getCpf()));
        assertEquals(1, frequenciaController.contarPresencas(aluno.getCpf(), YearMonth.now()));

        SwingUtilities.invokeAndWait(() -> {
            alunoViewRef.get().recarregar();
            instrutorViewRef.get().recarregar();
            matriculaViewRef.get().recarregar();
        });

        assertEquals(StatusMatricula.CANCELADA, matriculaController.listar().get(0).getStatus());
        assertEquals(0.0, matriculaController.calcularFaturamentoMensal(), 0.001);
        assertTrue(matriculaViewRef.get().getUltimaNotificacao().contains("CANCELADA"));
        assertTrue(alertaObserver.getUltimoAlerta().orElse("").contains("cancelada"));
        assertEquals(2, logObserver.getRegistros().size());
    }
}
