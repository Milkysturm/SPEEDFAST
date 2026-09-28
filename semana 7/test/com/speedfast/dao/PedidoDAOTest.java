package com.speedfast.dao;

import com.speedfast.modelo.EstadoPedido;
import com.speedfast.modelo.Pedido;
import com.speedfast.modelo.PedidoComida;
import com.speedfast.modelo.PedidoEncomienda;
import com.speedfast.modelo.PedidoExpress;
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
 * Pruebas de PedidoDAO contra la base de datos.
 *
 * Comprueban que un pedido guardado se pueda volver a leer con sus datos y
 * con la subclase que le corresponde, y que la base rechace lo que no debe
 * aceptar.
 *
 * Si MySQL no esta encendido las pruebas se omiten en lugar de fallar.
 *
 * @author Olga Rivas
 * @version 7.0
 */
class PedidoDAOTest {

    /** Objeto que se esta probando. */
    private final PedidoDAO dao = new PedidoDAO();

    /**
     * Deja las tablas vacias antes de cada prueba.
     *
     * @throws SQLException si falla la limpieza
     */
    @BeforeEach
    void prepararBase() throws SQLException {
        Assumptions.assumeTrue(BaseDeDatosDisponible.hay(),
                "se necesita MySQL encendido con la base speedfast_db");
        BaseDeDatosDisponible.limpiar();
    }

    /**
     * Busca en la lista el pedido que tiene ese codigo.
     *
     * @param pedidos  lista leida de la base
     * @param codigo   codigo buscado
     * @return el pedido, o null si no esta
     */
    private Pedido buscar(List<Pedido> pedidos, String codigo) {
        for (Pedido pedido : pedidos) {
            if (codigo.equals(pedido.getIdPedido())) {
                return pedido;
            }
        }
        return null;
    }

    @Test
    @DisplayName("Guardar un pedido le asigna el id que genera la base")
    void guardarAsignaElId() throws SQLException {
        Pedido pedido = new PedidoComida("P-001", "Av. Providencia 1234, Nunoa", 4.5,
                "Sushi Kai", true);

        dao.guardar(pedido);

        assertTrue(pedido.getId() > 0, "el pedido deberia quedar con el id de la base");
    }

    @Test
    @DisplayName("Un pedido de comida vuelve como PedidoComida y con sus datos")
    void elPedidoDeComidaVuelveCompleto() throws SQLException {
        dao.guardar(new PedidoComida("P-001", "Av. Providencia 1234, Nunoa", 4.5,
                "Sushi Kai", true));

        Pedido leido = buscar(dao.listarTodos(), "P-001");

        assertNotNull(leido);
        assertTrue(leido instanceof PedidoComida, "deberia reconstruirse como PedidoComida");
        assertEquals("Sushi Kai", ((PedidoComida) leido).getRestaurante());
        assertTrue(((PedidoComida) leido).isRequiereMochilaTermica());
        assertEquals("Av. Providencia 1234, Nunoa", leido.getDireccionEntrega());
        assertEquals(4.5, leido.getDistanciaKm(), 0.001);
        assertEquals(EstadoPedido.RESERVADO, leido.getEstado());
    }

    @Test
    @DisplayName("Una encomienda vuelve con su peso y su embalaje")
    void laEncomiendaVuelveCompleta() throws SQLException {
        dao.guardar(new PedidoEncomienda("P-002", "Los Aromos 456, Maipu", 8.0,
                12.5, "Caja reforzada"));

        Pedido leido = buscar(dao.listarTodos(), "P-002");

        assertTrue(leido instanceof PedidoEncomienda);
        assertEquals(12.5, ((PedidoEncomienda) leido).getPesoKg(), 0.001);
        assertEquals("Caja reforzada", ((PedidoEncomienda) leido).getTipoEmbalaje());
    }

    @Test
    @DisplayName("Una compra express vuelve con su local")
    void laCompraExpressVuelveCompleta() throws SQLException {
        dao.guardar(new PedidoExpress("P-003", "El Roble 789, La Florida", 2.0,
                "Farmacia Central", true));

        Pedido leido = buscar(dao.listarTodos(), "P-003");

        assertTrue(leido instanceof PedidoExpress);
        assertEquals("Farmacia Central", ((PedidoExpress) leido).getLocal());
    }

    @Test
    @DisplayName("listarTodos trae todos los pedidos guardados")
    void listaTodosLosPedidos() throws SQLException {
        dao.guardar(new PedidoComida("P-001", "Uno", 1.0, "Local", false));
        dao.guardar(new PedidoEncomienda("P-002", "Dos", 2.0, 1.0, "Caja"));
        dao.guardar(new PedidoExpress("P-003", "Tres", 3.0, "Farmacia", false));

        assertEquals(3, dao.listarTodos().size());
    }

    @Test
    @DisplayName("La base rechaza dos pedidos con el mismo codigo")
    void rechazaCodigoRepetido() throws SQLException {
        dao.guardar(new PedidoComida("P-001", "Uno", 1.0, "Local", false));

        assertThrows(SQLException.class,
                () -> dao.guardar(new PedidoComida("P-001", "Otro", 2.0, "Otro local", false)));
    }

    @Test
    @DisplayName("No se acepta guardar un pedido nulo")
    void rechazaPedidoNulo() {
        assertThrows(IllegalArgumentException.class, () -> dao.guardar(null));
    }

    @Test
    @DisplayName("El cambio de estado se guarda y se vuelve a leer igual")
    void guardaElCambioDeEstado() throws SQLException {
        Pedido pedido = new PedidoComida("P-001", "Uno", 1.0, "Local", false);
        dao.guardar(pedido);

        pedido.asignarRepartidor(new Repartidor("Camila Soto"));
        dao.actualizarEstado(pedido);
        assertEquals(EstadoPedido.ASIGNADO, buscar(dao.listarTodos(), "P-001").getEstado());

        pedido.despachar();
        dao.actualizarEstado(pedido);
        assertEquals(EstadoPedido.DESPACHADO, buscar(dao.listarTodos(), "P-001").getEstado());

        pedido.registrarEntrega(900);
        dao.actualizarEstado(pedido);
        assertEquals(EstadoPedido.ENTREGADO, buscar(dao.listarTodos(), "P-001").getEstado());
    }

    @Test
    @DisplayName("Un pedido cancelado se guarda como cancelado")
    void guardaLaCancelacion() throws SQLException {
        Pedido pedido = new PedidoComida("P-001", "Uno", 1.0, "Local", false);
        dao.guardar(pedido);

        pedido.cancelar("el cliente se arrepintio");
        dao.actualizarEstado(pedido);

        assertEquals(EstadoPedido.CANCELADO, buscar(dao.listarTodos(), "P-001").getEstado());
    }

    @Test
    @DisplayName("Actualizar un pedido que nunca se guardo no hace nada")
    void actualizarPedidoSinGuardarNoFalla() throws SQLException {
        dao.actualizarEstado(new PedidoComida("P-099", "Uno", 1.0, "Local", false));

        assertEquals(0, dao.listarTodos().size());
    }
}
