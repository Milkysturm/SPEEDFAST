package com.speedfast.servicio;

import com.speedfast.modelo.Repartidor;
import com.speedfast.reporte.ConsolaSegura;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Pone a correr todas las rutas al mismo tiempo.
 *
 * Usa un {@code ExecutorService} en lugar de crear los Thread a mano porque
 * el pool se encarga de administrar los hilos, permite fijar cuantos corren a
 * la vez y entrega un {@link Future} por tarea, que es donde aparecen los
 * errores que ocurrieron dentro de un hilo. Con un Thread suelto esa
 * excepcion se pierde sin que nadie se entere.
 *
 * La simulacion termina cuando todas las rutas terminaron: eso lo garantiza
 * {@code awaitTermination}, que ademas actua como punto de sincronizacion, de
 * modo que despues de esa linea es seguro leer el estado final de los pedidos
 * escrito por los otros hilos.
 *
 * @author Olga Rivas
 * @version 4.0
 */
public class SimuladorDeEntregas {

    /** Tope de espera para que no quede colgado si algo sale muy mal. */
    private static final int MINUTOS_MAXIMOS_ESPERA = 2;

    /** Segundos que se espera a los hilos despues de pedirles que se detengan. */
    private static final int SEGUNDOS_ESPERA_AL_DETENER = 10;

    /** Rutas que se van a ejecutar, una por repartidor. */
    private final List<Repartidor> repartidores;

    /** Marcador compartido que los hilos actualizan al terminar. */
    private final MonitorDeEntregas monitor;

    /**
     * @param repartidores repartidores con sus rutas ya armadas
     */
    public SimuladorDeEntregas(List<Repartidor> repartidores) {
        this.repartidores = new ArrayList<>(repartidores);
        this.monitor = new MonitorDeEntregas();
    }

    /**
     * Ejecuta todas las rutas en paralelo y espera a que terminen.
     *
     * @return true si todas las rutas alcanzaron a completarse
     */
    public boolean ejecutar() {
        if (repartidores.isEmpty()) {
            ConsolaSegura.imprimir("No hay repartidores en ruta: la simulacion no tiene nada que hacer.");
            return false;
        }

        // Un hilo por repartidor: es lo que pide el caso, cada uno reparte por
        // su cuenta. Si manana fueran cien, aca se limitaria el tamano del pool.
        ExecutorService pool = Executors.newFixedThreadPool(repartidores.size());
        List<Future<?>> tareas = new ArrayList<>();

        try {
            for (Repartidor repartidor : repartidores) {
                tareas.add(pool.submit(() -> {
                    repartidor.run();
                    // El marcador se actualiza desde el hilo del repartidor:
                    // por eso MonitorDeEntregas tiene que ser thread-safe.
                    monitor.registrarRuta(repartidor);
                }));
            }

            // shutdown() no interrumpe nada: solo avisa que no llegaran mas
            // tareas. Las que ya estan enviadas se ejecutan hasta el final.
            pool.shutdown();

            if (!pool.awaitTermination(MINUTOS_MAXIMOS_ESPERA, TimeUnit.MINUTES)) {
                ConsolaSegura.imprimir("Se agoto el tiempo maximo de simulacion: "
                        + "se detienen las rutas que siguen abiertas.");
                detenerYEsperar(pool);
                return false;
            }

            revisarFallas(tareas);
            return true;

        } catch (RejectedExecutionException e) {
            // Solo puede ocurrir si el pool ya no acepta tareas. Se informa en
            // vez de dejar caer la excepcion sobre el usuario.
            ConsolaSegura.imprimir("No se pudo poner en marcha una ruta: " + e.getMessage());
            detenerYEsperar(pool);
            return false;

        } catch (InterruptedException e) {
            // Si interrumpen al hilo principal mientras espera, se corta la
            // simulacion de forma ordenada y se conserva la marca de interrupcion.
            Thread.currentThread().interrupt();
            ConsolaSegura.imprimir("La simulacion fue interrumpida antes de terminar.");
            pool.shutdownNow();
            return false;
        }
    }

    /**
     * Detiene el pool y espera a que los hilos realmente terminen.
     *
     * {@code shutdownNow()} solo interrumpe: vuelve de inmediato, con las
     * rutas todavia cerrandose. Si se leyeran los resultados en ese momento,
     * el informe podria mostrar datos a medio escribir por otro hilo. Por eso
     * hay una segunda espera, esta vez corta.
     *
     * @param pool pool a detener
     */
    private void detenerYEsperar(ExecutorService pool) {
        pool.shutdownNow();
        try {
            if (!pool.awaitTermination(SEGUNDOS_ESPERA_AL_DETENER, TimeUnit.SECONDS)) {
                ConsolaSegura.imprimir("Hay rutas que no respondieron a la interrupcion: "
                        + "los resultados pueden estar incompletos.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Revisa el resultado de cada tarea. Una excepcion lanzada dentro de un
     * hilo no aparece en la consola por si sola: queda guardada en su Future y
     * solo sale a la luz al pedir el resultado con get().
     *
     * @param tareas futuros devueltos por el pool al enviar cada ruta
     */
    private void revisarFallas(List<Future<?>> tareas) {
        for (Future<?> tarea : tareas) {
            try {
                tarea.get();
            } catch (ExecutionException e) {
                ConsolaSegura.imprimir("Una ruta termino con error: " + e.getCause());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /**
     * @return marcador con los totales de la simulacion
     */
    public MonitorDeEntregas getMonitor() {
        return monitor;
    }

    /**
     * @return copia de los repartidores que participaron
     */
    public List<Repartidor> getRepartidores() {
        return new ArrayList<>(repartidores);
    }
}
