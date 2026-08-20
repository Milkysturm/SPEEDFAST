package com.speedfast;

import com.speedfast.modelo.Pedido;
import com.speedfast.modelo.PedidoComida;
import com.speedfast.modelo.PedidoEncomienda;
import com.speedfast.modelo.PedidoExpress;
import com.speedfast.reporte.ReporteConsola;

/**
 * Clase de prueba del sistema de reparto SpeedFast.
 *
 * Semana 2: definicion de una clase abstracta y su jerarquia.
 *
 * Main se limita a crear los objetos y a recorrerlos. El calculo vive en cada
 * subclase y el formato de la salida en {@link ReporteConsola}.
 *
 * @author Olga Rivas
 * @version 2.0
 */
public class Main {

    public static void main(String[] args) {

        // Arreglo polimorfico: referencias de tipo Pedido apuntando a subclases
        Pedido[] pedidos = crearPedidos();

        ReporteConsola.imprimirEncabezado(
                "SISTEMA DE REPARTO SPEEDFAST - Semana 2",
                "Clase abstracta Pedido y calculo de tiempo de entrega");

        // Se invoca mostrarResumen() sobre cada pedido. Java resuelve en tiempo
        // de ejecucion la version de calcularTiempoEntrega() que corresponde.
        ReporteConsola.imprimirSeccion("DETALLE DE LOS PEDIDOS");
        for (Pedido pedido : pedidos) {
            pedido.mostrarResumen();
            System.out.println();
        }

        // Comparacion directa de los tiempos estimados
        ReporteConsola.imprimirSeccion("COMPARATIVA DE TIEMPOS ESTIMADOS");
        ReporteConsola.imprimirTablaComparativa(pedidos);
        ReporteConsola.imprimirExtremos(pedidos);

        ReporteConsola.imprimirCierre(pedidos.length);
    }

    /**
     * Crea un pedido de cada tipo. Se incluyen dos compras express para
     * evidenciar el recargo que se aplica sobre los 5 km.
     *
     * @return arreglo con los pedidos de prueba
     */
    private static Pedido[] crearPedidos() {
        PedidoComida comida = new PedidoComida(
                "P-001", "Av. Providencia 1234, Santiago", 3.5,
                "Sushi Kai", true);

        PedidoEncomienda encomienda = new PedidoEncomienda(
                "P-002", "Calle Los Aromos 456, Maipu", 8.0,
                25.5, "Caja reforzada");

        PedidoExpress expressCerca = new PedidoExpress(
                "P-003", "Pasaje El Roble 789, La Florida", 2.0,
                "Farmacia Central", true);

        PedidoExpress expressLejos = new PedidoExpress(
                "P-004", "Av. Vicuna Mackenna 1500, Nunoa", 7.5,
                "Supermercado Lider", false);

        return new Pedido[] { comida, encomienda, expressCerca, expressLejos };
    }
}
