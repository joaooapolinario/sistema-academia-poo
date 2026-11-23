package br.ufal.academia.dao;

import br.ufal.academia.exception.DadosInvalidosException;
import br.ufal.academia.model.entities.Instrutor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InstrutorDAOImpl implements InstrutorDAO {

    private static final String COLUNAS = "id, nome, cpf, telefone, cref, especialidade";
    private final ConnectionFactory connectionFactory;

    public InstrutorDAOImpl() {
        this(ConnectionFactory.getInstancia());
    }

    // Construtor para os testes
    InstrutorDAOImpl(ConnectionFactory connectionFactory) {
        if (connectionFactory == null) {
            throw new IllegalArgumentException("A ConnectionFactory não pode ser nula.");
        }
        this.connectionFactory = connectionFactory;
    }

    @Override
    public void salvar(Instrutor instrutor) {
        exigirInstrutor(instrutor);
        if (instrutor.getId() != 0) {
            throw new IllegalArgumentException("O instrutor já foi salvo (id " + instrutor.getId() + "). Use atualizar.");
        }
        String sql = "INSERT INTO instrutores (nome, cpf, telefone, cref, especialidade) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = connectionFactory.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preencherCampos(ps, instrutor);
            ps.executeUpdate();

            try (ResultSet chaves = ps.getGeneratedKeys()) {
                if (chaves.next()) {
                    instrutor.atribuirId(chaves.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao salvar instrutor com CREF " + instrutor.getCref()
                    + " (verifique se o CPF ou o CREF já estão cadastrados).", e);
        }
    }

    @Override
    public void atualizar(Instrutor instrutor) {
        exigirInstrutor(instrutor);
        if (instrutor.getId() <= 0) {
            throw new IllegalArgumentException("O instrutor ainda não foi salvo. Use salvar.");
        }
        String sql = "UPDATE instrutores SET nome = ?, cpf = ?, telefone = ?, cref = ?, especialidade = ? "
                + "WHERE id = ?";

        try (Connection conn = connectionFactory.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            preencherCampos(ps, instrutor);
            ps.setInt(6, instrutor.getId());

            if (ps.executeUpdate() == 0) {
                throw new IllegalArgumentException("Instrutor com id " + instrutor.getId() + " não encontrado.");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao atualizar instrutor com id " + instrutor.getId() + ".", e);
        }
    }

    // Instrutor com treinos cadastrados não pode ser removido
    @Override
    public void remover(int id) {
        String sql = "DELETE FROM instrutores WHERE id = ?";

        try (Connection conn = connectionFactory.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao remover instrutor com id " + id
                    + " (verifique se existem treinos vinculados).", e);
        }
    }

    @Override
    public Optional<Instrutor> buscarPorId(int id) {
        String sql = "SELECT " + COLUNAS + " FROM instrutores WHERE id = ?";

        try (Connection conn = connectionFactory.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar instrutor com id " + id + ".", e);
        }
    }

    @Override
    public List<Instrutor> listarTodos() {
        String sql = "SELECT " + COLUNAS + " FROM instrutores ORDER BY nome, id";
        List<Instrutor> instrutores = new ArrayList<>();

        try (Connection conn = connectionFactory.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                instrutores.add(mapear(rs));
            }
            return instrutores;
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao listar instrutores.", e);
        }
    }

    // Busca pelo CREF (aceita com letras minúsculas ou espaços)
    @Override
    public Optional<Instrutor> buscarPorCref(String cref) {
        if (cref == null) {
            throw new IllegalArgumentException("O CREF não pode ser nulo.");
        }
        String sql = "SELECT " + COLUNAS + " FROM instrutores WHERE cref = ?";

        try (Connection conn = connectionFactory.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, cref.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar instrutor com CREF " + cref + ".", e);
        }
    }

    private void preencherCampos(PreparedStatement ps, Instrutor instrutor) throws SQLException {
        ps.setString(1, instrutor.getNome());
        ps.setString(2, instrutor.getCpf());
        ps.setString(3, instrutor.getTelefone());
        ps.setString(4, instrutor.getCref());
        ps.setString(5, instrutor.getEspecialidade());
    }

    // Cria o Instrutor pelo construtor para passar pelas validações novamente
    private Instrutor mapear(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        try {
            Instrutor instrutor = new Instrutor(
                    rs.getString("nome"),
                    rs.getString("cpf"),
                    rs.getString("telefone"),
                    rs.getString("cref"),
                    rs.getString("especialidade"));
            instrutor.atribuirId(id);
            return instrutor;
        } catch (DadosInvalidosException | RuntimeException e) {
            throw new IllegalStateException("Registro de instrutor inválido no banco (id " + id + ").", e);
        }
    }

    private void exigirInstrutor(Instrutor instrutor) {
        if (instrutor == null) {
            throw new IllegalArgumentException("O instrutor não pode ser nulo.");
        }
    }
}