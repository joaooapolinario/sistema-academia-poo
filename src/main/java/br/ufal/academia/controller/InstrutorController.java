package br.ufal.academia.controller;

import br.ufal.academia.dao.InstrutorDAO;
import br.ufal.academia.exception.DadosInvalidosException;
import br.ufal.academia.model.entities.Instrutor;

import java.util.List;

// Controller responsável pelo cadastro, consulta, atualização e remoção de instrutores.
public class InstrutorController implements IInstrutorController {

    private final InstrutorDAO instrutorDAO;

    public InstrutorController(InstrutorDAO instrutorDAO) {
        if (instrutorDAO == null) {
            throw new IllegalArgumentException("O InstrutorDAO é obrigatório.");
        }
        this.instrutorDAO = instrutorDAO;
    }

    @Override
    public Instrutor cadastrar(String nome, String cpf, String telefone, String cref, String especialidade)
            throws DadosInvalidosException {
        Instrutor instrutor = new Instrutor(nome, cpf, telefone, cref, especialidade);
        if (instrutorDAO.buscarPorCref(instrutor.getCref()).isPresent()) {
            throw new DadosInvalidosException("Já existe instrutor cadastrado com o CREF: " + instrutor.getCref());
        }
        boolean cpfJaCadastrado = instrutorDAO.listarTodos().stream()
                .anyMatch(existente -> existente.getCpf().equals(instrutor.getCpf()));
        if (cpfJaCadastrado) {
            throw new DadosInvalidosException("Já existe instrutor cadastrado com o CPF: " + instrutor.getCpf());
        }
        instrutorDAO.salvar(instrutor);
        return instrutor;
    }

    @Override
    public Instrutor buscarPorCref(String cref) throws DadosInvalidosException {
        String crefNormalizado = normalizarCref(cref);
        return instrutorDAO.buscarPorCref(crefNormalizado).orElseThrow(
                () -> new DadosInvalidosException("Instrutor não encontrado para o CREF informado."));
    }

    @Override
    public List<Instrutor> listar() {
        return List.copyOf(instrutorDAO.listarTodos());
    }

    @Override
    public void atualizarEspecialidade(String cref, String especialidade) throws DadosInvalidosException {
        Instrutor instrutor = buscarPorCref(cref);
        instrutor.alterarEspecialidade(especialidade);
        instrutorDAO.atualizar(instrutor);
    }

    @Override
    public void remover(String cref) throws DadosInvalidosException {
        Instrutor instrutor = buscarPorCref(cref);
        instrutorDAO.remover(instrutor.getId());
    }

    private String normalizarCref(String cref) throws DadosInvalidosException {
        if (cref == null || cref.isBlank()) {
            throw new DadosInvalidosException("O CREF do instrutor é obrigatório.");
        }
        return cref.trim().toUpperCase();
    }
}
