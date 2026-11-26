package br.ufal.academia.model.patterns.strategy;

import br.ufal.academia.model.entities.Plano;

// Mensalidade padrão: valor cheio do plano, sem desconto.
public class CalculoPadraoStrategy implements CalculoMensalidadeStrategy {

    public static final String ID = "PADRAO";

    @Override
    public double calcular(Plano plano) {
        if (plano == null) {
            throw new IllegalArgumentException("O plano não pode ser nulo.");
        }
        return arredondar(plano.getValorMensal());
    }

    @Override
    public String identificador() {
        return ID;
    }
}
