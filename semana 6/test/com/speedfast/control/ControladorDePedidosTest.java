package com.speedfast.control;

import com.speedfast.modelo.EstadoPedido;
import com.speedfast.modelo.Pedido;
import com.speedfast.modelo.PedidoComida;
import com.speedfast.modelo.Repartidor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del controlador: registro de pedidos, identificadores sugeridos,
 * busqueda, asignacion de repartidores y cancelacion.
 *
 * No interviene Swing en ninguna prueba: el controlador guarda los datos y
 * las ventanas solo los muestran, asi que se puede probar por separado.
 *
 * @author Olga Rivas
 * @version 6.0
 */
class ControladorDePedidosTest {

    /** Controlador nuevo antes de cada prueba. */
    private ControladorDePedidos controlador;

    /**
     * Crea un controlador limpio para que una prueba no dependa de otra.
     */
    @BeforeEach
    void prepararControlador() {
        controlador = new ControladorDePedidos();
    }

    /**
     * Crea un pedido de comida con los datos minimos.
     *
     * @param id identificador del pedido
     * @return pedido listo para registrar
     */
    private Pedido pedidoDePrueba(String id) {
        return new PedidoComida(id, "Calle Falsa 123, Nunoa", 4.0, "Restaurante Uno", true);
    }

    // ------------------------------------------------------------------
    // Registro de pedidos
    // ------------------------------------------------------------------

    @Test
    @DisplayName("El controlador parte con los tres pedidos de ejemplo")
    void parteConPedidosDeEjemplo() {
        assertEquals(3, controlador.getTotalPedidos());
        assertEquals(3, controlador.contarPorEstado(EstadoPedido.RESERVADO));
    }

    @Test
    @DisplayName("Un pedido agregado queda en la lista y se puede buscar")
    void agregarSumaElPedido() {
        controlador.agregarPedido(pedidoDePrueba("P-010"));

        assertEquals(4, controlador.getTotalPedidos());
        assertNotNull(controlador.buscarPorId("P-010"));
    }

    @Test
    @DisplayName("No se acepta un pedido con un ID que ya existe")
    void rechazaIdRepetido() {
        controlador.agregarPedido(pedidoDePrueba("P-010"));

        assertThrows(IllegalArgumentException.class,
                () -> controlador.agregarPedido(pedidoDePrueba("P-010")));
        assertEquals(4, controlador.getTotalPedidos(),
                "el pedido rechazado no debe quedar guardado");
    }

    @Test
    @DisplayName("No se acepta un pedido nulo")
    void rechazaPedidoNulo() {
        assertThrows(IllegalArgumentException.class, () -> controlador.agregarPedido(null));
    }

    @Test
    @DisplayName("La lista que entrega el controlador es una copia")
    void getPedidosDevuelveUnaCopia() {
        List<Pedido> copia = controlador.getPedidos();
        copia.clear();

        assertEquals(3, controlador.getTotalPedidos(),
                "vaciar la copia no debe vaciar el controlador");
    }

    // ------------------------------------------------------------------
    // Identificador sugerido
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Con P-001, P-002 y P-003 cargados, el siguiente ID es P-004")
    void sugiereElSiguienteIdentificador() {
        assertEquals("P-004", controlador.sugerirIdPedido());
    }

    @Test
    @DisplayName("La sugerencia avanza a medida que se registran pedidos")
    void laSugerenciaAvanza() {
        controlador.agregarPedido(pedidoDePrueba(controlador.sugerirIdPedido()));

        assertEquals("P-005", controlador.sugerirIdPedido());
    }

    @Test
    @DisplayName("Un ID escrito a mano con otro formato no rompe la sugerencia")
    void ignoraLosIdConOtroFormato() {
        controlador.agregarPedido(pedidoDePrueba("PEDIDO-URGENTE"));

        assertEquals("P-004", controlador.sugerirIdPedido());
    }

    @Test
    @DisplayName("La sugerencia toma el numero mas alto, no la cantidad de pedidos")
    void sugiereApartirDelNumeroMasAlto() {
        controlador.agregarPedido(pedidoDePrueba("P-050"));

        assertEquals("P-051", controlador.sugerirIdPedido());
    }

    // ------------------------------------------------------------------
    // Busqueda
    // ------------------------------------------------------------------

    @Test
    @DisplayName("La busqueda no distingue mayusculas ni espacios sobrantes")
    void buscaSinDistinguirMayusculas() {
        assertNotNull(controlador.buscarPorId("p-001"));
        assertNotNull(controlador.buscarPorId("  P-001  "));
    }

