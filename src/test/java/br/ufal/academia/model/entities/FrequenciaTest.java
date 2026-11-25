package br.ufal.academia.model.entities;

import br.ufal.academia.exception.AcessoNegadoException;
import br.ufal.academia.exception.DadosInvalidosException;
import br.ufal.academia.model.enums.ObjetivoTreino;
import br.ufal.academia.model.enums.TipoPlano;
import br.ufal.academia.model.patterns.strategy.CalculoPadraoStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FrequenciaTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 11, 1);

    private Matricula matricula;

    @BeforeEach
    void setUp() throws Exception {
        Aluno aluno = new Aluno("Maria Souza", "52998224725", "82999990000",
                LocalDate.of(2000, 1, 10), ObjetivoTreino.HIPERTROFIA);
        Plano mensal = new Plano(TipoPlano.MENSAL, "Mensal", 1, 99.9);
        matricula = new Matricula(aluno, mensal, INICIO, new CalculoPadraoStrategy());
    }

    @Test
    void deveCriarFrequenciaParaMatriculaAtivaDentroDoPrazo() throws Exception {
        LocalDateTime ultimoDia = LocalDateTime.of(2026, 12, 1, 20, 30);

        Frequencia frequencia = new Frequencia(matricula, ultimoDia);

        assertEquals(matricula, frequencia.getMatricula());
        assertEquals(ultimoDia, frequencia.getDataHoraEntrada());
    }

    @Test
    void deveNegarAcessoParaMatriculaCancelada() throws Exception {
        matricula.cancelar();

        assertThrows(AcessoNegadoException.class,
                () -> new Frequencia(matricula, LocalDateTime.of(2026, 11, 10, 8, 0)));
    }

    @Test
    void deveNegarAcessoParaMatriculaVencida() throws Exception {
        // Mesmo antes de verificarVencimento(), a data após dataFim deve negar acesso.
        assertThrows(AcessoNegadoException.class,
                () -> new Frequencia(matricula, LocalDateTime.of(2026, 12, 2, 8, 0)));

        matricula.verificarVencimento(LocalDate.of(2026, 12, 2));
        assertThrows(AcessoNegadoException.class,
                () -> new Frequencia(matricula, LocalDateTime.of(2026, 11, 15, 8, 0)));
    }

    @Test
    void deveReconstituirFrequenciaHistoricaMesmoComMatriculaInativa() throws Exception {
        matricula.cancelar();
        LocalDateTime entradaAntiga = LocalDateTime.of(2026, 11, 5, 7, 15);

        Frequencia frequencia = Frequencia.reconstituir(10, matricula, entradaAntiga);

        assertEquals(10, frequencia.getId());
        assertEquals(matricula, frequencia.getMatricula());
        assertEquals(entradaAntiga, frequencia.getDataHoraEntrada());
    }

    @Test
    void deveRejeitarDadosInvalidos() throws Exception {
        LocalDateTime agora = LocalDateTime.of(2026, 11, 10, 9, 0);

        assertThrows(DadosInvalidosException.class, () -> new Frequencia(null, agora));
        assertThrows(DadosInvalidosException.class, () -> new Frequencia(matricula, null));
        assertThrows(DadosInvalidosException.class, () -> Frequencia.reconstituir(1, null, agora));
        assertThrows(DadosInvalidosException.class, () -> Frequencia.reconstituir(1, matricula, null));

        Frequencia frequencia = new Frequencia(matricula, agora);
        assertThrows(IllegalArgumentException.class, () -> frequencia.atribuirId(0));
        assertThrows(IllegalArgumentException.class, () -> frequencia.atribuirId(-1));
        assertThrows(IllegalArgumentException.class, () -> Frequencia.reconstituir(0, matricula, agora));
    }

    @Test
    void idSoPodeSerAtribuidoUmaVezEDeveSerPositivo() throws Exception {
        Frequencia frequencia = new Frequencia(matricula, LocalDateTime.of(2026, 11, 10, 9, 0));

        assertEquals(0, frequencia.getId());
        assertThrows(IllegalArgumentException.class, () -> frequencia.atribuirId(0));
        assertThrows(IllegalArgumentException.class, () -> frequencia.atribuirId(-5));

        frequencia.atribuirId(9);
        assertEquals(9, frequencia.getId());
        assertThrows(IllegalStateException.class, () -> frequencia.atribuirId(10));
    }
}
