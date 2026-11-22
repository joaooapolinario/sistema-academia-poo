package br.ufal.academia.model.patterns.strategy;

import br.ufal.academia.model.entities.Plano;

// Mensalidade com desconto para idosos.
public class DescontoIdosoStrategy implements CalculoMensalidadeStrategy {

    public static final String ID = "IDOSO";
    public static final double PERCENTUAL_DESCONTO = 0.30;

    @Override
    public double calcular(Plano plano) {
        return Dinheiro.arredondar(plano.getValorMensal() * (1 - PERCENTUAL_DESCONTO));
    }

    @Override
    public String identificador() {
        return ID;
    }
}
