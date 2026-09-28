package com.speedfast.dao;

import com.speedfast.modelo.Repartidor;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de RepartidorDAO contra la base de datos.
 *
 * Si MySQL no esta encendido las pruebas se omiten en lugar de fallar.
 *
 * @author Olga Rivas
 * @version 7.0
 */
class RepartidorDAOTest {

    /** Objeto que se esta probando. */
    private final RepartidorDAO dao = new RepartidorDAO();

    /**
     * Deja las tablas con los tres repartidores de siempre.
     *
     * @throws SQLException si falla la limpieza
     */
    @BeforeEach
    void prepararBase() throws SQLException {
        Assumptions.assumeTrue(BaseDeDatosDisponible.hay(),
                "se necesita MySQL encendido con la base speedfast_db");
        BaseDeDatosDisponible.limpiar();
    }

    @Test
    @DisplayName("listarTodos devuelve los repartidores guardados")
    void listaLosRepartidores() throws SQLException {
        List<Repartidor> repartidores = dao.listarTodos();

        assertEquals(3, repartidores.size());
    }

    @Test
    @DisplayName("Cada repartidor trae su id y su vehiculo desde la base")
    void cadaRepartidorTraeSusDatos() throws SQLException {
        Repartidor primero = dao.listarTodos().get(0);

        assertTrue(primero.getId() > 0, "deberia traer el id de la base");
        assertNotNull(primero.getNombre());
        assertNotNull(primero.getVehiculo());
    }

    @Test
    @DisplayName("Los repartidores vienen ordenados por nombre")
    void vienenOrdenadosPorNombre() throws SQLException {
        List<Repartidor> repartidores = dao.listarTodos();

        assertEquals("Camila Soto", repartidores.get(0).getNombre());
        assertEquals("Diego Fuentes", repartidores.get(1).getNombre());
        assertEquals("Luis Vera", repartidores.get(2).getNombre());
    }

    @Test
    @DisplayName("Guardar un repartidor le asigna el id que genera la base")
    void guardarAsignaElId() throws SQLException {
        Repartidor nuevo = new Repartidor("Ana Perez", "moto");

        dao.guardar(nuevo);

        assertTrue(nuevo.getId() > 0);
        assertEquals(4, dao.listarTodos().size());
    }

    @Test
    @DisplayName("Un repartidor guardado se vuelve a leer con sus datos")
    void elRepartidorGuardadoSeLeeIgual() throws SQLException {
        dao.guardar(new Repartidor("Ana Perez", "moto"));

        Repartidor leido = null;
        for (Repartidor repartidor : dao.listarTodos()) {
            if ("Ana Perez".equals(repartidor.getNombre())) {
                leido = repartidor;
            }
        }

        assertNotNull(leido);
        assertEquals("moto", leido.getVehiculo());
    }

    @Test
    @DisplayName("No se acepta un repartidor sin nombre")
    void rechazaNombreVacio() {
        assertThrows(IllegalArgumentException.class, () -> dao.guardar(new Repartidor("   ")));
        assertThrows(IllegalArgumentException.class, () -> dao.guardar(null));
    }
}
