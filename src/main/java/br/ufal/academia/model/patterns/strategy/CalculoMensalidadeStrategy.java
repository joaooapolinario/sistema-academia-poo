package br.ufal.academia.model.patterns.strategy;

import br.ufal.academia.model.entities.Plano;

import java.math.BigDecimal;
import java.math.RoundingMode;

// Strategy para cálculo do valor da mensalidade de um plano.
public interface CalculoMensalidadeStrategy {
    double calcular(Plano plano);

    // Identificador persistido em matriculas.tipo_calculo (PADRAO, ESTUDANTE, IDOSO).
    String identificador();

    // Arredonda valores monetários para 2 casas decimais usando HALF_UP.
    default double arredondar(double valor) {
        if (Double.isNaN(valor) || Double.isInfinite(valor)) {
            throw new IllegalArgumentException("O valor monetário deve ser finito.");
        }
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    // Recria a estratégia a partir do identificador gravado no banco.
    static CalculoMensalidadeStrategy deIdentificador(String identificador) {
        if (identificador == null) {
            throw new IllegalArgumentException("O identificador da estratégia de cálculo não pode ser nulo.");
        }
        return switch (identificador) {
            case CalculoPadraoStrategy.ID -> new CalculoPadraoStrategy();
            case DescontoEstudanteStrategy.ID -> new DescontoEstudanteStrategy();
            case DescontoIdosoStrategy.ID -> new DescontoIdosoStrategy();
            default -> throw new IllegalArgumentException("Estratégia de cálculo desconhecida: " + identificador);
        };
    }
}
