package com.speedfast.reporte;

import java.util.Locale;

/**
 * Imprime por consola desde varios hilos sin que los mensajes se mezclen.
 *
 * Sus metodos son synchronized, de modo que cada mensaje sale completo. Ademas
 * antepone el tiempo transcurrido, lo que permite ver en la salida que los
 * repartidores trabajan en paralelo.
 *
 * @author Olga Rivas
 * @version 5.0
 */
public final class ConsolaSegura {

    /** Instante de referencia para las marcas de tiempo. */
    private static long inicioNs = System.nanoTime();

    /** Constructor privado: es una clase de utilidad y no se instancia. */
    private ConsolaSegura() {
    }

    /**
     * Reinicia el cronometro para que cada escenario mida desde cero.
     */
    public static synchronized void reiniciarCronometro() {
        inicioNs = System.nanoTime();
    }

    /**
     * Imprime un mensaje con la marca de tiempo por delante.
     *
     * @param mensaje texto a mostrar
     */
    public static synchronized void imprimir(String mensaje) {
        double segundos = (System.nanoTime() - inicioNs) / 1_000_000_000.0;
        System.out.println(String.format(Locale.US, "   [%5.2f s] %s", segundos, mensaje));
    }

    /**
     * Imprime un mensaje sin marca de tiempo, para las lineas de contexto.
     *
     * @param mensaje texto a mostrar
     */
    public static synchronized void imprimirSimple(String mensaje) {
        System.out.println("   " + mensaje);
    }

    /**
     * Imprime una linea en blanco.
     */
    public static synchronized void saltoDeLinea() {
        System.out.println();
    }
}
