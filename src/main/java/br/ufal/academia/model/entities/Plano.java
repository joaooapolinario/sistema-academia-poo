package br.ufal.academia.model.entities;

import br.ufal.academia.exception.DadosInvalidosException;
import br.ufal.academia.model.enums.TipoPlano;

// Plano oferecido pela academia (tipo, duração e valor mensal base).
public class Plano {

    private int id;
    private final TipoPlano tipo;
    private final String nome;
    private final int duracaoMeses;
    private final double valorMensal;

    public Plano(TipoPlano tipo, String nome, int duracaoMeses, double valorMensal) throws DadosInvalidosException {
        if (tipo == null) {
            throw new DadosInvalidosException("O tipo do plano é obrigatório.");
        }
        if (nome == null || nome.isBlank()) {
            throw new DadosInvalidosException("O nome do plano é obrigatório.");
        }
        if (duracaoMeses <= 0) {
            throw new DadosInvalidosException("A duração do plano deve ser maior que zero.");
        }
        if (Double.isNaN(valorMensal) || Double.isInfinite(valorMensal) || valorMensal <= 0) {
            throw new DadosInvalidosException("O valor mensal do plano deve ser maior que zero.");
        }
        this.tipo = tipo;
        this.nome = nome.trim();
        this.duracaoMeses = duracaoMeses;
        this.valorMensal = valorMensal;
    }

    public int getId() {
        return id;
    }

    // Chamado pelo DAO após o INSERT para registrar o id gerado pelo banco.
    public void atribuirId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("O id deve ser positivo.");
        }
        if (this.id != 0) {
            throw new IllegalStateException("O id deste plano já foi atribuído.");
        }
        this.id = id;
    }

    public TipoPlano getTipo() {
        return tipo;
    }

    public String getNome() {
        return nome;
    }

    public int getDuracaoMeses() {
        return duracaoMeses;
    }

    public double getValorMensal() {
        return valorMensal;
    }
}
