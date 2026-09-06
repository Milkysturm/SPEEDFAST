package com.speedfast.servicio;

import com.speedfast.modelo.Repartidor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Marcador compartido de la simulacion.
 *
 * Es el unico objeto que varios hilos escriben a la vez, asi que esta armado
 * con estructuras pensadas para eso:
 *
 * <ul>
 *   <li>{@link AtomicInteger} para los contadores: {@code i++} no es una
 *       operacion atomica (lee, suma y escribe), de modo que con un int comun
 *       dos rutas que terminan al mismo tiempo pueden pisarse el valor y
 *       perderse una entrega en el total. {@code incrementAndGet()} no.</li>
 *   <li>{@link CopyOnWriteArrayList} para la bitacora: permite que un hilo
 *       agregue mientras otro recorre, sin ConcurrentModificationException y
 *       sin bloquear a nadie. Es ideal aca porque se escribe muy poco (una
 *       vez por ruta) y se lee al final.</li>
 * </ul>
 *
 * @author Olga Rivas
 * @version 4.0
 */
public class MonitorDeEntregas {

    /** Total de entregas completadas por todas las rutas. */
    private final AtomicInteger entregasCompletadas = new AtomicInteger();

    /** Cantidad de rutas que ya terminaron. */
    private final AtomicInteger rutasTerminadas = new AtomicInteger();

    /** Resumen de cada ruta, en el orden en que fueron terminando. */
    private final List<String> bitacora = new CopyOnWriteArrayList<>();

    /**
     * Registra el cierre de una ruta. Lo llama cada hilo al terminar, por lo
     * que puede ejecutarse en paralelo desde varios repartidores.
     *
     * El metodo es synchronized aunque los campos ya sean atomicos: cada
     * operacion por separado es segura, pero entre pedir el numero de orden y
     * escribir la linea puede colarse otro hilo y dejar la bitacora numerada
     * en desorden. Lo que hay que volver indivisible es el conjunto, no cada
     * paso. El costo es nulo: se ejecuta una sola vez por ruta.
     *
     * @param repartidor repartidor que termino su recorrido
     */
    public synchronized void registrarRuta(Repartidor repartidor) {
        entregasCompletadas.addAndGet(repartidor.getEntregasCompletadas());
        int posicion = rutasTerminadas.incrementAndGet();

        bitacora.add(posicion + ". " + repartidor.getNombre()
                + " cerro su ruta con " + repartidor.getEntregasCompletadas()
                + " de " + repartidor.getTotalPedidos() + " entrega(s), "
                + repartidor.getTiempoRutaMs() + " ms en ruta.");
    }

    public int getEntregasCompletadas() {
        return entregasCompletadas.get();
    }

    public int getRutasTerminadas() {
        return rutasTerminadas.get();
    }

    /**
     * @return copia de la bitacora de rutas cerradas
     */
    public List<String> getBitacora() {
        return new ArrayList<>(bitacora);
    }
}
