package br.ufal.academia.view;

import br.ufal.academia.controller.ITreinoController;
import br.ufal.academia.exception.DadosInvalidosException;
import br.ufal.academia.model.entities.Treino;
import br.ufal.academia.model.enums.ObjetivoTreino;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// Tela de treinos: prescreve o treino padrão e mantém os exercícios dos treinos do aluno
public class TreinoView extends JPanel {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ITreinoController controller;

    private final JTextField txtCpfAluno = new JTextField(14);
    private final JTextField txtCref = new JTextField(12);
    private final JComboBox<ObjetivoTreino> cmbObjetivo = new JComboBox<>(ObjetivoTreino.values());

    private final DefaultTableModel modeloTabela = new DefaultTableModel(
            new Object[]{"Id", "Objetivo", "Instrutor", "Criado em", "Exercícios"}, 0) {
        @Override
        public boolean isCellEditable(int linha, int coluna) {
            return false;
        }
    };
    private final JTable tabelaTreinos = new JTable(modeloTabela);
    private final DefaultListModel<String> modeloExercicios = new DefaultListModel<>();
    private final JList<String> listaExercicios = new JList<>(modeloExercicios);

    private List<Treino> treinosExibidos = new ArrayList<>();

    public TreinoView(ITreinoController controller) {
        if (controller == null) {
            throw new IllegalArgumentException("O controller de treinos é obrigatório.");
        }
        this.controller = controller;
        montarTela();
    }

