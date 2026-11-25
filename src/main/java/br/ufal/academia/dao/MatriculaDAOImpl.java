package br.ufal.academia.dao;

import br.ufal.academia.model.entities.Aluno;
import br.ufal.academia.model.entities.Matricula;
import br.ufal.academia.model.entities.Plano;
import br.ufal.academia.model.enums.StatusMatricula;
import br.ufal.academia.model.patterns.strategy.CalculoMensalidadeStrategy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Implementação JDBC/SQLite do contrato MatriculaDAO.
// O Aluno e o Plano da matrícula são carregados pelos respectivos DAOs.
public class MatriculaDAOImpl implements MatriculaDAO {

    private static final String COLUNAS =
            "id, aluno_id, plano_id, data_inicio, data_fim, status, tipo_calculo";

    // Linha crua da tabela, lida antes de resolver Aluno e Plano.
    private record Linha(int id, int alunoId, int planoId, String dataInicio, String dataFim,
                         String status, String tipoCalculo) {
    }

    private final ConnectionFactory connectionFactory;
    private final AlunoDAO alunoDAO;
    private final PlanoDAO planoDAO;

    public MatriculaDAOImpl(AlunoDAO alunoDAO, PlanoDAO planoDAO) {
        this(ConnectionFactory.getInstancia(), alunoDAO, planoDAO);
    }

    // Permite injetar outra fábrica (ex.: banco temporário nos testes).
    MatriculaDAOImpl(ConnectionFactory connectionFactory, AlunoDAO alunoDAO, PlanoDAO planoDAO) {
        if (connectionFactory == null || alunoDAO == null || planoDAO == null) {
            throw new IllegalArgumentException("ConnectionFactory, AlunoDAO e PlanoDAO são obrigatórios.");
        }
        this.connectionFactory = connectionFactory;
        this.alunoDAO = alunoDAO;
        this.planoDAO = planoDAO;
    }

