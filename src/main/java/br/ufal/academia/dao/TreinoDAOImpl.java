package br.ufal.academia.dao;

import br.ufal.academia.model.entities.Aluno;
import br.ufal.academia.model.entities.Instrutor;
import br.ufal.academia.model.entities.Treino;
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

// DAO do Treino usando SQLite
public class TreinoDAOImpl implements TreinoDAO {

    private static final String COLUNAS = "id, aluno_id, instrutor_id, objetivo, data_criacao";

    private record Linha(int id, int alunoId, int instrutorId, String objetivo, String dataCriacao,
                         List<String> exercicios) {
    }

    private final ConnectionFactory connectionFactory;
    private final AlunoDAO alunoDAO;
    private final InstrutorDAO instrutorDAO;

    public TreinoDAOImpl(AlunoDAO alunoDAO, InstrutorDAO instrutorDAO) {
        this(ConnectionFactory.getInstancia(), alunoDAO, instrutorDAO);
    }

    TreinoDAOImpl(ConnectionFactory connectionFactory, AlunoDAO alunoDAO, InstrutorDAO instrutorDAO) {
        if (connectionFactory == null || alunoDAO == null || instrutorDAO == null) {
            throw new IllegalArgumentException("ConnectionFactory, AlunoDAO e InstrutorDAO são obrigatórios.");
        }
        this.connectionFactory = connectionFactory;
        this.alunoDAO = alunoDAO;
        this.instrutorDAO = instrutorDAO;
    }

    @Override
    public void salvar(Treino treino) {
        exigirTreino(treino);
        if (treino.getId() != 0) {
            throw new IllegalArgumentException("O treino já foi salvo (id " + treino.getId() + "). Use atualizar.");
        }
        String sql = "INSERT INTO treinos (aluno_id, instrutor_id, objetivo, data_criacao) VALUES (?, ?, ?, ?)";

        try (Connection conn = connectionFactory.getConexao()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                preencherCampos(ps, treino);
                ps.executeUpdate();

                int idGerado;
                try (ResultSet chaves = ps.getGeneratedKeys()) {
                    if (!chaves.next()) {
                        throw new SQLException("O banco não retornou o id gerado.");
                    }
                    idGerado = chaves.getInt(1);
                }
                inserirExercicios(conn, idGerado, treino.getExercicios());
                conn.commit();
                treino.atribuirId(idGerado);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao salvar treino do aluno " + treino.getAluno().getId()
                    + " (o aluno e o instrutor precisam estar salvos antes).", e);
        }
    }

    @Override
    public void atualizar(Treino treino) {
        exigirTreino(treino);
        if (treino.getId() <= 0) {
            throw new IllegalArgumentException("O treino ainda não foi salvo. Use salvar.");
        }
        String sql = "UPDATE treinos SET aluno_id = ?, instrutor_id = ?, objetivo = ?, data_criacao = ? WHERE id = ?";

        try (Connection conn = connectionFactory.getConexao()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                preencherCampos(ps, treino);
                ps.setInt(5, treino.getId());
                if (ps.executeUpdate() == 0) {
                    throw new IllegalArgumentException("Treino com id " + treino.getId() + " não encontrado.");
                }
                removerExercicios(conn, treino.getId());
                inserirExercicios(conn, treino.getId(), treino.getExercicios());
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao atualizar treino com id " + treino.getId() + ".", e);
        }
    }

    @Override
    public void remover(int id) {
        String sql = "DELETE FROM treinos WHERE id = ?";

        try (Connection conn = connectionFactory.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao remover treino com id " + id + ".", e);
        }
    }

    @Override
    public Optional<Treino> buscarPorId(int id) {
        List<Treino> treinos = buscar("SELECT " + COLUNAS + " FROM treinos WHERE id = ?", id,
                "Erro ao buscar treino com id " + id + ".");
        return treinos.isEmpty() ? Optional.empty() : Optional.of(treinos.get(0));
    }

    @Override
    public List<Treino> listarTodos() {
        return buscar("SELECT " + COLUNAS + " FROM treinos ORDER BY data_criacao, id", null,
                "Erro ao listar treinos.");
    }

    @Override
    public List<Treino> listarPorAluno(int idAluno) {
        return buscar("SELECT " + COLUNAS + " FROM treinos WHERE aluno_id = ? ORDER BY data_criacao, id", idAluno,
                "Erro ao listar treinos do aluno " + idAluno + ".");
    }

    // Lê do banco e fecha a conexão antes de buscar aluno e instrutor
    private List<Treino> buscar(String sql, Integer parametro, String mensagemErro) {
        List<Linha> linhas = new ArrayList<>();

        try (Connection conn = connectionFactory.getConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (parametro != null) {
                ps.setInt(1, parametro);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    linhas.add(new Linha(
                            id,
                            rs.getInt("aluno_id"),
                            rs.getInt("instrutor_id"),
                            rs.getString("objetivo"),
                            rs.getString("data_criacao"),
                            buscarExercicios(conn, id)));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException(mensagemErro, e);
        }

        List<Treino> treinos = new ArrayList<>();
        for (Linha linha : linhas) {
            treinos.add(mapear(linha));
        }
        return treinos;
    }

    private Treino mapear(Linha linha) {
        Aluno aluno = alunoDAO.buscarPorId(linha.alunoId()).orElseThrow(() -> new IllegalStateException(
                "Aluno " + linha.alunoId() + " do treino " + linha.id() + " não encontrado."));
        Instrutor instrutor = instrutorDAO.buscarPorId(linha.instrutorId()).orElseThrow(() -> new IllegalStateException(
                "Instrutor " + linha.instrutorId() + " do treino " + linha.id() + " não encontrado."));

        try {
            return Treino.reconstituir(
                    linha.id(),
                    aluno,
                    instrutor,
                    ObjetivoTreino.valueOf(linha.objetivo()),
                    linha.exercicios(),
                    LocalDate.parse(linha.dataCriacao()));
        } catch (Exception e) {
            throw new IllegalStateException("Registro de treino inválido no banco (id " + linha.id() + ").", e);
        }
    }

    private void preencherCampos(PreparedStatement ps, Treino treino) throws SQLException {
        ps.setInt(1, treino.getAluno().getId());
        ps.setInt(2, treino.getInstrutor().getId());
        ps.setString(3, treino.getObjetivo().name());
        ps.setString(4, treino.getDataCriacao().toString());
    }

    private void inserirExercicios(Connection conn, int treinoId, List<String> exercicios) throws SQLException {
        String sql = "INSERT INTO treino_exercicios (treino_id, descricao) VALUES (?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String descricao : exercicios) {
                ps.setInt(1, treinoId);
                ps.setString(2, descricao);
                ps.executeUpdate();
            }
        }
    }

    private void removerExercicios(Connection conn, int treinoId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM treino_exercicios WHERE treino_id = ?")) {
            ps.setInt(1, treinoId);
            ps.executeUpdate();
        }
    }

    private List<String> buscarExercicios(Connection conn, int treinoId) throws SQLException {
        String sql = "SELECT descricao FROM treino_exercicios WHERE treino_id = ? ORDER BY id";
        List<String> exercicios = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, treinoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    exercicios.add(rs.getString("descricao"));
                }
            }
        }
        return exercicios;
    }

    private void exigirTreino(Treino treino) {
        if (treino == null) {
            throw new IllegalArgumentException("O treino não pode ser nulo.");
        }
    }
}