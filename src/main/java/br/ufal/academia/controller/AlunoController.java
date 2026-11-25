package br.ufal.academia.controller;

import br.ufal.academia.dao.AlunoDAO;
import br.ufal.academia.exception.DadosInvalidosException;
import br.ufal.academia.model.entities.Aluno;
import br.ufal.academia.model.enums.ObjetivoTreino;

import java.time.LocalDate;
import java.util.List;

// Controller responsável pelo cadastro, consulta, atualização e remoção de alunos.
public class AlunoController implements IAlunoController {

    private final AlunoDAO alunoDAO;

    public AlunoController(AlunoDAO alunoDAO) {
        if (alunoDAO == null) {
            throw new IllegalArgumentException("O AlunoDAO é obrigatório.");
        }
        this.alunoDAO = alunoDAO;
    }

    @Override
    public Aluno cadastrar(String nome, String cpf, String telefone, LocalDate dataNascimento, ObjetivoTreino objetivo)
            throws DadosInvalidosException {
        Aluno aluno = new Aluno(nome, cpf, telefone, dataNascimento, objetivo);
        if (alunoDAO.buscarPorCpf(aluno.getCpf()).isPresent()) {
            throw new DadosInvalidosException("Já existe aluno cadastrado com o CPF: " + aluno.getCpf());
        }
        alunoDAO.salvar(aluno);
        return aluno;
    }

    @Override
    public Aluno buscarPorCpf(String cpf) throws DadosInvalidosException {
        String cpfNormalizado = normalizarCpf(cpf);
        return alunoDAO.buscarPorCpf(cpfNormalizado).orElseThrow(
                () -> new DadosInvalidosException("Aluno não encontrado para o CPF informado."));
    }

    @Override
    public List<Aluno> listar() {
        return List.copyOf(alunoDAO.listarTodos());
    }

    @Override
    public void atualizarTelefone(String cpf, String telefone) throws DadosInvalidosException {
        Aluno aluno = buscarPorCpf(cpf);
        aluno.atualizarTelefone(telefone);
        alunoDAO.atualizar(aluno);
    }

    @Override
    public void adicionarRestricaoMedica(String cpf, String restricao) throws DadosInvalidosException {
        Aluno aluno = buscarPorCpf(cpf);
        aluno.adicionarRestricao(restricao);
        alunoDAO.atualizar(aluno);
    }

    @Override
    public void removerRestricaoMedica(String cpf, String restricao) throws DadosInvalidosException {
        Aluno aluno = buscarPorCpf(cpf);
        aluno.removerRestricao(restricao);
        alunoDAO.atualizar(aluno);
    }

    @Override
    public void remover(String cpf) throws DadosInvalidosException {
        Aluno aluno = buscarPorCpf(cpf);
        alunoDAO.remover(aluno.getId());
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
