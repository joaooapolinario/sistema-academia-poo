package br.ufal.academia.exception;

public class CpfInvalidoException extends DadosInvalidosException {
    public CpfInvalidoException(String cpf) {
        super("CPF inválido: " + cpf);
    }
}