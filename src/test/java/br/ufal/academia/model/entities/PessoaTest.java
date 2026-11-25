package br.ufal.academia.model.entities;

import br.ufal.academia.exception.CpfInvalidoException;
import br.ufal.academia.exception.DadosInvalidosException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PessoaTest {

    @Test
    void deveCriarPessoaValidaNormalizandoNomeCpfETelefone() throws Exception {
        Pessoa pessoa = new PessoaConcreta("  Maria Souza  ", "529.982.247-25", "(82) 99999-0000");

        assertEquals(0, pessoa.getId());
        assertEquals("Maria Souza", pessoa.getNome());
        assertEquals("52998224725", pessoa.getCpf());
        assertEquals("82999990000", pessoa.getTelefone());
    }

    @Test
    void deveAceitarCpfComDigitoVerificadorZeroETelefoneFixoDeDezDigitos() throws Exception {
        // 12345678909 exercita o caso em que o resto do cálculo do 1º dígito verificador é 10 -> 0.
        Pessoa pessoa = new PessoaConcreta("João Silva", "123.456.789-09", "(82) 3222-4455");

        assertEquals("12345678909", pessoa.getCpf());
        assertEquals("8232224455", pessoa.getTelefone());
    }

    @Test
    void deveRejeitarCpfsInvalidosComCpfInvalidoException() {
        CpfInvalidoException excecaoNulo = assertThrows(CpfInvalidoException.class,
                () -> new PessoaConcreta("Maria", null, "82999990000"));
        assertInstanceOf(DadosInvalidosException.class, excecaoNulo);

        assertThrows(CpfInvalidoException.class, () -> new PessoaConcreta("Maria", "", "82999990000"));
        assertThrows(CpfInvalidoException.class, () -> new PessoaConcreta("Maria", "1234567890", "82999990000"));
        assertThrows(CpfInvalidoException.class, () -> new PessoaConcreta("Maria", "123456789012", "82999990000"));
        assertThrows(CpfInvalidoException.class, () -> new PessoaConcreta("Maria", "11111111111", "82999990000"));
        assertThrows(CpfInvalidoException.class, () -> new PessoaConcreta("Maria", "000.000.000-00", "82999990000"));
        assertThrows(CpfInvalidoException.class, () -> new PessoaConcreta("Maria", "52998224715", "82999990000"));
        assertThrows(CpfInvalidoException.class, () -> new PessoaConcreta("Maria", "52998224726", "82999990000"));
    }

    @Test
    void deveRejeitarNomeObrigatorioInvalido() {
        assertThrows(DadosInvalidosException.class, () -> new PessoaConcreta(null, "52998224725", "82999990000"));
        assertThrows(DadosInvalidosException.class, () -> new PessoaConcreta("", "52998224725", "82999990000"));
        assertThrows(DadosInvalidosException.class, () -> new PessoaConcreta("   ", "52998224725", "82999990000"));
    }

    @Test
    void deveValidarEAtualizarTelefone() throws Exception {
        assertThrows(DadosInvalidosException.class, () -> new PessoaConcreta("Maria", "52998224725", null));
        assertThrows(DadosInvalidosException.class, () -> new PessoaConcreta("Maria", "52998224725", ""));
        assertThrows(DadosInvalidosException.class, () -> new PessoaConcreta("Maria", "52998224725", "829999900"));
        assertThrows(DadosInvalidosException.class, () -> new PessoaConcreta("Maria", "52998224725", "829999900001"));
        assertThrows(DadosInvalidosException.class, () -> new PessoaConcreta("Maria", "52998224725", "abc-def"));

        Pessoa pessoa = new PessoaConcreta("Maria", "52998224725", "82999990000");
        pessoa.atualizarTelefone("(82) 3333-2222");
        assertEquals("8233332222", pessoa.getTelefone());

        assertThrows(DadosInvalidosException.class, () -> pessoa.atualizarTelefone("12345"));
        assertEquals("8233332222", pessoa.getTelefone());
    }

    @Test
    void idSoPodeSerAtribuidoUmaVezEDeveSerPositivo() throws Exception {
        Pessoa pessoa = new PessoaConcreta("Maria", "52998224725", "82999990000");

        assertThrows(IllegalArgumentException.class, () -> pessoa.atribuirId(0));
        assertThrows(IllegalArgumentException.class, () -> pessoa.atribuirId(-5));

        pessoa.atribuirId(10);
        assertEquals(10, pessoa.getId());
        assertThrows(IllegalStateException.class, () -> pessoa.atribuirId(11));
    }

    private static class PessoaConcreta extends Pessoa {
        PessoaConcreta(String nome, String cpf, String telefone) throws DadosInvalidosException {
            super(nome, cpf, telefone);
        }
    }
}
