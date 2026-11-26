package br.ufal.academia.view;

import br.ufal.academia.controller.IAlunoController;
import br.ufal.academia.controller.IMatriculaController;
import br.ufal.academia.exception.DadosInvalidosException;
import br.ufal.academia.exception.MatriculaAtivaExistenteException;
import br.ufal.academia.model.entities.Aluno;
import br.ufal.academia.model.entities.Matricula;
import br.ufal.academia.model.entities.Plano;
import br.ufal.academia.model.enums.StatusMatricula;
import br.ufal.academia.model.enums.TipoPlano;
import br.ufal.academia.model.patterns.observer.MatriculaObserver;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Tela de gestão de matrículas: abertura com preview de mensalidade, renovação, cancelamento,
// atualização de vencimentos, filtro de matrículas a vencer, indicador de faturamento mensal e Observer.
public class MatriculaView extends JPanel implements MatriculaObserver {

    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale LOCALE_BR = Locale.forLanguageTag("pt-BR");
    private static final Color COR_PREVIEW = new Color(29, 78, 216);
    private static final Color COR_FATURAMENTO = new Color(22, 128, 61);
    private static final Color COR_ALERTA = new Color(180, 35, 24);
    private static final Color COR_SECUNDARIA = new Color(102, 112, 133);

    private final IMatriculaController controller;
    private final IAlunoController alunoController;

    private final DefaultComboBoxModel<Aluno> modeloAlunos = new DefaultComboBoxModel<>();
    private final JComboBox<Aluno> cmbAluno = new JComboBox<>(modeloAlunos);
    private final DefaultComboBoxModel<Plano> modeloPlanos = new DefaultComboBoxModel<>();
    private final JComboBox<Plano> cmbPlano = new JComboBox<>(modeloPlanos);
    private final JCheckBox chkEstudante = new JCheckBox("Estudante (desconto)");
    private final JLabel lblPreviewValor = new JLabel();

    private final JSpinner spnDiasAVencer = new JSpinner(new SpinnerNumberModel(7, 0, 3650, 1));
    private final JLabel lblStatusFiltro = new JLabel("Exibindo todas as matrículas");

    private final DefaultTableModel modeloTabela = new DefaultTableModel(
            new Object[]{"ID", "Aluno", "CPF", "Plano", "Início", "Vencimento", "Status", "Cálculo", "Mensalidade"}, 0) {
        @Override
        public boolean isCellEditable(int linha, int coluna) {
            return false;
        }
    };
    private final JTable tabelaMatriculas = new JTable(modeloTabela);

    private final JLabel lblUltimoEvento = new JLabel("Observer: nenhuma alteração recente de matrícula.");
    private final JLabel lblFaturamentoMensal = new JLabel();

    private List<Matricula> matriculasExibidas = new ArrayList<>();
    private Integer filtroDiasAtivo;
    private volatile String ultimaNotificacao;

    public MatriculaView(IMatriculaController controller, IAlunoController alunoController) {
        if (controller == null) {
            throw new IllegalArgumentException("O controller de matrículas é obrigatório.");
        }
        if (alunoController == null) {
            throw new IllegalArgumentException("O controller de alunos é obrigatório.");
        }
        this.controller = controller;
        this.alunoController = alunoController;
        montarTela();
        recarregar();
    }

