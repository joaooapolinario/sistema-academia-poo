package br.ufal.academia.dao;

import br.ufal.academia.model.entities.Aluno;
import br.ufal.academia.model.entities.Frequencia;
import br.ufal.academia.model.entities.Matricula;
import br.ufal.academia.model.entities.Plano;
import br.ufal.academia.model.enums.ObjetivoTreino;
import br.ufal.academia.model.enums.TipoPlano;
import br.ufal.academia.model.patterns.strategy.CalculoPadraoStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrequenciaDAOImplTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 11, 1);

    private ConnectionFactory factory;
    private MatriculaDAOImpl matriculaDAO;
    private FrequenciaDAOImpl dao;
    private Aluno maria;
    private Aluno joao;
    private Plano plano;
    private Matricula matriculaMaria;
    private Matricula matriculaJoao;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        factory = ConnectionFactory.criarParaTestes("jdbc:sqlite:" + tempDir.resolve("frequencias.db").toAbsolutePath());
        PlanoDAOImpl planoDAO = new PlanoDAOImpl(factory);
        maria = inserirAluno(1, "Maria Souza", "52998224725");
        joao = inserirAluno(2, "João Silva", "11144477735");
        plano = new Plano(TipoPlano.TRIMESTRAL, "Trimestral", 3, 100.0);
        planoDAO.salvar(plano);

        matriculaDAO = new MatriculaDAOImpl(factory, new AlunoDAOEmMemoria(maria, joao), planoDAO);
        matriculaMaria = new Matricula(maria, plano, INICIO, new CalculoPadraoStrategy());
        matriculaJoao = new Matricula(joao, plano, INICIO, new CalculoPadraoStrategy());
        matriculaDAO.salvar(matriculaMaria);
        matriculaDAO.salvar(matriculaJoao);

        dao = new FrequenciaDAOImpl(factory, matriculaDAO);
    }

    @Test
    void deveSalvarEAtribuirIdGerado() throws Exception {
        Frequencia frequencia = new Frequencia(matriculaMaria, LocalDateTime.of(2026, 11, 10, 8, 30));

        dao.salvar(frequencia);

        assertTrue(frequencia.getId() > 0);
    }

    @Test
    void deveBuscarPorIdPreservandoCamposEMatricula() throws Exception {
        LocalDateTime entrada = LocalDateTime.of(2026, 11, 10, 8, 30, 15);
        Frequencia frequencia = new Frequencia(matriculaMaria, entrada);
        dao.salvar(frequencia);

        Frequencia lida = dao.buscarPorId(frequencia.getId()).orElseThrow();

        assertEquals(frequencia.getId(), lida.getId());
        assertEquals(matriculaMaria.getId(), lida.getMatricula().getId());
        assertEquals(maria.getId(), lida.getMatricula().getAluno().getId());
        assertEquals(entrada, lida.getDataHoraEntrada());
    }

    @Test
    void deveRetornarVazioQuandoNaoEncontrar() {
        assertFalse(dao.buscarPorId(999).isPresent());
    }

    @Test
    void deveListarTodasOrdenadasPorDataHora() throws Exception {
        assertTrue(dao.listarTodos().isEmpty());
        LocalDateTime segunda = LocalDateTime.of(2026, 11, 12, 18, 0);
        LocalDateTime primeira = LocalDateTime.of(2026, 11, 10, 7, 30);
        dao.salvar(new Frequencia(matriculaMaria, segunda));
        dao.salvar(new Frequencia(matriculaJoao, primeira));

        List<Frequencia> todas = dao.listarTodos();

        assertEquals(2, todas.size());
        assertEquals(primeira, todas.get(0).getDataHoraEntrada());
        assertEquals(segunda, todas.get(1).getDataHoraEntrada());
    }

    @Test
    void deveListarPorAlunoEPeriodoInclusivoPreservandoHistoricoDeMatriculaCancelada() throws Exception {
        Frequencia inicioDia = new Frequencia(matriculaMaria, LocalDateTime.of(2026, 11, 1, 0, 0));
        Frequencia fimDia = new Frequencia(matriculaMaria, LocalDateTime.of(2026, 11, 30, 23, 59, 59));
        Frequencia outroMes = new Frequencia(matriculaMaria, LocalDateTime.of(2026, 12, 1, 8, 0));
        Frequencia outroAluno = new Frequencia(matriculaJoao, LocalDateTime.of(2026, 11, 15, 10, 0));
        dao.salvar(inicioDia);
        dao.salvar(fimDia);
        dao.salvar(outroMes);
        dao.salvar(outroAluno);

        // Cancela a matrícula depois das entradas: o histórico ainda deve ser lido.
        matriculaMaria.cancelar();
        matriculaDAO.atualizar(matriculaMaria);

        List<Frequencia> novembroMaria = dao.listarPorAlunoEPeriodo(
                maria.getId(), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));

        assertEquals(2, novembroMaria.size());
        assertEquals(inicioDia.getId(), novembroMaria.get(0).getId());
        assertEquals(fimDia.getId(), novembroMaria.get(1).getId());
    }

    @Test
    void deveAtualizarFrequencia() throws Exception {
        Frequencia frequencia = new Frequencia(matriculaMaria, LocalDateTime.of(2026, 11, 10, 8, 0));
        dao.salvar(frequencia);

        LocalDateTime novaEntrada = LocalDateTime.of(2026, 11, 10, 9, 15);
        Frequencia alterada = Frequencia.reconstituir(frequencia.getId(), matriculaMaria, novaEntrada);
        dao.atualizar(alterada);

        Frequencia lida = dao.buscarPorId(frequencia.getId()).orElseThrow();
        assertEquals(novaEntrada, lida.getDataHoraEntrada());
    }

    @Test
    void deveFalharAoAtualizarFrequenciaInexistente() throws Exception {
        Frequencia naoSalva = new Frequencia(matriculaMaria, LocalDateTime.of(2026, 11, 10, 8, 0));
        assertThrows(IllegalArgumentException.class, () -> dao.atualizar(naoSalva));
    }

    @Test
    void deveRemoverPorId() throws Exception {
        Frequencia frequencia = new Frequencia(matriculaMaria, LocalDateTime.of(2026, 11, 10, 8, 0));
        dao.salvar(frequencia);

        dao.remover(frequencia.getId());

        assertFalse(dao.buscarPorId(frequencia.getId()).isPresent());
        dao.remover(999);
    }

    @Test
    void naoDeveSalvarFrequenciaDeMatriculaNaoPersistida() throws Exception {
        Matricula naoSalva = new Matricula(maria, plano, INICIO, new CalculoPadraoStrategy());
        Frequencia frequencia = new Frequencia(naoSalva, LocalDateTime.of(2026, 11, 10, 8, 0));

        assertThrows(IllegalStateException.class, () -> dao.salvar(frequencia));
    }

    @Test
    void removerMatriculaDeveApagarFrequenciasEmCascata() throws Exception {
        dao.salvar(new Frequencia(matriculaMaria, LocalDateTime.of(2026, 11, 10, 8, 0)));

        matriculaDAO.remover(matriculaMaria.getId());

        try (Connection conn = factory.getConexao();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM frequencias")) {
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }
    }

    @Test
    void deveRejeitarArgumentosInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> dao.salvar(null));
        assertThrows(IllegalArgumentException.class, () -> dao.atualizar(null));
        assertThrows(IllegalArgumentException.class,
                () -> dao.listarPorAlunoEPeriodo(1, null, LocalDate.of(2026, 11, 30)));
        assertThrows(IllegalArgumentException.class,
                () -> dao.listarPorAlunoEPeriodo(1, LocalDate.of(2026, 11, 1), null));
        assertThrows(IllegalArgumentException.class,
                () -> dao.listarPorAlunoEPeriodo(1, LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 1)));
    }

    private Aluno inserirAluno(int id, String nome, String cpf) throws Exception {
        try (Connection conn = factory.getConexao(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("INSERT INTO alunos (id, nome, cpf, telefone, data_nascimento, objetivo) VALUES ("
                    + id + ", '" + nome + "', '" + cpf + "', '82999990000', '2000-01-10', 'HIPERTROFIA')");
        }
        Aluno novo = new Aluno(nome, cpf, "82999990000", LocalDate.of(2000, 1, 10), ObjetivoTreino.HIPERTROFIA);
        novo.atribuirId(id);
        return novo;
    }

    private static class AlunoDAOEmMemoria implements AlunoDAO {
        private final List<Aluno> alunos;

        AlunoDAOEmMemoria(Aluno... alunos) {
            this.alunos = List.of(alunos);
        }

        @Override public void salvar(Aluno aluno) { throw new UnsupportedOperationException(); }
        @Override public void atualizar(Aluno aluno) { throw new UnsupportedOperationException(); }
        @Override public void remover(int id) { throw new UnsupportedOperationException(); }

        @Override
        public Optional<Aluno> buscarPorId(int id) {
            return alunos.stream().filter(a -> a.getId() == id).findFirst();
        }

        @Override public List<Aluno> listarTodos() { return alunos; }

        @Override
        public Optional<Aluno> buscarPorCpf(String cpf) {
            return alunos.stream().filter(a -> a.getCpf().equals(cpf)).findFirst();
        }
    }
}
