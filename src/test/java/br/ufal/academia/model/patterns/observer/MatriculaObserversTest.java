package br.ufal.academia.model.patterns.observer;

import br.ufal.academia.model.entities.Aluno;
import br.ufal.academia.model.entities.Matricula;
import br.ufal.academia.model.entities.Plano;
import br.ufal.academia.model.enums.ObjetivoTreino;
import br.ufal.academia.model.enums.TipoPlano;
import br.ufal.academia.model.patterns.strategy.CalculoPadraoStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatriculaObserversTest {

    private Matricula matricula;

    @BeforeEach
    void setUp() throws Exception {
        Aluno aluno = new Aluno("Maria Souza", "52998224725", "82999990000",
                LocalDate.of(2000, 1, 10), ObjetivoTreino.HIPERTROFIA);
        Plano mensal = new Plano(TipoPlano.MENSAL, "Mensal", 1, 99.9);
        matricula = new Matricula(aluno, mensal, LocalDate.of(2026, 11, 1), new CalculoPadraoStrategy());
        matricula.atribuirId(7);
    }

    @Test
    void alertaInterfaceObserverDeveGerarAlertasParaCadaMudancaDeStatus() throws Exception {
        List<String> exibidos = new ArrayList<>();
        AlertaInterfaceObserver observer = new AlertaInterfaceObserver(exibidos::add);
        assertFalse(observer.getUltimoAlerta().isPresent());

        matricula.adicionarObserver(observer);
        matricula.verificarVencimento(LocalDate.of(2026, 12, 2));
        matricula.renovar(LocalDate.of(2026, 12, 5));
        matricula.cancelar();

        assertEquals(3, observer.getAlertas().size());
        assertEquals(observer.getAlertas(), exibidos);
        assertTrue(observer.getAlertas().get(0).contains("venceu em 2026-12-01"));
        assertTrue(observer.getAlertas().get(1).contains("está ativa até 2027-01-05"));
        assertTrue(observer.getAlertas().get(2).contains("foi cancelada"));
        assertEquals(observer.getAlertas().get(2), observer.getUltimoAlerta().orElseThrow());
        assertThrows(UnsupportedOperationException.class, () -> observer.getAlertas().add("x"));
    }

    @Test
    void logAuditoriaObserverDeveRegistrarEventosComCarimboDeTempoEDadosDaMatricula() throws Exception {
        Clock relogioFixo = Clock.fixed(Instant.parse("2026-12-02T10:15:30Z"), ZoneOffset.UTC);
        List<String> linhas = new ArrayList<>();
        LogAuditoriaObserver observer = new LogAuditoriaObserver(linhas::add, relogioFixo);

        matricula.adicionarObserver(observer);
        matricula.verificarVencimento(LocalDate.of(2026, 12, 2));

        assertEquals(1, observer.getRegistros().size());
        assertEquals(observer.getRegistros(), linhas);
        String registro = observer.getRegistros().get(0);
        assertTrue(registro.startsWith("[2026-12-02 10:15:30] [AUDITORIA]"));
        assertTrue(registro.contains("Matrícula #7"));
        assertTrue(registro.contains("Maria Souza"));
        assertTrue(registro.contains("52998224725"));
        assertTrue(registro.contains("Mensal"));
        assertTrue(registro.contains("VENCIDA"));
        assertThrows(UnsupportedOperationException.class, () -> observer.getRegistros().add("x"));
    }

    @Test
    void deveNotificarAmbosObservadoresSimultaneamente() throws Exception {
        AlertaInterfaceObserver alerta = new AlertaInterfaceObserver(msg -> { });
        LogAuditoriaObserver log = new LogAuditoriaObserver(msg -> { });
        matricula.adicionarObserver(alerta);
        matricula.adicionarObserver(log);

        matricula.cancelar();

        assertEquals(1, alerta.getAlertas().size());
        assertEquals(1, log.getRegistros().size());
    }

    @Test
    void deveRejeitarArgumentosNulosNosObservadores() {
        assertThrows(IllegalArgumentException.class, () -> new AlertaInterfaceObserver(null));
        assertThrows(IllegalArgumentException.class, () -> new LogAuditoriaObserver(null));
        assertThrows(IllegalArgumentException.class, () -> new LogAuditoriaObserver(msg -> { }, null));

        AlertaInterfaceObserver alerta = new AlertaInterfaceObserver(msg -> { });
        LogAuditoriaObserver log = new LogAuditoriaObserver(msg -> { });
        assertThrows(IllegalArgumentException.class, () -> alerta.atualizar(null));
        assertThrows(IllegalArgumentException.class, () -> log.atualizar(null));
    }
}
