package com.speedfast.reporte;

import com.speedfast.modelo.Pedido;
import com.speedfast.modelo.Repartidor;
import com.speedfast.simulacion.SimulacionDeDespacho;
import com.speedfast.zona.RegistroDeIncidencias;

import java.util.List;
import java.util.Locale;

/**
 * Da formato a la salida por consola: encabezados, secciones, el resultado de
 * cada escenario, el estado final de los pedidos y la tabla comparativa.
 *
 * @author Olga Rivas
 * @version 5.0
 */
public final class ReporteConsola {

    /** Linea separadora de los reportes. */
    private static final String LINEA = "============================================================";

    /** Constructor privado: es una clase de utilidad y no se instancia. */
    private ReporteConsola() {
    }

    /**
     * Imprime el encabezado del programa.
     *
     * @param titulo    titulo principal
     * @param subtitulo linea descriptiva bajo el titulo
     */
    public static void imprimirEncabezado(String titulo, String subtitulo) {
        System.out.println(LINEA);
        System.out.println("  " + titulo);
        System.out.println("  " + subtitulo);
        System.out.println(LINEA);
    }

    /**
     * Imprime el titulo de una seccion.
     *
     * @param titulo nombre de la seccion
     */
    public static void imprimirSeccion(String titulo) {
        System.out.println();
        System.out.println(titulo);
        System.out.println();
    }

    /**
     * Imprime el resultado completo de un escenario.
     *
     * @param simulacion escenario ya ejecutado
     */
    public static void imprimirResultado(SimulacionDeDespacho simulacion) {
        RegistroDeIncidencias registro = simulacion.getRegistro();

        System.out.println();
        System.out.println("   RESULTADO: " + simulacion.getTitulo());
        System.out.println("   " + "-".repeat(52));
        System.out.printf("   %-34s %d%n", "Pedidos cargados", simulacion.getTotalPedidos());
        System.out.printf("   %-34s %d%n", "Pedidos distintos retirados", registro.getPedidosRetirados());
        System.out.printf("   %-34s %d%n", "Entregas realizadas", registro.getEntregas());
        System.out.printf("   %-34s %d%n", "Pedidos en estado ENTREGADO", simulacion.getPedidosEntregados());
        System.out.printf("   %-34s %d ms%n", "Duracion del turno", simulacion.getDuracionMs());
        System.out.println();
        System.out.printf("   %-34s %d%n", "Retiros duplicados", registro.getRetirosDuplicados());
        System.out.printf("   %-34s %d%n", "Estados inesperados", registro.getEstadosInesperados());
        System.out.printf("   %-34s %d%n", "Fallas tecnicas de la zona", registro.getFallasTecnicas());

        System.out.println();
        System.out.println("   Entregas por repartidor:");
        for (Repartidor repartidor : simulacion.getRepartidores()) {
            System.out.printf("      %-12s %d entrega(s)%n",
                    repartidor.getNombre(), repartidor.getEntregasRealizadas());
        }

        imprimirIncidencias(registro.getIncidencias());
    }

    /**
     * Imprime la lista de problemas detectados durante una corrida.
     *
     * @param incidencias problemas detectados
     */
    private static void imprimirIncidencias(List<String> incidencias) {
        System.out.println();
        if (incidencias.isEmpty()) {
            System.out.println("   Incidencias de concurrencia: NINGUNA.");
            return;
        }
        System.out.println("   Incidencias de concurrencia detectadas (" + incidencias.size() + "):");
        for (String incidencia : incidencias) {
            System.out.println("      - " + incidencia);
        }
    }

    /**
     * Imprime el estado final de cada pedido de un escenario.
     *
     * @param simulacion escenario ya ejecutado
     */
    public static void imprimirEstadoFinal(SimulacionDeDespacho simulacion) {
        System.out.printf("   %-10s %-14s %-28s %s%n", "PEDIDO", "ESTADO", "DESTINO", "REPARTIDOR");
        System.out.println("   " + "-".repeat(72));
        for (Pedido pedido : simulacion.getPedidos()) {
            String responsable = (pedido.getRepartidorAsignado() == null)
                    ? "sin retirar" : pedido.getRepartidorAsignado();
            System.out.printf("   #%-9d %-14s %-28s %s%n",
                    pedido.getId(),
                    pedido.getEstado(),
                    recortar(pedido.getDireccionEntrega(), 27),
                    responsable);
        }
    }

    /**
     * Compara las dos corridas equivalentes: la que no sincroniza y la que si.
     *
     * @param sinProteger escenario con la zona insegura
     * @param protegida   escenario con la zona sincronizada
     */
    public static void imprimirComparacion(SimulacionDeDespacho sinProteger,
                                           SimulacionDeDespacho protegida) {
        System.out.printf("   %-30s %18s %14s%n", "INDICADOR", "SIN SINCRONIZAR", "SINCRONIZADA");
        System.out.println("   " + "-".repeat(64));
        compararFila("Pedidos cargados", sinProteger.getTotalPedidos(), protegida.getTotalPedidos());
        compararFila("Pedidos distintos retirados",
                sinProteger.getRegistro().getPedidosRetirados(),
                protegida.getRegistro().getPedidosRetirados());
        compararFila("Entregados correctamente",
                sinProteger.getPedidosEntregados(), protegida.getPedidosEntregados());
        compararFila("Retiros duplicados",
                sinProteger.getRegistro().getRetirosDuplicados(),
                protegida.getRegistro().getRetirosDuplicados());
        compararFila("Fallas tecnicas",
                sinProteger.getRegistro().getFallasTecnicas(),
                protegida.getRegistro().getFallasTecnicas());
        compararFila("Incidencias totales",
                sinProteger.getRegistro().getTotalIncidencias(),
                protegida.getRegistro().getTotalIncidencias());
    }

    /**
     * Imprime una fila de la tabla comparativa.
     *
     * @param indicador   nombre del indicador
     * @param sinProteger valor de la corrida sin sincronizar
     * @param protegida   valor de la corrida sincronizada
     */
    private static void compararFila(String indicador, int sinProteger, int protegida) {
        System.out.printf(Locale.US, "   %-30s %18d %14d%n", indicador, sinProteger, protegida);
    }

    /**
     * Recorta un texto para que no rompa la tabla.
     *
     * @param texto  texto original
     * @param maximo largo maximo permitido
     * @return el texto recortado si hacia falta
     */
    private static String recortar(String texto, int maximo) {
        return (texto.length() <= maximo) ? texto : texto.substring(0, maximo - 1) + ".";
    }

    /**
     * Imprime el cierre del programa.
     *
     * @param mensaje mensaje final
     */
    public static void imprimirCierre(String mensaje) {
        System.out.println();
        System.out.println(LINEA);
        System.out.println("  " + mensaje);
        System.out.println(LINEA);
    }
}
