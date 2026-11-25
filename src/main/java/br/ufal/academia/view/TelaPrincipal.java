package br.ufal.academia.view;

import br.ufal.academia.controller.IAlunoController;
import br.ufal.academia.controller.IFrequenciaController;
import br.ufal.academia.controller.IInstrutorController;
import br.ufal.academia.controller.IMatriculaController;
import br.ufal.academia.controller.ITreinoController;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.function.Consumer;

// Janela principal da aplicação Swing, com navegação por abas para os 5 módulos e barra inferior de notificações.
public class TelaPrincipal extends JFrame {

    public static final int ABA_ALUNOS = 0;
    public static final int ABA_INSTRUTORES = 1;
    public static final int ABA_MATRICULAS = 2;
    public static final int ABA_TREINOS = 3;
    public static final int ABA_FREQUENCIA = 4;

    private static final String MENSAGEM_NOTIFICACAO_PADRAO =
            "Sistema pronto. Nenhuma notificação pendente.";
    private static final Color COR_ALERTA = new Color(180, 35, 24);
    private static final Color COR_AVISO = new Color(29, 78, 216);

    private final IAlunoController alunoController;
    private final IInstrutorController instrutorController;
    private final IMatriculaController matriculaController;
    private final ITreinoController treinoController;
    private final IFrequenciaController frequenciaController;

    private final JTabbedPane tabbedPane;
    private final JLabel lblNotificacao;
    private volatile String ultimaNotificacao;

    public TelaPrincipal(
            IAlunoController alunoController,
            IInstrutorController instrutorController,
            IMatriculaController matriculaController,
            ITreinoController treinoController,
            IFrequenciaController frequenciaController) {
        this(
                alunoController,
                instrutorController,
                matriculaController,
                treinoController,
                frequenciaController,
                new AlunoView(alunoController),
                new InstrutorView(instrutorController),
                new MatriculaView(matriculaController, alunoController),
                new TreinoView(treinoController),
                new FrequenciaView(frequenciaController));
    }

    public TelaPrincipal(
            IAlunoController alunoController,
            IInstrutorController instrutorController,
            IMatriculaController matriculaController,
            ITreinoController treinoController,
            IFrequenciaController frequenciaController,
            JComponent painelAlunos,
            JComponent painelInstrutores,
            JComponent painelMatriculas,
            JComponent painelTreinos,
            JComponent painelFrequencia) {
        super("Sistema de Gestão de Academia");
        if (alunoController == null
                || instrutorController == null
                || matriculaController == null
                || treinoController == null
                || frequenciaController == null) {
            throw new IllegalArgumentException("Todos os controllers dos 5 módulos são obrigatórios.");
        }
        if (painelAlunos == null
                || painelInstrutores == null
                || painelMatriculas == null
                || painelTreinos == null
                || painelFrequencia == null) {
            throw new IllegalArgumentException("Todos os painéis dos 5 módulos são obrigatórios.");
        }

        this.alunoController = alunoController;
        this.instrutorController = instrutorController;
        this.matriculaController = matriculaController;
        this.treinoController = treinoController;
        this.frequenciaController = frequenciaController;

        this.tabbedPane = new JTabbedPane();
        this.lblNotificacao = new JLabel(MENSAGEM_NOTIFICACAO_PADRAO);

        configurarJanela(
                painelAlunos,
                painelInstrutores,
                painelMatriculas,
                painelTreinos,
                painelFrequencia);
    }

    private void configurarJanela(
            JComponent painelAlunos,
            JComponent painelInstrutores,
            JComponent painelMatriculas,
            JComponent painelTreinos,
            JComponent painelFrequencia) {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(960, 640));
        setPreferredSize(new Dimension(1120, 720));

        JPanel raiz = new JPanel(new BorderLayout(0, 0));
        raiz.add(criarCabecalho(), BorderLayout.NORTH);

        tabbedPane.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        tabbedPane.addTab("Alunos", painelAlunos);
        tabbedPane.addTab("Instrutores", painelInstrutores);
        tabbedPane.addTab("Matrículas", painelMatriculas);
        tabbedPane.addTab("Treinos", painelTreinos);
        tabbedPane.addTab("Catraca / Frequência", painelFrequencia);
        tabbedPane.addChangeListener(e -> atualizarAbaSelecionada());
        raiz.add(tabbedPane, BorderLayout.CENTER);

        raiz.add(criarBarraNotificacoes(), BorderLayout.SOUTH);

