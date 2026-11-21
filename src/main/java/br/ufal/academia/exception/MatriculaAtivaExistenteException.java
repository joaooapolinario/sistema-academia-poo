package br.ufal.academia.exception;

public class MatriculaAtivaExistenteException extends Exception {
    public MatriculaAtivaExistenteException(String cpfAluno) {
        super("Já existe matrícula ativa para o aluno com CPF: " + cpfAluno);
    }
}