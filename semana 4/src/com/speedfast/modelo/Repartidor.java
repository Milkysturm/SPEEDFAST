package com.speedfast.modelo;

import com.speedfast.reporte.ConsolaSegura;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Persona que realiza las entregas y, desde la semana 4, tarea ejecutable.
 *
 * Implementa {@link Runnable} y no extiende Thread porque lo que se quiere
 * modelar es EL TRABAJO ("recorrer mi ruta"), no un tipo especial de hilo.
 * Separarlos permite que sea el {@code ExecutorService} quien decida en que
 * hilo corre cada ruta y cuantos hilos hay en total, y deja a Repartidor
 * libre para seguir heredando de otra clase si el dia de manana hiciera falta.
 *
 * Cada repartidor trabaja unicamente sobre los pedidos de su propia lista, y
 * un pedido pertenece a una sola ruta. Gracias a eso los hilos no comparten
 * objetos mutables entre si y no hay condiciones de carrera que resolver con
 * bloqueos: lo unico realmente compartido es la salida por consola, que se
 * canaliza por {@link ConsolaSegura}.
 *
 * @author Olga Rivas
 * @version 4.0
 */
public class Repartidor implements Runnable {

    /** Duracion minima simulada de una entrega, en milisegundos. */
    private static final int PAUSA_MINIMA_MS = 400;

    /** Duracion maxima simulada de una entrega, en milisegundos. */
    private static final int PAUSA_MAXIMA_MS = 1500;

    /** Nombre de la persona que reparte. */
    private String nombre;

    /** Vehiculo con el que realiza el reparto. */
    private String vehiculo;

    /** Pedidos que este repartidor debe entregar durante su ruta. */
    private final List<Pedido> pedidosAsignados;

    /** Entregas que alcanzo a completar. Solo lo toca su propio hilo. */
    private int entregasCompletadas;

    /** Tiempo total simulado de la ruta, en milisegundos. */
    private long tiempoRutaMs;

    /**
     * Constructor completo.
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
     * Constructor usado cuando el operador indica solo el nombre y el vehiculo
     * aun no se conoce.
     *
     * @param nombre nombre del repartidor
     */
    public Repartidor(String nombre) {
        this(nombre, "vehiculo por confirmar");
    }

    // ------------------------------------------------------------------
    // Armado de la ruta
    // ------------------------------------------------------------------

    /**
     * Suma un pedido a la ruta de este repartidor y deja constancia de la
     * asignacion en el propio pedido.
     *
     * @param pedido pedido a incorporar en la ruta
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

    // ------------------------------------------------------------------
    // Runnable: el trabajo que ejecuta el hilo
    // ------------------------------------------------------------------

    /**
     * Recorre la ruta entregando los pedidos uno tras otro.
     *
     * Dentro de una misma ruta las entregas son secuenciales, porque un
     * repartidor no puede estar en dos lugares a la vez; lo que ocurre en
     * paralelo es una ruta respecto de las otras.
     *
     * El metodo esta escrito para no dejar nunca la simulacion a medias: si
     * un pedido falla se informa y se sigue con el siguiente, y si el hilo es
     * interrumpido se respeta la interrupcion y se corta la ruta.
     */
    @Override
    public void run() {
        ConsolaSegura.imprimir("[Repartidor: " + nombre + "] inicia su ruta con "
                + pedidosAsignados.size() + " pedido(s) en " + vehiculo
                + ". (hilo " + Thread.currentThread().getName() + ")");

        for (Pedido pedido : pedidosAsignados) {
            try {
                entregar(pedido);
            } catch (InterruptedException e) {
                // Se restaura la marca de interrupcion en lugar de tragarsela:
                // asi quien administra el hilo se entera de que fue detenido.
                Thread.currentThread().interrupt();
                ConsolaSegura.imprimir("[Repartidor: " + nombre + "] ruta INTERRUMPIDA en el pedido "
                        + pedido.getIdPedido() + ". Quedan entregas sin completar.");
                return;
            } catch (RuntimeException e) {
                // Un pedido con datos inconsistentes no puede botar la ruta
                // completa ni, menos aun, el resto de la simulacion.
                ConsolaSegura.imprimir("[Repartidor: " + nombre + "] problema con el pedido "
                        + pedido.getIdPedido() + ": " + e.getMessage()
                        + ". Continua con el siguiente.");
            }
        }

        ConsolaSegura.imprimir("[Repartidor: " + nombre + "] termino su ruta: "
                + entregasCompletadas + " de " + pedidosAsignados.size()
                + " entrega(s) completada(s) en " + tiempoRutaMs + " ms.");
    }

    /**
     * Entrega un pedido concreto: lo pone en ruta, simula el traslado con una
     * pausa aleatoria y lo marca como entregado.
     *
     * @param pedido pedido a entregar
     * @throws InterruptedException si el hilo es interrumpido durante la pausa
     */
    private void entregar(Pedido pedido) throws InterruptedException {
        if (pedido.getEstado() == EstadoPedido.CANCELADO) {
            ConsolaSegura.imprimir("[Repartidor: " + nombre + "] omite el pedido #"
                    + pedido.getIdPedido() + ": fue cancelado antes de salir.");
            return;
        }

        if (!pedido.despachar("entrega simulada en paralelo")) {
            ConsolaSegura.imprimir("[Repartidor: " + nombre + "] no puede despachar el pedido #"
                    + pedido.getIdPedido() + " (" + pedido.getEstado().getDescripcion() + ").");
            return;
        }

        ConsolaSegura.imprimir("[Repartidor: " + nombre + "] Entregando "
                + pedido.getClass().getSimpleName() + " #" + pedido.getIdPedido()
                + " (" + pedido.getDistanciaKm() + " km, estimado "
                + pedido.calcularTiempoEntrega() + " min)...");

        // ThreadLocalRandom en vez de un Random compartido: cada hilo usa su
        // propio generador y no hay contencion ni resultados repetidos.
        long demoraMs = ThreadLocalRandom.current().nextLong(PAUSA_MINIMA_MS, PAUSA_MAXIMA_MS + 1);
        Thread.sleep(demoraMs);

        pedido.registrarEntrega(demoraMs);
        entregasCompletadas++;
        tiempoRutaMs += demoraMs;

        ConsolaSegura.imprimir("[Repartidor: " + nombre + "] Pedido #" + pedido.getIdPedido()
                + " entregado. (" + demoraMs + " ms de traslado simulado)");
    }

    // ------------------------------------------------------------------
    // Getters y setters (encapsulamiento)
    // ------------------------------------------------------------------

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getVehiculo() {
        return vehiculo;
    }

    public void setVehiculo(String vehiculo) {
        this.vehiculo = vehiculo;
    }

    /**
     * Vista de solo lectura de la ruta, para que nadie de afuera pueda
     * agregar o quitar pedidos mientras el hilo esta trabajando.
     *
     * @return lista inmodificable con los pedidos asignados
     */
    public List<Pedido> getPedidosAsignados() {
        return Collections.unmodifiableList(pedidosAsignados);
    }

    public int getTotalPedidos() {
        return pedidosAsignados.size();
    }

    public int getEntregasCompletadas() {
        return entregasCompletadas;
    }

    public long getTiempoRutaMs() {
        return tiempoRutaMs;
    }

    @Override
    public String toString() {
        return nombre + " (" + vehiculo + ")";
    }
}
