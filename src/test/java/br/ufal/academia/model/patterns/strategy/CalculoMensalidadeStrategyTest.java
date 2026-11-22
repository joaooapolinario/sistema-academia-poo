package br.ufal.academia.model.patterns.strategy;

import br.ufal.academia.model.entities.Plano;
import br.ufal.academia.model.enums.TipoPlano;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CalculoMensalidadeStrategyTest {

    private Plano plano(double valor) throws Exception {
        return new Plano(TipoPlano.MENSAL, "Mensal", 1, valor);
    }

    @Test
    void padraoCobraValorCheio() throws Exception {
        assertEquals(100.0, new CalculoPadraoStrategy().calcular(plano(100.0)), 0.001);
    }

    @Test
    void estudanteAplica20PorCentoDeDesconto() throws Exception {
        assertEquals(80.0, new DescontoEstudanteStrategy().calcular(plano(100.0)), 0.001);
    }

    @Test
    void idosoAplica30PorCentoDeDesconto() throws Exception {
        assertEquals(70.0, new DescontoIdosoStrategy().calcular(plano(100.0)), 0.001);
    }

    @Test
    void deveArredondarParaDuasCasasDecimais() throws Exception {
        // 89,90 * 0,80 = 71,92 ; 99,99 * 0,70 = 69,993 -> 69,99
        assertEquals(71.92, new DescontoEstudanteStrategy().calcular(plano(89.90)), 0.0001);
        assertEquals(69.99, new DescontoIdosoStrategy().calcular(plano(99.99)), 0.0001);
    }

    @Test
    void deveRecriarEstrategiaPeloIdentificador() {
        assertInstanceOf(CalculoPadraoStrategy.class, CalculoMensalidadeStrategy.deIdentificador("PADRAO"));
        assertInstanceOf(DescontoEstudanteStrategy.class, CalculoMensalidadeStrategy.deIdentificador("ESTUDANTE"));
        assertInstanceOf(DescontoIdosoStrategy.class, CalculoMensalidadeStrategy.deIdentificador("IDOSO"));
        assertThrows(IllegalArgumentException.class, () -> CalculoMensalidadeStrategy.deIdentificador("XYZ"));
        assertThrows(IllegalArgumentException.class, () -> CalculoMensalidadeStrategy.deIdentificador(null));
    }

    @Test
    void identificadoresDevemCasarComOCheckDoBanco() {
        assertEquals("PADRAO", new CalculoPadraoStrategy().identificador());
        assertEquals("ESTUDANTE", new DescontoEstudanteStrategy().identificador());
        assertEquals("IDOSO", new DescontoIdosoStrategy().identificador());
    }
}
