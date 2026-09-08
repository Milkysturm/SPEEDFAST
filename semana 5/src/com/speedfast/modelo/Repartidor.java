package com.speedfast.modelo;

import com.speedfast.reporte.ConsolaSegura;
import com.speedfast.zona.FuenteDePedidos;
import com.speedfast.zona.RegistroDeIncidencias;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Repartidor que retira pedidos de la zona de carga y los entrega.
 *
 * Implementa Runnable, de modo que cada repartidor se ejecuta en su propio
 * hilo. Su metodo run() repite el ciclo de retirar un pedido, simular el
 * viaje con Thread.sleep() y marcarlo como entregado, hasta que la zona ya no
 * tiene mas trabajo.
 *
 * @author Olga Rivas
 * @version 5.0
 */
public class Repartidor implements Runnable {

    /** Duracion minima simulada de una entrega, en milisegundos. */
    private static final int PAUSA_MINIMA_MS = 300;

    /** Duracion maxima simulada de una entrega, en milisegundos. */
    private static final int PAUSA_MAXIMA_MS = 900;

    /** Tope de fallas seguidas antes de abandonar el turno. */
    private static final int MAXIMO_FALLAS_SEGUIDAS = 5;

    /** Nombre del repartidor. */
    private final String nombre;

    /** Zona de carga compartida desde la que retira los pedidos. */
    private final FuenteDePedidos zonaDeCarga;

    /** Registro donde se anotan los retiros, las entregas y las incidencias. */
    private final RegistroDeIncidencias registro;

    /** Entregas que alcanzo a completar. */
    private int entregasRealizadas;

    /**
     * Crea un repartidor asociado a una zona de carga.
     *
     * @param nombre      nombre del repartidor
     * @param zonaDeCarga zona compartida desde donde retira
     * @param registro    registro donde anota lo que va ocurriendo
     */
    public Repartidor(String nombre, FuenteDePedidos zonaDeCarga, RegistroDeIncidencias registro) {
        this.nombre = nombre;
        this.zonaDeCarga = zonaDeCarga;
        this.registro = registro;
    }

    /**
     * Ejecuta el turno completo del repartidor.
     *
     * Retira y entrega pedidos hasta que la zona devuelve null, que significa
     * que ya no queda trabajo. Si la zona lanza una excepcion, la informa y lo
     * intenta de nuevo; tras varias fallas seguidas abandona el turno.
     */
    @Override
    public void run() {
        ConsolaSegura.imprimir("[Repartidor - " + nombre + "] comienza su turno. (hilo "
                + Thread.currentThread().getName() + ")");

        int fallasSeguidas = 0;

        while (fallasSeguidas < MAXIMO_FALLAS_SEGUIDAS) {
            Pedido pedido;

            try {
                pedido = zonaDeCarga.retirarPedido();
            } catch (RuntimeException e) {
                registro.anotarFallaTecnica(nombre, e);
                ConsolaSegura.imprimir("[Repartidor - " + nombre + "] la zona fallo al entregarle un pedido: "
                        + e.getClass().getSimpleName() + ". Reintenta.");
                fallasSeguidas++;
                continue;
            }

            if (pedido == null) {
                break;
            }

            fallasSeguidas = 0;

            try {
                entregar(pedido);
            } catch (InterruptedException e) {
                // Se restaura la marca de interrupcion y se corta el turno.
                Thread.currentThread().interrupt();
                ConsolaSegura.imprimir("[Repartidor - " + nombre + "] turno INTERRUMPIDO con el pedido #"
                        + pedido.getId() + " en la mano.");
                return;
            }
        }

        ConsolaSegura.imprimir("[Repartidor - " + nombre + "] termina su turno con "
                + entregasRealizadas + " entrega(s).");
    }

    /**
     * Entrega un pedido: lo anota en el registro, simula el viaje con una
     * pausa aleatoria y lo deja en estado ENTREGADO.
     *
     * @param pedido pedido a entregar
     * @throws InterruptedException si el hilo es interrumpido durante el viaje
     */
    private void entregar(Pedido pedido) throws InterruptedException {
        boolean retiroExclusivo = registro.anotarRetiro(pedido, nombre);
        pedido.setRepartidorAsignado(nombre);

        ConsolaSegura.imprimir("[Repartidor - " + nombre + "] Retirando pedido #"
                + pedido.getId() + "... Destino: " + pedido.getDireccionEntrega());

        if (!retiroExclusivo) {
            ConsolaSegura.imprimir("[Repartidor - " + nombre + "] ATENCION: el pedido #"
                    + pedido.getId() + " ya habia sido retirado por otro repartidor.");
        }

        if (pedido.getEstado() != EstadoPedido.EN_REPARTO) {
            registro.anotarEstadoInesperado(pedido, nombre, EstadoPedido.EN_REPARTO);
        }

        ConsolaSegura.imprimir("[Repartidor - " + nombre + "] Estado: " + pedido.getEstado());

        // Cada hilo usa su propio generador aleatorio.
        long viajeMs = ThreadLocalRandom.current().nextLong(PAUSA_MINIMA_MS, PAUSA_MAXIMA_MS + 1);
        Thread.sleep(viajeMs);

        pedido.setEstado(EstadoPedido.ENTREGADO);
        entregasRealizadas++;
        registro.anotarEntrega(pedido, nombre);

        ConsolaSegura.imprimir("[Repartidor - " + nombre + "] Entregado pedido #"
                + pedido.getId() + ". Estado: " + pedido.getEstado()
                + " (" + viajeMs + " ms de viaje)");
    }

    /**
     * @return nombre del repartidor
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * @return cantidad de entregas que completo en su turno
     */
    public int getEntregasRealizadas() {
        return entregasRealizadas;
    }

    /**
     * Devuelve el repartidor y su cantidad de entregas en una linea.
     *
     * @return descripcion del repartidor
     */
    @Override
    public String toString() {
        return "Repartidor " + nombre + " (" + entregasRealizadas + " entregas)";
    }
}
