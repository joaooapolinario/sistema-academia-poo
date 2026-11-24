package br.ufal.academia.model.entities;

import br.ufal.academia.exception.DadosInvalidosException;
import br.ufal.academia.model.enums.ObjetivoTreino;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Ficha de treino prescrita por um Instrutor para um Aluno, com a lista de exercícios.
public class Treino {

    private int id;
    private final Aluno aluno;
    private final Instrutor instrutor;
    private final ObjetivoTreino objetivo;
    private final List<String> exercicios = new ArrayList<>();
    private final LocalDate dataCriacao;

    // Novo treino: começa sem exercícios e com a data de criação de hoje.
    public Treino(Aluno aluno, Instrutor instrutor, ObjetivoTreino objetivo) throws DadosInvalidosException {
        this(aluno, instrutor, objetivo, LocalDate.now());
    }

    private Treino(Aluno aluno, Instrutor instrutor, ObjetivoTreino objetivo, LocalDate dataCriacao)
            throws DadosInvalidosException {
        if (aluno == null) {
            throw new DadosInvalidosException("O aluno do treino é obrigatório.");
        }
        if (instrutor == null) {
            throw new DadosInvalidosException("O instrutor do treino é obrigatório.");
        }
        if (objetivo == null) {
            throw new DadosInvalidosException("O objetivo do treino é obrigatório.");
        }
        if (dataCriacao == null) {
            throw new DadosInvalidosException("A data de criação do treino é obrigatória.");
        }
        this.aluno = aluno;
        this.instrutor = instrutor;
        this.objetivo = objetivo;
        this.dataCriacao = dataCriacao;
    }

    // Reconstrói um treino já persistido (usado pelo DAO), preservando exercícios e data gravados.
    public static Treino reconstituir(int id, Aluno aluno, Instrutor instrutor, ObjetivoTreino objetivo,
                                      List<String> exercicios, LocalDate dataCriacao)
            throws DadosInvalidosException {
        Treino treino = new Treino(aluno, instrutor, objetivo, dataCriacao);
        if (exercicios != null) {
            for (String exercicio : exercicios) {
                treino.adicionarExercicio(exercicio);
            }
        }
        treino.atribuirId(id);
        return treino;
    }

    public void adicionarExercicio(String descricao) throws DadosInvalidosException {
        if (descricao == null || descricao.isBlank()) {
            throw new DadosInvalidosException("A descrição do exercício não pode ser vazia.");
        }
        String texto = descricao.trim();
        if (exercicios.contains(texto)) {
            throw new DadosInvalidosException("Exercício já presente no treino: " + texto);
        }
        exercicios.add(texto);
    }

    public void removerExercicio(String descricao) throws DadosInvalidosException {
        if (descricao == null || !exercicios.remove(descricao.trim())) {
            throw new DadosInvalidosException("Exercício não encontrado no treino: " + descricao);
        }
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
            throw new IllegalStateException("O id deste treino já foi atribuído.");
        }
        this.id = id;
    }

    public Aluno getAluno() {
        return aluno;
    }

    public Instrutor getInstrutor() {
        return instrutor;
    }

    public ObjetivoTreino getObjetivo() {
        return objetivo;
    }

    public List<String> getExercicios() {
        return List.copyOf(exercicios);
    }

    public LocalDate getDataCriacao() {
        return dataCriacao;
    }
}
