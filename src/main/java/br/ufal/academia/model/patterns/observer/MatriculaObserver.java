package br.ufal.academia.model.patterns.observer;

import br.ufal.academia.model.entities.Matricula;

// Observer notificado sobre mudanças de estado em uma matrícula.
public interface MatriculaObserver {
    void atualizar(Matricula matricula);
}
