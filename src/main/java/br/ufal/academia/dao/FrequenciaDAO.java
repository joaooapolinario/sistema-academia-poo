package br.ufal.academia.dao;

import br.ufal.academia.model.entities.Frequencia;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

// Contrato DAO para persistência de Frequencia.
public interface FrequenciaDAO {
    void salvar(Frequencia frequencia);
    void atualizar(Frequencia frequencia);
    void remover(int id);
    Optional<Frequencia> buscarPorId(int id);
    List<Frequencia> listarTodos();
    List<Frequencia> listarPorAlunoEPeriodo(int idAluno, LocalDate inicio, LocalDate fim);
}
