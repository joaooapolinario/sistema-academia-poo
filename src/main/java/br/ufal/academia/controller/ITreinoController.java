package br.ufal.academia.controller;

import br.ufal.academia.exception.DadosInvalidosException;
import br.ufal.academia.model.entities.Treino;
import br.ufal.academia.model.enums.ObjetivoTreino;

import java.util.List;

// Contrato do controller de prescrição e manutenção de fichas de treino.
public interface ITreinoController {
    Treino criar(String cpfAluno, String crefInstrutor, ObjetivoTreino objetivo) throws DadosInvalidosException;
    void adicionarExercicio(int idTreino, String descricao) throws DadosInvalidosException;
    void removerExercicio(int idTreino, String descricao) throws DadosInvalidosException;
    List<Treino> listarPorAluno(String cpf) throws DadosInvalidosException;
    void remover(int idTreino) throws DadosInvalidosException;
}
