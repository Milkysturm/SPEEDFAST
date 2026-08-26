package com.speedfast.reporte;

import com.speedfast.modelo.Pedido;

/**
 * Se encarga del formato de la salida por consola.
 *
 * Separar la presentacion del modelo permite que las clases de negocio solo
 * calculen y devuelvan datos. Si manana cambia el formato del reporte, solo
 * se modifica esta clase.
 *
 * @author Olga
 * @version 2.0
 */
public class ReporteConsola {

    /** Linea separadora de los reportes. */
    private static final String LINEA = "============================================================";

    /** Clase de utilidad: no se instancia. */
    private ReporteConsola() {
    }

    /**
     * Imprime el encabezado del reporte.
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
     * Imprime el titulo de una seccion del reporte.
     *
     * @param titulo nombre de la seccion
     */
    public static void imprimirSeccion(String titulo) {
        System.out.println();
        System.out.println(titulo);
        System.out.println();
    }

    /**
     * Imprime una tabla que compara el tiempo estimado de todos los pedidos.
     *
     * @param pedidos pedidos a comparar
     */
    public static void imprimirTablaComparativa(Pedido[] pedidos) {
        if (pedidos == null || pedidos.length == 0) {
            System.out.println("No hay pedidos que comparar.");
            return;
        }

        System.out.printf("%-8s %-16s %10s %10s%n",
                "PEDIDO", "TIPO", "DISTANCIA", "TIEMPO");
        System.out.println("------------------------------------------------");

        for (Pedido pedido : pedidos) {
            System.out.printf("%-8s %-16s %7.1f km %6d min%n",
                    pedido.getIdPedido(),
                    pedido.getTipoEntrega(),
                    pedido.getDistanciaKm(),
                    pedido.calcularTiempoEntrega());
        }
        System.out.println();
    }

    /**
     * Imprime cual pedido llega mas rapido y cual mas lento.
     *
     * @param pedidos pedidos a evaluar
     */
    public static void imprimirExtremos(Pedido[] pedidos) {
        if (pedidos == null || pedidos.length == 0) {
            System.out.println("No hay pedidos que evaluar.");
            return;
        }

        Pedido masRapido = pedidos[0];
        Pedido masLento = pedidos[0];
        int tiempoMinimo = masRapido.calcularTiempoEntrega();
        int tiempoMaximo = masLento.calcularTiempoEntrega();

        for (Pedido pedido : pedidos) {
            int tiempo = pedido.calcularTiempoEntrega();
            if (tiempo < tiempoMinimo) {
                tiempoMinimo = tiempo;
                masRapido = pedido;
            }
            if (tiempo > tiempoMaximo) {
                tiempoMaximo = tiempo;
                masLento = pedido;
            }
        }

        System.out.println("Entrega mas rapida : " + masRapido.getIdPedido()
                + " (" + masRapido.getTipoEntrega() + ") con " + tiempoMinimo + " min.");
        System.out.println("Entrega mas lenta  : " + masLento.getIdPedido()
                + " (" + masLento.getTipoEntrega() + ") con " + tiempoMaximo + " min.");
        System.out.println("Diferencia entre ambas: " + (tiempoMaximo - tiempoMinimo) + " min.");
        System.out.println();
    }

    /**
     * Imprime el cierre del reporte.
     *
     * @param totalPedidos cantidad de pedidos procesados
     */
    public static void imprimirCierre(int totalPedidos) {
        System.out.println(LINEA);
        System.out.println("  Fin del reporte - " + totalPedidos + " pedidos procesados");
        System.out.println(LINEA);
    }
}
