package br.ufal.academia.model.patterns.factory;

import br.ufal.academia.exception.DadosInvalidosException;
import br.ufal.academia.model.entities.Aluno;
import br.ufal.academia.model.entities.Instrutor;
import br.ufal.academia.model.entities.Treino;
import br.ufal.academia.model.enums.ObjetivoTreino;

import java.util.List;

// PADRÃO FACTORY METHOD: centraliza a criação de fichas de treino padrão a partir do ObjetivoTreino.
// Quem precisa de um treino não conhece a composição de cada ficha, e novos objetivos só mexem aqui.
public class TreinoFactory {

    // Cria o treino padrão para o objetivo informado, já com os exercícios iniciais.
    public Treino criar(Aluno aluno, Instrutor instrutor, ObjetivoTreino objetivo) throws DadosInvalidosException {
        Treino treino = new Treino(aluno, instrutor, objetivo);
        for (String exercicio : exerciciosPadrao(objetivo)) {
            treino.adicionarExercicio(exercicio);
        }
        return treino;
    }

    // Cria o treino padrão usando o objetivo atualmente cadastrado no aluno.
    public Treino criar(Aluno aluno, Instrutor instrutor) throws DadosInvalidosException {
        if (aluno == null) {
            throw new DadosInvalidosException("O aluno do treino é obrigatório.");
        }
        return criar(aluno, instrutor, aluno.getObjetivo());
    }

    // O switch sem default garante que um novo ObjetivoTreino só compila depois de ganhar sua ficha.
    private List<String> exerciciosPadrao(ObjetivoTreino objetivo) {
        return switch (objetivo) {
            case HIPERTROFIA -> List.of(
                    "Supino reto - 4x8 a 12",
                    "Agachamento livre - 4x8 a 12",
                    "Remada curvada - 4x8 a 12",
                    "Desenvolvimento com halteres - 3x10 a 12",
                    "Rosca direta - 3x10 a 12",
                    "Tríceps testa - 3x10 a 12");
            case EMAGRECIMENTO -> List.of(
                    "Esteira - 30 min em ritmo moderado",
                    "Circuito funcional - 3 voltas de 40s de esforço por 20s de descanso",
                    "Agachamento com salto - 3x15",
                    "Remada alta com kettlebell - 3x15",
                    "Prancha abdominal - 3x40s",
                    "Bicicleta ergométrica intervalada - 15 min");
            case CONDICIONAMENTO -> List.of(
                    "Aquecimento na bicicleta - 10 min",
                    "Corrida intervalada - 20 min",
                    "Burpees - 3x12",
                    "Polichinelos - 3x30",
                    "Remo ergômetro - 10 min",
                    "Alongamento geral - 10 min");
            case REABILITACAO -> List.of(
                    "Mobilidade articular - 10 min",
                    "Leg press com carga leve - 3x15",
                    "Elevação pélvica - 3x15",
                    "Remada na polia com carga mínima - 3x15",
                    "Alongamento assistido - 10 min");
        };
    }
}
