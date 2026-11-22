package br.ufal.academia.model.entities;

import br.ufal.academia.exception.DadosInvalidosException;
import br.ufal.academia.model.enums.TipoPlano;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlanoTest {

    @Test
    void deveCriarPlanoValido() throws Exception {
        Plano plano = new Plano(TipoPlano.TRIMESTRAL, "  Trimestral ", 3, 89.9);

        assertEquals(TipoPlano.TRIMESTRAL, plano.getTipo());
        assertEquals("Trimestral", plano.getNome());
        assertEquals(3, plano.getDuracaoMeses());
        assertEquals(89.9, plano.getValorMensal(), 0.001);
    }

    @Test
    void deveRejeitarDadosInvalidos() {
        assertThrows(DadosInvalidosException.class, () -> new Plano(null, "Mensal", 1, 10));
        assertThrows(DadosInvalidosException.class, () -> new Plano(TipoPlano.MENSAL, " ", 1, 10));
        assertThrows(DadosInvalidosException.class, () -> new Plano(TipoPlano.MENSAL, null, 1, 10));
        assertThrows(DadosInvalidosException.class, () -> new Plano(TipoPlano.MENSAL, "Mensal", 0, 10));
        assertThrows(DadosInvalidosException.class, () -> new Plano(TipoPlano.MENSAL, "Mensal", 1, 0));
        assertThrows(DadosInvalidosException.class, () -> new Plano(TipoPlano.MENSAL, "Mensal", 1, -5));
        assertThrows(DadosInvalidosException.class, () -> new Plano(TipoPlano.MENSAL, "Mensal", 1, Double.NaN));
    }
}