    @Test
    @DisplayName("Buscar un ID que no existe devuelve null")
    void buscarIdInexistenteDevuelveNull() {
        assertNull(controlador.buscarPorId("P-999"));
        assertNull(controlador.buscarPorId(null));
        assertFalse(controlador.existeId("P-999"));
    }

    // ------------------------------------------------------------------
    // Asignacion de repartidores
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Asignar un repartidor deja el pedido ASIGNADO y en su ruta")
    void asignarDejaElPedidoAsignado() {
        Pedido pedido = controlador.buscarPorId("P-001");
        Repartidor repartidor = controlador.getRepartidores().get(0);

        assertTrue(controlador.asignarRepartidor(pedido, repartidor));
        assertEquals(EstadoPedido.ASIGNADO, pedido.getEstado());
        assertEquals(1, repartidor.getTotalPedidos());
    }

    @Test
    @DisplayName("Un pedido que ya tiene repartidor no se vuelve a asignar")
    void noSeAsignaDosVeces() {
        Pedido pedido = controlador.buscarPorId("P-001");
        List<Repartidor> repartidores = controlador.getRepartidores();
        controlador.asignarRepartidor(pedido, repartidores.get(0));

        assertFalse(controlador.asignarRepartidor(pedido, repartidores.get(1)),
                "solo se asignan pedidos que estan RESERVADO");
    }

    @Test
    @DisplayName("Asignar con datos nulos no hace nada")
    void asignarConNulosDevuelveFalse() {
        assertFalse(controlador.asignarRepartidor(null, controlador.getRepartidores().get(0)));
        assertFalse(controlador.asignarRepartidor(controlador.buscarPorId("P-001"), null));
    }

    // ------------------------------------------------------------------
    // Cancelacion
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Un pedido reservado se puede cancelar")
    void cancelaUnPedidoReservado() {
        Pedido pedido = controlador.buscarPorId("P-001");

        assertTrue(controlador.cancelarPedido(pedido, "el cliente se arrepintio"));
        assertEquals(EstadoPedido.CANCELADO, pedido.getEstado());
    }

    @Test
    @DisplayName("Cancelar un pedido asignado lo saca de la ruta del repartidor")
    void cancelarLoSacaDeLaRuta() {
        Pedido pedido = controlador.buscarPorId("P-001");
        Repartidor repartidor = controlador.getRepartidores().get(0);
        controlador.asignarRepartidor(pedido, repartidor);

        assertTrue(controlador.cancelarPedido(pedido, "direccion equivocada"));
        assertEquals(0, repartidor.getTotalPedidos(),
                "el repartidor no debe seguir cargando un pedido cancelado");
    }

    @Test
    @DisplayName("Un pedido que ya va en ruta no se puede cancelar")
    void noCancelaUnPedidoEnRuta() {
        Pedido pedido = controlador.buscarPorId("P-001");
        controlador.asignarRepartidor(pedido, controlador.getRepartidores().get(0));
        pedido.despachar();

        assertFalse(controlador.cancelarPedido(pedido, "ya es tarde"));
        assertEquals(EstadoPedido.DESPACHADO, pedido.getEstado());
    }

    @Test
    @DisplayName("Cancelar un pedido nulo no lanza excepcion")
    void cancelarNuloDevuelveFalse() {
        assertFalse(controlador.cancelarPedido(null, "sin pedido"));
    }

    // ------------------------------------------------------------------
    // Filtros, observadores y entregas
    // ------------------------------------------------------------------

    @Test
    @DisplayName("El filtro por estado devuelve solo los pedidos de ese estado")
    void filtraPorEstado() {
        Pedido pedido = controlador.buscarPorId("P-002");
        controlador.cancelarPedido(pedido, "prueba");

        assertEquals(1, controlador.getPedidosPorEstado(EstadoPedido.CANCELADO).size());
        assertEquals(2, controlador.contarPorEstado(EstadoPedido.RESERVADO));
    }

    @Test
    @DisplayName("Los observadores reciben aviso cuando cambian los datos")
    void avisaALosObservadores() {
        int[] avisos = {0};
        controlador.agregarObservador(() -> avisos[0]++);

        controlador.agregarPedido(pedidoDePrueba("P-010"));
        controlador.cancelarPedido(controlador.buscarPorId("P-010"), "prueba");

        assertEquals(2, avisos[0], "un aviso por el registro y otro por la cancelacion");
    }

    @Test
    @DisplayName("Sin pedidos asignados no sale ningun repartidor a ruta")
    void noIniciaEntregasSinAsignaciones() {
        assertEquals(0, controlador.iniciarEntregas(null));
        assertFalse(controlador.hayEntregasEnCurso());
    }
}
