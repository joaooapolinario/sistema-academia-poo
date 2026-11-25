package br.ufal.academia.controller;

import br.ufal.academia.exception.DadosInvalidosException;
import br.ufal.academia.model.entities.Aluno;
import br.ufal.academia.model.enums.ObjetivoTreino;

import java.time.LocalDate;
import java.util.List;

// Contrato do controller de gerenciamento de alunos.
public interface IAlunoController {
    Aluno cadastrar(String nome, String cpf, String telefone, LocalDate dataNascimento, ObjetivoTreino objetivo)
            throws DadosInvalidosException;
    Aluno buscarPorCpf(String cpf) throws DadosInvalidosException;
    List<Aluno> listar();
    void atualizarTelefone(String cpf, String telefone) throws DadosInvalidosException;
    void adicionarRestricaoMedica(String cpf, String restricao) throws DadosInvalidosException;
    void removerRestricaoMedica(String cpf, String restricao) throws DadosInvalidosException;
    void remover(String cpf) throws DadosInvalidosException;
}
