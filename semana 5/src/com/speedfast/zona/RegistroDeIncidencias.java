package com.speedfast.zona;

import com.speedfast.modelo.EstadoPedido;
import com.speedfast.modelo.Pedido;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lleva la cuenta de lo que ocurre durante la simulacion y detecta los
 * problemas de concurrencia.
 *
 * Anota quien retiro y quien entrego cada pedido, y registra una incidencia
 * cuando un pedido sale dos veces de la zona, cuando llega en un estado que no
 * corresponde, cuando la zona lanza una excepcion o cuando un pedido
 * desaparece sin que nadie lo retire.
 *
 * Como lo escriben varios hilos a la vez, usa estructuras preparadas para el
 * acceso concurrente: ConcurrentHashMap, AtomicInteger y CopyOnWriteArrayList.
 *
 * @author Olga Rivas
 * @version 5.0
 */
public class RegistroDeIncidencias {

    /** Primer repartidor que retiro cada pedido, por id de pedido. */
    private final Map<Integer, String> primerRetiro = new ConcurrentHashMap<>();

    /** Quien entrego cada pedido, por id de pedido. */
    private final Map<Integer, String> entregadoPor = new ConcurrentHashMap<>();

    /** Descripcion de cada problema detectado. */
    private final List<String> incidencias = new CopyOnWriteArrayList<>();

    /** Entregas completadas en total. */
    private final AtomicInteger entregas = new AtomicInteger();

    /** Veces que un pedido fue retirado por mas de un repartidor. */
    private final AtomicInteger retirosDuplicados = new AtomicInteger();

    /** Veces que un pedido fue entregado mas de una vez. */
    private final AtomicInteger entregasDuplicadas = new AtomicInteger();

    /** Excepciones que lanzo la zona de carga durante un retiro. */
    private final AtomicInteger fallasTecnicas = new AtomicInteger();

    /** Pedidos que llegaron al repartidor en un estado que no correspondia. */
    private final AtomicInteger estadosInesperados = new AtomicInteger();

    /**
     * Anota que un repartidor retiro un pedido.
     *
     * @param pedido     pedido retirado
     * @param repartidor nombre de quien lo retiro
     * @return true si es el unico que lo retiro; false si otro se le adelanto
     */
    public boolean anotarRetiro(Pedido pedido, String repartidor) {
        // putIfAbsent es atomico: si dos hilos anotan a la vez, solo uno queda
        // como primero y el otro recibe el valor ya guardado.
        String anterior = primerRetiro.putIfAbsent(pedido.getId(), repartidor);
        if (anterior != null) {
            retirosDuplicados.incrementAndGet();
            incidencias.add("RETIRO DUPLICADO: el pedido #" + pedido.getId()
                    + " ya lo habia retirado " + anterior + " y tambien lo tomo " + repartidor);
            return false;
        }
        return true;
    }

    /**
     * Anota una entrega completada y detecta si el pedido ya habia sido
     * entregado antes.
     *
     * @param pedido     pedido entregado
     * @param repartidor nombre de quien lo entrego
     */
    public void anotarEntrega(Pedido pedido, String repartidor) {
        entregas.incrementAndGet();

        String anterior = entregadoPor.putIfAbsent(pedido.getId(), repartidor);
        if (anterior != null) {
            entregasDuplicadas.incrementAndGet();
            incidencias.add("ENTREGA DUPLICADA: el pedido #" + pedido.getId()
                    + " ya lo habia entregado " + anterior + " y tambien lo entrego " + repartidor);
        }
    }

    /**
     * Anota que un pedido llego al repartidor en un estado que no correspondia.
     *
     * @param pedido     pedido con el estado incorrecto
     * @param repartidor nombre de quien lo recibio
     * @param esperado   estado que deberia haber tenido
     */
    public void anotarEstadoInesperado(Pedido pedido, String repartidor, EstadoPedido esperado) {
        estadosInesperados.incrementAndGet();
        incidencias.add("ESTADO INESPERADO: " + repartidor + " recibio el pedido #"
                + pedido.getId() + " en estado " + pedido.getEstado()
                + " cuando deberia estar " + esperado + ".");
    }

    /**
     * Anota que la zona de carga lanzo una excepcion al entregar un pedido.
     *
     * @param repartidor nombre de quien intentaba retirar
     * @param falla      excepcion recibida
     */
    public void anotarFallaTecnica(String repartidor, RuntimeException falla) {
        fallasTecnicas.incrementAndGet();
        incidencias.add("FALLA EN LA ZONA: " + repartidor + " recibio "
                + falla.getClass().getSimpleName()
                + (falla.getMessage() == null ? "" : " (" + falla.getMessage() + ")"));
    }

    /**
     * Revisa la lista de pedidos y anota los que nunca fueron retirados.
     *
     * @param pedidos pedidos que se habian cargado en la zona
     */
    public void revisarPedidosPerdidos(List<Pedido> pedidos) {
        for (Pedido pedido : pedidos) {
            if (!primerRetiro.containsKey(pedido.getId())) {
                incidencias.add("PEDIDO PERDIDO: el pedido #" + pedido.getId()
                        + " desaparecio de la zona sin que nadie lo retirara.");
            }
        }
    }

    /**
     * @param idPedido identificador del pedido
     * @return nombre de quien lo entrego, o null si nadie lo hizo
     */
    public String getQuienEntrego(int idPedido) {
        return entregadoPor.get(idPedido);
    }

    /**
     * @return cantidad de entregas completadas
     */
    public int getEntregas() {
        return entregas.get();
    }

    /**
     * @return cantidad de pedidos retirados por mas de un repartidor
     */
    public int getRetirosDuplicados() {
        return retirosDuplicados.get();
    }

    /**
     * @return cantidad de pedidos entregados mas de una vez
     */
    public int getEntregasDuplicadas() {
        return entregasDuplicadas.get();
    }

    /**
     * @return cantidad de excepciones lanzadas por la zona de carga
     */
    public int getFallasTecnicas() {
        return fallasTecnicas.get();
    }

    /**
     * @return cantidad de pedidos recibidos en un estado incorrecto
     */
    public int getEstadosInesperados() {
        return estadosInesperados.get();
    }

    /**
     * @return cantidad de pedidos distintos que fueron retirados
     */
    public int getPedidosRetirados() {
        return primerRetiro.size();
    }

    /**
     * @return copia de las incidencias detectadas
     */
    public List<String> getIncidencias() {
        return new ArrayList<>(incidencias);
    }

    /**
     * @return cantidad total de problemas detectados
     */
    public int getTotalIncidencias() {
        return incidencias.size();
    }

    /**
     * @return true si la corrida termino sin ningun problema de concurrencia
     */
    public boolean sinIncidencias() {
        return incidencias.isEmpty();
    }
}
