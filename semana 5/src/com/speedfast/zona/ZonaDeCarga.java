package com.speedfast.zona;

import com.speedfast.modelo.EstadoPedido;
import com.speedfast.modelo.Pedido;
import com.speedfast.reporte.ConsolaSegura;

import java.util.ArrayList;
import java.util.List;

/**
 * Zona de carga de SpeedFast: el recurso compartido del sistema.
 *
 * Guarda los pedidos pendientes en una lista y controla el acceso de varios
 * repartidores a la vez. Sus metodos son synchronized, de modo que solo un
 * hilo puede estar agregando o retirando en un momento dado, y un repartidor
 * que no encuentra trabajo queda en espera con wait() hasta que llegue un
 * pedido nuevo o se cierre la recepcion.
 *
 * @author Olga Rivas
 * @version 5.0
 */
public class ZonaDeCarga implements FuenteDePedidos {

    /** Pedidos disponibles para retirar. */
    private final List<Pedido> pedidosPendientes = new ArrayList<>();

    /** Cuando es true ya no se aceptan mas pedidos. */
    private boolean recepcionCerrada;

    /** Total de pedidos que entraron a la zona. */
    private int totalRecibidos;

    /** Total de pedidos que salieron de la zona. */
    private int totalRetirados;

    /** Repartidores dormidos en wait() en este momento. */
    private int repartidoresEnEspera;

    /**
     * Deja un pedido disponible en la zona de carga y avisa a los repartidores
     * que esten esperando.
     *
     * @param p pedido que llega a la zona
     * @throws IllegalArgumentException si el pedido es nulo o no viene PENDIENTE
     * @throws IllegalStateException    si la recepcion ya fue cerrada
     */
    @Override
    public synchronized void agregarPedido(Pedido p) {
        if (p == null) {
            throw new IllegalArgumentException("No se puede agregar un pedido nulo a la zona de carga.");
        }
        if (recepcionCerrada) {
            throw new IllegalStateException("La recepcion esta cerrada: el pedido #"
                    + p.getId() + " ya no puede ingresar.");
        }
        if (p.getEstado() != EstadoPedido.PENDIENTE) {
            throw new IllegalArgumentException("A la zona de carga solo entran pedidos PENDIENTE. "
                    + "El pedido #" + p.getId() + " llego en estado " + p.getEstado() + ".");
        }

        pedidosPendientes.add(p);
        totalRecibidos++;

        // notifyAll despierta a todos los repartidores que estan esperando.
        notifyAll();
    }

    /**
     * Entrega un pedido a un repartidor y lo marca como EN_REPARTO.
     *
     * Si la zona esta vacia y la recepcion sigue abierta, el repartidor queda
     * esperando en wait() hasta que llegue un pedido o se cierre la recepcion.
     *
     * @return el pedido retirado, o null si la zona cerro y quedo vacia
     */
    @Override
    public synchronized Pedido retirarPedido() {
        // While y no if: al despertar hay que volver a comprobar la condicion,
        // porque otro repartidor puede haberse llevado el pedido que llego.
        while (pedidosPendientes.isEmpty() && !recepcionCerrada) {
            repartidoresEnEspera++;
            ConsolaSegura.imprimir("[Zona de carga] no quedan pedidos disponibles: "
                    + Thread.currentThread().getName() + " queda a la espera ("
                    + repartidoresEnEspera + " en espera).");
            try {
                wait();
            } catch (InterruptedException e) {
                // Se restaura la marca de interrupcion y se corta la espera.
                Thread.currentThread().interrupt();
                repartidoresEnEspera--;
                return null;
            }
            repartidoresEnEspera--;
        }

        if (pedidosPendientes.isEmpty()) {
            return null;
        }

        Pedido pedido = pedidosPendientes.remove(0);
        pedido.setEstado(EstadoPedido.EN_REPARTO);
        totalRetirados++;
        return pedido;
    }

    /**
     * Cierra la recepcion y despierta a los repartidores que esten esperando,
     * para que puedan terminar su turno.
     */
    @Override
    public synchronized void cerrarRecepcion() {
        recepcionCerrada = true;
        notifyAll();
    }

    /**
     * @return cantidad de pedidos que aun esperan en la zona
     */
    @Override
    public synchronized int getPedidosPendientes() {
        return pedidosPendientes.size();
    }

    /**
     * @return total de pedidos que entraron a la zona
     */
    public synchronized int getTotalRecibidos() {
        return totalRecibidos;
    }

    /**
     * @return total de pedidos que fueron retirados
     */
    public synchronized int getTotalRetirados() {
        return totalRetirados;
    }

    /**
     * @return true si la recepcion ya fue cerrada
     */
    public synchronized boolean estaRecepcionCerrada() {
        return recepcionCerrada;
    }

    /**
     * @return cuantos repartidores estan esperando trabajo en este momento
     */
    public synchronized int getRepartidoresEnEspera() {
        return repartidoresEnEspera;
    }

    /**
     * @return descripcion de la zona
     */
    @Override
    public String toString() {
        return "ZonaDeCarga (protegida con synchronized)";
    }
}
