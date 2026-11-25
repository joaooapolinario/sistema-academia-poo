package br.ufal.academia.controller;

import br.ufal.academia.exception.DadosInvalidosException;
import br.ufal.academia.model.entities.Instrutor;

import java.util.List;

// Contrato do controller de gerenciamento de instrutores.
public interface IInstrutorController {
    Instrutor cadastrar(String nome, String cpf, String telefone, String cref, String especialidade)
            throws DadosInvalidosException;
    Instrutor buscarPorCref(String cref) throws DadosInvalidosException;
    List<Instrutor> listar();
    void atualizarEspecialidade(String cref, String especialidade) throws DadosInvalidosException;
    void remover(String cref) throws DadosInvalidosException;
}
