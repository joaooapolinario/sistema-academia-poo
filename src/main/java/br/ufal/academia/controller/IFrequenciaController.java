package br.ufal.academia.controller;

import br.ufal.academia.exception.AcessoNegadoException;
import br.ufal.academia.exception.DadosInvalidosException;
import br.ufal.academia.model.entities.Frequencia;

import java.time.YearMonth;
import java.util.List;

// Contrato do controller de frequência e controle de acesso na catraca.
public interface IFrequenciaController {
    Frequencia registrarEntrada(String cpf) throws DadosInvalidosException, AcessoNegadoException;
    List<Frequencia> listarPorAluno(String cpf, YearMonth mes) throws DadosInvalidosException;
    int contarPresencas(String cpf, YearMonth mes) throws DadosInvalidosException;
}
