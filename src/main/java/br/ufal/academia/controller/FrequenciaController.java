package br.ufal.academia.controller;

import br.ufal.academia.dao.AlunoDAO;
import br.ufal.academia.dao.FrequenciaDAO;
import br.ufal.academia.dao.MatriculaDAO;
import br.ufal.academia.exception.AcessoNegadoException;
import br.ufal.academia.exception.DadosInvalidosException;
import br.ufal.academia.model.entities.Aluno;
import br.ufal.academia.model.entities.Frequencia;
import br.ufal.academia.model.entities.Matricula;
import br.ufal.academia.model.patterns.observer.MatriculaObserver;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

// Controller responsável pela validação de acesso na catraca e consultas de frequência.
public class FrequenciaController implements IFrequenciaController {

    private final FrequenciaDAO frequenciaDAO;
    private final AlunoDAO alunoDAO;
    private final MatriculaDAO matriculaDAO;
    private final List<MatriculaObserver> observers;
    private final Clock clock;

    public FrequenciaController(FrequenciaDAO frequenciaDAO, AlunoDAO alunoDAO, MatriculaDAO matriculaDAO) {
        this(frequenciaDAO, alunoDAO, matriculaDAO, List.of(), Clock.systemDefaultZone());
    }

    public FrequenciaController(FrequenciaDAO frequenciaDAO, AlunoDAO alunoDAO, MatriculaDAO matriculaDAO,
                                List<MatriculaObserver> observers) {
        this(frequenciaDAO, alunoDAO, matriculaDAO, observers, Clock.systemDefaultZone());
    }

    // Permite injetar relógio fixo para testes determinísticos de vencimento e acesso.
    FrequenciaController(FrequenciaDAO frequenciaDAO, AlunoDAO alunoDAO, MatriculaDAO matriculaDAO,
                         List<MatriculaObserver> observers, Clock clock) {
        if (frequenciaDAO == null || alunoDAO == null || matriculaDAO == null) {
            throw new IllegalArgumentException("FrequenciaDAO, AlunoDAO e MatriculaDAO são obrigatórios.");
        }
        if (observers == null || clock == null) {
            throw new IllegalArgumentException("Lista de observers e relógio são obrigatórios.");
        }
        this.frequenciaDAO = frequenciaDAO;
        this.alunoDAO = alunoDAO;
        this.matriculaDAO = matriculaDAO;
        this.observers = List.copyOf(observers);
        this.clock = clock;
    }

    @Override
    public Frequencia registrarEntrada(String cpf) throws DadosInvalidosException, AcessoNegadoException {
        String cpfNormalizado = normalizarCpf(cpf);
        Aluno aluno = alunoDAO.buscarPorCpf(cpfNormalizado).orElseThrow(
                () -> new AcessoNegadoException("Acesso negado: aluno não encontrado para o CPF informado."));

        Matricula matricula = matriculaDAO.buscarAtivaPorAluno(aluno.getId()).orElseThrow(
                () -> new AcessoNegadoException(
                        "Acesso negado: aluno " + aluno.getNome() + " não possui matrícula ativa."));

        for (MatriculaObserver observer : observers) {
            matricula.adicionarObserver(observer);
        }

        LocalDateTime agora = LocalDateTime.now(clock);
        matricula.verificarVencimento(agora.toLocalDate());
        if (!matricula.estaAtiva()) {
            matriculaDAO.atualizar(matricula);
        }

        Frequencia frequencia = new Frequencia(matricula, agora);
        frequenciaDAO.salvar(frequencia);
        return frequencia;
    }

    @Override
    public List<Frequencia> listarPorAluno(String cpf, YearMonth mes) throws DadosInvalidosException {
        String cpfNormalizado = normalizarCpf(cpf);
        if (mes == null) {
            throw new DadosInvalidosException("O mês de referência é obrigatório.");
        }
        Aluno aluno = alunoDAO.buscarPorCpf(cpfNormalizado).orElseThrow(
                () -> new DadosInvalidosException("Aluno não encontrado para o CPF informado."));

        return List.copyOf(frequenciaDAO.listarPorAlunoEPeriodo(aluno.getId(), mes.atDay(1), mes.atEndOfMonth()));
    }

    @Override
    public int contarPresencas(String cpf, YearMonth mes) throws DadosInvalidosException {
        return listarPorAluno(cpf, mes).size();
    }

    private String normalizarCpf(String cpf) throws DadosInvalidosException {
        if (cpf == null || cpf.isBlank()) {
            throw new DadosInvalidosException("O CPF do aluno é obrigatório.");
        }
        String digitos = cpf.replaceAll("\\D", "");
        if (digitos.isEmpty()) {
            throw new DadosInvalidosException("O CPF do aluno é obrigatório.");
        }
        return digitos;
    }
}
