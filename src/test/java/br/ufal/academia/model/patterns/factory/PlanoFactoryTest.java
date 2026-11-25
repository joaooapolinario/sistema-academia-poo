package br.ufal.academia.model.patterns.factory;

import br.ufal.academia.exception.DadosInvalidosException;
import br.ufal.academia.model.entities.Plano;
import br.ufal.academia.model.enums.TipoPlano;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanoFactoryTest {

    @Test
    void deveCriarPlanoValidoParaCadaTipoPlano() throws Exception {
        for (TipoPlano tipo : TipoPlano.values()) {
            Plano plano = PlanoFactory.criar(tipo);

            assertEquals(tipo, plano.getTipo());
            assertFalse(plano.getNome().isBlank(), "Nome vazio para " + tipo);
            assertTrue(plano.getDuracaoMeses() > 0, "Duração inválida para " + tipo);
            assertTrue(plano.getValorMensal() > 0, "Valor mensal inválido para " + tipo);
        }
    }

    @Test
    void deveConfigurarDuracaoEValorEsperadosParaCadaPlano() throws Exception {
        Plano mensal = PlanoFactory.criar(TipoPlano.MENSAL);
        Plano trimestral = PlanoFactory.criar(TipoPlano.TRIMESTRAL);
        Plano semestral = PlanoFactory.criar(TipoPlano.SEMESTRAL);
        Plano anual = PlanoFactory.criar(TipoPlano.ANUAL);

        assertEquals("Plano Mensal", mensal.getNome());
        assertEquals(1, mensal.getDuracaoMeses());
        assertEquals(99.90, mensal.getValorMensal(), 0.001);

        assertEquals("Plano Trimestral", trimestral.getNome());
        assertEquals(3, trimestral.getDuracaoMeses());
        assertEquals(89.90, trimestral.getValorMensal(), 0.001);

        assertEquals("Plano Semestral", semestral.getNome());
        assertEquals(6, semestral.getDuracaoMeses());
        assertEquals(79.90, semestral.getValorMensal(), 0.001);

        assertEquals("Plano Anual", anual.getNome());
        assertEquals(12, anual.getDuracaoMeses());
        assertEquals(69.90, anual.getValorMensal(), 0.001);

        assertTrue(mensal.getValorMensal() > trimestral.getValorMensal());
        assertTrue(trimestral.getValorMensal() > semestral.getValorMensal());
        assertTrue(semestral.getValorMensal() > anual.getValorMensal());
    }

    @Test
    void cadaChamadaDeveRetornarNovaInstancia() throws Exception {
        Plano primeiro = PlanoFactory.criar(TipoPlano.MENSAL);
        Plano segundo = PlanoFactory.criar(TipoPlano.MENSAL);

        assertNotSame(primeiro, segundo);
        assertEquals(primeiro.getTipo(), segundo.getTipo());
        assertEquals(primeiro.getNome(), segundo.getNome());
        assertEquals(primeiro.getDuracaoMeses(), segundo.getDuracaoMeses());
        assertEquals(primeiro.getValorMensal(), segundo.getValorMensal(), 0.001);
    }

    @Test
    void deveRejeitarTipoPlanoNulo() {
        assertThrows(DadosInvalidosException.class, () -> PlanoFactory.criar(null));
    }
}
