package br.ufal.academia.model.entities;

import br.ufal.academia.exception.CpfInvalidoException;
import br.ufal.academia.exception.DadosInvalidosException;

public abstract class Pessoa {
    private int id;
    private final String nome;
    private final String cpf;
    private String telefone;

    protected Pessoa(String nome, String cpf, String telefone) throws DadosInvalidosException {
        this.nome = validarNome(nome);
        this.cpf = validarCpf(cpf);
        this.telefone = validarTelefone(telefone);
    }

    public int getId() {
        return id;
    }

    public void atribuirId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("O id deve ser positivo.");
        }
        if (this.id != 0) {
            throw new IllegalStateException("O id desta pessoa já foi atribuído.");
        }
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public String getCpf() {
        return cpf;
    }

    public String getTelefone() {
        return telefone;
    }

    public void atualizarTelefone(String telefone) throws DadosInvalidosException {
        this.telefone = validarTelefone(telefone);
    }

    private String validarNome(String nome) throws DadosInvalidosException {
        if (nome == null || nome.isBlank()) {
            throw new DadosInvalidosException("O nome é obrigatório.");
        }
        return nome.trim();
    }

    private String validarCpf(String cpf) throws CpfInvalidoException {
        if (cpf == null) {
            throw new CpfInvalidoException("(vazio)");
        }
        String digitos = cpf.replaceAll("\\D", "");
        if (digitos.length() != 11 || digitos.chars().distinct().count() == 1) {
            throw new CpfInvalidoException(cpf);
        }
        int primeiroDigito = calcularDigitoVerificador(digitos, 9);
        int segundoDigito = calcularDigitoVerificador(digitos, 10);
        if (primeiroDigito != digitos.charAt(9) - '0' || segundoDigito != digitos.charAt(10) - '0') {
            throw new CpfInvalidoException(cpf);
        }
        return digitos;
    }

    private int calcularDigitoVerificador(String digitos, int quantidade) {
        int soma = 0;
        for (int i = 0; i < quantidade; i++) {
            int digito = digitos.charAt(i) - '0';
            soma += digito * (quantidade + 1 - i);
        }
        int resto = (soma * 10) % 11;
        if (resto == 10) {
            return 0;
        } else {
            return resto;
        }
    }

    private String validarTelefone(String telefone) throws DadosInvalidosException {
        if (telefone == null) {
            throw new DadosInvalidosException("O telefone é obrigatório.");
        }
        String digitos = telefone.replaceAll("\\D", "");
        if (digitos.length() < 10 || digitos.length() > 11) {
            throw new DadosInvalidosException("Telefone inválido: " + telefone);
        }
        return digitos;
    }
}
