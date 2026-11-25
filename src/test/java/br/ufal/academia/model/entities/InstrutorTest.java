package br.ufal.academia.model.entities;

import br.ufal.academia.exception.DadosInvalidosException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InstrutorTest {

    @Test
    void deveCriarInstrutorValidoNormalizandoCrefEEspecialidade() throws Exception {
        Instrutor graduado = new Instrutor("Carlos Lima", "529.982.247-25", "(82) 98888-1111",
                " 012345-g/al ", "  Musculação ");
        Instrutor provisionado = new Instrutor("Ana Costa", "11144477735", "8232221111",
                "654321-p/sp", "Pilates");

        assertEquals("Carlos Lima", graduado.getNome());
        assertEquals("52998224725", graduado.getCpf());
        assertEquals("82988881111", graduado.getTelefone());
        assertEquals("012345-G/AL", graduado.getCref());
        assertEquals("Musculação", graduado.getEspecialidade());

        assertEquals("654321-P/SP", provisionado.getCref());
        assertEquals("Pilates", provisionado.getEspecialidade());
    }

    @Test
    void deveRejeitarCrefForaDoFormatoEsperado() {
        assertThrows(DadosInvalidosException.class, () -> new Instrutor(
                "Carlos", "52998224725", "82988881111", null, "Musculação"));
        assertThrows(DadosInvalidosException.class, () -> new Instrutor(
                "Carlos", "52998224725", "82988881111", "   ", "Musculação"));
        assertThrows(DadosInvalidosException.class, () -> new Instrutor(
                "Carlos", "52998224725", "82988881111", "12345-G/AL", "Musculação"));
        assertThrows(DadosInvalidosException.class, () -> new Instrutor(
                "Carlos", "52998224725", "82988881111", "1234567-G/AL", "Musculação"));
        assertThrows(DadosInvalidosException.class, () -> new Instrutor(
                "Carlos", "52998224725", "82988881111", "123456-X/AL", "Musculação"));
        assertThrows(DadosInvalidosException.class, () -> new Instrutor(
                "Carlos", "52998224725", "82988881111", "123456-G/A", "Musculação"));
        assertThrows(DadosInvalidosException.class, () -> new Instrutor(
                "Carlos", "52998224725", "82988881111", "123456-G/ALA", "Musculação"));
        assertThrows(DadosInvalidosException.class, () -> new Instrutor(
                "Carlos", "52998224725", "82988881111", "123456-G/12", "Musculação"));
        assertThrows(DadosInvalidosException.class, () -> new Instrutor(
                "Carlos", "52998224725", "82988881111", "123456G/AL", "Musculação"));
        assertThrows(DadosInvalidosException.class, () -> new Instrutor(
                "Carlos", "52998224725", "82988881111", "123456-GAL", "Musculação"));
    }

    @Test
    void deveValidarEAlterarEspecialidade() throws Exception {
        assertThrows(DadosInvalidosException.class, () -> new Instrutor(
                "Carlos", "52998224725", "82988881111", "012345-G/AL", null));
        assertThrows(DadosInvalidosException.class, () -> new Instrutor(
                "Carlos", "52998224725", "82988881111", "012345-G/AL", "   "));

        Instrutor instrutor = new Instrutor("Carlos", "52998224725", "82988881111", "012345-G/AL", "Musculação");

        instrutor.alterarEspecialidade("  Crossfit ");
        assertEquals("Crossfit", instrutor.getEspecialidade());

        assertThrows(DadosInvalidosException.class, () -> instrutor.alterarEspecialidade(null));
        assertThrows(DadosInvalidosException.class, () -> instrutor.alterarEspecialidade("   "));
        assertEquals("Crossfit", instrutor.getEspecialidade());
    }
}
