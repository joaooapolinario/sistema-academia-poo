package br.ufal.academia.model.entities;

import br.ufal.academia.exception.DadosInvalidosException;
import br.ufal.academia.model.enums.ObjetivoTreino;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreinoTest {

    private Aluno aluno;
    private Instrutor instrutor;

    @BeforeEach
    void setUp() throws Exception {
        aluno = new Aluno("Maria Souza", "52998224725", "82999990000",
                LocalDate.of(2000, 1, 10), ObjetivoTreino.HIPERTROFIA);
        instrutor = new Instrutor("Carlos Lima", "11144477735", "82988887777",
                "123456-G/AL", "Musculação");
    }

    @Test
    void deveCriarTreinoVazioComDataDeHoje() throws Exception {
        Treino treino = new Treino(aluno, instrutor, ObjetivoTreino.HIPERTROFIA);

        assertEquals(aluno, treino.getAluno());
        assertEquals(instrutor, treino.getInstrutor());
        assertEquals(ObjetivoTreino.HIPERTROFIA, treino.getObjetivo());
        assertEquals(LocalDate.now(), treino.getDataCriacao());
        assertTrue(treino.getExercicios().isEmpty());
    }

    @Test
    void deveRejeitarCamposObrigatoriosNulos() {
        assertThrows(DadosInvalidosException.class, () -> new Treino(null, instrutor, ObjetivoTreino.HIPERTROFIA));
        assertThrows(DadosInvalidosException.class, () -> new Treino(aluno, null, ObjetivoTreino.HIPERTROFIA));
        assertThrows(DadosInvalidosException.class, () -> new Treino(aluno, instrutor, null));
    }

    @Test
    void deveAdicionarERemoverExercicios() throws Exception {
        Treino treino = new Treino(aluno, instrutor, ObjetivoTreino.HIPERTROFIA);

        treino.adicionarExercicio("  Supino reto - 4x10 ");
        treino.adicionarExercicio("Agachamento - 4x10");
        assertEquals(List.of("Supino reto - 4x10", "Agachamento - 4x10"), treino.getExercicios());

        treino.removerExercicio("Supino reto - 4x10");
        assertEquals(List.of("Agachamento - 4x10"), treino.getExercicios());
    }

    @Test
    void deveRejeitarExercicioVazioDuplicadoOuInexistente() throws Exception {
        Treino treino = new Treino(aluno, instrutor, ObjetivoTreino.HIPERTROFIA);
        treino.adicionarExercicio("Supino reto - 4x10");

        assertThrows(DadosInvalidosException.class, () -> treino.adicionarExercicio("  "));
        assertThrows(DadosInvalidosException.class, () -> treino.adicionarExercicio(null));
        assertThrows(DadosInvalidosException.class, () -> treino.adicionarExercicio("Supino reto - 4x10"));
        assertThrows(DadosInvalidosException.class, () -> treino.removerExercicio("Barra fixa"));
    }

    @Test
    void listaDeExerciciosDeveSerImutavel() throws Exception {
        Treino treino = new Treino(aluno, instrutor, ObjetivoTreino.HIPERTROFIA);
        treino.adicionarExercicio("Supino reto - 4x10");

        List<String> copia = treino.getExercicios();

        assertThrows(UnsupportedOperationException.class, () -> copia.add("Outro"));
        assertThrows(UnsupportedOperationException.class, () -> copia.remove(0));
        assertEquals(1, treino.getExercicios().size());
    }

    @Test
    void idSoPodeSerAtribuidoUmaVezEDeveSerPositivo() throws Exception {
        Treino treino = new Treino(aluno, instrutor, ObjetivoTreino.HIPERTROFIA);

        assertThrows(IllegalArgumentException.class, () -> treino.atribuirId(0));
        treino.atribuirId(7);
        assertEquals(7, treino.getId());
        assertThrows(IllegalStateException.class, () -> treino.atribuirId(8));
    }

    @Test
    void deveReconstituirTreinoPersistido() throws Exception {
        LocalDate criacao = LocalDate.of(2026, 10, 1);

        Treino treino = Treino.reconstituir(3, aluno, instrutor, ObjetivoTreino.REABILITACAO,
                List.of("Mobilidade - 10 min", "Leg press leve - 3x15"), criacao);

        assertEquals(3, treino.getId());
        assertEquals(criacao, treino.getDataCriacao());
        assertEquals(ObjetivoTreino.REABILITACAO, treino.getObjetivo());
        assertEquals(2, treino.getExercicios().size());
    }
}
