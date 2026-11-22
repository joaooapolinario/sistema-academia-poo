package br.ufal.academia.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

// Singleton responsável pela configuração de conexão JDBC com SQLite e criação automática do DDL.
public class ConnectionFactory {

    private static final String URL = "jdbc:sqlite:academia.db";
    private static ConnectionFactory instancia;

    private final String url;

    private ConnectionFactory() {
        this(URL);
    }

    private ConnectionFactory(String url) {
        this.url = url;
        criarTabelas();
    }

    public static synchronized ConnectionFactory getInstancia() {
        if (instancia == null) {
            instancia = new ConnectionFactory();
        }
        return instancia;
    }

    // Fábrica package-private para isolar testes de persistência em bancos temporários.
    static ConnectionFactory criarParaTestes(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("A URL JDBC não pode ser nula ou vazia.");
        }
        return new ConnectionFactory(url);
    }

    public Connection getConexao() throws SQLException {
        Connection conexao = DriverManager.getConnection(url);
        try (Statement stmt = conexao.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
            return conexao;
        } catch (SQLException e) {
            conexao.close();
            throw e;
        }
    }

    private void criarTabelas() {
        List<String> comandosDdl = List.of(
                """
                CREATE TABLE IF NOT EXISTS alunos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nome TEXT NOT NULL,
                    cpf TEXT NOT NULL UNIQUE,
                    telefone TEXT NOT NULL,
                    data_nascimento TEXT NOT NULL,
                    objetivo TEXT NOT NULL CHECK (
                        objetivo IN ('HIPERTROFIA', 'EMAGRECIMENTO', 'CONDICIONAMENTO', 'REABILITACAO')
                    )
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS instrutores (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nome TEXT NOT NULL,
                    cpf TEXT NOT NULL UNIQUE,
                    telefone TEXT NOT NULL,
                    cref TEXT NOT NULL UNIQUE,
                    especialidade TEXT NOT NULL
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS planos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    tipo TEXT NOT NULL UNIQUE CHECK (
                        tipo IN ('MENSAL', 'TRIMESTRAL', 'SEMESTRAL', 'ANUAL')
                    ),
                    nome TEXT NOT NULL,
                    duracao_meses INTEGER NOT NULL CHECK (duracao_meses > 0),
                    valor_mensal REAL NOT NULL CHECK (valor_mensal > 0)
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS matriculas (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    aluno_id INTEGER NOT NULL,
                    plano_id INTEGER NOT NULL,
                    data_inicio TEXT NOT NULL,
                    data_fim TEXT NOT NULL,
                    status TEXT NOT NULL CHECK (
                        status IN ('ATIVA', 'VENCIDA', 'CANCELADA')
                    ),
                    tipo_calculo TEXT NOT NULL DEFAULT 'PADRAO' CHECK (
                        tipo_calculo IN ('PADRAO', 'ESTUDANTE', 'IDOSO')
                    ),
                    FOREIGN KEY (aluno_id) REFERENCES alunos(id) ON DELETE CASCADE,
                    FOREIGN KEY (plano_id) REFERENCES planos(id) ON DELETE RESTRICT
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS treinos (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    aluno_id INTEGER NOT NULL,
                    instrutor_id INTEGER NOT NULL,
                    objetivo TEXT NOT NULL CHECK (
                        objetivo IN ('HIPERTROFIA', 'EMAGRECIMENTO', 'CONDICIONAMENTO', 'REABILITACAO')
                    ),
                    data_criacao TEXT NOT NULL,
                    FOREIGN KEY (aluno_id) REFERENCES alunos(id) ON DELETE CASCADE,
                    FOREIGN KEY (instrutor_id) REFERENCES instrutores(id) ON DELETE RESTRICT
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS treino_exercicios (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    treino_id INTEGER NOT NULL,
                    descricao TEXT NOT NULL,
                    FOREIGN KEY (treino_id) REFERENCES treinos(id) ON DELETE CASCADE
                )
                """,
                """
                CREATE TABLE IF NOT EXISTS frequencias (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    matricula_id INTEGER NOT NULL,
                    data_hora_entrada TEXT NOT NULL,
                    FOREIGN KEY (matricula_id) REFERENCES matriculas(id) ON DELETE CASCADE
                )
                """,
                "CREATE INDEX IF NOT EXISTS idx_matriculas_aluno_id ON matriculas(aluno_id)",
                "CREATE INDEX IF NOT EXISTS idx_matriculas_status ON matriculas(status)",
                "CREATE INDEX IF NOT EXISTS idx_treinos_aluno_id ON treinos(aluno_id)",
                "CREATE INDEX IF NOT EXISTS idx_treino_exercicios_treino_id ON treino_exercicios(treino_id)",
                "CREATE INDEX IF NOT EXISTS idx_frequencias_matricula_id ON frequencias(matricula_id)"
        );

        try (Connection conexao = getConexao();
             Statement stmt = conexao.createStatement()) {
            for (String ddl : comandosDdl) {
                stmt.execute(ddl);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Erro ao inicializar tabelas do banco SQLite.", e);
        }
    }
}
