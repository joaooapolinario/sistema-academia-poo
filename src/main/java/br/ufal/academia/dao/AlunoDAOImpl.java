package br.ufal.academia.dao;

import br.ufal.academia.exception.DadosInvalidosException;
import br.ufal.academia.model.entities.Aluno;
import br.ufal.academia.model.enums.ObjetivoTreino;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// DAO do Aluno usando SQLite. As restrições médicas ficam numa tabela separada
public class AlunoDAOImpl implements AlunoDAO {

    private static final String COLUNAS = "id, nome, cpf, telefone, data_nascimento, objetivo";
    private final ConnectionFactory connectionFactory;

    public AlunoDAOImpl() {
        this(ConnectionFactory.getInstancia());
    }

    // Construtor para os testes
    AlunoDAOImpl(ConnectionFactory connectionFactory) {
        if (connectionFactory == null) {
            throw new IllegalArgumentException("A ConnectionFactory não pode ser nula.");
        }
        this.connectionFactory = connectionFactory;
    }

    // Usa transação para salvar o aluno e as restrições juntos (se der erro, desfaz tudo)
    @Override
    public void salvar(Aluno aluno) {
        exigirAluno(aluno);
        if (aluno.getId() != 0) {
            throw new IllegalArgumentException("O aluno já foi salvo (id " + aluno.getId() + "). Use atualizar.");
        }
        String sql = "INSERT INTO alunos (nome, cpf, telefone, data_nascimento, objetivo) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = connectionFactory.getConexao()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                preencherCampos(ps, aluno);
                ps.executeUpdate();

                int idGerado;
                try (ResultSet chaves = ps.getGeneratedKeys()) {
                    if (!chaves.next()) {
                        throw new SQLException("O banco não retornou o id gerado.");
                    }
                    idGerado = chaves.getInt(1);
                }
                inserirRestricoes(conn, idGerado, aluno.getRestricoesMedicas());
                conn.commit();
                aluno.atribuirId(idGerado);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao salvar aluno com CPF " + aluno.getCpf()
                    + " (verifique se o CPF já está cadastrado).", e);
        }
    }

    // Atualiza os dados e regrava as restrições do aluno
    @Override
    public void atualizar(Aluno aluno) {
        exigirAluno(aluno);
        if (aluno.getId() <= 0) {
            throw new IllegalArgumentException("O aluno ainda não foi salvo. Use salvar.");
        }
        String sql = "UPDATE alunos SET nome = ?, cpf = ?, telefone = ?, data_nascimento = ?, objetivo = ? "
                + "WHERE id = ?";

        try (Connection conn = connectionFactory.getConexao()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                preencherCampos(ps, aluno);
                ps.setInt(6, aluno.getId());
                if (ps.executeUpdate() == 0) {
                    throw new IllegalArgumentException("Aluno com id " + aluno.getId() + " não encontrado.");
                }
                removerRestricoes(conn, aluno.getId());
                inserirRestricoes(conn, aluno.getId(), aluno.getRestricoesMedicas());
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao atualizar aluno com id " + aluno.getId() + ".", e);
        }
    }

    // o banco apaga as restrições, matrículas e treinos do aluno de forma automática
    @Override
    public void remover(int id) {
        String sql = "DELETE FROM alunos WHERE id = ?";

        try (Connection conn = connectionFactory.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao remover aluno com id " + id + ".", e);
        }
    }

    @Override
    public Optional<Aluno> buscarPorId(int id) {
        String sql = "SELECT " + COLUNAS + " FROM alunos WHERE id = ?";

        try (Connection conn = connectionFactory.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(conn, rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar aluno com id " + id + ".", e);
        }
    }

    @Override
    public List<Aluno> listarTodos() {
        String sql = "SELECT " + COLUNAS + " FROM alunos ORDER BY nome, id";
        List<Aluno> alunos = new ArrayList<>();

        try (Connection conn = connectionFactory.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                alunos.add(mapear(conn, rs));
            }
            return alunos;
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao listar alunos.", e);
        }
    }

    // Busca pelo CPF (aceita com ou sem pontuação)
    @Override
    public Optional<Aluno> buscarPorCpf(String cpf) {
        if (cpf == null) {
            throw new IllegalArgumentException("O CPF não pode ser nulo.");
        }
        String sql = "SELECT " + COLUNAS + " FROM alunos WHERE cpf = ?";

        try (Connection conn = connectionFactory.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cpf.replaceAll("\\D", ""));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(conn, rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar aluno com CPF " + cpf + ".", e);
        }
    }

    private void preencherCampos(PreparedStatement ps, Aluno aluno) throws SQLException {
        ps.setString(1, aluno.getNome());
        ps.setString(2, aluno.getCpf());
        ps.setString(3, aluno.getTelefone());
        ps.setString(4, aluno.getDataNascimento().toString());
        ps.setString(5, aluno.getObjetivo().name());
    }

    private void inserirRestricoes(Connection conn, int alunoId, List<String> restricoes) throws SQLException {
        String sql = "INSERT INTO restricoes_medicas (aluno_id, descricao) VALUES (?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String descricao : restricoes) {
                ps.setInt(1, alunoId);
                ps.setString(2, descricao);
                ps.executeUpdate();
            }
        }
    }

    private void removerRestricoes(Connection conn, int alunoId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM restricoes_medicas WHERE aluno_id = ?")) {
            ps.setInt(1, alunoId);
            ps.executeUpdate();
        }
    }

    private List<String> buscarRestricoes(Connection conn, int alunoId) throws SQLException {
        String sql = "SELECT descricao FROM restricoes_medicas WHERE aluno_id = ? ORDER BY id";
        List<String> restricoes = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, alunoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    restricoes.add(rs.getString("descricao"));
                }
            }
        }
        return restricoes;
    }

    // Cria a classe Aluno pelo construtor para passar pelas validações novamente
    private Aluno mapear(Connection conn, ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        try {
            Aluno aluno = new Aluno(
                    rs.getString("nome"),
                    rs.getString("cpf"),
                    rs.getString("telefone"),
                    LocalDate.parse(rs.getString("data_nascimento")),
                    ObjetivoTreino.valueOf(rs.getString("objetivo")));
            for (String restricao : buscarRestricoes(conn, id)) {
                aluno.adicionarRestricao(restricao);
            }
            aluno.atribuirId(id);
            return aluno;
        } catch (DadosInvalidosException | RuntimeException e) {
            throw new IllegalStateException("Registro de aluno inválido no banco (id " + id + ").", e);
        }
    }

    private void exigirAluno(Aluno aluno) {
        if (aluno == null) {
            throw new IllegalArgumentException("O aluno não pode ser nulo.");
        }
    }
}