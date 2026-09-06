package com.speedfast.reporte;

import java.util.Locale;

/**
 * Unica puerta de salida a consola mientras hay hilos trabajando.
 *
 * System.out ya es thread-safe por linea, pero eso no basta: un mensaje
 * armado con varias concatenaciones puede quedar partido entre dos hilos y
 * la salida se vuelve ilegible. Al centralizar la impresion en un metodo
 * sincronizado, cada mensaje sale completo y en un solo bloque.
 *
 * Ademas antepone el tiempo transcurrido desde la primera impresion, que en
 * la practica es el instante en que arrancan las rutas, porque esta clase se
 * usa unicamente dentro de la simulacion. Esa marca es lo que permite ver en
 * la consola que las rutas avanzan al mismo tiempo y no una despues de otra.
 *
 * @author Olga Rivas
 * @version 4.0
 */
public final class ConsolaSegura {

    /** Instante en que se cargo la clase, usado como tiempo cero. */
    private static final long INICIO_NS = System.nanoTime();

    /** Clase de utilidad: no se instancia. */
    private ConsolaSegura() {
    }

    /**
     * Imprime un mensaje con marca de tiempo, sin que se mezcle con los
     * mensajes de otros hilos.
     *
     * @param mensaje texto a mostrar
     */
    public static synchronized void imprimir(String mensaje) {
        double segundos = (System.nanoTime() - INICIO_NS) / 1_000_000_000.0;
        System.out.println(String.format(Locale.US, "[%6.2f s] %s", segundos, mensaje));
    }

    /**
     * Imprime una linea en blanco respetando el mismo turno que el resto de
     * los mensajes.
     */
    public static synchronized void saltoDeLinea() {
        System.out.println();
    }
}
