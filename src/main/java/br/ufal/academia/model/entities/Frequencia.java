package br.ufal.academia.model.entities;

import br.ufal.academia.exception.AcessoNegadoException;
import br.ufal.academia.exception.DadosInvalidosException;

import java.time.LocalDateTime;

// Registro de entrada de um aluno na catraca vinculado a uma matrícula.
public class Frequencia {

    private int id;
    private final Matricula matricula;
    private final LocalDateTime dataHoraEntrada;

    // Novo acesso: só permite entrada se a matrícula estiver ativa e dentro do prazo.
    public Frequencia(Matricula matricula, LocalDateTime dataHoraEntrada)
            throws DadosInvalidosException, AcessoNegadoException {
        validarCampos(matricula, dataHoraEntrada);
        if (!matricula.estaAtiva()) {
            throw new AcessoNegadoException("Acesso negado: matrícula com status " + matricula.getStatus() + ".");
        }
        if (matricula.isVencida(dataHoraEntrada.toLocalDate())) {
            throw new AcessoNegadoException("Acesso negado: matrícula vencida em " + matricula.getDataFim() + ".");
        }
        this.matricula = matricula;
        this.dataHoraEntrada = dataHoraEntrada;
    }

    public Frequencia(Matricula matricula) throws DadosInvalidosException, AcessoNegadoException {
        this(matricula, LocalDateTime.now());
    }

    private Frequencia(int id, Matricula matricula, LocalDateTime dataHoraEntrada) throws DadosInvalidosException {
        validarCampos(matricula, dataHoraEntrada);
        this.matricula = matricula;
        this.dataHoraEntrada = dataHoraEntrada;
        atribuirId(id);
    }

    // Reconstrói um registro já persistido (usado pelo DAO), preservando histórico de matrículas inativas.
    public static Frequencia reconstituir(int id, Matricula matricula, LocalDateTime dataHoraEntrada)
            throws DadosInvalidosException {
        return new Frequencia(id, matricula, dataHoraEntrada);
    }

    private static void validarCampos(Matricula matricula, LocalDateTime dataHoraEntrada)
            throws DadosInvalidosException {
        if (matricula == null) {
            throw new DadosInvalidosException("A matrícula da frequência é obrigatória.");
        }
        if (dataHoraEntrada == null) {
            throw new DadosInvalidosException("A data e hora de entrada são obrigatórias.");
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
            throw new IllegalStateException("O id desta frequência já foi atribuído.");
        }
        this.id = id;
    }

    public Matricula getMatricula() {
        return matricula;
    }

    public LocalDateTime getDataHoraEntrada() {
        return dataHoraEntrada;
    }
}
