package br.ufal.academia.dao;

import br.ufal.academia.model.entities.Matricula;
import br.ufal.academia.model.enums.StatusMatricula;

import java.util.List;
import java.util.Optional;

// Contrato DAO para persistência de Matricula.
public interface MatriculaDAO {
    void salvar(Matricula matricula);
    void atualizar(Matricula matricula);
    void remover(int id);
    Optional<Matricula> buscarPorId(int id);
    List<Matricula> listarTodos();
    Optional<Matricula> buscarAtivaPorAluno(int idAluno);
    List<Matricula> listarPorStatus(StatusMatricula status);
}
