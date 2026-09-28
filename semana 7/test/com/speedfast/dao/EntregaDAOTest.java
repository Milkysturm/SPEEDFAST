package com.speedfast.dao;

import com.speedfast.modelo.Entrega;
import com.speedfast.modelo.Pedido;
import com.speedfast.modelo.PedidoComida;
import com.speedfast.modelo.Repartidor;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de EntregaDAO contra la base de datos.
 *
 * Ademas de guardar y leer, comprueban que las llaves foraneas hagan su
 * trabajo: una entrega no puede apuntar a un pedido o a un repartidor que no
 * existen.
 *
 * Si MySQL no esta encendido las pruebas se omiten en lugar de fallar.
 *
 * @author Olga Rivas
 * @version 7.0
 */
class EntregaDAOTest {

    /** Objeto que se esta probando. */
    private final EntregaDAO dao = new EntregaDAO();

    /** Acceso a pedidos, para tener uno al que apuntar. */
    private final PedidoDAO pedidoDAO = new PedidoDAO();

    /** Acceso a repartidores, para tener uno al que apuntar. */
    private final RepartidorDAO repartidorDAO = new RepartidorDAO();

    /** Pedido ya guardado que usan las pruebas. */
    private Pedido pedido;

    /** Repartidor ya guardado que usan las pruebas. */
    private Repartidor repartidor;

    /**
     * Deja la base limpia y crea un pedido y un repartidor guardados.
     *
     * @throws SQLException si falla la preparacion
     */
    @BeforeEach
    void prepararBase() throws SQLException {
        Assumptions.assumeTrue(BaseDeDatosDisponible.hay(),
                "se necesita MySQL encendido con la base speedfast_db");
        BaseDeDatosDisponible.limpiar();

        pedido = new PedidoComida("P-001", "Av. Providencia 1234, Nunoa", 4.5, "Sushi Kai", true);
        pedidoDAO.guardar(pedido);
        repartidor = repartidorDAO.listarTodos().get(0);
    }

    @Test
    @DisplayName("Guardar una entrega le asigna el id que genera la base")
    void guardarAsignaElId() throws SQLException {
        Entrega entrega = Entrega.ahora(pedido.getId(), repartidor.getId());

        dao.guardar(entrega);

        assertTrue(entrega.getId() > 0);
    }

    @Test
    @DisplayName("La entrega guardada aparece al listar, con su fecha y su hora")
    void laEntregaSeVuelveALeer() throws SQLException {
        Entrega entrega = new Entrega(pedido.getId(), repartidor.getId(),
                LocalDate.of(2026, 9, 28), LocalTime.of(14, 30, 0));

        dao.guardar(entrega);

        Entrega leida = dao.listarTodas().get(0);
        assertEquals(pedido.getId(), leida.getIdPedido());
        assertEquals(repartidor.getId(), leida.getIdRepartidor());
        assertEquals(LocalDate.of(2026, 9, 28), leida.getFecha());
        assertEquals(LocalTime.of(14, 30, 0), leida.getHora());
    }

    @Test
    @DisplayName("Un pedido puede tener varias entregas")
    void unPedidoPuedeTenerVariasEntregas() throws SQLException {
        dao.guardar(Entrega.ahora(pedido.getId(), repartidor.getId()));
        dao.guardar(Entrega.ahora(pedido.getId(), repartidor.getId()));

        assertEquals(2, dao.listarTodas().size());
    }

    @Test
    @DisplayName("La llave foranea rechaza una entrega de un pedido que no existe")
    void rechazaPedidoInexistente() {
        assertThrows(SQLException.class,
                () -> dao.guardar(Entrega.ahora(999999, repartidor.getId())));
    }

    @Test
    @DisplayName("La llave foranea rechaza una entrega de un repartidor que no existe")
    void rechazaRepartidorInexistente() {
        assertThrows(SQLException.class,
                () -> dao.guardar(Entrega.ahora(pedido.getId(), 999999)));
    }

    @Test
    @DisplayName("No se acepta una entrega incompleta")
    void rechazaEntregaIncompleta() {
        assertThrows(IllegalArgumentException.class, () -> dao.guardar(null));
        assertThrows(IllegalArgumentException.class,
                () -> dao.guardar(Entrega.ahora(0, repartidor.getId())));
        assertThrows(IllegalArgumentException.class,
                () -> dao.guardar(new Entrega(pedido.getId(), repartidor.getId(), null, null)));
    }
}
