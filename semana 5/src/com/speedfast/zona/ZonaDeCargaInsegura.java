package com.speedfast.zona;

import com.speedfast.modelo.EstadoPedido;
import com.speedfast.modelo.Pedido;

import java.util.ArrayList;
import java.util.List;

/**
 * Version de la zona de carga SIN sincronizar, usada solo para la
 * demostracion.
 *
 * Hace lo mismo que ZonaDeCarga pero sin proteger el acceso, por lo que dos
 * repartidores pueden retirar el mismo pedido o hacer desaparecer otro. Sirve
 * para comparar ambas versiones en la misma jornada.
 *
 * @author Olga Rivas
 * @version 5.0
 */
public class ZonaDeCargaInsegura implements FuenteDePedidos {

    /** Duracion de la ventana entre consultar y extraer, en milisegundos. */
    private static final long VENTANA_MS = 2;

    /** Pedidos disponibles, sin proteccion frente a accesos simultaneos. */
    private final List<Pedido> pedidosPendientes = new ArrayList<>();

    /**
     * Agrega un pedido a la lista, sin sincronizar.
     *
     * @param p pedido que llega a la zona
     * @throws IllegalArgumentException si el pedido es nulo o no viene PENDIENTE
     */
    @Override
    public void agregarPedido(Pedido p) {
        if (p == null) {
            throw new IllegalArgumentException("No se puede agregar un pedido nulo a la zona de carga.");
        }
        if (p.getEstado() != EstadoPedido.PENDIENTE) {
            throw new IllegalArgumentException("A la zona de carga solo entran pedidos PENDIENTE. "
                    + "El pedido #" + p.getId() + " llego en estado " + p.getEstado() + ".");
        }
        pedidosPendientes.add(p);
    }

    /**
     * Retira un pedido sin ninguna proteccion.
     *
     * Consulta, lee y extrae en pasos separados, de modo que dos hilos pueden
     * quedarse con el mismo pedido, perder otro o provocar una
     * IndexOutOfBoundsException.
     *
     * @return un pedido, o null si en ese instante la lista parecia vacia
     */
    @Override
    public Pedido retirarPedido() {
        if (pedidosPendientes.isEmpty()) {
            return null;
        }

        Pedido pedido = pedidosPendientes.get(0);

        // Ventana entre consultar y extraer: aca es donde otro hilo alcanza a
        // ver y tomar el mismo pedido.
        esperarUnInstante();

        pedidosPendientes.remove(0);
        pedido.setEstado(EstadoPedido.EN_REPARTO);
        return pedido;
    }

    /**
     * Mantiene abierta la ventana entre consultar y extraer durante unos
     * milisegundos, para que el error se vea en cada corrida.
     */
    private void esperarUnInstante() {
        try {
            Thread.sleep(VENTANA_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * No hace nada: en esta version los repartidores terminan apenas la lista
     * parece vacia, asi que no hay nadie esperando a quien avisar.
     */
    @Override
    public void cerrarRecepcion() {
        // Sin espera que interrumpir.
    }

    /**
     * @return cantidad de pedidos que aun esperan en la zona
     */
    @Override
    public int getPedidosPendientes() {
        return pedidosPendientes.size();
    }

    /**
     * @return descripcion de la zona
     */
    @Override
    public String toString() {
        return "ZonaDeCargaInsegura (sin sincronizar, solo para la demostracion)";
    }
}
