package br.ufal.academia.model.entities;

import br.ufal.academia.exception.DadosInvalidosException;
import br.ufal.academia.model.enums.ObjetivoTreino;
import br.ufal.academia.model.enums.StatusMatricula;
import br.ufal.academia.model.enums.TipoPlano;
import br.ufal.academia.model.patterns.observer.MatriculaObserver;
import br.ufal.academia.model.patterns.strategy.CalculoPadraoStrategy;
import br.ufal.academia.model.patterns.strategy.DescontoEstudanteStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatriculaTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 1, 31);

    private Aluno aluno;
    private Plano trimestral;
    private Matricula matricula;
    private final List<StatusMatricula> notificacoes = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        aluno = new Aluno("Maria Souza", "52998224725", "82999990000", LocalDate.of(2000, 1, 10),
                ObjetivoTreino.HIPERTROFIA);
        trimestral = new Plano(TipoPlano.TRIMESTRAL, "Trimestral", 3, 100.0);
        matricula = new Matricula(aluno, trimestral, INICIO, new CalculoPadraoStrategy());
        notificacoes.clear();
        MatriculaObserver observer = m -> notificacoes.add(m.getStatus());
        matricula.adicionarObserver(observer);
    }

    @Test
    void deveNascerAtivaComDataFimSomandoDuracaoDoPlano() {
        assertTrue(matricula.estaAtiva());
        assertEquals(StatusMatricula.ATIVA, matricula.getStatus());
        assertEquals(INICIO, matricula.getDataInicio());
        // 31/01 + 3 meses = 30/04 (ajuste de fim de mês)
        assertEquals(LocalDate.of(2026, 4, 30), matricula.getDataFim());
        assertEquals(aluno, matricula.getAluno());
        assertEquals(trimestral, matricula.getPlano());
    }

    @Test
    void naoDeveEstarVencidaAteOUltimoDia() {
        assertFalse(matricula.isVencida(LocalDate.of(2026, 4, 30)));
        assertTrue(matricula.isVencida(LocalDate.of(2026, 5, 1)));
    }

    @Test
    void verificarVencimentoDeveMarcarVencidaENotificar() {
        matricula.verificarVencimento(LocalDate.of(2026, 4, 30));
        assertEquals(StatusMatricula.ATIVA, matricula.getStatus());
        assertTrue(notificacoes.isEmpty());

        matricula.verificarVencimento(LocalDate.of(2026, 5, 1));
        assertEquals(StatusMatricula.VENCIDA, matricula.getStatus());
        assertFalse(matricula.estaAtiva());
        assertEquals(List.of(StatusMatricula.VENCIDA), notificacoes);
    }

    @Test
    void matriculaVencidaOuCanceladaNaoDeveSerReavaliada() throws Exception {
        matricula.cancelar();
        notificacoes.clear();

        matricula.verificarVencimento(LocalDate.of(2030, 1, 1));

        assertEquals(StatusMatricula.CANCELADA, matricula.getStatus());
        assertFalse(matricula.isVencida(LocalDate.of(2030, 1, 1)));
        assertTrue(notificacoes.isEmpty());
    }

    @Test
    void cancelarDeveMudarStatusENotificar() throws Exception {
        matricula.cancelar();

        assertEquals(StatusMatricula.CANCELADA, matricula.getStatus());
        assertEquals(List.of(StatusMatricula.CANCELADA), notificacoes);
        assertThrows(DadosInvalidosException.class, () -> matricula.cancelar());
    }

    @Test
    void renovarMatriculaAtivaEstendeAPartirDaDataFim() throws Exception {
        matricula.renovar(LocalDate.of(2026, 4, 1));

        assertEquals(StatusMatricula.ATIVA, matricula.getStatus());
        assertEquals(LocalDate.of(2026, 4, 30), matricula.getDataInicio());
        assertEquals(LocalDate.of(2026, 7, 30), matricula.getDataFim());
        assertEquals(List.of(StatusMatricula.ATIVA), notificacoes);
    }

    @Test
    void renovarMatriculaVencidaRecomecaAPartirDeHoje() throws Exception {
        LocalDate hoje = LocalDate.of(2026, 8, 10);
        matricula.verificarVencimento(hoje);
        notificacoes.clear();

        matricula.renovar(hoje);

        assertEquals(StatusMatricula.ATIVA, matricula.getStatus());
        assertEquals(hoje, matricula.getDataInicio());
        assertEquals(LocalDate.of(2026, 11, 10), matricula.getDataFim());
        assertEquals(List.of(StatusMatricula.ATIVA), notificacoes);
    }

    @Test
    void renovarMatriculaAtivaComPrazoExpiradoRecomecaAPartirDeHoje() throws Exception {
        LocalDate hoje = LocalDate.of(2026, 8, 10);

        // Status ainda está ATIVA, mas isVencida(hoje) é true (prazo terminou em 30/04).
        matricula.renovar(hoje);

        assertEquals(StatusMatricula.ATIVA, matricula.getStatus());
        assertEquals(hoje, matricula.getDataInicio());
        assertEquals(LocalDate.of(2026, 11, 10), matricula.getDataFim());
        assertEquals(List.of(StatusMatricula.ATIVA), notificacoes);
    }

    @Test
    void naoDeveRenovarComDataNula() {
        assertThrows(DadosInvalidosException.class, () -> matricula.renovar(null));
    }

    @Test
    void naoDeveRenovarMatriculaCancelada() throws Exception {
        matricula.cancelar();

        assertThrows(DadosInvalidosException.class, () -> matricula.renovar(LocalDate.of(2026, 4, 1)));
        assertEquals(StatusMatricula.CANCELADA, matricula.getStatus());
    }

    @Test
    void deveDelegarMensalidadeParaAEstrategiaETrocarEmTempoDeExecucao() throws Exception {
        assertEquals(100.0, matricula.calcularMensalidade(), 0.001);

        matricula.alterarCalculo(new DescontoEstudanteStrategy());

        assertEquals(80.0, matricula.calcularMensalidade(), 0.001);
        assertThrows(DadosInvalidosException.class, () -> matricula.alterarCalculo(null));
    }

    @Test
    void removerObserverDeveInterromperNotificacoes() throws Exception {
        MatriculaObserver outro = m -> notificacoes.add(null);
        matricula.adicionarObserver(outro);
        matricula.removerObserver(outro);

        matricula.cancelar();

        assertEquals(List.of(StatusMatricula.CANCELADA), notificacoes);
    }

    @Test
    void deveRejeitarArgumentosInvalidos() {
        CalculoPadraoStrategy calculo = new CalculoPadraoStrategy();
        assertThrows(DadosInvalidosException.class, () -> new Matricula(null, trimestral, INICIO, calculo));
        assertThrows(DadosInvalidosException.class, () -> new Matricula(aluno, null, INICIO, calculo));
        assertThrows(DadosInvalidosException.class, () -> new Matricula(aluno, trimestral, null, calculo));
        assertThrows(DadosInvalidosException.class, () -> new Matricula(aluno, trimestral, INICIO, null));
    }
}
