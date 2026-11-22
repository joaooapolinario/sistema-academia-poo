package br.ufal.academia.model.patterns.strategy;

import br.ufal.academia.model.entities.Plano;

// Strategy para cálculo do valor da mensalidade de um plano.
public interface CalculoMensalidadeStrategy {
    double calcular(Plano plano);
}