    @Override
    public void salvar(Matricula matricula) {
        exigirMatricula(matricula);
        if (matricula.getId() != 0) {
            throw new IllegalArgumentException(
                    "A matrícula já foi salva (id " + matricula.getId() + "). Use atualizar.");
        }
        String sql = "INSERT INTO matriculas (aluno_id, plano_id, data_inicio, data_fim, status, tipo_calculo) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = connectionFactory.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preencherCampos(ps, matricula);
            ps.executeUpdate();

            try (ResultSet chaves = ps.getGeneratedKeys()) {
                if (chaves.next()) {
                    matricula.atribuirId(chaves.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao salvar matrícula do aluno " + matricula.getAluno().getId()
                    + " (o aluno e o plano precisam estar salvos antes).", e);
        }
    }

    @Override
    public void atualizar(Matricula matricula) {
        exigirMatricula(matricula);
        if (matricula.getId() <= 0) {
            throw new IllegalArgumentException("A matrícula ainda não foi salva. Use salvar.");
        }
        String sql = "UPDATE matriculas SET aluno_id = ?, plano_id = ?, data_inicio = ?, data_fim = ?, "
                + "status = ?, tipo_calculo = ? WHERE id = ?";

        try (Connection conn = connectionFactory.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            preencherCampos(ps, matricula);
            ps.setInt(7, matricula.getId());

            if (ps.executeUpdate() == 0) {
                throw new IllegalArgumentException("Matrícula com id " + matricula.getId() + " não encontrada.");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao atualizar matrícula com id " + matricula.getId() + ".", e);
        }
    }

    @Override
    public void remover(int id) {
        String sql = "DELETE FROM matriculas WHERE id = ?";

        try (Connection conn = connectionFactory.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao remover matrícula com id " + id + ".", e);
        }
    }

    @Override
    public Optional<Matricula> buscarPorId(int id) {
        return buscarUma("SELECT " + COLUNAS + " FROM matriculas WHERE id = ?", ps -> ps.setInt(1, id),
                "Erro ao buscar matrícula com id " + id + ".");
    }

    @Override
    public List<Matricula> listarTodos() {
        return buscarVarias("SELECT " + COLUNAS + " FROM matriculas ORDER BY id", ps -> { },
                "Erro ao listar matrículas.");
    }

    @Override
    public Optional<Matricula> buscarAtivaPorAluno(int idAluno) {
        String sql = "SELECT " + COLUNAS + " FROM matriculas WHERE aluno_id = ? AND status = ? "
                + "ORDER BY data_fim DESC, id DESC LIMIT 1";
        return buscarUma(sql, ps -> {
            ps.setInt(1, idAluno);
            ps.setString(2, StatusMatricula.ATIVA.name());
        }, "Erro ao buscar matrícula ativa do aluno " + idAluno + ".");
    }

    @Override
    public List<Matricula> listarPorStatus(StatusMatricula status) {
        if (status == null) {
            throw new IllegalArgumentException("O status não pode ser nulo.");
        }
        return buscarVarias("SELECT " + COLUNAS + " FROM matriculas WHERE status = ? ORDER BY id",
                ps -> ps.setString(1, status.name()), "Erro ao listar matrículas com status " + status + ".");
    }

    @FunctionalInterface
    private interface Parametros {
        void aplicar(PreparedStatement ps) throws SQLException;
    }

    private Optional<Matricula> buscarUma(String sql, Parametros parametros, String mensagemErro) {
        List<Matricula> resultado = buscarVarias(sql, parametros, mensagemErro);
        return resultado.isEmpty() ? Optional.empty() : Optional.of(resultado.get(0));
    }

    // Lê as linhas, fecha a conexão e só então resolve Aluno e Plano pelos DAOs.
    private List<Matricula> buscarVarias(String sql, Parametros parametros, String mensagemErro) {
        List<Linha> linhas = new ArrayList<>();

        try (Connection conn = connectionFactory.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            parametros.aplicar(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    linhas.add(new Linha(
                            rs.getInt("id"),
                            rs.getInt("aluno_id"),
                            rs.getInt("plano_id"),
                            rs.getString("data_inicio"),
                            rs.getString("data_fim"),
                            rs.getString("status"),
                            rs.getString("tipo_calculo")));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(mensagemErro, e);
        }

        List<Matricula> matriculas = new ArrayList<>();
        for (Linha linha : linhas) {
            matriculas.add(mapear(linha));
        }
        return matriculas;
    }

    private Matricula mapear(Linha linha) {
        Aluno aluno = alunoDAO.buscarPorId(linha.alunoId()).orElseThrow(() -> new IllegalStateException(
                "Aluno " + linha.alunoId() + " da matrícula " + linha.id() + " não encontrado."));
        Plano plano = planoDAO.buscarPorId(linha.planoId()).orElseThrow(() -> new IllegalStateException(
                "Plano " + linha.planoId() + " da matrícula " + linha.id() + " não encontrado."));

        try {
            return Matricula.reconstituir(
                    linha.id(),
                    aluno,
                    plano,
                    LocalDate.parse(linha.dataInicio()),
                    LocalDate.parse(linha.dataFim()),
                    StatusMatricula.valueOf(linha.status()),
                    CalculoMensalidadeStrategy.deIdentificador(linha.tipoCalculo()));
        } catch (Exception e) {
            throw new IllegalStateException("Registro de matrícula inválido no banco (id " + linha.id() + ").", e);
        }
    }

    private void preencherCampos(PreparedStatement ps, Matricula matricula) throws SQLException {
        ps.setInt(1, matricula.getAluno().getId());
        ps.setInt(2, matricula.getPlano().getId());
        ps.setString(3, matricula.getDataInicio().toString()); // ISO-8601 (yyyy-MM-dd)
        ps.setString(4, matricula.getDataFim().toString());
        ps.setString(5, matricula.getStatus().name());
        ps.setString(6, matricula.getCalculoMensalidade().identificador());
    }

    private void exigirMatricula(Matricula matricula) {
        if (matricula == null) {
            throw new IllegalArgumentException("A matrícula não pode ser nula.");
        }
    }
}
