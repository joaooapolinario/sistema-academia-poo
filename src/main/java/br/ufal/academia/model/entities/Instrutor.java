package br.ufal.academia.model.entities;

import br.ufal.academia.exception.DadosInvalidosException;

public class Instrutor extends Pessoa {
    private static final String FORMATO_CREF = "\\d{6}-[GP]/[A-Z]{2}";

    private final String cref;
    private String especialidade;

    public Instrutor(String nome, String cpf, String telefone, String cref,
                     String especialidade) throws DadosInvalidosException {
        super(nome, cpf, telefone);
        this.cref = validarCref(cref);
        this.especialidade = validarEspecialidade(especialidade);
    }

    public String getCref() {
        return cref;
    }

    public String getEspecialidade() {
        return especialidade;
    }

    public void alterarEspecialidade(String especialidade) throws DadosInvalidosException {
        this.especialidade = validarEspecialidade(especialidade);
    }

    private String validarCref(String cref) throws DadosInvalidosException {
        if (cref == null) {
            throw new DadosInvalidosException("O CREF é obrigatório.");
        }
        String texto = cref.trim().toUpperCase();
        if (!texto.matches(FORMATO_CREF)) {
            throw new DadosInvalidosException("CREF inválido: " + cref + " (formato esperado: 000000-G/UF)");
        }
        return texto;
    }

    private String validarEspecialidade(String especialidade) throws DadosInvalidosException {
        if (especialidade == null || especialidade.isBlank()) {
            throw new DadosInvalidosException("A especialidade é obrigatória.");
        }
        return especialidade.trim();
    }
}
