package br.ufal.academia.model.patterns.strategy;

// Utilitário interno do pacote: arredonda valores monetários para 2 casas decimais.
final class Dinheiro {

    private Dinheiro() {
    }

    static double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
