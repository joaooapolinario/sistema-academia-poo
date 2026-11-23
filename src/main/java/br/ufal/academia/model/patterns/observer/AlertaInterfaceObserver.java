package br.ufal.academia.model.patterns.observer;

import br.ufal.academia.model.entities.Matricula;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

// Observer que gera alertas de mudança de status da matrícula para a interface.
public class AlertaInterfaceObserver implements MatriculaObserver {

    private final List<String> alertas = new ArrayList<>();
    private final Consumer<String> exibidor;

    public AlertaInterfaceObserver() {
        this(System.out::println);
    }

    // Permite injetar o callback da interface sem acoplar o model à camada view.
    public AlertaInterfaceObserver(Consumer<String> exibidor) {
        if (exibidor == null) {
            throw new IllegalArgumentException("O exibidor de alertas não pode ser nulo.");
        }
        this.exibidor = exibidor;
    }

    @Override
    public void atualizar(Matricula matricula) {
        if (matricula == null) {
            throw new IllegalArgumentException("A matrícula não pode ser nula.");
        }
        String mensagem = formatarMensagem(matricula);
        alertas.add(mensagem);
        exibidor.accept(mensagem);
    }

    public List<String> getAlertas() {
        return List.copyOf(alertas);
    }

    public Optional<String> getUltimoAlerta() {
        return alertas.isEmpty() ? Optional.empty() : Optional.of(alertas.get(alertas.size() - 1));
    }

    private String formatarMensagem(Matricula matricula) {
        String aluno = matricula.getAluno().getNome();
        return switch (matricula.getStatus()) {
            case VENCIDA -> "ALERTA: Matrícula de " + aluno + " venceu em " + matricula.getDataFim()
                    + ". Acesso bloqueado na catraca.";
            case CANCELADA -> "ALERTA: Matrícula de " + aluno + " foi cancelada. Acesso bloqueado na catraca.";
            case ATIVA -> "AVISO: Matrícula de " + aluno + " está ativa até " + matricula.getDataFim() + ".";
        };
    }
}
