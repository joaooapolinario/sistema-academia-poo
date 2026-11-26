package br.ufal.academia.view;

import br.ufal.academia.controller.IAlunoController;
import br.ufal.academia.exception.DadosInvalidosException;
import br.ufal.academia.model.entities.Aluno;
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
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

// Tela de gestão de alunos: CRUD completo, busca por CPF e gerenciamento de restrições médicas.
public class AlunoView extends JPanel {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final IAlunoController controller;

    private final JTextField txtNome = new JTextField(20);
    private final JTextField txtCpf = new JTextField(14);
    private final JTextField txtTelefone = new JTextField(14);
    private final JTextField txtDataNascimento = new JTextField(10);
    private final JComboBox<ObjetivoTreino> cmbObjetivo = new JComboBox<>(ObjetivoTreino.values());

    private final JTextField txtBuscaCpf = new JTextField(14);
    private final DefaultTableModel modeloTabela = new DefaultTableModel(
            new Object[]{"ID", "Nome", "CPF", "Telefone", "Nascimento", "Idade", "Objetivo", "Restrições"}, 0) {
        @Override
        public boolean isCellEditable(int linha, int coluna) {
            return false;
        }
    };
    private final JTable tabelaAlunos = new JTable(modeloTabela);

    private final JLabel lblAlunoSelecionado = new JLabel("Selecione um aluno na tabela.");
    private final DefaultListModel<String> modeloRestricoes = new DefaultListModel<>();
    private final JList<String> listaRestricoes = new JList<>(modeloRestricoes);
    private final JTextField txtNovaRestricao = new JTextField(16);

    private List<Aluno> alunosExibidos = new ArrayList<>();
    private String cpfFiltroAtivo;

    public AlunoView(IAlunoController controller) {
        if (controller == null) {
            throw new IllegalArgumentException("O controller de alunos é obrigatório.");
        }
        this.controller = controller;
        montarTela();
        recarregar();
    }

