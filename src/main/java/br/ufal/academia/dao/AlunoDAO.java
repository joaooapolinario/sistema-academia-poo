package br.ufal.academia.dao;

import br.ufal.academia.model.entities.Aluno;

import java.util.List;
import java.util.Optional;

// Contrato DAO para persistência de Aluno.
public interface AlunoDAO {
    void salvar(Aluno aluno);
    void atualizar(Aluno aluno);
    void remover(int id);
    Optional<Aluno> buscarPorId(int id);
    List<Aluno> listarTodos();
    Optional<Aluno> buscarPorCpf(String cpf);
}
