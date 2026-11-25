package br.ufal.academia.model.entities;

import br.ufal.academia.exception.DadosInvalidosException;
import br.ufal.academia.model.enums.ObjetivoTreino;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlunoTest {

    @Test
    void deveCriarAlunoValidoSemRestricoesIniciais() throws Exception {
        LocalDate nascimento = LocalDate.of(2000, 1, 10);
        Aluno aluno = new Aluno("Maria Souza", "529.982.247-25", "(82) 99999-0000",
                nascimento, ObjetivoTreino.HIPERTROFIA);

        assertEquals("Maria Souza", aluno.getNome());
        assertEquals("52998224725", aluno.getCpf());
        assertEquals("82999990000", aluno.getTelefone());
        assertEquals(nascimento, aluno.getDataNascimento());
        assertEquals(ObjetivoTreino.HIPERTROFIA, aluno.getObjetivo());
        assertTrue(aluno.getRestricoesMedicas().isEmpty());
    }

    @Test
    void deveCalcularIdadeCorretamenteAntesENoDiaDoAniversario() throws Exception {
        LocalDate hoje = LocalDate.now();

        Aluno completouHoje = new Aluno("Ana", "52998224725", "82999990000",
                hoje.minusYears(25), ObjetivoTreino.HIPERTROFIA);
        Aluno fazAmanha = new Aluno("Bruno", "11144477735", "82988887777",
                hoje.minusYears(25).plusDays(1), ObjetivoTreino.CONDICIONAMENTO);
        Aluno nascidoHoje = new Aluno("Caio", "12345678909", "82977776666",
                hoje, ObjetivoTreino.REABILITACAO);

        assertEquals(25, completouHoje.getIdade());
        assertEquals(24, fazAmanha.getIdade());
        assertEquals(0, nascidoHoje.getIdade());
    }

    @Test
    void deveRejeitarDataNascimentoNulaOuNoFuturo() {
        assertThrows(DadosInvalidosException.class, () -> new Aluno(
                "Maria", "52998224725", "82999990000", null, ObjetivoTreino.HIPERTROFIA));
        assertThrows(DadosInvalidosException.class, () -> new Aluno(
                "Maria", "52998224725", "82999990000", LocalDate.now().plusDays(1), ObjetivoTreino.HIPERTROFIA));
    }

    @Test
    void deveValidarEAlterarObjetivoDeTreino() throws Exception {
        assertThrows(DadosInvalidosException.class, () -> new Aluno(
                "Maria", "52998224725", "82999990000", LocalDate.of(2000, 1, 10), null));

        Aluno aluno = new Aluno("Maria", "52998224725", "82999990000",
                LocalDate.of(2000, 1, 10), ObjetivoTreino.HIPERTROFIA);

        aluno.alterarObjetivo(ObjetivoTreino.EMAGRECIMENTO);
        assertEquals(ObjetivoTreino.EMAGRECIMENTO, aluno.getObjetivo());

        assertThrows(DadosInvalidosException.class, () -> aluno.alterarObjetivo(null));
        assertEquals(ObjetivoTreino.EMAGRECIMENTO, aluno.getObjetivo());
    }

    @Test
    void deveAdicionarERemoverRestricoesMedicas() throws Exception {
        Aluno aluno = new Aluno("Maria", "52998224725", "82999990000",
                LocalDate.of(2000, 1, 10), ObjetivoTreino.REABILITACAO);

        aluno.adicionarRestricao("  Asma ");
        aluno.adicionarRestricao("Hérnia de disco");
        assertEquals(List.of("Asma", "Hérnia de disco"), aluno.getRestricoesMedicas());

        aluno.removerRestricao(" Asma ");
        assertEquals(List.of("Hérnia de disco"), aluno.getRestricoesMedicas());
    }

    @Test
    void deveRejeitarRestricaoVaziaDuplicadaOuInexistente() throws Exception {
        Aluno aluno = new Aluno("Maria", "52998224725", "82999990000",
                LocalDate.of(2000, 1, 10), ObjetivoTreino.REABILITACAO);
        aluno.adicionarRestricao("Asma");

        assertThrows(DadosInvalidosException.class, () -> aluno.adicionarRestricao(null));
        assertThrows(DadosInvalidosException.class, () -> aluno.adicionarRestricao("   "));
        assertThrows(DadosInvalidosException.class, () -> aluno.adicionarRestricao("  Asma "));
        assertThrows(DadosInvalidosException.class, () -> aluno.removerRestricao(null));
        assertThrows(DadosInvalidosException.class, () -> aluno.removerRestricao("Hipertensão"));
    }

    @Test
    void listaDeRestricoesMedicasDeveSerImutavel() throws Exception {
        Aluno aluno = new Aluno("Maria", "52998224725", "82999990000",
                LocalDate.of(2000, 1, 10), ObjetivoTreino.REABILITACAO);
        aluno.adicionarRestricao("Asma");

        List<String> copia = aluno.getRestricoesMedicas();

        assertThrows(UnsupportedOperationException.class, () -> copia.add("Outra"));
        assertThrows(UnsupportedOperationException.class, () -> copia.remove(0));
        assertEquals(1, aluno.getRestricoesMedicas().size());
    }
}
