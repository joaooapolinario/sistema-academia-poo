package br.ufal.academia.dao;

import br.ufal.academia.model.entities.Frequencia;
import br.ufal.academia.model.entities.Matricula;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Implementação JDBC/SQLite do contrato FrequenciaDAO.
// A Matricula de cada frequência é resolvida pelo MatriculaDAO.
public class FrequenciaDAOImpl implements FrequenciaDAO {

    private static final String COLUNAS = "id, matricula_id, data_hora_entrada";

    // Linha crua da tabela, lida antes de resolver a Matricula associada.
    private record Linha(int id, int matriculaId, String dataHoraEntrada) {
    }

    private final ConnectionFactory connectionFactory;
    private final MatriculaDAO matriculaDAO;

    public FrequenciaDAOImpl(MatriculaDAO matriculaDAO) {
        this(ConnectionFactory.getInstancia(), matriculaDAO);
    }

    // Permite injetar outra fábrica (ex.: banco temporário nos testes).
    FrequenciaDAOImpl(ConnectionFactory connectionFactory, MatriculaDAO matriculaDAO) {
        if (connectionFactory == null || matriculaDAO == null) {
            throw new IllegalArgumentException("ConnectionFactory e MatriculaDAO são obrigatórios.");
        }
        this.connectionFactory = connectionFactory;
        this.matriculaDAO = matriculaDAO;
    }

    @Override
    public void salvar(Frequencia frequencia) {
        exigirFrequencia(frequencia);
        String sql = "INSERT INTO frequencias (matricula_id, data_hora_entrada) VALUES (?, ?)";

        try (Connection conn = connectionFactory.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preencherCampos(ps, frequencia);
            ps.executeUpdate();

            try (ResultSet chaves = ps.getGeneratedKeys()) {
                if (chaves.next()) {
                    frequencia.atribuirId(chaves.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao salvar frequência da matrícula "
                    + frequencia.getMatricula().getId() + " (a matrícula precisa estar salva antes).", e);
        }
    }

    @Override
    public void atualizar(Frequencia frequencia) {
        exigirFrequencia(frequencia);
        String sql = "UPDATE frequencias SET matricula_id = ?, data_hora_entrada = ? WHERE id = ?";

        try (Connection conn = connectionFactory.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            preencherCampos(ps, frequencia);
            ps.setInt(3, frequencia.getId());

            if (ps.executeUpdate() == 0) {
                throw new IllegalArgumentException("Frequência com id " + frequencia.getId() + " não encontrada.");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao atualizar frequência com id " + frequencia.getId() + ".", e);
        }
    }

    @Override
    public void remover(int id) {
        String sql = "DELETE FROM frequencias WHERE id = ?";

        try (Connection conn = connectionFactory.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao remover frequência com id " + id + ".", e);
        }
    }

    @Override
    public Optional<Frequencia> buscarPorId(int id) {
        return buscarUma("SELECT " + COLUNAS + " FROM frequencias WHERE id = ?", ps -> ps.setInt(1, id),
                "Erro ao buscar frequência com id " + id + ".");
    }

    @Override
    public List<Frequencia> listarTodos() {
        return buscarVarias("SELECT " + COLUNAS + " FROM frequencias ORDER BY data_hora_entrada, id", ps -> { },
                "Erro ao listar frequências.");
    }

    @Override
    public List<Frequencia> listarPorAlunoEPeriodo(int idAluno, LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null) {
            throw new IllegalArgumentException("As datas de início e fim do período são obrigatórias.");
        }
        if (fim.isBefore(inicio)) {
            throw new IllegalArgumentException("A data de fim não pode ser anterior à data de início.");
        }
        String sql = "SELECT f.id, f.matricula_id, f.data_hora_entrada "
                + "FROM frequencias f "
                + "INNER JOIN matriculas m ON m.id = f.matricula_id "
                + "WHERE m.aluno_id = ? AND substr(f.data_hora_entrada, 1, 10) BETWEEN ? AND ? "
                + "ORDER BY f.data_hora_entrada, f.id";

        return buscarVarias(sql, ps -> {
            ps.setInt(1, idAluno);
            ps.setString(2, inicio.toString()); // ISO-8601 (yyyy-MM-dd)
            ps.setString(3, fim.toString());
        }, "Erro ao listar frequências do aluno " + idAluno + ".");
    }

    @FunctionalInterface
    private interface Parametros {
        void aplicar(PreparedStatement ps) throws SQLException;
    }

    private Optional<Frequencia> buscarUma(String sql, Parametros parametros, String mensagemErro) {
        List<Frequencia> resultado = buscarVarias(sql, parametros, mensagemErro);
        return resultado.isEmpty() ? Optional.empty() : Optional.of(resultado.get(0));
    }

    // Lê as linhas, fecha a conexão e só então resolve a Matricula pelo DAO.
    private List<Frequencia> buscarVarias(String sql, Parametros parametros, String mensagemErro) {
        List<Linha> linhas = new ArrayList<>();

        try (Connection conn = connectionFactory.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            parametros.aplicar(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    linhas.add(new Linha(
                            rs.getInt("id"),
                            rs.getInt("matricula_id"),
                            rs.getString("data_hora_entrada")));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(mensagemErro, e);
        }

        List<Frequencia> frequencias = new ArrayList<>();
        for (Linha linha : linhas) {
            frequencias.add(mapear(linha));
        }
        return frequencias;
    }

    private Frequencia mapear(Linha linha) {
        Matricula matricula = matriculaDAO.buscarPorId(linha.matriculaId()).orElseThrow(() -> new IllegalStateException(
                "Matrícula " + linha.matriculaId() + " da frequência " + linha.id() + " não encontrada."));

        try {
            return Frequencia.reconstituir(
                    linha.id(),
                    matricula,
                    LocalDateTime.parse(linha.dataHoraEntrada()));
        } catch (Exception e) {
            throw new IllegalStateException("Registro de frequência inválido no banco (id " + linha.id() + ").", e);
        }
    }

    private void preencherCampos(PreparedStatement ps, Frequencia frequencia) throws SQLException {
        ps.setInt(1, frequencia.getMatricula().getId());
        ps.setString(2, DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(frequencia.getDataHoraEntrada()));
    }

    private void exigirFrequencia(Frequencia frequencia) {
        if (frequencia == null) {
            throw new IllegalArgumentException("A frequência não pode ser nula.");
        }
    }
}