    private void montarTela() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        add(criarPainelFormulario(), BorderLayout.NORTH);
        add(criarPainelConteudo(), BorderLayout.CENTER);
    }

    private JPanel criarPainelFormulario() {
        JPanel painel = new JPanel(new GridBagLayout());
        painel.setBorder(BorderFactory.createTitledBorder("Cadastro de aluno"));

        txtCpf.setToolTipText("Informe 11 dígitos ou no formato 000.000.000-00");
        txtTelefone.setToolTipText("DDD + número (10 ou 11 dígitos)");
        txtDataNascimento.setToolTipText("Formato dd/MM/aaaa (ex.: 15/08/2000)");
        cmbObjetivo.setSelectedIndex(-1);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;

        // Linha 0: Nome e CPF
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        painel.add(new JLabel("Nome:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        painel.add(txtNome, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        painel.add(new JLabel("CPF:"), gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.6;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        painel.add(txtCpf, gbc);

        // Linha 1: Telefone e Data de nascimento
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        painel.add(new JLabel("Telefone:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        painel.add(txtTelefone, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        painel.add(new JLabel("Nascimento (dd/MM/aaaa):"), gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.6;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        painel.add(txtDataNascimento, gbc);

        // Linha 2: Objetivo e ações do formulário
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        painel.add(new JLabel("Objetivo:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        painel.add(cmbObjetivo, gbc);

        JPanel botoesFormulario = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btnLimpar = new JButton("Limpar");
        btnLimpar.addActionListener(e -> limparFormulario());
        JButton btnCadastrar = new JButton("Cadastrar");
        btnCadastrar.addActionListener(e -> cadastrarAluno());
        botoesFormulario.add(btnLimpar);
        botoesFormulario.add(btnCadastrar);

        gbc.gridx = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        painel.add(botoesFormulario, gbc);

        return painel;
    }

    private JSplitPane criarPainelConteudo() {
        tabelaAlunos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabelaAlunos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                atualizarPainelRestricoes();
            }
        });

        JPanel painelTabela = new JPanel(new BorderLayout(0, 8));
        painelTabela.setBorder(BorderFactory.createTitledBorder("Alunos cadastrados"));

        JPanel barraBusca = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton btnBuscar = new JButton("Buscar por CPF");
        btnBuscar.addActionListener(e -> buscarPorCpf());
        txtBuscaCpf.addActionListener(e -> buscarPorCpf());
        JButton btnListarTodos = new JButton("Listar todos");
        btnListarTodos.addActionListener(e -> listarTodos());
        barraBusca.add(new JLabel("CPF:"));
        barraBusca.add(txtBuscaCpf);
        barraBusca.add(btnBuscar);
        barraBusca.add(btnListarTodos);

        JPanel botoesAcao = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton btnAtualizarTelefone = new JButton("Atualizar telefone");
        btnAtualizarTelefone.addActionListener(e -> atualizarTelefone());
        JButton btnRemover = new JButton("Remover aluno");
        btnRemover.addActionListener(e -> removerAluno());
        botoesAcao.add(btnAtualizarTelefone);
        botoesAcao.add(btnRemover);

        painelTabela.add(barraBusca, BorderLayout.NORTH);
        painelTabela.add(new JScrollPane(tabelaAlunos), BorderLayout.CENTER);
        painelTabela.add(botoesAcao, BorderLayout.SOUTH);

        JPanel painelRestricoes = new JPanel(new BorderLayout(0, 8));
        painelRestricoes.setBorder(BorderFactory.createTitledBorder("Restrições médicas"));

        lblAlunoSelecionado.setFont(lblAlunoSelecionado.getFont().deriveFont(Font.BOLD));
        listaRestricoes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel rodapeRestricoes = new JPanel(new BorderLayout(6, 6));
        JPanel linhaAdicionar = new JPanel(new BorderLayout(6, 0));
        JButton btnAdicionarRestricao = new JButton("Adicionar");
        btnAdicionarRestricao.addActionListener(e -> adicionarRestricao());
        txtNovaRestricao.addActionListener(e -> adicionarRestricao());
        linhaAdicionar.add(txtNovaRestricao, BorderLayout.CENTER);
        linhaAdicionar.add(btnAdicionarRestricao, BorderLayout.EAST);

        JButton btnRemoverRestricao = new JButton("Remover selecionada");
        btnRemoverRestricao.addActionListener(e -> removerRestricao());

        rodapeRestricoes.add(linhaAdicionar, BorderLayout.NORTH);
        rodapeRestricoes.add(btnRemoverRestricao, BorderLayout.SOUTH);

        painelRestricoes.add(lblAlunoSelecionado, BorderLayout.NORTH);
        painelRestricoes.add(new JScrollPane(listaRestricoes), BorderLayout.CENTER);
        painelRestricoes.add(rodapeRestricoes, BorderLayout.SOUTH);

        JSplitPane divisao = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, painelTabela, painelRestricoes);
        divisao.setResizeWeight(0.68);
        return divisao;
    }

    public void recarregar() {
        Aluno selecionado = alunoSelecionado();
        String cpfSelecionado = selecionado != null ? selecionado.getCpf() : null;
        if (cpfFiltroAtivo != null) {
            try {
                Aluno encontrado = controller.buscarPorCpf(cpfFiltroAtivo);
                preencherTabela(List.of(encontrado), encontrado.getCpf());
                return;
            } catch (DadosInvalidosException e) {
                cpfFiltroAtivo = null;
                txtBuscaCpf.setText("");
            } catch (RuntimeException e) {
                mostrarErroInesperado(e);
                return;
            }
        }
        try {
            preencherTabela(controller.listar(), cpfSelecionado);
        } catch (RuntimeException e) {
            mostrarErroInesperado(e);
        }
    }

    private void cadastrarAluno() {
        LocalDate dataNascimento;
        try {
            dataNascimento = LocalDate.parse(txtDataNascimento.getText().trim(), FORMATO_DATA);
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Informe a data de nascimento no formato dd/MM/aaaa (ex.: 15/08/2000).",
                    "Data inválida",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            ObjetivoTreino objetivo = (ObjetivoTreino) cmbObjetivo.getSelectedItem();
            Aluno aluno = controller.cadastrar(
                    txtNome.getText(),
                    txtCpf.getText(),
                    txtTelefone.getText(),
                    dataNascimento,
                    objetivo);
            limparFormulario();
            cpfFiltroAtivo = null;
            txtBuscaCpf.setText("");
            preencherTabela(controller.listar(), aluno.getCpf());
            JOptionPane.showMessageDialog(
                    this,
                    "Aluno \"" + aluno.getNome() + "\" cadastrado com sucesso.",
                    "Cadastro realizado",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (DadosInvalidosException e) {
            mostrarAviso(e);
        } catch (RuntimeException e) {
            mostrarErroInesperado(e);
        }
    }

    private void buscarPorCpf() {
        try {
            Aluno aluno = controller.buscarPorCpf(txtBuscaCpf.getText());
            cpfFiltroAtivo = aluno.getCpf();
            preencherTabela(List.of(aluno), aluno.getCpf());
        } catch (DadosInvalidosException e) {
            mostrarAviso(e);
        } catch (RuntimeException e) {
            mostrarErroInesperado(e);
        }
    }

    private void listarTodos() {
        cpfFiltroAtivo = null;
        txtBuscaCpf.setText("");
        recarregar();
    }

    private void atualizarTelefone() {
        Aluno aluno = alunoSelecionado();
        if (aluno == null) {
            JOptionPane.showMessageDialog(this, "Selecione um aluno na tabela.");
            return;
        }
        String novoTelefone = (String) JOptionPane.showInputDialog(
                this,
                "Novo telefone para " + aluno.getNome() + ":",
                "Atualizar telefone",
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                aluno.getTelefone());
        if (novoTelefone == null) {
            return;
        }
        try {
            controller.atualizarTelefone(aluno.getCpf(), novoTelefone);
            recarregar();
        } catch (DadosInvalidosException e) {
            mostrarAviso(e);
        } catch (RuntimeException e) {
            mostrarErroInesperado(e);
        }
    }

    private void removerAluno() {
        Aluno aluno = alunoSelecionado();
        if (aluno == null) {
            JOptionPane.showMessageDialog(this, "Selecione um aluno na tabela.");
            return;
        }
        int resposta = JOptionPane.showConfirmDialog(
                this,
                "Remover o aluno \"" + aluno.getNome() + "\" (CPF " + formatarCpf(aluno.getCpf()) + ")?\n"
                        + "As matrículas, treinos e restrições vinculadas também serão removidos.",
                "Confirmar remoção",
                JOptionPane.YES_NO_OPTION);
        if (resposta != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            controller.remover(aluno.getCpf());
            if (aluno.getCpf().equals(cpfFiltroAtivo)) {
                cpfFiltroAtivo = null;
                txtBuscaCpf.setText("");
            }
            recarregar();
        } catch (DadosInvalidosException e) {
            mostrarAviso(e);
        } catch (RuntimeException e) {
            mostrarErroInesperado(e);
        }
    }

    private void adicionarRestricao() {
        Aluno aluno = alunoSelecionado();
        if (aluno == null) {
            JOptionPane.showMessageDialog(this, "Selecione um aluno na tabela para adicionar restrição.");
            return;
        }
        String descricao = txtNovaRestricao.getText();
        try {
            controller.adicionarRestricaoMedica(aluno.getCpf(), descricao);
            txtNovaRestricao.setText("");
            recarregar();
        } catch (DadosInvalidosException e) {
            mostrarAviso(e);
        } catch (RuntimeException e) {
            mostrarErroInesperado(e);
        }
    }

    private void removerRestricao() {
        Aluno aluno = alunoSelecionado();
        String restricao = listaRestricoes.getSelectedValue();
        if (aluno == null || restricao == null) {
            JOptionPane.showMessageDialog(this, "Selecione um aluno e uma restrição médica.");
            return;
        }
        try {
            controller.removerRestricaoMedica(aluno.getCpf(), restricao);
            recarregar();
        } catch (DadosInvalidosException e) {
            mostrarAviso(e);
        } catch (RuntimeException e) {
            mostrarErroInesperado(e);
        }
    }

    private void preencherTabela(List<Aluno> alunos, String cpfParaSelecionar) {
        this.alunosExibidos = new ArrayList<>(alunos);
        modeloTabela.setRowCount(0);
        for (Aluno aluno : alunosExibidos) {
            modeloTabela.addRow(new Object[]{
                    aluno.getId(),
                    aluno.getNome(),
                    formatarCpf(aluno.getCpf()),
                    formatarTelefone(aluno.getTelefone()),
                    aluno.getDataNascimento().format(FORMATO_DATA),
                    aluno.getIdade(),
                    aluno.getObjetivo(),
                    aluno.getRestricoesMedicas().size()});
        }
        if (cpfParaSelecionar != null) {
            for (int i = 0; i < alunosExibidos.size(); i++) {
                if (alunosExibidos.get(i).getCpf().equals(cpfParaSelecionar)) {
                    tabelaAlunos.setRowSelectionInterval(i, i);
                    break;
                }
            }
        }
        atualizarPainelRestricoes();
    }

    private void atualizarPainelRestricoes() {
        modeloRestricoes.clear();
        Aluno aluno = alunoSelecionado();
        if (aluno == null) {
            lblAlunoSelecionado.setText("Selecione um aluno na tabela.");
            return;
        }
        lblAlunoSelecionado.setText("Aluno: " + aluno.getNome());
        for (String restricao : aluno.getRestricoesMedicas()) {
            modeloRestricoes.addElement(restricao);
        }
    }

    private void limparFormulario() {
        txtNome.setText("");
        txtCpf.setText("");
        txtTelefone.setText("");
        txtDataNascimento.setText("");
        cmbObjetivo.setSelectedIndex(-1);
    }

    private Aluno alunoSelecionado() {
        int linha = tabelaAlunos.getSelectedRow();
        if (linha < 0 || linha >= alunosExibidos.size()) {
            return null;
        }
        return alunosExibidos.get(linha);
    }

    static String formatarCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf != null ? cpf : "";
        }
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
    }

    static String formatarTelefone(String telefone) {
        if (telefone == null) {
            return "";
        }
        if (telefone.length() == 11) {
            return "(" + telefone.substring(0, 2) + ") " + telefone.substring(2, 7) + "-" + telefone.substring(7);
        }
        if (telefone.length() == 10) {
            return "(" + telefone.substring(0, 2) + ") " + telefone.substring(2, 6) + "-" + telefone.substring(6);
        }
        return telefone;
    }

    private void mostrarAviso(DadosInvalidosException e) {
        JOptionPane.showMessageDialog(this, e.getMessage(), "Dados inválidos", JOptionPane.WARNING_MESSAGE);
    }

    private void mostrarErroInesperado(RuntimeException e) {
        JOptionPane.showMessageDialog(this, "Erro inesperado: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
    }
}
