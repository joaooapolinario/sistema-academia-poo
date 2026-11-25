package br.ufal.academia;

import br.ufal.academia.dao.PlanoDAO;
import br.ufal.academia.model.entities.Plano;
import br.ufal.academia.model.enums.TipoPlano;
import br.ufal.academia.model.patterns.factory.PlanoFactory;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {

    @Test
    void seedDePlanosDevePopularTodosOsTiposDeFormaIdempotente() {
        PlanoDAOEmMemoria planoDAO = new PlanoDAOEmMemoria();

        Main.inicializarPlanosPadrao(planoDAO);
        Main.inicializarPlanosPadrao(planoDAO);

        assertEquals(TipoPlano.values().length, planoDAO.listarTodos().size());
        assertEquals(TipoPlano.values().length, planoDAO.totalSalvamentos);
        for (TipoPlano tipo : TipoPlano.values()) {
            assertTrue(planoDAO.buscarPorTipo(tipo).isPresent(), "Plano ausente: " + tipo);
        }
    }

    @Test
    void seedDePlanosDevePreencherApenasTiposFaltantes() throws Exception {
        PlanoDAOEmMemoria planoDAO = new PlanoDAOEmMemoria();
        planoDAO.salvar(PlanoFactory.criar(TipoPlano.MENSAL));
        planoDAO.totalSalvamentos = 0;

        Main.inicializarPlanosPadrao(planoDAO);

        assertEquals(TipoPlano.values().length, planoDAO.listarTodos().size());
        assertEquals(TipoPlano.values().length - 1, planoDAO.totalSalvamentos);
    }

    @Test
    void seedDePlanosDeveRejeitarDaoNulo() {
        assertThrows(IllegalArgumentException.class, () -> Main.inicializarPlanosPadrao(null));
    }

    private static class PlanoDAOEmMemoria implements PlanoDAO {

        private final Map<TipoPlano, Plano> porTipo = new EnumMap<>(TipoPlano.class);
        private int proximoId = 1;
        private int totalSalvamentos = 0;

        @Override
        public void salvar(Plano plano) {
            if (porTipo.containsKey(plano.getTipo())) {
                throw new IllegalStateException("Tipo duplicado: " + plano.getTipo());
            }
            plano.atribuirId(proximoId++);
            porTipo.put(plano.getTipo(), plano);
            totalSalvamentos++;
        }

        @Override
        public void atualizar(Plano plano) {
            porTipo.put(plano.getTipo(), plano);
        }

        @Override
        public void remover(int id) {
            porTipo.values().removeIf(p -> p.getId() == id);
        }

        @Override
        public Optional<Plano> buscarPorId(int id) {
            return porTipo.values().stream().filter(p -> p.getId() == id).findFirst();
        }

        @Override
        public List<Plano> listarTodos() {
            return new ArrayList<>(porTipo.values());
        }

        @Override
        public Optional<Plano> buscarPorTipo(TipoPlano tipo) {
            return Optional.ofNullable(porTipo.get(tipo));
        }
    }
}
