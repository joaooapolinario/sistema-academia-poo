package br.ufal.academia.controller;

import br.ufal.academia.dao.AlunoDAO;
import br.ufal.academia.dao.MatriculaDAO;
import br.ufal.academia.dao.PlanoDAO;
import br.ufal.academia.exception.DadosInvalidosException;
import br.ufal.academia.exception.MatriculaAtivaExistenteException;
import br.ufal.academia.model.entities.Aluno;
import br.ufal.academia.model.entities.Matricula;
import br.ufal.academia.model.entities.Plano;
import br.ufal.academia.model.enums.StatusMatricula;
import br.ufal.academia.model.enums.TipoPlano;
import br.ufal.academia.model.patterns.observer.MatriculaObserver;
import br.ufal.academia.model.patterns.strategy.CalculoMensalidadeStrategy;
import br.ufal.academia.model.patterns.strategy.CalculoPadraoStrategy;
import br.ufal.academia.model.patterns.strategy.DescontoEstudanteStrategy;
import br.ufal.academia.model.patterns.strategy.DescontoIdosoStrategy;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

// Controller responsável pela gestão de matrículas, estratégias de desconto, vigências e notificações.
public class MatriculaController implements IMatriculaController {

    private static final int IDADE_DESCONTO_IDOSO = 60;
    private static final CalculoMensalidadeStrategy CALCULO_PADRAO = new CalculoPadraoStrategy();

    private final MatriculaDAO matriculaDAO;
    private final AlunoDAO alunoDAO;
    private final PlanoDAO planoDAO;
    private final List<MatriculaObserver> observers;
    private final Clock clock;

    public MatriculaController(MatriculaDAO matriculaDAO, AlunoDAO alunoDAO, PlanoDAO planoDAO) {
        this(matriculaDAO, alunoDAO, planoDAO, List.of(), Clock.systemDefaultZone());
    }

    public MatriculaController(MatriculaDAO matriculaDAO, AlunoDAO alunoDAO, PlanoDAO planoDAO,
                               List<MatriculaObserver> observers) {
        this(matriculaDAO, alunoDAO, planoDAO, observers, Clock.systemDefaultZone());
    }

    // Permite injetar relógio fixo para testes determinísticos de vigência, vencimento e renovação.
    MatriculaController(MatriculaDAO matriculaDAO, AlunoDAO alunoDAO, PlanoDAO planoDAO,
                        List<MatriculaObserver> observers, Clock clock) {
        if (matriculaDAO == null || alunoDAO == null || planoDAO == null) {
            throw new IllegalArgumentException("MatriculaDAO, AlunoDAO e PlanoDAO são obrigatórios.");
        }
        if (observers == null || clock == null) {
            throw new IllegalArgumentException("Lista de observers e relógio são obrigatórios.");
        }
        this.matriculaDAO = matriculaDAO;
        this.alunoDAO = alunoDAO;
        this.planoDAO = planoDAO;
        this.observers = List.copyOf(observers);
        this.clock = clock;
    }

    @Override
    public Matricula matricular(String cpfAluno, TipoPlano tipo, boolean estudante)
            throws DadosInvalidosException, MatriculaAtivaExistenteException {
        String cpfNormalizado = normalizarCpf(cpfAluno);
        if (tipo == null) {
            throw new DadosInvalidosException("O tipo do plano é obrigatório.");
        }

        Aluno aluno = alunoDAO.buscarPorCpf(cpfNormalizado).orElseThrow(
                () -> new DadosInvalidosException("Aluno não encontrado para o CPF informado."));
        Plano plano = planoDAO.buscarPorTipo(tipo).orElseThrow(
                () -> new DadosInvalidosException("Plano não encontrado para o tipo informado: " + tipo));

        LocalDate hoje = LocalDate.now(clock);
        Optional<Matricula> ativaExistente = matriculaDAO.buscarAtivaPorAluno(aluno.getId());
        if (ativaExistente.isPresent()) {
            Matricula existente = ativaExistente.get();
            acoplarObservers(existente);
            existente.verificarVencimento(hoje);
            if (existente.estaAtiva()) {
                throw new MatriculaAtivaExistenteException(aluno.getCpf());
            }
            matriculaDAO.atualizar(existente);
        }

        CalculoMensalidadeStrategy calculo = escolherCalculo(aluno, estudante);
        Matricula novaMatricula = new Matricula(aluno, plano, hoje, calculo);
        acoplarObservers(novaMatricula);
        matriculaDAO.salvar(novaMatricula);
        return novaMatricula;
    }

