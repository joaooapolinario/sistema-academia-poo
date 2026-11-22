package br.ufal.academia.dao;

import br.ufal.academia.model.entities.Plano;
import br.ufal.academia.model.enums.TipoPlano;

import java.util.List;
import java.util.Optional;

// Contrato DAO para persistência de Plano.
public interface PlanoDAO {
    void salvar(Plano plano);
    void atualizar(Plano plano);
    void remover(int id);
    Optional<Plano> buscarPorId(int id);
    List<Plano> listarTodos();
    Optional<Plano> buscarPorTipo(TipoPlano tipo);
}
