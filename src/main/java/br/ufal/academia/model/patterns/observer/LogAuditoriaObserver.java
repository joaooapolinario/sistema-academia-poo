package br.ufal.academia.model.patterns.observer;

import br.ufal.academia.model.entities.Matricula;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// Observer que registra em log de auditoria as mudanças de status de uma matrícula.
public class LogAuditoriaObserver implements MatriculaObserver {

    private static final DateTimeFormatter FORMATO_DATA_HORA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final List<String> registros = new ArrayList<>();
    private final Consumer<String> saida;
    private final Clock clock;

    public LogAuditoriaObserver() {
        this(System.out::println, Clock.systemDefaultZone());
    }

    public LogAuditoriaObserver(Consumer<String> saida) {
        this(saida, Clock.systemDefaultZone());
    }

    // Permite injetar relógio fixo nos testes de auditoria.
    LogAuditoriaObserver(Consumer<String> saida, Clock clock) {
        if (saida == null || clock == null) {
            throw new IllegalArgumentException("Saída e relógio são obrigatórios.");
        }
        this.saida = saida;
        this.clock = clock;
    }

    @Override
    public void atualizar(Matricula matricula) {
        if (matricula == null) {
            throw new IllegalArgumentException("A matrícula não pode ser nula.");
        }
        String registro = String.format("[%s] [AUDITORIA] Matrícula #%d | Aluno: %s (CPF: %s) | Plano: %s | Status: %s",
                LocalDateTime.now(clock).format(FORMATO_DATA_HORA),
                matricula.getId(),
                matricula.getAluno().getNome(),
                matricula.getAluno().getCpf(),
                matricula.getPlano().getNome(),
                matricula.getStatus());
        registros.add(registro);
        saida.accept(registro);
    }

    public List<String> getRegistros() {
        return List.copyOf(registros);
    }
}