    @Override
    public void cancelar(int idMatricula) throws DadosInvalidosException {
        Matricula matricula = buscarMatriculaPorId(idMatricula);
        acoplarObservers(matricula);
        matricula.cancelar();
        matriculaDAO.atualizar(matricula);
    }

    @Override
    public void renovar(int idMatricula) throws DadosInvalidosException {
        Matricula matricula = buscarMatriculaPorId(idMatricula);
        LocalDate hoje = LocalDate.now(clock);

        if (matricula.getStatus() == StatusMatricula.VENCIDA) {
            Optional<Matricula> outraAtiva = matriculaDAO.buscarAtivaPorAluno(matricula.getAluno().getId());
            if (outraAtiva.isPresent() && outraAtiva.get().getId() != matricula.getId()) {
                Matricula existente = outraAtiva.get();
                acoplarObservers(existente);
                existente.verificarVencimento(hoje);
                if (existente.estaAtiva()) {
                    throw new DadosInvalidosException("O aluno já possui outra matrícula ativa.");
                }
                matriculaDAO.atualizar(existente);
            }
        }

        acoplarObservers(matricula);
        matricula.renovar(hoje);
        matriculaDAO.atualizar(matricula);
    }

    @Override
    public void atualizarVencimentos() {
        LocalDate hoje = LocalDate.now(clock);
        List<Matricula> ativas = matriculaDAO.listarPorStatus(StatusMatricula.ATIVA);
        for (Matricula matricula : ativas) {
            acoplarObservers(matricula);
            matricula.verificarVencimento(hoje);
            if (!matricula.estaAtiva()) {
                matriculaDAO.atualizar(matricula);
            }
        }
    }

    @Override
    public List<Matricula> listar() {
        return List.copyOf(matriculaDAO.listarTodos());
    }

    @Override
    public List<Matricula> listarAVencer(int dias) throws DadosInvalidosException {
        if (dias < 0) {
            throw new DadosInvalidosException("A quantidade de dias não pode ser negativa.");
        }
        LocalDate hoje = LocalDate.now(clock);
        LocalDate limite = hoje.plusDays(dias);
        return matriculaDAO.listarPorStatus(StatusMatricula.ATIVA).stream()
                .filter(m -> !m.getDataFim().isBefore(hoje) && !m.getDataFim().isAfter(limite))
                .sorted(Comparator.comparing(Matricula::getDataFim))
                .toList();
    }

    @Override
    public List<Plano> listarPlanos() {
        return List.copyOf(planoDAO.listarTodos());
    }

    @Override
    public double simularMensalidade(String cpfAluno, TipoPlano tipo, boolean estudante)
            throws DadosInvalidosException {
        String cpfNormalizado = normalizarCpf(cpfAluno);
        if (tipo == null) {
            throw new DadosInvalidosException("O tipo do plano é obrigatório.");
        }
        Aluno aluno = alunoDAO.buscarPorCpf(cpfNormalizado).orElseThrow(
                () -> new DadosInvalidosException("Aluno não encontrado para o CPF informado."));
        Plano plano = planoDAO.buscarPorTipo(tipo).orElseThrow(
                () -> new DadosInvalidosException("Plano não encontrado para o tipo informado: " + tipo));
        return escolherCalculo(aluno, estudante).calcular(plano);
    }

    @Override
    public double calcularFaturamentoMensal() {
        LocalDate hoje = LocalDate.now(clock);
        double total = matriculaDAO.listarPorStatus(StatusMatricula.ATIVA).stream()
                .filter(m -> !m.isVencida(hoje))
                .mapToDouble(Matricula::calcularMensalidade)
                .sum();
        return CALCULO_PADRAO.arredondar(total);
    }

    private CalculoMensalidadeStrategy escolherCalculo(Aluno aluno, boolean estudante) {
        int idade = Period.between(aluno.getDataNascimento(), LocalDate.now(clock)).getYears();
        if (idade >= IDADE_DESCONTO_IDOSO) {
            return new DescontoIdosoStrategy();
        }
        if (estudante) {
            return new DescontoEstudanteStrategy();
        }
        return new CalculoPadraoStrategy();
    }

    private void acoplarObservers(Matricula matricula) {
        for (MatriculaObserver observer : observers) {
            matricula.adicionarObserver(observer);
        }
    }

    private Matricula buscarMatriculaPorId(int idMatricula) throws DadosInvalidosException {
        if (idMatricula <= 0) {
            throw new DadosInvalidosException("O identificador da matrícula deve ser positivo.");
        }
        return matriculaDAO.buscarPorId(idMatricula).orElseThrow(
                () -> new DadosInvalidosException("Matrícula não encontrada para o id: " + idMatricula));
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
