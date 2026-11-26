package br.ufal.academia.model.entities;

import br.ufal.academia.exception.DadosInvalidosException;
import br.ufal.academia.model.enums.StatusMatricula;
import br.ufal.academia.model.patterns.observer.MatriculaObserver;
import br.ufal.academia.model.patterns.strategy.CalculoMensalidadeStrategy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Matrícula de um aluno em um plano. Sujeito do Observer e contexto do Strategy de mensalidade.
public class Matricula {

    private int id;
    private final Aluno aluno;
    private final Plano plano;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private StatusMatricula status;
    private CalculoMensalidadeStrategy calculoMensalidade;
    private final List<MatriculaObserver> observers = new ArrayList<>();

    // Nova matrícula: nasce ATIVA e termina duracaoMeses após o início.
    public Matricula(Aluno aluno, Plano plano, LocalDate dataInicio, CalculoMensalidadeStrategy calculo)
            throws DadosInvalidosException {
        this(aluno, plano, dataInicio, calcularFim(plano, dataInicio), StatusMatricula.ATIVA, calculo);
    }

    private Matricula(Aluno aluno, Plano plano, LocalDate dataInicio, LocalDate dataFim,
                      StatusMatricula status, CalculoMensalidadeStrategy calculo) throws DadosInvalidosException {
        if (aluno == null) {
            throw new DadosInvalidosException("O aluno da matrícula é obrigatório.");
        }
        if (calculo == null) {
            throw new DadosInvalidosException("A estratégia de cálculo da mensalidade é obrigatória.");
        }
        if (status == null) {
            throw new DadosInvalidosException("O status da matrícula é obrigatório.");
        }
        if (dataFim.isBefore(dataInicio)) {
            throw new DadosInvalidosException("A data de fim não pode ser anterior à data de início.");
        }
        this.aluno = aluno;
        this.plano = plano;
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.status = status;
        this.calculoMensalidade = calculo;
    }

    // Reconstrói uma matrícula já persistida (usado pelo DAO), preservando datas e status gravados.
    public static Matricula reconstituir(int id, Aluno aluno, Plano plano, LocalDate dataInicio, LocalDate dataFim,
                                         StatusMatricula status, CalculoMensalidadeStrategy calculo)
            throws DadosInvalidosException {
        if (plano == null) {
            throw new DadosInvalidosException("O plano da matrícula é obrigatório.");
        }
        if (dataInicio == null || dataFim == null) {
            throw new DadosInvalidosException("As datas da matrícula são obrigatórias.");
        }
        Matricula matricula = new Matricula(aluno, plano, dataInicio, dataFim, status, calculo);
        matricula.atribuirId(id);
        return matricula;
    }

    private static LocalDate calcularFim(Plano plano, LocalDate dataInicio) throws DadosInvalidosException {
        if (plano == null) {
            throw new DadosInvalidosException("O plano da matrícula é obrigatório.");
        }
        if (dataInicio == null) {
            throw new DadosInvalidosException("A data de início da matrícula é obrigatória.");
        }
        return dataInicio.plusMonths(plano.getDuracaoMeses());
    }

    public double calcularMensalidade() {
        return calculoMensalidade.calcular(plano);
    }

    public boolean estaAtiva() {
        return status == StatusMatricula.ATIVA;
    }

    // Uma matrícula está vencida quando continua ATIVA após a data de fim (o último dia ainda é válido).
    public boolean isVencida(LocalDate hoje) {
        if (hoje == null) {
            throw new IllegalArgumentException("A data de referência não pode ser nula.");
        }
        return status == StatusMatricula.ATIVA && hoje.isAfter(dataFim);
    }

    // Atualiza o status para VENCIDA se o prazo acabou e notifica os observers.
    public void verificarVencimento(LocalDate hoje) {
        if (isVencida(hoje)) {
            status = StatusMatricula.VENCIDA;
            notificarObservers();
        }
    }

    public void cancelar() throws DadosInvalidosException {
        if (status == StatusMatricula.CANCELADA) {
            throw new DadosInvalidosException("A matrícula já está cancelada.");
        }
        status = StatusMatricula.CANCELADA;
        notificarObservers();
    }

    public void renovar() throws DadosInvalidosException {
        renovar(LocalDate.now());
    }

    // ATIVA vigente estende a partir da data de fim atual; VENCIDA (ou com prazo expirado) recomeça a partir de hoje; CANCELADA não renova.
    public void renovar(LocalDate hoje) throws DadosInvalidosException {
        if (hoje == null) {
            throw new DadosInvalidosException("A data de referência para renovação é obrigatória.");
        }
        if (status == StatusMatricula.CANCELADA) {
            throw new DadosInvalidosException("Uma matrícula cancelada não pode ser renovada.");
        }
        LocalDate base = (status == StatusMatricula.VENCIDA || isVencida(hoje)) ? hoje : dataFim;
        dataInicio = base;
        dataFim = base.plusMonths(plano.getDuracaoMeses());
        status = StatusMatricula.ATIVA;
        notificarObservers();
    }

    public void alterarCalculo(CalculoMensalidadeStrategy calculo) throws DadosInvalidosException {
        if (calculo == null) {
            throw new DadosInvalidosException("A estratégia de cálculo da mensalidade é obrigatória.");
        }
        this.calculoMensalidade = calculo;
    }

    public void adicionarObserver(MatriculaObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void removerObserver(MatriculaObserver observer) {
        observers.remove(observer);
    }

    private void notificarObservers() {
        for (MatriculaObserver observer : List.copyOf(observers)) {
            observer.atualizar(this);
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
            throw new IllegalStateException("O id desta matrícula já foi atribuído.");
        }
        this.id = id;
    }

    public Aluno getAluno() {
        return aluno;
    }

    public Plano getPlano() {
        return plano;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public StatusMatricula getStatus() {
        return status;
    }

    public CalculoMensalidadeStrategy getCalculoMensalidade() {
        return calculoMensalidade;
    }
}