    private void montarTela() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        add(criarPainelAbertura(), BorderLayout.NORTH);
        add(criarPainelMatriculas(), BorderLayout.CENTER);
        add(criarPainelIndicadores(), BorderLayout.SOUTH);
    }

    private JPanel criarPainelAbertura() {
        JPanel painel = new JPanel(new GridBagLayout());
        painel.setBorder(BorderFactory.createTitledBorder("Abrir nova matrícula"));

        cmbAluno.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Aluno aluno) {
                    setText(aluno.getNome() + " — " + AlunoView.formatarCpf(aluno.getCpf())
                            + " (" + aluno.getIdade() + " anos)");
                } else {
                    setText("Nenhum aluno cadastrado");
                }
                return this;
            }
        });

        cmbPlano.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Plano plano) {
                    String unidade = plano.getDuracaoMeses() == 1 ? "mês" : "meses";
                    setText(plano.getNome() + " (" + plano.getDuracaoMeses() + " " + unidade
                            + " — " + formatarMoeda(plano.getValorMensal()) + "/mês)");
                } else {
                    setText("Selecione um plano");
                }
                return this;
            }
        });

        cmbPlano.setSelectedIndex(-1);
        cmbAluno.addActionListener(e -> atualizarPreview());
        cmbPlano.addActionListener(e -> atualizarPreview());
        chkEstudante.addActionListener(e -> atualizarPreview());

        JButton btnAtualizarAlunos = new JButton("Atualizar alunos");
        btnAtualizarAlunos.addActionListener(e -> recarregarAlunos());

        lblPreviewValor.setFont(lblPreviewValor.getFont().deriveFont(Font.BOLD, 13f));
        lblPreviewValor.setForeground(COR_PREVIEW);

        JButton btnMatricular = new JButton("Abrir matrícula");
        btnMatricular.addActionListener(e -> abrirMatricula());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.anchor = GridBagConstraints.WEST;

        // Linha 0: Seleção de aluno
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        painel.add(new JLabel("Aluno:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        painel.add(cmbAluno, gbc);

        gbc.gridx = 3;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        painel.add(btnAtualizarAlunos, gbc);

        // Linha 1: TipoPlano e Estudante
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        painel.add(new JLabel("Plano:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        painel.add(cmbPlano, gbc);

        gbc.gridx = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        painel.add(chkEstudante, gbc);

        // Linha 2: Preview do valor final e botão de abertura
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        painel.add(lblPreviewValor, gbc);

        gbc.gridx = 3;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE;
        painel.add(btnMatricular, gbc);

        return painel;
    }

    private JPanel criarPainelMatriculas() {
        tabelaMatriculas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel painel = new JPanel(new BorderLayout(0, 8));
        painel.setBorder(BorderFactory.createTitledBorder("Matrículas"));

        JPanel barraFiltro = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton btnFiltrarAVencer = new JButton("Filtrar a vencer");
        btnFiltrarAVencer.addActionListener(e -> filtrarAVencer());
        JButton btnMostrarTodas = new JButton("Mostrar todas");
        btnMostrarTodas.addActionListener(e -> mostrarTodas());
        lblStatusFiltro.setForeground(COR_SECUNDARIA);

        barraFiltro.add(new JLabel("Vencendo em até (dias):"));
        barraFiltro.add(spnDiasAVencer);
        barraFiltro.add(btnFiltrarAVencer);
        barraFiltro.add(btnMostrarTodas);
        barraFiltro.add(lblStatusFiltro);

        JPanel botoesAcao = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton btnRenovar = new JButton("Renovar");
        btnRenovar.addActionListener(e -> renovarMatricula());
        JButton btnCancelar = new JButton("Cancelar");
        btnCancelar.addActionListener(e -> cancelarMatricula());
        JButton btnAtualizarVencimentos = new JButton("Atualizar vencimentos");
        btnAtualizarVencimentos.addActionListener(e -> atualizarVencimentos());

        botoesAcao.add(btnRenovar);
        botoesAcao.add(btnCancelar);
        botoesAcao.add(btnAtualizarVencimentos);

        painel.add(barraFiltro, BorderLayout.NORTH);
        painel.add(new JScrollPane(tabelaMatriculas), BorderLayout.CENTER);
        painel.add(botoesAcao, BorderLayout.SOUTH);
        return painel;
    }

    private JPanel criarPainelIndicadores() {
        JPanel painel = new JPanel(new BorderLayout(12, 0));
        painel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Resumo e monitoramento"),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));

        lblUltimoEvento.setForeground(COR_SECUNDARIA);

        lblFaturamentoMensal.setFont(lblFaturamentoMensal.getFont().deriveFont(Font.BOLD, 14f));
        lblFaturamentoMensal.setForeground(COR_FATURAMENTO);

        painel.add(lblUltimoEvento, BorderLayout.CENTER);
        painel.add(lblFaturamentoMensal, BorderLayout.EAST);
        return painel;
    }

    // Reage a eventos de mudança de status das matrículas (vencimento, cancelamento, renovação).
    // Sempre agenda a atualização via invokeLater para garantir execução na EDT após a persistência no DAO.
    @Override
    public void atualizar(Matricula matricula) {
        if (matricula == null) {
            throw new IllegalArgumentException("A matrícula não pode ser nula.");
        }
        String mensagem = formatarEventoObserver(matricula);
        this.ultimaNotificacao = mensagem;

        SwingUtilities.invokeLater(() -> {
            lblUltimoEvento.setText(mensagem);
            lblUltimoEvento.setToolTipText(mensagem);
            lblUltimoEvento.setForeground(matricula.getStatus() == StatusMatricula.ATIVA ? COR_PREVIEW : COR_ALERTA);
            recarregarMatriculas(matricula.getId());
            atualizarFaturamento();
        });
    }

    public String getUltimaNotificacao() {
        return ultimaNotificacao;
    }

    public String getTextoPreview() {
        return lblPreviewValor.getText();
    }

    public String getTextoFaturamento() {
        return lblFaturamentoMensal.getText();
    }

    public void recarregar() {
        recarregarPlanos();
        recarregarAlunos();
        Matricula selecionada = matriculaSelecionada();
        int idSelecionado = selecionada != null ? selecionada.getId() : 0;
        recarregarMatriculas(idSelecionado);
        atualizarFaturamento();
        atualizarPreview();
    }

    private void recarregarPlanos() {
        Plano selecionadoAnterior = (Plano) cmbPlano.getSelectedItem();
        TipoPlano tipoSelecionado = selecionadoAnterior != null ? selecionadoAnterior.getTipo() : null;

        try {
            List<Plano> planos = controller.listarPlanos();
            modeloPlanos.removeAllElements();
            Plano paraSelecionar = null;
            for (Plano plano : planos) {
                modeloPlanos.addElement(plano);
                if (tipoSelecionado != null && plano.getTipo() == tipoSelecionado) {
                    paraSelecionar = plano;
                }
            }
            if (paraSelecionar != null) {
                cmbPlano.setSelectedItem(paraSelecionar);
            } else {
                cmbPlano.setSelectedIndex(-1);
            }
        } catch (RuntimeException e) {
            mostrarErroInesperado(e);
        }
    }

    private void recarregarAlunos() {
        Aluno selecionadoAnterior = (Aluno) cmbAluno.getSelectedItem();
        String cpfSelecionado = selecionadoAnterior != null ? selecionadoAnterior.getCpf() : null;

        try {
            List<Aluno> alunos = alunoController.listar();
            modeloAlunos.removeAllElements();
            Aluno paraSelecionar = null;
            for (Aluno aluno : alunos) {
                modeloAlunos.addElement(aluno);
                if (cpfSelecionado != null && aluno.getCpf().equals(cpfSelecionado)) {
                    paraSelecionar = aluno;
                }
            }
            if (paraSelecionar != null) {
                cmbAluno.setSelectedItem(paraSelecionar);
            } else if (!alunos.isEmpty()) {
                cmbAluno.setSelectedIndex(0);
            }
        } catch (RuntimeException e) {
            mostrarErroInesperado(e);
        }
        atualizarPreview();
    }

    private void atualizarPreview() {
        Plano plano = (Plano) cmbPlano.getSelectedItem();
        Aluno aluno = (Aluno) cmbAluno.getSelectedItem();
        lblPreviewValor.setText(formatarPreview(aluno, plano, chkEstudante.isSelected()));
    }

    String formatarPreview(Aluno aluno, Plano plano, boolean estudante) {
        if (plano == null) {
            return "Selecione um plano para calcular a mensalidade.";
        }
        if (aluno == null) {
            return "Selecione um aluno para calcular a mensalidade.";
        }
        try {
            double valorFinal = controller.simularMensalidade(aluno.getCpf(), plano.getTipo(), estudante);
            String unidade = plano.getDuracaoMeses() == 1 ? "mês" : "meses";
            return "Valor final: " + formatarMoeda(valorFinal) + "/mês"
                    + " (vigência de " + plano.getDuracaoMeses() + " " + unidade + ")";
        } catch (DadosInvalidosException | RuntimeException e) {
            return "Não foi possível calcular o valor do plano.";
        }
    }

    private void abrirMatricula() {
        Aluno aluno = (Aluno) cmbAluno.getSelectedItem();
        if (aluno == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Cadastre e selecione um aluno antes de abrir uma matrícula.",
                    "Aluno obrigatório",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        Plano plano = (Plano) cmbPlano.getSelectedItem();
        TipoPlano tipo = plano != null ? plano.getTipo() : null;
        try {
            Matricula matricula = controller.matricular(aluno.getCpf(), tipo, chkEstudante.isSelected());
            filtroDiasAtivo = null;
            lblStatusFiltro.setText("Exibindo todas as matrículas");
            recarregarMatriculas(matricula.getId());
            atualizarFaturamento();
            JOptionPane.showMessageDialog(
                    this,
                    "Matrícula #" + matricula.getId() + " aberta para " + aluno.getNome()
                            + " — mensalidade: " + formatarMoeda(matricula.calcularMensalidade())
                            + " (válida até " + matricula.getDataFim().format(FORMATO_DATA) + ").",
                    "Matrícula aberta",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (MatriculaAtivaExistenteException | DadosInvalidosException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Não foi possível matricular",
                    JOptionPane.WARNING_MESSAGE);
        } catch (RuntimeException e) {
            mostrarErroInesperado(e);
        }
    }

    private void filtrarAVencer() {
        int dias = (Integer) spnDiasAVencer.getValue();
        try {
            List<Matricula> aVencer = controller.listarAVencer(dias);
            filtroDiasAtivo = dias;
            lblStatusFiltro.setText("Exibindo ativas a vencer em até " + dias + " dia(s) (" + aVencer.size() + ")");
            preencherTabela(aVencer, 0);
        } catch (DadosInvalidosException e) {
            mostrarAviso(e);
        } catch (RuntimeException e) {
            mostrarErroInesperado(e);
        }
    }

    private void mostrarTodas() {
        filtroDiasAtivo = null;
        lblStatusFiltro.setText("Exibindo todas as matrículas");
        recarregarMatriculas(0);
    }

    private void renovarMatricula() {
        Matricula matricula = matriculaSelecionada();
        if (matricula == null) {
            JOptionPane.showMessageDialog(this, "Selecione uma matrícula na tabela.");
            return;
        }
        int resposta = JOptionPane.showConfirmDialog(
                this,
                "Renovar a matrícula #" + matricula.getId() + " de " + matricula.getAluno().getNome() + "?",
                "Confirmar renovação",
                JOptionPane.YES_NO_OPTION);
        if (resposta != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            controller.renovar(matricula.getId());
            recarregarMatriculas(matricula.getId());
            atualizarFaturamento();
        } catch (DadosInvalidosException e) {
            mostrarAviso(e);
        } catch (RuntimeException e) {
            mostrarErroInesperado(e);
        }
    }

    private void cancelarMatricula() {
        Matricula matricula = matriculaSelecionada();
        if (matricula == null) {
            JOptionPane.showMessageDialog(this, "Selecione uma matrícula na tabela.");
            return;
        }
        int resposta = JOptionPane.showConfirmDialog(
                this,
                "Cancelar a matrícula #" + matricula.getId() + " de " + matricula.getAluno().getNome() + "?",
                "Confirmar cancelamento",
                JOptionPane.YES_NO_OPTION);
        if (resposta != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            controller.cancelar(matricula.getId());
            recarregarMatriculas(matricula.getId());
            atualizarFaturamento();
        } catch (DadosInvalidosException e) {
            mostrarAviso(e);
        } catch (RuntimeException e) {
            mostrarErroInesperado(e);
        }
    }

    private void atualizarVencimentos() {
        try {
            controller.atualizarVencimentos();
            recarregarMatriculas(0);
            atualizarFaturamento();
            JOptionPane.showMessageDialog(
                    this,
                    "Verificação de vencimentos concluída.",
                    "Vencimentos atualizados",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (RuntimeException e) {
            mostrarErroInesperado(e);
        }
    }

    private void recarregarMatriculas(int idParaSelecionar) {
        try {
            List<Matricula> matriculas;
            if (filtroDiasAtivo != null) {
                matriculas = controller.listarAVencer(filtroDiasAtivo);
                lblStatusFiltro.setText("Exibindo ativas a vencer em até " + filtroDiasAtivo
                        + " dia(s) (" + matriculas.size() + ")");
            } else {
                matriculas = controller.listar();
                lblStatusFiltro.setText("Exibindo todas as matrículas (" + matriculas.size() + ")");
            }
            preencherTabela(matriculas, idParaSelecionar);
        } catch (DadosInvalidosException e) {
            mostrarAviso(e);
        } catch (RuntimeException e) {
            mostrarErroInesperado(e);
        }
    }

    private void preencherTabela(List<Matricula> matriculas, int idParaSelecionar) {
        this.matriculasExibidas = new ArrayList<>(matriculas);
        modeloTabela.setRowCount(0);
        for (Matricula matricula : matriculasExibidas) {
            modeloTabela.addRow(new Object[]{
                    matricula.getId(),
                    matricula.getAluno().getNome(),
                    AlunoView.formatarCpf(matricula.getAluno().getCpf()),
                    matricula.getPlano().getNome(),
                    matricula.getDataInicio().format(FORMATO_DATA),
                    matricula.getDataFim().format(FORMATO_DATA),
                    matricula.getStatus(),
                    matricula.getCalculoMensalidade().identificador(),
                    formatarMoeda(matricula.calcularMensalidade())});
        }
        if (idParaSelecionar > 0) {
            for (int i = 0; i < matriculasExibidas.size(); i++) {
                if (matriculasExibidas.get(i).getId() == idParaSelecionar) {
                    tabelaMatriculas.setRowSelectionInterval(i, i);
                    break;
                }
            }
        }
    }

    private void atualizarFaturamento() {
        try {
            double total = controller.calcularFaturamentoMensal();
            lblFaturamentoMensal.setText("Faturamento mensal: " + formatarMoeda(total));
        } catch (RuntimeException e) {
            mostrarErroInesperado(e);
        }
    }

    private Matricula matriculaSelecionada() {
        int linha = tabelaMatriculas.getSelectedRow();
        if (linha < 0 || linha >= matriculasExibidas.size()) {
            return null;
        }
        return matriculasExibidas.get(linha);
    }

    private String formatarEventoObserver(Matricula matricula) {
        return "Observer: matrícula #" + matricula.getId() + " (" + matricula.getAluno().getNome()
                + ") atualizada para " + matricula.getStatus()
                + " — vencimento em " + matricula.getDataFim().format(FORMATO_DATA) + ".";
    }

    private static String formatarMoeda(double valor) {
        return NumberFormat.getCurrencyInstance(LOCALE_BR).format(valor);
    }

    private void mostrarAviso(DadosInvalidosException e) {
        JOptionPane.showMessageDialog(this, e.getMessage(), "Dados inválidos", JOptionPane.WARNING_MESSAGE);
    }

    private void mostrarErroInesperado(RuntimeException e) {
        JOptionPane.showMessageDialog(this, "Erro inesperado: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
    }
}
