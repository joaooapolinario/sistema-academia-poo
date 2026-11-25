package br.ufal.academia.view;

import br.ufal.academia.controller.IInstrutorController;
import br.ufal.academia.exception.DadosInvalidosException;
import br.ufal.academia.model.entities.Instrutor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

// Tela de gestão de instrutores: CRUD completo, busca por CREF e listagem em tabela.
public class InstrutorView extends JPanel {

    private final IInstrutorController controller;

    private final JTextField txtNome = new JTextField(20);
    private final JTextField txtCpf = new JTextField(14);
    private final JTextField txtTelefone = new JTextField(14);
    private final JTextField txtCref = new JTextField(14);
    private final JTextField txtEspecialidade = new JTextField(20);

    private final JTextField txtBuscaCref = new JTextField(14);
    private final DefaultTableModel modeloTabela = new DefaultTableModel(
            new Object[]{"ID", "Nome", "CPF", "Telefone", "CREF", "Especialidade"}, 0) {
        @Override
        public boolean isCellEditable(int linha, int coluna) {
            return false;
        }
    };
    private final JTable tabelaInstrutores = new JTable(modeloTabela);

    private List<Instrutor> instrutoresExibidos = new ArrayList<>();
    private String crefFiltroAtivo;

    public InstrutorView(IInstrutorController controller) {
        if (controller == null) {
            throw new IllegalArgumentException("O controller de instrutores é obrigatório.");
        }
        this.controller = controller;
        montarTela();
        recarregar();
    }

