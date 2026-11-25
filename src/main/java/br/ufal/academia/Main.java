package br.ufal.academia;

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
import br.ufal.academia.dao.AlunoDAO;
import br.ufal.academia.dao.AlunoDAOImpl;
import br.ufal.academia.dao.ConnectionFactory;
import br.ufal.academia.dao.FrequenciaDAO;
import br.ufal.academia.dao.FrequenciaDAOImpl;
import br.ufal.academia.dao.InstrutorDAO;
import br.ufal.academia.dao.InstrutorDAOImpl;
import br.ufal.academia.dao.MatriculaDAO;
import br.ufal.academia.dao.MatriculaDAOImpl;
import br.ufal.academia.dao.PlanoDAO;
import br.ufal.academia.dao.PlanoDAOImpl;
import br.ufal.academia.dao.TreinoDAO;
import br.ufal.academia.dao.TreinoDAOImpl;
import br.ufal.academia.exception.DadosInvalidosException;
import br.ufal.academia.model.enums.TipoPlano;
import br.ufal.academia.model.patterns.factory.PlanoFactory;
import br.ufal.academia.model.patterns.observer.AlertaInterfaceObserver;
import br.ufal.academia.model.patterns.observer.LogAuditoriaObserver;
import br.ufal.academia.model.patterns.observer.MatriculaObserver;
import br.ufal.academia.view.AlunoView;
import br.ufal.academia.view.FrequenciaView;
import br.ufal.academia.view.InstrutorView;
import br.ufal.academia.view.MatriculaView;
import br.ufal.academia.view.TelaPrincipal;
import br.ufal.academia.view.TreinoView;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

// Ponto de entrada da aplicação: configura FlatLaf, inicializa o banco SQLite,
// garante o seed idempotente dos planos e conecta DAOs, Observers, Controllers e a TelaPrincipal.
public class Main {

    public static void main(String[] args) {
        FlatLightLaf.setup();

        ConnectionFactory.getInstancia();

        AlunoDAO alunoDAO = new AlunoDAOImpl();
        InstrutorDAO instrutorDAO = new InstrutorDAOImpl();
        PlanoDAO planoDAO = new PlanoDAOImpl();
        MatriculaDAO matriculaDAO = new MatriculaDAOImpl(alunoDAO, planoDAO);
        TreinoDAO treinoDAO = new TreinoDAOImpl(alunoDAO, instrutorDAO);
        FrequenciaDAO frequenciaDAO = new FrequenciaDAOImpl(matriculaDAO);

        inicializarPlanosPadrao(planoDAO);

        AtomicReference<TelaPrincipal> telaRef = new AtomicReference<>();
        AtomicReference<MatriculaView> matriculaViewRef = new AtomicReference<>();
        LogAuditoriaObserver logAuditoriaObserver = new LogAuditoriaObserver();
        AlertaInterfaceObserver alertaInterfaceObserver = new AlertaInterfaceObserver(mensagem -> {
            TelaPrincipal tela = telaRef.get();
            if (tela != null) {
                tela.exibirNotificacao(mensagem);
            }
        });
        MatriculaObserver matriculaViewObserver = matricula -> {
            MatriculaView matriculaView = matriculaViewRef.get();
            if (matriculaView != null) {
                matriculaView.atualizar(matricula);
            }
        };
        List<MatriculaObserver> observers = List.of(
                logAuditoriaObserver, alertaInterfaceObserver, matriculaViewObserver);

        IAlunoController alunoController = new AlunoController(alunoDAO);
        IInstrutorController instrutorController = new InstrutorController(instrutorDAO);
        IMatriculaController matriculaController = new MatriculaController(
                matriculaDAO, alunoDAO, planoDAO, observers);
        ITreinoController treinoController = new TreinoController(treinoDAO, alunoDAO, instrutorDAO);
        IFrequenciaController frequenciaController = new FrequenciaController(
                frequenciaDAO, alunoDAO, matriculaDAO, observers);

        SwingUtilities.invokeLater(() -> {
            AlunoView alunoView = new AlunoView(alunoController);
            InstrutorView instrutorView = new InstrutorView(instrutorController);
            MatriculaView matriculaView = new MatriculaView(matriculaController, alunoController);
            TreinoView treinoView = new TreinoView(treinoController);
            FrequenciaView frequenciaView = new FrequenciaView(frequenciaController);
            matriculaViewRef.set(matriculaView);

            TelaPrincipal telaPrincipal = new TelaPrincipal(
                    alunoController,
                    instrutorController,
                    matriculaController,
                    treinoController,
                    frequenciaController,
                    alunoView,
                    instrutorView,
                    matriculaView,
                    treinoView,
                    frequenciaView);
            telaRef.set(telaPrincipal);
            alertaInterfaceObserver.getUltimoAlerta().ifPresent(telaPrincipal::exibirNotificacao);
            telaPrincipal.setVisible(true);
        });
    }

    // Seed idempotente dos 4 planos padrão no SQLite via PlanoFactory.
    public static void inicializarPlanosPadrao(PlanoDAO planoDAO) {
        if (planoDAO == null) {
            throw new IllegalArgumentException("O PlanoDAO é obrigatório para o seed de planos.");
        }
        for (TipoPlano tipo : TipoPlano.values()) {
            if (planoDAO.buscarPorTipo(tipo).isEmpty()) {
                try {
                    planoDAO.salvar(PlanoFactory.criar(tipo));
                } catch (DadosInvalidosException e) {
                    throw new IllegalStateException("Falha ao criar plano padrão " + tipo + ".", e);
                }
            }
        }
    }
}
