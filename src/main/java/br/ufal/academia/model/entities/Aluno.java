package br.ufal.academia.model.entities;

import br.ufal.academia.exception.DadosInvalidosException;
import br.ufal.academia.model.enums.ObjetivoTreino;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

public class Aluno extends Pessoa {
    private final LocalDate dataNascimento;
    private ObjetivoTreino objetivo;
    private final List<String> restricoesMedicas = new ArrayList<>();

    public Aluno(String nome, String cpf, String telefone, LocalDate dataNascimento,
                 ObjetivoTreino objetivo) throws DadosInvalidosException {
        super(nome, cpf, telefone);
        this.dataNascimento = validarDataNascimento(dataNascimento);
        this.objetivo = validarObjetivo(objetivo);
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public int getIdade() {
        return Period.between(dataNascimento, LocalDate.now()).getYears();
    }

    public ObjetivoTreino getObjetivo() {
        return objetivo;
    }

    public void alterarObjetivo(ObjetivoTreino objetivo) throws DadosInvalidosException {
        this.objetivo = validarObjetivo(objetivo);
    }

    public List<String> getRestricoesMedicas() {
        return List.copyOf(restricoesMedicas);
    }

    public void adicionarRestricao(String restricao) throws DadosInvalidosException {
        if (restricao == null || restricao.isBlank()) {
            throw new DadosInvalidosException("A restrição médica não pode ser vazia.");
        }
        String texto = restricao.trim();
        if (restricoesMedicas.contains(texto)) {
            throw new DadosInvalidosException("Restrição já cadastrada: " + texto);
        }
        restricoesMedicas.add(texto);
    }

    public void removerRestricao(String restricao) throws DadosInvalidosException {
        if (restricao == null || !restricoesMedicas.remove(restricao.trim())) {
            throw new DadosInvalidosException("Restrição não encontrada: " + restricao);
        }
    }

    private LocalDate validarDataNascimento(LocalDate data) throws DadosInvalidosException {
        if (data == null) {
            throw new DadosInvalidosException("A data de nascimento é obrigatória.");
        }
        if (data.isAfter(LocalDate.now())) {
            throw new DadosInvalidosException("A data de nascimento não pode ser no futuro.");
        }
        return data;
    }

    private ObjetivoTreino validarObjetivo(ObjetivoTreino objetivo) throws DadosInvalidosException {
        if (objetivo == null) {
            throw new DadosInvalidosException("O objetivo de treino é obrigatório.");
        }
        return objetivo;
    }
}
