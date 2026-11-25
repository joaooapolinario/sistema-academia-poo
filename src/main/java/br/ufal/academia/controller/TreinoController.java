package br.ufal.academia.controller;

import br.ufal.academia.dao.AlunoDAO;
import br.ufal.academia.dao.InstrutorDAO;
import br.ufal.academia.dao.TreinoDAO;
import br.ufal.academia.exception.DadosInvalidosException;
import br.ufal.academia.model.entities.Aluno;
import br.ufal.academia.model.entities.Instrutor;
import br.ufal.academia.model.entities.Treino;
import br.ufal.academia.model.enums.ObjetivoTreino;
import br.ufal.academia.model.patterns.factory.TreinoFactory;

import java.util.List;

// Controller responsável pela criação e manutenção de fichas de treino via TreinoFactory.
public class TreinoController implements ITreinoController {

    private final TreinoDAO treinoDAO;
    private final AlunoDAO alunoDAO;
    private final InstrutorDAO instrutorDAO;
    private final TreinoFactory treinoFactory;

    public TreinoController(TreinoDAO treinoDAO, AlunoDAO alunoDAO, InstrutorDAO instrutorDAO) {
        this(treinoDAO, alunoDAO, instrutorDAO, new TreinoFactory());
    }

    TreinoController(TreinoDAO treinoDAO, AlunoDAO alunoDAO, InstrutorDAO instrutorDAO, TreinoFactory treinoFactory) {
        if (treinoDAO == null || alunoDAO == null || instrutorDAO == null) {
            throw new IllegalArgumentException("TreinoDAO, AlunoDAO e InstrutorDAO são obrigatórios.");
        }
        if (treinoFactory == null) {
            throw new IllegalArgumentException("TreinoFactory é obrigatória.");
        }
        this.treinoDAO = treinoDAO;
        this.alunoDAO = alunoDAO;
        this.instrutorDAO = instrutorDAO;
        this.treinoFactory = treinoFactory;
    }

    @Override
    public Treino criar(String cpfAluno, String crefInstrutor, ObjetivoTreino objetivo)
            throws DadosInvalidosException {
        String cpfNormalizado = normalizarCpf(cpfAluno);
        String crefNormalizado = normalizarCref(crefInstrutor);
        if (objetivo == null) {
            throw new DadosInvalidosException("O objetivo do treino é obrigatório.");
        }

        Aluno aluno = alunoDAO.buscarPorCpf(cpfNormalizado).orElseThrow(
                () -> new DadosInvalidosException("Aluno não encontrado para o CPF informado."));
        Instrutor instrutor = instrutorDAO.buscarPorCref(crefNormalizado).orElseThrow(
                () -> new DadosInvalidosException("Instrutor não encontrado para o CREF informado."));

        Treino treino = treinoFactory.criar(aluno, instrutor, objetivo);
        treinoDAO.salvar(treino);
        return treino;
    }

    @Override
    public void adicionarExercicio(int idTreino, String descricao) throws DadosInvalidosException {
        Treino treino = buscarTreinoPorId(idTreino);
        treino.adicionarExercicio(descricao);
        treinoDAO.atualizar(treino);
    }

    @Override
    public void removerExercicio(int idTreino, String descricao) throws DadosInvalidosException {
        Treino treino = buscarTreinoPorId(idTreino);
        treino.removerExercicio(descricao);
        treinoDAO.atualizar(treino);
    }

    @Override
    public List<Treino> listarPorAluno(String cpf) throws DadosInvalidosException {
        String cpfNormalizado = normalizarCpf(cpf);
        Aluno aluno = alunoDAO.buscarPorCpf(cpfNormalizado).orElseThrow(
                () -> new DadosInvalidosException("Aluno não encontrado para o CPF informado."));
        return List.copyOf(treinoDAO.listarPorAluno(aluno.getId()));
    }

    @Override
    public void remover(int idTreino) throws DadosInvalidosException {
        Treino treino = buscarTreinoPorId(idTreino);
        treinoDAO.remover(treino.getId());
    }

    private Treino buscarTreinoPorId(int idTreino) throws DadosInvalidosException {
        if (idTreino <= 0) {
            throw new DadosInvalidosException("O identificador do treino deve ser positivo.");
        }
        return treinoDAO.buscarPorId(idTreino).orElseThrow(
                () -> new DadosInvalidosException("Treino não encontrado para o id: " + idTreino));
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

    private String normalizarCref(String cref) throws DadosInvalidosException {
        if (cref == null || cref.isBlank()) {
            throw new DadosInvalidosException("O CREF do instrutor é obrigatório.");
        }
        return cref.trim().toUpperCase();
    }
}
