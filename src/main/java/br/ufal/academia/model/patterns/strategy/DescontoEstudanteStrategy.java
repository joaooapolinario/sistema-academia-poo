package br.ufal.academia.model.patterns.strategy;

import br.ufal.academia.model.entities.Plano;

// Mensalidade com desconto para estudantes.
public class DescontoEstudanteStrategy implements CalculoMensalidadeStrategy {

    public static final String ID = "ESTUDANTE";
    public static final double PERCENTUAL_DESCONTO = 0.20;

    @Override
    public double calcular(Plano plano) {
        if (plano == null) {
            throw new IllegalArgumentException("O plano não pode ser nulo.");
        }
        return arredondar(plano.getValorMensal() * (1 - PERCENTUAL_DESCONTO));
    }

    @Override
    public String identificador() {
        return ID;
    }
}
