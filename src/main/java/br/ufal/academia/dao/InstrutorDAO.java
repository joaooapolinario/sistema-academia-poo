package br.ufal.academia.dao;

import br.ufal.academia.model.entities.Instrutor;

import java.util.List;
import java.util.Optional;

// Contrato DAO para persistência de Instrutor.
public interface InstrutorDAO {
    void salvar(Instrutor instrutor);
    void atualizar(Instrutor instrutor);
    void remover(int id);
    Optional<Instrutor> buscarPorId(int id);
    List<Instrutor> listarTodos();
    Optional<Instrutor> buscarPorCref(String cref);
}