    private void montarTela() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        add(criarPainelFormulario(), BorderLayout.NORTH);
        add(criarPainelTreinos(), BorderLayout.CENTER);
    }

    private JPanel criarPainelFormulario() {
        cmbObjetivo.setSelectedIndex(-1);

        JPanel painel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        painel.setBorder(BorderFactory.createTitledBorder("Prescrever treino padrão"));

        JButton btnPrescrever = new JButton("Prescrever");
        btnPrescrever.addActionListener(e -> prescreverTreino());
        JButton btnBuscar = new JButton("Buscar treinos do aluno");
        btnBuscar.addActionListener(e -> carregarTreinos());

        painel.add(new JLabel("CPF do aluno:"));
        painel.add(txtCpfAluno);
        painel.add(new JLabel("CREF do instrutor:"));
        painel.add(txtCref);
        painel.add(new JLabel("Objetivo:"));
        painel.add(cmbObjetivo);
        painel.add(btnPrescrever);
        painel.add(btnBuscar);
        return painel;
    }

    private JSplitPane criarPainelTreinos() {
        tabelaTreinos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabelaTreinos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostrarExercicios();
            }
        });

        JPanel painelTabela = new JPanel(new BorderLayout(0, 8));
        painelTabela.setBorder(BorderFactory.createTitledBorder("Treinos do aluno"));
        JButton btnRemoverTreino = new JButton("Remover treino");
        btnRemoverTreino.addActionListener(e -> removerTreino());
        JPanel botoesTreino = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botoesTreino.add(btnRemoverTreino);
        painelTabela.add(new JScrollPane(tabelaTreinos), BorderLayout.CENTER);
        painelTabela.add(botoesTreino, BorderLayout.SOUTH);

        JPanel painelExercicios = new JPanel(new BorderLayout(0, 8));
        painelExercicios.setBorder(BorderFactory.createTitledBorder("Exercícios do treino selecionado"));
        JButton btnAdicionar = new JButton("Adicionar exercício");
        btnAdicionar.addActionListener(e -> adicionarExercicio());
        JButton btnRemover = new JButton("Remover exercício");
        btnRemover.addActionListener(e -> removerExercicio());
        JPanel botoesExercicio = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botoesExercicio.add(btnAdicionar);
        botoesExercicio.add(btnRemover);
        painelExercicios.add(new JScrollPane(listaExercicios), BorderLayout.CENTER);
        painelExercicios.add(botoesExercicio, BorderLayout.SOUTH);

        JSplitPane divisao = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, painelTabela, painelExercicios);
        divisao.setResizeWeight(0.6);
        return divisao;
    }

    private void prescreverTreino() {
        try {
            ObjetivoTreino objetivo = (ObjetivoTreino) cmbObjetivo.getSelectedItem();
            Treino treino = controller.criar(txtCpfAluno.getText(), txtCref.getText(), objetivo);
            JOptionPane.showMessageDialog(this, "Treino #" + treino.getId() + " prescrito com "
                    + treino.getExercicios().size() + " exercícios.");
            carregarTreinos(treino.getId());
        } catch (DadosInvalidosException e) {
            mostrarAviso(e);
        } catch (RuntimeException e) {
            mostrarErroInesperado(e);
        }
    }

    private void carregarTreinos() {
        carregarTreinos(0);
    }

    // Recarrega a tabela e mantém selecionado o treino com o id informado, se ele ainda existir na lista
    private void carregarTreinos(int idParaSelecionar) {
        try {
            treinosExibidos = controller.listarPorAluno(txtCpfAluno.getText());
        } catch (DadosInvalidosException e) {
            mostrarAviso(e);
            return;
        } catch (RuntimeException e) {
            mostrarErroInesperado(e);
            return;
        }

        modeloTabela.setRowCount(0);
        for (Treino treino : treinosExibidos) {
            modeloTabela.addRow(new Object[]{
                    treino.getId(),
                    treino.getObjetivo(),
                    treino.getInstrutor().getNome(),
                    treino.getDataCriacao().format(FORMATO_DATA),
                    treino.getExercicios().size()});
        }
        for (int i = 0; i < treinosExibidos.size(); i++) {
            if (treinosExibidos.get(i).getId() == idParaSelecionar) {
                tabelaTreinos.setRowSelectionInterval(i, i);
            }
        }
        mostrarExercicios();
    }

    private void mostrarExercicios() {
        modeloExercicios.clear();
        Treino treino = treinoSelecionado();
        if (treino != null) {
            for (String exercicio : treino.getExercicios()) {
                modeloExercicios.addElement(exercicio);
            }
        }
    }

    private void adicionarExercicio() {
        Treino treino = treinoSelecionado();
        if (treino == null) {
            JOptionPane.showMessageDialog(this, "Selecione um treino na tabela.");
            return;
        }
        String descricao = JOptionPane.showInputDialog(this, "Descrição do exercício (ex.: Leg press - 4x12):");
        if (descricao == null) {
            return;
        }
        try {
            controller.adicionarExercicio(treino.getId(), descricao);
            carregarTreinos(treino.getId());
        } catch (DadosInvalidosException e) {
            mostrarAviso(e);
        } catch (RuntimeException e) {
            mostrarErroInesperado(e);
        }
    }

    private void removerExercicio() {
        Treino treino = treinoSelecionado();
        String exercicio = listaExercicios.getSelectedValue();
        if (treino == null || exercicio == null) {
            JOptionPane.showMessageDialog(this, "Selecione um treino e um exercício.");
            return;
        }
        try {
            controller.removerExercicio(treino.getId(), exercicio);
            carregarTreinos(treino.getId());
        } catch (DadosInvalidosException e) {
            mostrarAviso(e);
        } catch (RuntimeException e) {
            mostrarErroInesperado(e);
        }
    }

    private void removerTreino() {
        Treino treino = treinoSelecionado();
        if (treino == null) {
            JOptionPane.showMessageDialog(this, "Selecione um treino na tabela.");
            return;
        }
        int resposta = JOptionPane.showConfirmDialog(this, "Remover o treino #" + treino.getId() + "?",
                "Confirmar remoção", JOptionPane.YES_NO_OPTION);
        if (resposta != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            controller.remover(treino.getId());
            carregarTreinos();
        } catch (DadosInvalidosException e) {
            mostrarAviso(e);
        } catch (RuntimeException e) {
            mostrarErroInesperado(e);
        }
    }

    private Treino treinoSelecionado() {
        int linha = tabelaTreinos.getSelectedRow();
        if (linha < 0 || linha >= treinosExibidos.size()) {
            return null;
        }
        return treinosExibidos.get(linha);
    }

    private void mostrarAviso(DadosInvalidosException e) {
        JOptionPane.showMessageDialog(this, e.getMessage(), "Dados inválidos", JOptionPane.WARNING_MESSAGE);
    }

    private void mostrarErroInesperado(RuntimeException e) {
        JOptionPane.showMessageDialog(this, "Erro inesperado: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
    }
}