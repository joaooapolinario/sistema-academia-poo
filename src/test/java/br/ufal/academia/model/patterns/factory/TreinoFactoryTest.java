package br.ufal.academia.model.patterns.factory;

import br.ufal.academia.exception.DadosInvalidosException;
import br.ufal.academia.model.entities.Aluno;
import br.ufal.academia.model.entities.Instrutor;
import br.ufal.academia.model.entities.Treino;
import br.ufal.academia.model.enums.ObjetivoTreino;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TreinoFactoryTest {

    private final TreinoFactory factory = new TreinoFactory();
    private Aluno aluno;
    private Instrutor instrutor;

    @BeforeEach
    void setUp() throws Exception {
        aluno = new Aluno("Maria Souza", "52998224725", "82999990000",
                LocalDate.of(2000, 1, 10), ObjetivoTreino.EMAGRECIMENTO);
        instrutor = new Instrutor("Carlos Lima", "11144477735", "82988887777",
                "123456-G/AL", "Musculação");
    }

    @Test
    void deveGerarTreinoComExerciciosParaCadaObjetivo() throws Exception {
        for (ObjetivoTreino objetivo : ObjetivoTreino.values()) {
            Treino treino = factory.criar(aluno, instrutor, objetivo);

            assertEquals(objetivo, treino.getObjetivo());
            assertEquals(aluno, treino.getAluno());
            assertEquals(instrutor, treino.getInstrutor());
            assertFalse(treino.getExercicios().isEmpty(), "Sem exercícios para " + objetivo);
        }
    }

    @Test
    void fichasDeObjetivosDiferentesDevemSerDiferentes() throws Exception {
        Treino hipertrofia = factory.criar(aluno, instrutor, ObjetivoTreino.HIPERTROFIA);
        Treino reabilitacao = factory.criar(aluno, instrutor, ObjetivoTreino.REABILITACAO);

        assertNotEquals(hipertrofia.getExercicios(), reabilitacao.getExercicios());
    }

    @Test
    void deveUsarObjetivoDoAlunoQuandoNaoInformado() throws Exception {
        Treino treino = factory.criar(aluno, instrutor);

        assertEquals(ObjetivoTreino.EMAGRECIMENTO, treino.getObjetivo());
    }

    @Test
    void cadaChamadaDeveRetornarTreinoIndependente() throws Exception {
        Treino primeiro = factory.criar(aluno, instrutor, ObjetivoTreino.HIPERTROFIA);
        Treino segundo = factory.criar(aluno, instrutor, ObjetivoTreino.HIPERTROFIA);

        primeiro.adicionarExercicio("Exercício extra");

        assertEquals(primeiro.getExercicios().size() - 1, segundo.getExercicios().size());
    }

    @Test
    void deveRejeitarDadosInvalidos() {
        assertThrows(DadosInvalidosException.class, () -> factory.criar(null, instrutor, ObjetivoTreino.HIPERTROFIA));
        assertThrows(DadosInvalidosException.class, () -> factory.criar(aluno, null, ObjetivoTreino.HIPERTROFIA));
        assertThrows(DadosInvalidosException.class, () -> factory.criar(aluno, instrutor, null));
        assertThrows(DadosInvalidosException.class, () -> factory.criar(null, instrutor));
    }
}
