package com.speedfast.simulacion;

import com.speedfast.modelo.EstadoPedido;
import com.speedfast.modelo.Pedido;
import com.speedfast.modelo.Repartidor;
import com.speedfast.reporte.ConsolaSegura;
import com.speedfast.zona.FuenteDePedidos;
import com.speedfast.zona.RegistroDeIncidencias;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Ejecuta un escenario completo de despacho: carga los pedidos en la zona,
 * lanza a los repartidores en paralelo con un ExecutorService y espera a que
 * todos terminen su turno.
 *
 * Al final deja disponibles los resultados de la corrida: pedidos entregados,
 * duracion y las incidencias detectadas.
 *
 * @author Olga Rivas
 * @version 5.0
 */
public class SimulacionDeDespacho {

    /** Tope de espera para no quedar colgado si algo sale muy mal. */
    private static final int MINUTOS_MAXIMOS_ESPERA = 2;

    /** Segundos que se espera a los hilos despues de pedirles que se detengan. */
    private static final int SEGUNDOS_ESPERA_AL_DETENER = 10;

    /** Nombre del escenario, para los reportes. */
    private final String titulo;

    /** Recurso compartido que se esta poniendo a prueba. */
    private final FuenteDePedidos zonaDeCarga;

    /** Testigo de lo que ocurre durante la corrida. */
    private final RegistroDeIncidencias registro;

    /** Pedidos que entraron a la zona. */
    private final List<Pedido> pedidos;

    /** Repartidores que trabajan en este escenario. */
    private final List<Repartidor> repartidores;

    /** Duracion real de la corrida en milisegundos. */
    private long duracionMs;

    /** True si todas las rutas alcanzaron a terminar. */
    private boolean completa;

    /**
     * @param titulo      nombre del escenario
     * @param zonaDeCarga zona de carga a utilizar
     */
    public SimulacionDeDespacho(String titulo, FuenteDePedidos zonaDeCarga) {
        this.titulo = titulo;
        this.zonaDeCarga = zonaDeCarga;
        this.registro = new RegistroDeIncidencias();
        this.pedidos = new ArrayList<>();
        this.repartidores = new ArrayList<>();
    }

    /**
     * Carga pedidos en la zona antes de que empiece el trabajo.
     *
     * @param nuevos pedidos que llegan a la zona de carga
     */
    public void cargarPedidos(List<Pedido> nuevos) {
        for (Pedido pedido : nuevos) {
            zonaDeCarga.agregarPedido(pedido);
            pedidos.add(pedido);
            ConsolaSegura.imprimirSimple("Pedido #" + pedido.getId()
                    + " agregado. Destino: " + pedido.getDireccionEntrega());
        }
    }

    /**
     * Incorpora los repartidores del turno.
     *
     * @param nombres nombres de los repartidores
     */
    public void contratarRepartidores(String... nombres) {
        for (String nombre : nombres) {
            repartidores.add(new Repartidor(nombre, zonaDeCarga, registro));
        }
    }

    /**
     * Ejecuta el escenario sin pedidos tardios.
     *
     * @return true si todos los repartidores terminaron su turno
     */
    public boolean ejecutar() {
        return ejecutar(Collections.emptyList(), 0);
    }

    /**
     * Ejecuta el escenario, opcionalmente con pedidos que llegan cuando los
     * repartidores ya estan trabajando.
     *
     * @param pedidosTardios pedidos que llegan una vez iniciado el turno
     * @param retrasoMs      tiempo maximo que se espera antes de ingresarlos
     * @return true si todos los repartidores terminaron su turno
     */
    public boolean ejecutar(List<Pedido> pedidosTardios, long retrasoMs) {
        if (repartidores.isEmpty()) {
            ConsolaSegura.imprimirSimple("No hay repartidores en el turno: no hay nada que simular.");
            return false;
        }

        ExecutorService pool = Executors.newFixedThreadPool(repartidores.size());
        List<Future<?>> turnos = new ArrayList<>();
        long inicio = System.currentTimeMillis();

        // El finally cierra el pool pase lo que pase, para que no queden hilos
        // vivos si ocurre una excepcion inesperada.
        try {
            for (Repartidor repartidor : repartidores) {
                turnos.add(pool.submit(repartidor));
            }

            if (!pedidosTardios.isEmpty()) {
                esperarQueLaZonaSeVacie(retrasoMs);
                ConsolaSegura.imprimir(">>> Llega un camion con " + pedidosTardios.size()
                        + " pedido(s) mas a la zona de carga.");
                for (Pedido pedido : pedidosTardios) {
                    zonaDeCarga.agregarPedido(pedido);
                    pedidos.add(pedido);
                    ConsolaSegura.imprimir(">>> Pedido #" + pedido.getId()
                            + " agregado. Destino: " + pedido.getDireccionEntrega());
                }
            }

            // Avisa que no llegaran mas pedidos, para que los repartidores que
            // estan esperando puedan terminar.
            zonaDeCarga.cerrarRecepcion();

            pool.shutdown();
            if (!pool.awaitTermination(MINUTOS_MAXIMOS_ESPERA, TimeUnit.MINUTES)) {
                ConsolaSegura.imprimir("Se agoto el tiempo maximo del turno: se detiene lo que siga abierto.");
                completa = false;
            } else {
                completa = true;
            }

            revisarFallas(turnos);

        } catch (RejectedExecutionException e) {
            ConsolaSegura.imprimir("No se pudo iniciar un turno: " + e.getMessage());
            completa = false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ConsolaSegura.imprimir("La simulacion fue interrumpida antes de terminar.");
            completa = false;
        } catch (RuntimeException e) {
            // Cualquier otro problema, por ejemplo un pedido invalido entre los
            // tardios: se informa y el escenario se da por fallido.
            ConsolaSegura.imprimir("El turno se corto por un problema inesperado: "
                    + e.getClass().getSimpleName() + " - " + e.getMessage());
            completa = false;
        } finally {
            // Cierra la recepcion y el pool aunque el escenario haya fallado.
            zonaDeCarga.cerrarRecepcion();
            if (!pool.isTerminated()) {
                cerrarPool(pool);
            }
        }

        duracionMs = System.currentTimeMillis() - inicio;

        // Con los hilos ya detenidos se revisa que paso con cada pedido.
        registro.revisarPedidosPerdidos(pedidos);
        return completa;
    }

