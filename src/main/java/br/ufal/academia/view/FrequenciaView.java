package br.ufal.academia.view;

import br.ufal.academia.controller.IFrequenciaController;
import br.ufal.academia.exception.AcessoNegadoException;
import br.ufal.academia.exception.DadosInvalidosException;
import br.ufal.academia.model.entities.Frequencia;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

// Tela da catraca: registra a entrada pelo CPF e mostra o histórico de presenças do mês
public class FrequenciaView extends JPanel {

    private static final DateTimeFormatter FORMATO_MES = DateTimeFormatter.ofPattern("MM/yyyy");
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final Color COR_LIBERADO = new Color(22, 128, 61);
    private static final Color COR_NEGADO = new Color(185, 28, 28);

    private final IFrequenciaController controller;

    private final JTextField txtCpfCatraca = new JTextField(14);
    private final JLabel lblResultado = new JLabel("Aguardando CPF...", SwingConstants.CENTER);

    private final JTextField txtCpfHistorico = new JTextField(14);
    private final JTextField txtMes = new JTextField(7);
    private final JLabel lblTotal = new JLabel("Presenças no mês: -");
    private final DefaultTableModel modeloTabela = new DefaultTableModel(new Object[]{"Data", "Hora"}, 0) {
        @Override
        public boolean isCellEditable(int linha, int coluna) {
            return false;
        }
    };

    public FrequenciaView(IFrequenciaController controller) {
        if (controller == null) {
            throw new IllegalArgumentException("O controller de frequência é obrigatório.");
        }
        this.controller = controller;
        montarTela();
    }

    private void montarTela() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        add(criarPainelCatraca(), BorderLayout.NORTH);
        add(criarPainelHistorico(), BorderLayout.CENTER);
    }

    private JPanel criarPainelCatraca() {
        JPanel painel = new JPanel(new BorderLayout(0, 10));
        painel.setBorder(BorderFactory.createTitledBorder("Catraca"));

        JPanel linha = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnRegistrar = new JButton("Registrar entrada");
        btnRegistrar.addActionListener(e -> registrarEntrada());
        txtCpfCatraca.addActionListener(e -> registrarEntrada());
        linha.add(new JLabel("CPF do aluno:"));
        linha.add(txtCpfCatraca);
        linha.add(btnRegistrar);

        lblResultado.setOpaque(true);
        lblResultado.setFont(lblResultado.getFont().deriveFont(Font.BOLD, 16f));
        lblResultado.setBorder(BorderFactory.createEmptyBorder(14, 10, 14, 10));

        painel.add(linha, BorderLayout.NORTH);
        painel.add(lblResultado, BorderLayout.CENTER);
        return painel;
    }

    private JPanel criarPainelHistorico() {
        JPanel painel = new JPanel(new BorderLayout(0, 8));
        painel.setBorder(BorderFactory.createTitledBorder("Histórico de presenças"));

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnConsultar = new JButton("Consultar");
        btnConsultar.addActionListener(e -> consultarHistorico());
        filtros.add(new JLabel("CPF:"));
        filtros.add(txtCpfHistorico);
        filtros.add(new JLabel("Mês (MM/AAAA):"));
        filtros.add(txtMes);
        filtros.add(btnConsultar);

        lblTotal.setFont(lblTotal.getFont().deriveFont(Font.BOLD));

        painel.add(filtros, BorderLayout.NORTH);
        painel.add(new JScrollPane(new JTable(modeloTabela)), BorderLayout.CENTER);
        painel.add(lblTotal, BorderLayout.SOUTH);
        return painel;
    }

    private void registrarEntrada() {
        try {
            Frequencia frequencia = controller.registrarEntrada(txtCpfCatraca.getText());
            String nome = frequencia.getMatricula().getAluno().getNome();
            String hora = frequencia.getDataHoraEntrada().format(FORMATO_HORA);
            mostrarResultado("ACESSO LIBERADO - " + nome + " (" + hora + ")", COR_LIBERADO);
            txtCpfCatraca.setText("");
        } catch (AcessoNegadoException e) {
            mostrarResultado("ACESSO NEGADO", COR_NEGADO);
            JOptionPane.showMessageDialog(this, e.getMessage(), "Acesso negado", JOptionPane.WARNING_MESSAGE);
        } catch (DadosInvalidosException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Dados inválidos", JOptionPane.WARNING_MESSAGE);
        } catch (RuntimeException e) {
            mostrarErroInesperado(e);
        }
    }

    private void consultarHistorico() {
        YearMonth mes;
        try {
            mes = YearMonth.parse(txtMes.getText().trim(), FORMATO_MES);
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Informe o mês no formato MM/AAAA, ex.: 11/2026.",
                    "Mês inválido", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            List<Frequencia> frequencias = controller.listarPorAluno(txtCpfHistorico.getText(), mes);
            modeloTabela.setRowCount(0);
            for (Frequencia frequencia : frequencias) {
                modeloTabela.addRow(new Object[]{
                        frequencia.getDataHoraEntrada().format(FORMATO_DATA),
                        frequencia.getDataHoraEntrada().format(FORMATO_HORA)});
            }
            lblTotal.setText("Presenças no mês: " + controller.contarPresencas(txtCpfHistorico.getText(), mes));
        } catch (DadosInvalidosException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Dados inválidos", JOptionPane.WARNING_MESSAGE);
        } catch (RuntimeException e) {
            mostrarErroInesperado(e);
        }
    }

    private void mostrarResultado(String texto, Color cor) {
        lblResultado.setText(texto);
        lblResultado.setBackground(cor);
        lblResultado.setForeground(Color.WHITE);
    }

    private void mostrarErroInesperado(RuntimeException e) {
        JOptionPane.showMessageDialog(this, "Erro inesperado: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
    }
}