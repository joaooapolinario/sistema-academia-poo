package br.ufal.academia.dao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConnectionFactoryTest {

    @Test
    void deveRetornarMesmaInstanciaSingleton() throws Exception {
        Path bancoPadrao = Path.of("academia.db");
        boolean existiaAntes = Files.exists(bancoPadrao);

        try {
            ConnectionFactory primeira = ConnectionFactory.getInstancia();
            ConnectionFactory segunda = ConnectionFactory.getInstancia();

            assertNotNull(primeira);
            assertSame(primeira, segunda);

            try (Connection conn = primeira.getConexao()) {
                assertNotNull(conn);
                assertFalse(conn.isClosed());
            }
        } finally {
            if (!existiaAntes) {
                Files.deleteIfExists(bancoPadrao);
            }
        }
    }

    @Test
    void deveAbrirConexaoValidaComForeignKeysAtivadas(@TempDir Path tempDir) throws SQLException {
        ConnectionFactory factory = ConnectionFactory.criarParaTestes(urlTemporaria(tempDir, "fk.db"));

        try (Connection conn = factory.getConexao();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA foreign_keys")) {
            assertFalse(conn.isClosed());
            assertTrue(conn.isValid(2));
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    void deveCriarTodasAsTabelasAutomaticamenteNaInicializacao(@TempDir Path tempDir) throws SQLException {
        ConnectionFactory factory = ConnectionFactory.criarParaTestes(urlTemporaria(tempDir, "tabelas.db"));
        Set<String> tabelasCriadas = new HashSet<>();

        try (Connection conn = factory.getConexao();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type = 'table'")) {
            while (rs.next()) {
                tabelasCriadas.add(rs.getString("name"));
            }
        }

        Set<String> tabelasEsperadas = Set.of(
                "alunos",
                "restricoes_medicas",
                "instrutores",
                "planos",
                "matriculas",
                "treinos",
                "treino_exercicios",
                "frequencias"
        );
        assertTrue(tabelasCriadas.containsAll(tabelasEsperadas),
                "Tabelas encontradas: " + tabelasCriadas);
    }

    @Test
    void deveSerIdempotenteAoInicializarBancoJaExistente(@TempDir Path tempDir) throws SQLException {
        String url = urlTemporaria(tempDir, "idempotente.db");
        ConnectionFactory primeira = ConnectionFactory.criarParaTestes(url);

        try (Connection conn = primeira.getConexao();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                    INSERT INTO planos (tipo, nome, duracao_meses, valor_mensal)
                    VALUES ('MENSAL', 'Plano Mensal', 1, 99.90)
                    """);
        }

        ConnectionFactory segunda = ConnectionFactory.criarParaTestes(url);

        try (Connection conn = segunda.getConexao();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM planos")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    void deveAplicarChavesEstrangeirasERestricoesDeDominio(@TempDir Path tempDir) throws SQLException {
        ConnectionFactory factory = ConnectionFactory.criarParaTestes(urlTemporaria(tempDir, "integridade.db"));

        try (Connection conn = factory.getConexao();
             Statement stmt = conn.createStatement()) {
            // CHECK de enum inválido deve falhar
            assertThrows(SQLException.class, () -> stmt.executeUpdate("""
                    INSERT INTO alunos (nome, cpf, telefone, data_nascimento, objetivo)
                    VALUES ('Maria', '12345678901', '82999990000', '2000-01-10', 'INVALIDO')
                    """));

            // FK inexistente deve falhar
            assertThrows(SQLException.class, () -> stmt.executeUpdate("""
                    INSERT INTO matriculas (aluno_id, plano_id, data_inicio, data_fim, status, tipo_calculo)
                    VALUES (999, 999, '2026-11-01', '2026-12-01', 'ATIVA', 'PADRAO')
                    """));

            // Fluxo completo válido + remoção em cascata ao excluir aluno
            stmt.executeUpdate("""
                    INSERT INTO alunos (id, nome, cpf, telefone, data_nascimento, objetivo)
                    VALUES (1, 'João Silva', '12345678901', '82999991111', '1999-05-20', 'HIPERTROFIA')
                    """);
            stmt.executeUpdate("""
                    INSERT INTO instrutores (id, nome, cpf, telefone, cref, especialidade)
                    VALUES (1, 'Carlos Lima', '98765432100', '82988882222', 'CREF-001234', 'Musculação')
                    """);
            stmt.executeUpdate("""
                    INSERT INTO planos (id, tipo, nome, duracao_meses, valor_mensal)
                    VALUES (1, 'ANUAL', 'Plano Anual', 12, 89.90)
                    """);
            stmt.executeUpdate("""
                    INSERT INTO matriculas (id, aluno_id, plano_id, data_inicio, data_fim, status, tipo_calculo)
                    VALUES (1, 1, 1, '2026-11-01', '2027-11-01', 'ATIVA', 'ESTUDANTE')
                    """);
            stmt.executeUpdate("""
                    INSERT INTO frequencias (id, matricula_id, data_hora_entrada)
                    VALUES (1, 1, '2026-11-22T08:30:00')
                    """);
            stmt.executeUpdate("""
                    INSERT INTO treinos (id, aluno_id, instrutor_id, objetivo, data_criacao)
                    VALUES (1, 1, 1, 'HIPERTROFIA', '2026-11-22')
                    """);
            stmt.executeUpdate("""
                    INSERT INTO treino_exercicios (treino_id, descricao)
                    VALUES (1, 'Supino reto 4x10')
                    """);

            stmt.executeUpdate("DELETE FROM alunos WHERE id = 1");

            assertEquals(0, contarLinhas(stmt, "matriculas"));
            assertEquals(0, contarLinhas(stmt, "frequencias"));
            assertEquals(0, contarLinhas(stmt, "treinos"));
            assertEquals(0, contarLinhas(stmt, "treino_exercicios"));
        }
    }

    private String urlTemporaria(Path tempDir, String nomeArquivo) {
        return "jdbc:sqlite:" + tempDir.resolve(nomeArquivo).toAbsolutePath();
    }

    private int contarLinhas(Statement stmt, String tabela) throws SQLException {
        try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tabela)) {
            assertTrue(rs.next());
            return rs.getInt(1);
        }
    }
}