        setContentPane(raiz);
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel criarCabecalho() {
        JPanel cabecalho = new JPanel();
        cabecalho.setLayout(new BoxLayout(cabecalho, BoxLayout.Y_AXIS));
        cabecalho.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, obterCorSeparador()),
                BorderFactory.createEmptyBorder(14, 18, 12, 18)));

        JLabel titulo = new JLabel("Sistema de Gestão de Academia");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 18f));

        JLabel subtitulo = new JLabel(
                "Gerenciamento integrado de alunos, instrutores, matrículas, treinos e controle de catraca");
        subtitulo.setForeground(obterCorTextoSecundario());

        cabecalho.add(titulo);
        cabecalho.add(Box.createVerticalStrut(3));
        cabecalho.add(subtitulo);
        return cabecalho;
    }

    private JPanel criarBarraNotificacoes() {
        JPanel barra = new JPanel(new BorderLayout(12, 0));
        barra.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, obterCorSeparador()),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)));

        JLabel lblTituloBarra = new JLabel("Notificações:");
        lblTituloBarra.setFont(lblTituloBarra.getFont().deriveFont(Font.BOLD));

        lblNotificacao.setForeground(obterCorTextoSecundario());
        lblNotificacao.setToolTipText(MENSAGEM_NOTIFICACAO_PADRAO);

        JButton btnLimpar = new JButton("Limpar");
        btnLimpar.setFocusable(false);
        btnLimpar.addActionListener(e -> limparNotificacao());

        barra.add(lblTituloBarra, BorderLayout.WEST);
        barra.add(lblNotificacao, BorderLayout.CENTER);
        barra.add(btnLimpar, BorderLayout.EAST);
        return barra;
    }

    // Callback conectado ao AlertaInterfaceObserver; garante atualização segura na thread de eventos do Swing (EDT).
    public void exibirNotificacao(String mensagem) {
        if (mensagem == null || mensagem.isBlank()) {
            return;
        }
        String texto = mensagem.trim();
        this.ultimaNotificacao = texto;

        Runnable atualizacao = () -> {
            lblNotificacao.setText(texto);
            lblNotificacao.setToolTipText(texto);
            if (texto.startsWith("ALERTA:")) {
                lblNotificacao.setForeground(COR_ALERTA);
            } else if (texto.startsWith("AVISO:")) {
                lblNotificacao.setForeground(COR_AVISO);
            } else {
                lblNotificacao.setForeground(obterCorTextoPadrao());
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            atualizacao.run();
        } else {
            SwingUtilities.invokeLater(atualizacao);
        }
    }

    public void limparNotificacao() {
        this.ultimaNotificacao = null;
        Runnable atualizacao = () -> {
            lblNotificacao.setText(MENSAGEM_NOTIFICACAO_PADRAO);
            lblNotificacao.setToolTipText(MENSAGEM_NOTIFICACAO_PADRAO);
            lblNotificacao.setForeground(obterCorTextoSecundario());
        };
        if (SwingUtilities.isEventDispatchThread()) {
            atualizacao.run();
        } else {
            SwingUtilities.invokeLater(atualizacao);
        }
    }

    public Consumer<String> getCallbackNotificacao() {
        return this::exibirNotificacao;
    }

    public String getUltimaNotificacao() {
        return ultimaNotificacao;
    }

    public void definirConteudoAba(int indiceAba, JComponent componente) {
        if (componente == null) {
            throw new IllegalArgumentException("O componente da aba não pode ser nulo.");
        }
        tabbedPane.setComponentAt(indiceAba, componente);
    }

    public JTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    public IAlunoController getAlunoController() {
        return alunoController;
    }

    public IInstrutorController getInstrutorController() {
        return instrutorController;
    }

    public IMatriculaController getMatriculaController() {
        return matriculaController;
    }

    public ITreinoController getTreinoController() {
        return treinoController;
    }

    public IFrequenciaController getFrequenciaController() {
        return frequenciaController;
    }

    private void atualizarAbaSelecionada() {
        Component componente = tabbedPane.getSelectedComponent();
        if (componente instanceof AlunoView alunoView) {
            alunoView.recarregar();
        } else if (componente instanceof InstrutorView instrutorView) {
            instrutorView.recarregar();
        } else if (componente instanceof MatriculaView matriculaView) {
            matriculaView.recarregar();
        }
    }

    private static Color obterCorSeparador() {
        Color cor = UIManager.getColor("Separator.foreground");
        return cor != null ? cor : new Color(215, 219, 224);
    }

    private static Color obterCorTextoPadrao() {
        Color cor = UIManager.getColor("Label.foreground");
        return cor != null ? cor : Color.DARK_GRAY;
    }

    private static Color obterCorTextoSecundario() {
        Color cor = UIManager.getColor("Label.disabledForeground");
        return cor != null ? cor : new Color(102, 112, 133);
    }
}
