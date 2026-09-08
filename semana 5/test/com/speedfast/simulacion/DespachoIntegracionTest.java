package com.speedfast.simulacion;

import com.speedfast.modelo.EstadoPedido;
import com.speedfast.modelo.Pedido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.speedfast.zona.ZonaDeCarga;
import com.speedfast.zona.ZonaDeCargaInsegura;

/**
 * Pruebas del sistema completo: la zona de carga, los repartidores, el
 * registro y el pool de hilos funcionando juntos, tal como en Main.
 *
 * @author Olga Rivas
 * @version 5.0
 */
class DespachoIntegracionTest {

    @RepeatedTest(5)
    @DisplayName("Con la zona sincronizada, todo pedido se entrega una sola vez")
    void laJornadaSincronizadaTerminaSinIncidencias() {
        SimulacionDeDespacho simulacion =
                new SimulacionDeDespacho("Prueba sincronizada", new ZonaDeCarga());
        simulacion.cargarPedidos(pedidos(6));
        simulacion.contratarRepartidores("Juan", "Camila", "Luis");

        boolean completa = simulacion.ejecutar();

        assertTrue(completa, "todos los repartidores debian terminar su turno");
        assertEquals(6, simulacion.getPedidosEntregados(), "faltaron pedidos por entregar");
        assertEquals(6, simulacion.getRegistro().getEntregas());
        assertEquals(6, simulacion.getRegistro().getPedidosRetirados());
        assertEquals(0, simulacion.getRegistro().getRetirosDuplicados());
        assertEquals(0, simulacion.getRegistro().getTotalIncidencias(),
                "una jornada sincronizada no puede tener incidencias");
        assertTrue(simulacion.todoEnOrden());

        for (Pedido pedido : simulacion.getPedidos()) {
            assertEquals(EstadoPedido.ENTREGADO, pedido.getEstado());
            assertTrue(pedido.getRepartidorAsignado() != null,
                    "el pedido #" + pedido.getId() + " quedo sin responsable");
        }
    }

    @RepeatedTest(3)
    @DisplayName("Los pedidos que llegan durante el turno tambien terminan entregados")
    void losPedidosTardiosSeEntregan() {
        SimulacionDeDespacho simulacion =
                new SimulacionDeDespacho("Prueba con llegada tardia", new ZonaDeCarga());
        simulacion.cargarPedidos(pedidos(3));
        simulacion.contratarRepartidores("Marcela", "Diego");

        List<Pedido> tardios = new ArrayList<>(Arrays.asList(
                new Pedido(101, "Las Condes"),
                new Pedido(102, "La Florida")));

        boolean completa = simulacion.ejecutar(tardios, 300);

        assertTrue(completa);
        assertEquals(5, simulacion.getTotalPedidos());
        assertEquals(5, simulacion.getPedidosEntregados(),
                "los pedidos que llegaron tarde tambien deben repartirse");
        assertEquals(0, simulacion.getRegistro().getTotalIncidencias());
    }

    @RepeatedTest(3)
    @DisplayName("La misma jornada sin sincronizar si presenta incidencias (control)")
    void laJornadaSinSincronizarFalla() {
        SimulacionDeDespacho simulacion =
                new SimulacionDeDespacho("Prueba sin sincronizar", new ZonaDeCargaInsegura());
        simulacion.cargarPedidos(pedidos(12));
        simulacion.contratarRepartidores("Juan", "Camila", "Luis");

        simulacion.ejecutar();

        assertTrue(simulacion.getRegistro().getTotalIncidencias() > 0,
                "se esperaba que la zona sin proteger fallara");
        assertTrue(!simulacion.todoEnOrden(),
                "una jornada sin sincronizar no puede darse por correcta");
    }

    /**
     * Crea pedidos numerados desde 1.
     *
     * @param cantidad cantidad de pedidos
     * @return los pedidos creados
     */
    private List<Pedido> pedidos(int cantidad) {
        List<Pedido> pedidos = new ArrayList<>();
        for (int i = 1; i <= cantidad; i++) {
            pedidos.add(new Pedido(i, "Destino " + i));
        }
        return pedidos;
    }
}
