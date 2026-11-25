package br.ufal.academia.model.patterns.factory;

import br.ufal.academia.exception.DadosInvalidosException;
import br.ufal.academia.model.entities.Plano;
import br.ufal.academia.model.enums.TipoPlano;

// PADRÃO FACTORY: centraliza a criação dos planos padrão da academia a partir do TipoPlano.
// O restante do sistema não instancia new Plano(...) diretamente, evitando espalhar durações e valores base.
public final class PlanoFactory {

    private PlanoFactory() {
    }

    // Cria uma instância configurada de Plano para o TipoPlano informado.
    // O switch sem default garante que um novo TipoPlano só compila depois de ser configurado aqui.
    public static Plano criar(TipoPlano tipo) throws DadosInvalidosException {
        if (tipo == null) {
            throw new DadosInvalidosException("O tipo do plano é obrigatório.");
        }
        return switch (tipo) {
            case MENSAL -> new Plano(TipoPlano.MENSAL, "Plano Mensal", 1, 99.90);
            case TRIMESTRAL -> new Plano(TipoPlano.TRIMESTRAL, "Plano Trimestral", 3, 89.90);
            case SEMESTRAL -> new Plano(TipoPlano.SEMESTRAL, "Plano Semestral", 6, 79.90);
            case ANUAL -> new Plano(TipoPlano.ANUAL, "Plano Anual", 12, 69.90);
        };
    }
}
