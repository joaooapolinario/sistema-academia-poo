package br.ufal.academia.dao;

import br.ufal.academia.model.entities.Plano;
import br.ufal.academia.model.enums.TipoPlano;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanoDAOImplTest {

    private ConnectionFactory factory;
    private PlanoDAOImpl dao;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        factory = ConnectionFactory.criarParaTestes("jdbc:sqlite:" + tempDir.resolve("planos.db").toAbsolutePath());
        dao = new PlanoDAOImpl(factory);
    }

    @Test
    void deveSalvarEAtribuirIdGerado() throws Exception {
        Plano plano = new Plano(TipoPlano.MENSAL, "Mensal", 1, 99.9);

        dao.salvar(plano);

        assertTrue(plano.getId() > 0);
    }

    @Test
    void deveBuscarPorIdPreservandoTodosOsCampos() throws Exception {
        Plano plano = new Plano(TipoPlano.SEMESTRAL, "Semestral", 6, 79.9);
        dao.salvar(plano);

        Plano encontrado = dao.buscarPorId(plano.getId()).orElseThrow();

        assertEquals(plano.getId(), encontrado.getId());
        assertEquals(TipoPlano.SEMESTRAL, encontrado.getTipo());
        assertEquals("Semestral", encontrado.getNome());
        assertEquals(6, encontrado.getDuracaoMeses());
        assertEquals(79.9, encontrado.getValorMensal(), 0.0001);
    }

    @Test
    void deveBuscarPorTipo() throws Exception {
        dao.salvar(new Plano(TipoPlano.MENSAL, "Mensal", 1, 99.9));
        dao.salvar(new Plano(TipoPlano.ANUAL, "Anual", 12, 69.9));

        assertEquals("Anual", dao.buscarPorTipo(TipoPlano.ANUAL).orElseThrow().getNome());
        assertFalse(dao.buscarPorTipo(TipoPlano.TRIMESTRAL).isPresent());
        assertThrows(IllegalArgumentException.class, () -> dao.buscarPorTipo(null));
    }

    @Test
    void deveRetornarVazioQuandoIdNaoExiste() {
        assertFalse(dao.buscarPorId(999).isPresent());
    }

    @Test
    void deveListarOrdenadosPorDuracao() throws Exception {
        assertTrue(dao.listarTodos().isEmpty());
        dao.salvar(new Plano(TipoPlano.ANUAL, "Anual", 12, 69.9));
        dao.salvar(new Plano(TipoPlano.MENSAL, "Mensal", 1, 99.9));

        List<Plano> todos = dao.listarTodos();

        assertEquals(2, todos.size());
        assertEquals(TipoPlano.MENSAL, todos.get(0).getTipo());
        assertEquals(TipoPlano.ANUAL, todos.get(1).getTipo());
    }

    @Test
    void deveAtualizarPlano() throws Exception {
        Plano plano = new Plano(TipoPlano.MENSAL, "Mensal", 1, 99.9);
        dao.salvar(plano);
        Plano alterado = new Plano(TipoPlano.MENSAL, "Mensal Promo", 1, 89.9);
        alterado.atribuirId(plano.getId());

        dao.atualizar(alterado);

        Plano lido = dao.buscarPorId(plano.getId()).orElseThrow();
        assertEquals("Mensal Promo", lido.getNome());
        assertEquals(89.9, lido.getValorMensal(), 0.0001);
    }

    @Test
    void deveFalharAoAtualizarPlanoInexistente() throws Exception {
        Plano naoSalvo = new Plano(TipoPlano.MENSAL, "Mensal", 1, 99.9);
        assertThrows(IllegalArgumentException.class, () -> dao.atualizar(naoSalvo));

        Plano inexistente = new Plano(TipoPlano.MENSAL, "Mensal", 1, 99.9);
        inexistente.atribuirId(999);
        assertThrows(IllegalArgumentException.class, () -> dao.atualizar(inexistente));
    }

    @Test
    void naoDeveSalvarOMesmoPlanoDuasVezes() throws Exception {
        Plano plano = new Plano(TipoPlano.MENSAL, "Mensal", 1, 99.9);
        dao.salvar(plano);

        assertThrows(IllegalArgumentException.class, () -> dao.salvar(plano));
        assertEquals(1, dao.listarTodos().size());
    }

    @Test
    void deveRemoverPorId() throws Exception {
        Plano plano = new Plano(TipoPlano.MENSAL, "Mensal", 1, 99.9);
        dao.salvar(plano);

        dao.remover(plano.getId());

        assertFalse(dao.buscarPorId(plano.getId()).isPresent());
        dao.remover(999); // id inexistente não deve falhar
    }

    @Test
    void naoDevePermitirTipoDuplicado() throws Exception {
        dao.salvar(new Plano(TipoPlano.MENSAL, "Mensal", 1, 99.9));

        assertThrows(IllegalStateException.class,
                () -> dao.salvar(new Plano(TipoPlano.MENSAL, "Outro Mensal", 1, 50.0)));
        assertEquals(1, dao.listarTodos().size());
    }

    @Test
    void naoDeveRemoverPlanoComMatriculasVinculadas() throws Exception {
        Plano plano = new Plano(TipoPlano.MENSAL, "Mensal", 1, 99.9);
        dao.salvar(plano);
        try (Connection conn = factory.getConexao(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("INSERT INTO alunos (id, nome, cpf, telefone, data_nascimento, objetivo) "
                    + "VALUES (1, 'Maria', '52998224725', '82999990000', '2000-01-10', 'HIPERTROFIA')");
            stmt.executeUpdate("INSERT INTO matriculas (aluno_id, plano_id, data_inicio, data_fim, status, tipo_calculo) "
                    + "VALUES (1, " + plano.getId() + ", '2026-11-01', '2026-12-01', 'ATIVA', 'PADRAO')");
        }

        assertThrows(IllegalStateException.class, () -> dao.remover(plano.getId()));
        assertTrue(dao.buscarPorId(plano.getId()).isPresent());
    }

    @Test
    void deveRejeitarPlanoNulo() {
        assertThrows(IllegalArgumentException.class, () -> dao.salvar(null));
        assertThrows(IllegalArgumentException.class, () -> dao.atualizar(null));
    }
}
