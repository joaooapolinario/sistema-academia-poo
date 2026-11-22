package br.ufal.academia.dao;

import br.ufal.academia.model.entities.Treino;

import java.util.List;
import java.util.Optional;

// Contrato DAO para persistência de Treino.
public interface TreinoDAO {
    void salvar(Treino treino);
    void atualizar(Treino treino);
    void remover(int id);
    Optional<Treino> buscarPorId(int id);
    List<Treino> listarTodos();
    List<Treino> listarPorAluno(int idAluno);
}
