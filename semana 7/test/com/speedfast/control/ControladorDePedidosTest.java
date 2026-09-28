package com.speedfast.control;

import com.speedfast.dao.ConexionBD;
import com.speedfast.modelo.EstadoPedido;
import com.speedfast.modelo.Pedido;
import com.speedfast.modelo.PedidoComida;
import com.speedfast.modelo.Repartidor;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del controlador ya conectado a la base de datos.
 *
 * Lo que comprueban, sobre todo, es que lo que se ve en la interfaz sea lo
 * mismo que quedo guardado: por eso varias pruebas vuelven a leer con
 * cargarDesdeBaseDatos antes de revisar el resultado.
 *
 * Si MySQL no esta encendido las pruebas se omiten en lugar de fallar.
 *
 * @author Olga Rivas
 * @version 7.0
 */
class ControladorDePedidosTest {

    /** Objeto que se esta probando. */
    private ControladorDePedidos controlador;

    /**
     * Deja la base limpia y el controlador recien cargado.
     *
     * @throws SQLException si falla la preparacion
     */
    @BeforeEach
    void prepararControlador() throws SQLException {
        Assumptions.assumeTrue(hayBaseDeDatos(),
                "se necesita MySQL encendido con la base speedfast_db");
        limpiar();
        controlador = new ControladorDePedidos();
        controlador.cargarDesdeBaseDatos();
    }

