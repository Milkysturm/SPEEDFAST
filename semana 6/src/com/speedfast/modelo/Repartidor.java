package com.speedfast.modelo;

import com.speedfast.contrato.EscuchaDeEntregas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Persona que realiza las entregas y, a la vez, tarea ejecutable.
 *
 * Implementa Runnable para que cada repartidor recorra su ruta en su propio
 * hilo. A diferencia de las semanas anteriores, aca no imprime en consola:
 * informa cada avance a un EscuchaDeEntregas, que es la ventana encargada de
 * mostrarlo. Asi el modelo no depende de la interfaz grafica.
 *
 * @author Olga Rivas
 * @version 6.0
 */
public class Repartidor implements Runnable {

    /** Duracion minima simulada de una entrega, en milisegundos. */
    private static final int PAUSA_MINIMA_MS = 700;

    /** Duracion maxima simulada de una entrega, en milisegundos. */
    private static final int PAUSA_MAXIMA_MS = 1800;

    /** Nombre de la persona que reparte. */
    private String nombre;

    /** Vehiculo con el que realiza el reparto. */
    private String vehiculo;

    /** Pedidos que este repartidor debe entregar. */
    private final List<Pedido> pedidosAsignados;

    /** Quien recibe los avisos de avance. Puede ser null. */
    private EscuchaDeEntregas escucha;

    /** Entregas completadas en la ruta. */
    private int entregasCompletadas;

    /**
     * Crea un repartidor con su vehiculo.
     *
     * @param nombre   nombre del repartidor
     * @param vehiculo vehiculo asignado
     */
    public Repartidor(String nombre, String vehiculo) {
        this.nombre = nombre;
        this.vehiculo = vehiculo;
        this.pedidosAsignados = new ArrayList<>();
    }

    /**
     * Crea un repartidor sin vehiculo definido.
     *
     * @param nombre nombre del repartidor
     */
    public Repartidor(String nombre) {
        this(nombre, "vehiculo por confirmar");
    }

    /**
     * Suma un pedido a la ruta y lo deja asignado a este repartidor.
     *
     * @param pedido pedido a incorporar
     * @return true si el pedido quedo incorporado
     */
    public boolean asignarPedido(Pedido pedido) {
        if (pedido == null || pedidosAsignados.contains(pedido)) {
            return false;
        }
        pedidosAsignados.add(pedido);
        pedido.asignarRepartidor(this);
        return true;
    }

    /**
     * Quita un pedido de la ruta, por ejemplo cuando se cancela.
     *
     * @param pedido pedido a quitar
     * @return true si el pedido estaba en la ruta
     */
    public boolean quitarPedido(Pedido pedido) {
        return pedidosAsignados.remove(pedido);
    }

    /**
     * Quita de la ruta los pedidos que ya fueron entregados.
     */
    public void limpiarEntregados() {
        pedidosAsignados.removeIf(pedido -> pedido.getEstado() == EstadoPedido.ENTREGADO);
    }

    /**
     * Recorre la ruta entregando los pedidos uno tras otro.
     *
     * Por cada pedido lo despacha, simula el viaje con una pausa aleatoria y
     * lo marca como entregado, avisando de cada paso al escucha.
     */
    @Override
    public void run() {
        entregasCompletadas = 0;

        // El finally garantiza que el aviso de termino salga siempre, incluso
        // si la ruta se interrumpe: si no, la ventana quedaria esperando un
        // aviso que nunca llega y sus botones no se volverian a habilitar.
        try {
            for (Pedido pedido : new ArrayList<>(pedidosAsignados)) {
                try {
                    entregar(pedido);
                } catch (InterruptedException e) {
                    // Se restaura la marca de interrupcion y se corta la ruta.
                    Thread.currentThread().interrupt();
                    avisar(pedido, "ruta interrumpida con el pedido "
                            + pedido.getIdPedido() + " en la mano");
                    return;
                } catch (RuntimeException e) {
                    // Un pedido con problemas no detiene el resto de la ruta.
                    avisar(pedido, "problema con el pedido " + pedido.getIdPedido()
                            + ": " + e.getMessage());
                }
            }
        } finally {
            if (escucha != null) {
                escucha.rutaTerminada(nombre, entregasCompletadas);
            }
        }
    }

    /**
     * Entrega un pedido: lo despacha, simula el viaje y lo marca entregado.
     *
     * @param pedido pedido a entregar
     * @throws InterruptedException si el hilo es interrumpido durante el viaje
     */
    private void entregar(Pedido pedido) throws InterruptedException {
        if (pedido.getEstado() == EstadoPedido.ENTREGADO) {
            return;
        }

        if (!pedido.despachar("entrega iniciada desde la interfaz")) {
            avisar(pedido, "no se pudo despachar (" + pedido.getEstado().getDescripcion() + ")");
            return;
        }
        avisar(pedido, "en ruta con " + nombre);

        long viajeMs = ThreadLocalRandom.current().nextLong(PAUSA_MINIMA_MS, PAUSA_MAXIMA_MS + 1);
        Thread.sleep(viajeMs);

        pedido.registrarEntrega(viajeMs);
        entregasCompletadas++;
        avisar(pedido, "entregado por " + nombre + " en " + viajeMs + " ms");
    }

    /**
     * Informa un avance al escucha, si hay alguno registrado.
     *
     * @param pedido  pedido afectado
     * @param mensaje descripcion de lo ocurrido
     */
    private void avisar(Pedido pedido, String mensaje) {
        if (escucha != null) {
            escucha.pedidoActualizado(pedido, mensaje);
        }
    }

    /**
     * Define quien recibira los avisos de avance.
     *
     * @param escucha objeto que mostrara los avances
     */
    public void setEscucha(EscuchaDeEntregas escucha) {
        this.escucha = escucha;
    }

    /**
     * @return nombre del repartidor
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * @param nombre nuevo nombre del repartidor
     */
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    /**
     * @return vehiculo del repartidor
     */
    public String getVehiculo() {
        return vehiculo;
    }

    /**
     * @param vehiculo nuevo vehiculo del repartidor
     */
    public void setVehiculo(String vehiculo) {
        this.vehiculo = vehiculo;
    }

    /**
     * @return lista de solo lectura con los pedidos de la ruta
     */
    public List<Pedido> getPedidosAsignados() {
        return Collections.unmodifiableList(pedidosAsignados);
    }

    /**
     * @return cantidad de pedidos en la ruta
     */
    public int getTotalPedidos() {
        return pedidosAsignados.size();
    }

    /**
     * @return cantidad de entregas completadas
     */
    public int getEntregasCompletadas() {
        return entregasCompletadas;
    }

    /**
     * Devuelve el nombre y el vehiculo, que es lo que se muestra en los
     * combos de la interfaz.
     *
     * @return descripcion del repartidor
     */
    @Override
    public String toString() {
        return nombre + " (" + vehiculo + ")";
    }
}
