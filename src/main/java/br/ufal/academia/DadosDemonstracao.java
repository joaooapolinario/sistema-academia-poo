package br.ufal.academia;

import br.ufal.academia.controller.IAlunoController;
import br.ufal.academia.controller.IFrequenciaController;
import br.ufal.academia.controller.IInstrutorController;
import br.ufal.academia.controller.IMatriculaController;
import br.ufal.academia.controller.ITreinoController;
import br.ufal.academia.exception.AcessoNegadoException;
import br.ufal.academia.exception.DadosInvalidosException;
import br.ufal.academia.exception.MatriculaAtivaExistenteException;
import br.ufal.academia.model.enums.ObjetivoTreino;
import br.ufal.academia.model.enums.TipoPlano;

import java.time.LocalDate;

// Seed com dados de demonstração, executado quando o banco está vazio
public class DadosDemonstracao {

    private final IAlunoController alunoController;
    private final IInstrutorController instrutorController;
    private final IMatriculaController matriculaController;
    private final ITreinoController treinoController;
    private final IFrequenciaController frequenciaController;

    public DadosDemonstracao(IAlunoController alunoController, IInstrutorController instrutorController,
                             IMatriculaController matriculaController, ITreinoController treinoController,
                             IFrequenciaController frequenciaController) {
        if (alunoController == null || instrutorController == null || matriculaController == null
                || treinoController == null || frequenciaController == null) {
            throw new IllegalArgumentException("Todos os controllers são obrigatórios.");
        }
        this.alunoController = alunoController;
        this.instrutorController = instrutorController;
        this.matriculaController = matriculaController;
        this.treinoController = treinoController;
        this.frequenciaController = frequenciaController;
    }

    public boolean carregarSeBancoVazio() {
        if (!alunoController.listar().isEmpty()) {
            return false;
        }
        try {
            cadastrarInstrutores();
            cadastrarAlunos();
            cadastrarMatriculas();
            cadastrarTreinos();
            registrarEntradas();
            return true;
        } catch (DadosInvalidosException | MatriculaAtivaExistenteException | AcessoNegadoException e) {
            throw new IllegalStateException("Falha ao carregar os dados de demonstração.", e);
        }
    }

    private void cadastrarInstrutores() throws DadosInvalidosException {
        instrutorController.cadastrar("Rafael Torres", "741.852.963-55", "(82) 98811-2233",
                "012345-G/AL", "Musculação");
        instrutorController.cadastrar("Fernanda Melo", "852.963.741-00", "(82) 98822-3344",
                "067890-G/AL", "Reabilitação e funcional");
    }

    private void cadastrarAlunos() throws DadosInvalidosException {
        alunoController.cadastrar("Ana Beatriz Souza", "123.456.789-09", "(82) 99901-1111",
                LocalDate.of(2005, 3, 15), ObjetivoTreino.HIPERTROFIA);
        alunoController.adicionarRestricaoMedica("123.456.789-09", "Asma");

        alunoController.cadastrar("Carlos Eduardo Lima", "987.654.321-00", "(82) 99902-2222",
                LocalDate.of(1994, 7, 2), ObjetivoTreino.EMAGRECIMENTO);

        alunoController.cadastrar("Maria José Santos", "456.123.789-55", "(82) 99903-3333",
                LocalDate.of(1958, 9, 20), ObjetivoTreino.REABILITACAO);
        alunoController.adicionarRestricaoMedica("456.123.789-55", "Hipertensão");
        alunoController.adicionarRestricaoMedica("456.123.789-55", "Artrose no joelho");

        alunoController.cadastrar("Pedro Henrique Alves", "321.654.987-91", "(82) 99904-4444",
                LocalDate.of(2001, 5, 10), ObjetivoTreino.CONDICIONAMENTO);
    }

    private void cadastrarMatriculas() throws DadosInvalidosException, MatriculaAtivaExistenteException {
        matriculaController.matricular("123.456.789-09", TipoPlano.MENSAL, true);
        matriculaController.matricular("987.654.321-00", TipoPlano.TRIMESTRAL, false);
        matriculaController.matricular("456.123.789-55", TipoPlano.ANUAL, false);
    }

    private void cadastrarTreinos() throws DadosInvalidosException {
        treinoController.criar("123.456.789-09", "012345-G/AL", ObjetivoTreino.HIPERTROFIA);
        treinoController.criar("987.654.321-00", "012345-G/AL", ObjetivoTreino.EMAGRECIMENTO);
        treinoController.criar("456.123.789-55", "067890-G/AL", ObjetivoTreino.REABILITACAO);
    }

    private void registrarEntradas() throws DadosInvalidosException, AcessoNegadoException {
        frequenciaController.registrarEntrada("123.456.789-09");
        frequenciaController.registrarEntrada("987.654.321-00");
    }
}