    /**
     * @return true si la base de datos responde
     */
    private boolean hayBaseDeDatos() {
        try {
            ConexionBD.comprobarConexion();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Vacia las tablas y repone los tres repartidores de la empresa.
     *
     * @throws SQLException si falla alguna sentencia
     */
    private void limpiar() throws SQLException {
        try (Connection conexion = ConexionBD.conectar();
             Statement sentencia = conexion.createStatement()) {

            sentencia.executeUpdate("DELETE FROM entrega");
            sentencia.executeUpdate("DELETE FROM pedido");
            sentencia.executeUpdate("DELETE FROM repartidor");
            sentencia.executeUpdate("INSERT INTO repartidor (nombre, vehiculo) VALUES"
                    + " ('Camila Soto', 'moto con mochila termica'),"
                    + " ('Diego Fuentes', 'furgon de carga'),"
                    + " ('Luis Vera', 'bicicleta electrica')");
        }
    }

    /**
     * Crea un pedido de comida con los datos minimos.
     *
     * @param codigo codigo visible del pedido
     * @return pedido listo para registrar
     */
    private Pedido pedidoDePrueba(String codigo) {
        return new PedidoComida(codigo, "Calle Falsa 123, Nunoa", 4.0, "Restaurante Uno", true);
    }

    // ------------------------------------------------------------------
    // Carga y registro
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Al cargar trae los repartidores que hay en la base")
    void cargaLosRepartidores() {
        assertEquals(3, controlador.getRepartidores().size());
    }

    @Test
    @DisplayName("Un pedido registrado queda guardado en la base")
    void elPedidoRegistradoQuedaGuardado() throws SQLException {
        controlador.agregarPedido(pedidoDePrueba("P-001"));

        ControladorDePedidos otro = new ControladorDePedidos();
        otro.cargarDesdeBaseDatos();

        assertEquals(1, otro.getTotalPedidos(),
                "un controlador nuevo deberia ver el pedido que guardo el anterior");
        assertNotNull(otro.buscarPorId("P-001"));
    }

    @Test
    @DisplayName("No se acepta un pedido con un codigo que ya existe")
    void rechazaCodigoRepetido() throws SQLException {
        controlador.agregarPedido(pedidoDePrueba("P-001"));

        assertThrows(IllegalArgumentException.class,
                () -> controlador.agregarPedido(pedidoDePrueba("P-001")));
        assertEquals(1, controlador.getTotalPedidos());
    }

    @Test
    @DisplayName("No se acepta un pedido nulo")
    void rechazaPedidoNulo() {
        assertThrows(IllegalArgumentException.class, () -> controlador.agregarPedido(null));
    }

    @Test
    @DisplayName("La lista que entrega el controlador es una copia")
    void getPedidosDevuelveUnaCopia() throws SQLException {
        controlador.agregarPedido(pedidoDePrueba("P-001"));

        List<Pedido> copia = controlador.getPedidos();
        copia.clear();

        assertEquals(1, controlador.getTotalPedidos());
    }

    // ------------------------------------------------------------------
    // Identificador sugerido y busqueda
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Con la base vacia el primer identificador sugerido es P-001")
    void sugiereElPrimerIdentificador() {
        assertEquals("P-001", controlador.sugerirIdPedido());
    }

    @Test
    @DisplayName("La sugerencia avanza a medida que se registran pedidos")
    void laSugerenciaAvanza() throws SQLException {
        controlador.agregarPedido(pedidoDePrueba(controlador.sugerirIdPedido()));

        assertEquals("P-002", controlador.sugerirIdPedido());
    }

    @Test
    @DisplayName("La sugerencia toma el numero mas alto, no la cantidad de pedidos")
    void sugiereApartirDelNumeroMasAlto() throws SQLException {
        controlador.agregarPedido(pedidoDePrueba("P-050"));

        assertEquals("P-051", controlador.sugerirIdPedido());
    }

    @Test
    @DisplayName("La busqueda no distingue mayusculas ni espacios sobrantes")
    void buscaSinDistinguirMayusculas() throws SQLException {
        controlador.agregarPedido(pedidoDePrueba("P-001"));

        assertNotNull(controlador.buscarPorId("p-001"));
        assertNotNull(controlador.buscarPorId("  P-001  "));
        assertNull(controlador.buscarPorId("P-999"));
        assertFalse(controlador.existeId("P-999"));
    }

    // ------------------------------------------------------------------
    // Asignacion y cancelacion
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Asignar deja el pedido ASIGNADO y lo guarda asi en la base")
    void asignarQuedaGuardado() throws SQLException {
        controlador.agregarPedido(pedidoDePrueba("P-001"));
        Pedido pedido = controlador.buscarPorId("P-001");
        Repartidor repartidor = controlador.getRepartidores().get(0);

        assertTrue(controlador.asignarRepartidor(pedido, repartidor));
        assertEquals(EstadoPedido.ASIGNADO, pedido.getEstado());

        controlador.cargarDesdeBaseDatos();
        assertEquals(EstadoPedido.ASIGNADO, controlador.buscarPorId("P-001").getEstado());
    }

    @Test
    @DisplayName("Un pedido que ya tiene repartidor no se vuelve a asignar")
    void noSeAsignaDosVeces() throws SQLException {
        controlador.agregarPedido(pedidoDePrueba("P-001"));
        Pedido pedido = controlador.buscarPorId("P-001");
        List<Repartidor> repartidores = controlador.getRepartidores();
        controlador.asignarRepartidor(pedido, repartidores.get(0));

        assertFalse(controlador.asignarRepartidor(pedido, repartidores.get(1)));
    }

    @Test
    @DisplayName("Cancelar un pedido asignado lo saca de la ruta y lo guarda cancelado")
    void cancelarQuedaGuardado() throws SQLException {
        controlador.agregarPedido(pedidoDePrueba("P-001"));
        Pedido pedido = controlador.buscarPorId("P-001");
        Repartidor repartidor = controlador.getRepartidores().get(0);
        controlador.asignarRepartidor(pedido, repartidor);

        assertTrue(controlador.cancelarPedido(pedido, "direccion equivocada"));
        assertEquals(0, repartidor.getTotalPedidos());

        controlador.cargarDesdeBaseDatos();
        assertEquals(EstadoPedido.CANCELADO, controlador.buscarPorId("P-001").getEstado());
    }

    @Test
    @DisplayName("Un pedido que ya va en ruta no se puede cancelar")
    void noCancelaUnPedidoEnRuta() throws SQLException {
        controlador.agregarPedido(pedidoDePrueba("P-001"));
        Pedido pedido = controlador.buscarPorId("P-001");
        controlador.asignarRepartidor(pedido, controlador.getRepartidores().get(0));
        pedido.despachar();

        assertFalse(controlador.cancelarPedido(pedido, "ya es tarde"));
        assertEquals(EstadoPedido.DESPACHADO, pedido.getEstado());
    }

    // ------------------------------------------------------------------
    // Entregas
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Al entregar un pedido se guarda el estado y la entrega")
    void sincronizarGuardaLaEntrega() throws SQLException {
        controlador.agregarPedido(pedidoDePrueba("P-001"));
        Pedido pedido = controlador.buscarPorId("P-001");
        controlador.asignarRepartidor(pedido, controlador.getRepartidores().get(0));
        pedido.despachar();
        pedido.registrarEntrega(900);

        controlador.sincronizarConBase(pedido);

        controlador.cargarDesdeBaseDatos();
        assertEquals(EstadoPedido.ENTREGADO, controlador.buscarPorId("P-001").getEstado());
        assertEquals(1, contarEntregas(), "deberia haber quedado una fila en la tabla entrega");
    }

    @Test
    @DisplayName("Sin pedidos asignados no sale ningun repartidor a ruta")
    void noIniciaEntregasSinAsignaciones() {
        assertEquals(0, controlador.iniciarEntregas(null));
        assertFalse(controlador.hayEntregasEnCurso());
    }

    @Test
    @DisplayName("Un repartidor registrado queda guardado en la base")
    void elRepartidorRegistradoQuedaGuardado() throws SQLException {
        controlador.agregarRepartidor(new Repartidor("Ana Perez", "moto"));

        controlador.cargarDesdeBaseDatos();
        assertEquals(4, controlador.getRepartidores().size());
    }

    /**
     * Cuenta las filas de la tabla entrega.
     *
     * @return cantidad de entregas registradas
     * @throws SQLException si falla la consulta
     */
    private int contarEntregas() throws SQLException {
        try (Connection conexion = ConexionBD.conectar();
             Statement sentencia = conexion.createStatement();
             java.sql.ResultSet filas = sentencia.executeQuery("SELECT COUNT(*) FROM entrega")) {

            return filas.next() ? filas.getInt(1) : 0;
        }
    }
}
