package br.ufal.academia.dao;

import br.ufal.academia.model.entities.Plano;
import br.ufal.academia.model.enums.TipoPlano;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Implementação JDBC/SQLite do contrato PlanoDAO.
public class PlanoDAOImpl implements PlanoDAO {

    private static final String COLUNAS = "id, tipo, nome, duracao_meses, valor_mensal";

    private final ConnectionFactory connectionFactory;

    public PlanoDAOImpl() {
        this(ConnectionFactory.getInstancia());
    }

    // Permite injetar outra fábrica (ex.: banco temporário nos testes).
    PlanoDAOImpl(ConnectionFactory connectionFactory) {
        if (connectionFactory == null) {
            throw new IllegalArgumentException("A ConnectionFactory não pode ser nula.");
        }
        this.connectionFactory = connectionFactory;
    }

    @Override
    public void salvar(Plano plano) {
        exigirPlano(plano);
        String sql = "INSERT INTO planos (tipo, nome, duracao_meses, valor_mensal) VALUES (?, ?, ?, ?)";

        try (Connection conn = connectionFactory.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            preencherCampos(ps, plano);
            ps.executeUpdate();

            try (ResultSet chaves = ps.getGeneratedKeys()) {
                if (chaves.next()) {
                    plano.atribuirId(chaves.getInt(1));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao salvar plano " + plano.getTipo() + ".", e);
        }
    }

    @Override
    public void atualizar(Plano plano) {
        exigirPlano(plano);
        String sql = "UPDATE planos SET tipo = ?, nome = ?, duracao_meses = ?, valor_mensal = ? WHERE id = ?";

        try (Connection conn = connectionFactory.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            preencherCampos(ps, plano);
            ps.setInt(5, plano.getId());

            if (ps.executeUpdate() == 0) {
                throw new IllegalArgumentException("Plano com id " + plano.getId() + " não encontrado.");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao atualizar plano com id " + plano.getId() + ".", e);
        }
    }

    @Override
    public void remover(int id) {
        String sql = "DELETE FROM planos WHERE id = ?";

        try (Connection conn = connectionFactory.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            // matriculas.plano_id usa ON DELETE RESTRICT: plano com matrículas não pode ser removido.
            throw new IllegalStateException("Erro ao remover plano com id " + id
                    + " (verifique se existem matrículas vinculadas).", e);
        }
    }

    @Override
    public Optional<Plano> buscarPorId(int id) {
        String sql = "SELECT " + COLUNAS + " FROM planos WHERE id = ?";

        try (Connection conn = connectionFactory.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar plano com id " + id + ".", e);
        }
    }

    @Override
    public List<Plano> listarTodos() {
        String sql = "SELECT " + COLUNAS + " FROM planos ORDER BY duracao_meses, id";
        List<Plano> planos = new ArrayList<>();

        try (Connection conn = connectionFactory.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                planos.add(mapear(rs));
            }
            return planos;
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao listar planos.", e);
        }
    }

    @Override
    public Optional<Plano> buscarPorTipo(TipoPlano tipo) {
        if (tipo == null) {
            throw new IllegalArgumentException("O tipo do plano não pode ser nulo.");
        }
        String sql = "SELECT " + COLUNAS + " FROM planos WHERE tipo = ?";

        try (Connection conn = connectionFactory.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tipo.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao buscar plano do tipo " + tipo + ".", e);
        }
    }

    private void preencherCampos(PreparedStatement ps, Plano plano) throws SQLException {
        ps.setString(1, plano.getTipo().name());
        ps.setString(2, plano.getNome());
        ps.setInt(3, plano.getDuracaoMeses());
        ps.setDouble(4, plano.getValorMensal());
    }

    // Reconstrói o Plano passando pelo construtor, para reaplicar as validações do domínio.
    private Plano mapear(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        try {
            Plano plano = new Plano(
                    TipoPlano.valueOf(rs.getString("tipo")),
                    rs.getString("nome"),
                    rs.getInt("duracao_meses"),
                    rs.getDouble("valor_mensal"));
            plano.atribuirId(id);
            return plano;
        } catch (Exception e) {
            throw new IllegalStateException("Registro de plano inválido no banco (id " + id + ").", e);
        }
    }

    private void exigirPlano(Plano plano) {
        if (plano == null) {
            throw new IllegalArgumentException("O plano não pode ser nulo.");
        }
    }
}