    /**
     * Cierra el pool y espera a que los hilos terminen.
     *
     * Primero pide el cierre normal, para que quien tenga un pedido en la mano
     * alcance a entregarlo, y solo si no responde interrumpe los turnos.
     *
     * @param pool pool a cerrar
     */
    private void cerrarPool(ExecutorService pool) {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(SEGUNDOS_ESPERA_AL_DETENER, TimeUnit.SECONDS)) {
                pool.shutdownNow();
                if (!pool.awaitTermination(SEGUNDOS_ESPERA_AL_DETENER, TimeUnit.SECONDS)) {
                    ConsolaSegura.imprimir("Hay turnos que no respondieron a la interrupcion.");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            pool.shutdownNow();
        }
    }

    /**
     * Revisa el resultado de cada turno para mostrar las excepciones que se
     * hayan producido dentro de los hilos, que quedan guardadas en su Future.
     *
     * @param turnos futuros devueltos al enviar cada repartidor al pool
     */
    private void revisarFallas(List<Future<?>> turnos) {
        for (Future<?> turno : turnos) {
            try {
                // Con tiempo maximo, para no quedar bloqueado esperando un
                // turno que no responde.
                turno.get(SEGUNDOS_ESPERA_AL_DETENER, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                ConsolaSegura.imprimir("Un turno termino con error: " + e.getCause());
            } catch (TimeoutException e) {
                ConsolaSegura.imprimir("Un turno no respondio a tiempo y se da por perdido.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /**
     * Espera a que la zona quede vacia y haya al menos un repartidor
     * esperando, antes de hacer llegar los pedidos tardios.
     *
     * @param maximoMs tiempo maximo que se espera antes de continuar igual
     * @throws InterruptedException si la espera es interrumpida
     */
    private void esperarQueLaZonaSeVacie(long maximoMs) throws InterruptedException {
        long limite = System.currentTimeMillis() + maximoMs;
        while (System.currentTimeMillis() < limite) {
            if (zonaDeCarga.getPedidosPendientes() == 0 && hayAlguienEsperando()) {
                return;
            }
            Thread.sleep(20);
        }
    }

    /**
     * Indica si algun repartidor esta esperando trabajo. La version insegura
     * de la zona no tiene espera, asi que en ese caso devuelve true.
     *
     * @return true si hay al menos un repartidor esperando
     */
    private boolean hayAlguienEsperando() {
        if (zonaDeCarga instanceof com.speedfast.zona.ZonaDeCarga) {
            return ((com.speedfast.zona.ZonaDeCarga) zonaDeCarga).getRepartidoresEnEspera() > 0;
        }
        return true;
    }

    // ------------------------------------------------------------------
    // Resultados
    // ------------------------------------------------------------------

    /**
     * @return cantidad de pedidos que quedaron en estado ENTREGADO
     */
    public int getPedidosEntregados() {
        int total = 0;
        for (Pedido pedido : pedidos) {
            if (pedido.getEstado() == EstadoPedido.ENTREGADO) {
                total++;
            }
        }
        return total;
    }

    /**
     * Comprueba que el escenario haya terminado bien: todos los pedidos
     * entregados una sola vez y sin incidencias.
     *
     * @return true si no hubo ningun problema
     */
    public boolean todoEnOrden() {
        return completa
                && registro.sinIncidencias()
                && getPedidosEntregados() == pedidos.size()
                && registro.getEntregas() == pedidos.size();
    }

    /**
     * @return nombre del escenario
     */
    public String getTitulo() {
        return titulo;
    }

    /**
     * @return registro con las incidencias y los totales de la corrida
     */
    public RegistroDeIncidencias getRegistro() {
        return registro;
    }

    /**
     * @return copia de los pedidos que participaron del escenario
     */
    public List<Pedido> getPedidos() {
        return new ArrayList<>(pedidos);
    }

    /**
     * @return copia de los repartidores del turno
     */
    public List<Repartidor> getRepartidores() {
        return new ArrayList<>(repartidores);
    }

    /**
     * @return duracion de la corrida en milisegundos
     */
    public long getDuracionMs() {
        return duracionMs;
    }

    /**
     * @return cantidad total de pedidos del escenario
     */
    public int getTotalPedidos() {
        return pedidos.size();
    }
}