    private void montarTela() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        add(criarPainelFormulario(), BorderLayout.NORTH);
        add(criarPainelTabela(), BorderLayout.CENTER);
    }

    private JPanel criarPainelFormulario() {
        JPanel painel = new JPanel(new GridBagLayout());
        painel.setBorder(BorderFactory.createTitledBorder("Cadastro de instrutor"));

        txtCpf.setToolTipText("Informe 11 dígitos ou no formato 000.000.000-00");
        txtTelefone.setToolTipText("DDD + número (10 ou 11 dígitos)");
        txtCref.setToolTipText("Formato esperado: 000000-G/UF ou 000000-P/UF");

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

        // Linha 1: Telefone e CREF
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
        painel.add(new JLabel("CREF:"), gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.6;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        painel.add(txtCref, gbc);

        // Linha 2: Especialidade e ações do formulário
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        painel.add(new JLabel("Especialidade:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        painel.add(txtEspecialidade, gbc);

        JPanel botoesFormulario = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btnLimpar = new JButton("Limpar");
        btnLimpar.addActionListener(e -> limparFormulario());
        JButton btnCadastrar = new JButton("Cadastrar");
        btnCadastrar.addActionListener(e -> cadastrarInstrutor());
        botoesFormulario.add(btnLimpar);
        botoesFormulario.add(btnCadastrar);

        gbc.gridx = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        painel.add(botoesFormulario, gbc);

        return painel;
    }

    private JPanel criarPainelTabela() {
        tabelaInstrutores.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel painel = new JPanel(new BorderLayout(0, 8));
        painel.setBorder(BorderFactory.createTitledBorder("Instrutores cadastrados"));

        JPanel barraBusca = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton btnBuscar = new JButton("Buscar por CREF");
        btnBuscar.addActionListener(e -> buscarPorCref());
        txtBuscaCref.addActionListener(e -> buscarPorCref());
        JButton btnListarTodos = new JButton("Listar todos");
        btnListarTodos.addActionListener(e -> listarTodos());
        barraBusca.add(new JLabel("CREF:"));
        barraBusca.add(txtBuscaCref);
        barraBusca.add(btnBuscar);
        barraBusca.add(btnListarTodos);

        JPanel botoesAcao = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton btnAtualizarEspecialidade = new JButton("Atualizar especialidade");
        btnAtualizarEspecialidade.addActionListener(e -> atualizarEspecialidade());
        JButton btnRemover = new JButton("Remover instrutor");
        btnRemover.addActionListener(e -> removerInstrutor());
        botoesAcao.add(btnAtualizarEspecialidade);
        botoesAcao.add(btnRemover);

        painel.add(barraBusca, BorderLayout.NORTH);
        painel.add(new JScrollPane(tabelaInstrutores), BorderLayout.CENTER);
        painel.add(botoesAcao, BorderLayout.SOUTH);
        return painel;
    }

    public void recarregar() {
        Instrutor selecionado = instrutorSelecionado();
        String crefSelecionado = selecionado != null ? selecionado.getCref() : null;
        if (crefFiltroAtivo != null) {
            try {
                Instrutor encontrado = controller.buscarPorCref(crefFiltroAtivo);
                preencherTabela(List.of(encontrado), encontrado.getCref());
                return;
            } catch (DadosInvalidosException e) {
                crefFiltroAtivo = null;
                txtBuscaCref.setText("");
            } catch (RuntimeException e) {
                mostrarErroInesperado(e);
                return;
            }
        }
        try {
            preencherTabela(controller.listar(), crefSelecionado);
        } catch (RuntimeException e) {
            mostrarErroInesperado(e);
        }
    }

    private void cadastrarInstrutor() {
        try {
            Instrutor instrutor = controller.cadastrar(
                    txtNome.getText(),
                    txtCpf.getText(),
                    txtTelefone.getText(),
                    txtCref.getText(),
                    txtEspecialidade.getText());
            limparFormulario();
            crefFiltroAtivo = null;
            txtBuscaCref.setText("");
            preencherTabela(controller.listar(), instrutor.getCref());
            JOptionPane.showMessageDialog(
                    this,
                    "Instrutor \"" + instrutor.getNome() + "\" (" + instrutor.getCref() + ") cadastrado com sucesso.",
                    "Cadastro realizado",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (DadosInvalidosException e) {
            mostrarAviso(e);
        } catch (RuntimeException e) {
            mostrarErroInesperado(e);
        }
    }

    private void buscarPorCref() {
        try {
            Instrutor instrutor = controller.buscarPorCref(txtBuscaCref.getText());
            crefFiltroAtivo = instrutor.getCref();
            preencherTabela(List.of(instrutor), instrutor.getCref());
        } catch (DadosInvalidosException e) {
            mostrarAviso(e);
        } catch (RuntimeException e) {
            mostrarErroInesperado(e);
        }
    }

    private void listarTodos() {
        crefFiltroAtivo = null;
        txtBuscaCref.setText("");
        recarregar();
    }

    private void atualizarEspecialidade() {
        Instrutor instrutor = instrutorSelecionado();
        if (instrutor == null) {
            JOptionPane.showMessageDialog(this, "Selecione um instrutor na tabela.");
            return;
        }
        String novaEspecialidade = (String) JOptionPane.showInputDialog(
                this,
                "Nova especialidade para " + instrutor.getNome() + ":",
                "Atualizar especialidade",
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                instrutor.getEspecialidade());
        if (novaEspecialidade == null) {
            return;
        }
        try {
            controller.atualizarEspecialidade(instrutor.getCref(), novaEspecialidade);
            recarregar();
        } catch (DadosInvalidosException e) {
            mostrarAviso(e);
        } catch (RuntimeException e) {
            mostrarErroInesperado(e);
        }
    }

    private void removerInstrutor() {
        Instrutor instrutor = instrutorSelecionado();
        if (instrutor == null) {
            JOptionPane.showMessageDialog(this, "Selecione um instrutor na tabela.");
            return;
        }
        int resposta = JOptionPane.showConfirmDialog(
                this,
                "Remover o instrutor \"" + instrutor.getNome() + "\" (CREF " + instrutor.getCref() + ")?",
                "Confirmar remoção",
                JOptionPane.YES_NO_OPTION);
        if (resposta != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            controller.remover(instrutor.getCref());
            if (instrutor.getCref().equals(crefFiltroAtivo)) {
                crefFiltroAtivo = null;
                txtBuscaCref.setText("");
            }
            recarregar();
        } catch (DadosInvalidosException e) {
            mostrarAviso(e);
        } catch (RuntimeException e) {
            mostrarErroInesperado(e);
        }
    }

    private void preencherTabela(List<Instrutor> instrutores, String crefParaSelecionar) {
        this.instrutoresExibidos = new ArrayList<>(instrutores);
        modeloTabela.setRowCount(0);
        for (Instrutor instrutor : instrutoresExibidos) {
            modeloTabela.addRow(new Object[]{
                    instrutor.getId(),
                    instrutor.getNome(),
                    AlunoView.formatarCpf(instrutor.getCpf()),
                    AlunoView.formatarTelefone(instrutor.getTelefone()),
                    instrutor.getCref(),
                    instrutor.getEspecialidade()});
        }
        if (crefParaSelecionar != null) {
            for (int i = 0; i < instrutoresExibidos.size(); i++) {
                if (instrutoresExibidos.get(i).getCref().equals(crefParaSelecionar)) {
                    tabelaInstrutores.setRowSelectionInterval(i, i);
                    break;
                }
            }
        }
    }

    private void limparFormulario() {
        txtNome.setText("");
        txtCpf.setText("");
        txtTelefone.setText("");
        txtCref.setText("");
        txtEspecialidade.setText("");
    }

    private Instrutor instrutorSelecionado() {
        int linha = tabelaInstrutores.getSelectedRow();
        if (linha < 0 || linha >= instrutoresExibidos.size()) {
            return null;
        }
        return instrutoresExibidos.get(linha);
    }

    private void mostrarAviso(DadosInvalidosException e) {
        JOptionPane.showMessageDialog(this, e.getMessage(), "Dados inválidos", JOptionPane.WARNING_MESSAGE);
    }

    private void mostrarErroInesperado(RuntimeException e) {
        JOptionPane.showMessageDialog(this, "Erro inesperado: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
    }
}
