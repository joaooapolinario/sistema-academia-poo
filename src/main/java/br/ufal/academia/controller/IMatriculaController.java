package br.ufal.academia.controller;

import br.ufal.academia.exception.DadosInvalidosException;
import br.ufal.academia.exception.MatriculaAtivaExistenteException;
import br.ufal.academia.model.entities.Matricula;
import br.ufal.academia.model.entities.Plano;
import br.ufal.academia.model.enums.TipoPlano;

import java.util.List;

// Contrato do controller de gerenciamento de matrículas, vigências e faturamento.
public interface IMatriculaController {
    Matricula matricular(String cpfAluno, TipoPlano tipo, boolean estudante)
            throws DadosInvalidosException, MatriculaAtivaExistenteException;
    void cancelar(int idMatricula) throws DadosInvalidosException;
    void renovar(int idMatricula) throws DadosInvalidosException;
    void atualizarVencimentos();
    List<Matricula> listar();
    List<Matricula> listarAVencer(int dias) throws DadosInvalidosException;
    List<Plano> listarPlanos();
    double simularMensalidade(String cpfAluno, TipoPlano tipo, boolean estudante) throws DadosInvalidosException;
    double calcularFaturamentoMensal();
}
