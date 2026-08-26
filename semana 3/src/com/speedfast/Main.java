package com.speedfast;

import com.speedfast.contrato.Rastreable;
import com.speedfast.modelo.Pedido;
import com.speedfast.modelo.PedidoComida;
import com.speedfast.modelo.PedidoEncomienda;
import com.speedfast.modelo.PedidoExpress;
import com.speedfast.reporte.ReporteConsola;
import com.speedfast.servicio.ControladorDeEnvios;

/**
 * Simulacion del sistema de reparto SpeedFast.
 *
 * Semana 3: version integral con polimorfismo, abstraccion e interfaces.
 *
 * Main no contiene reglas de negocio: solo arma el escenario y va pidiendo
 * operaciones. Quien decide como se calcula un tiempo o a quien se le asigna
 * un pedido son las subclases de {@link Pedido}; quien coordina la jornada es
 * {@link ControladorDeEnvios}; quien da formato a la salida es
 * {@link ReporteConsola}.
 *
 * @author Olga Rivas
 * @version 3.0
 */
public class Main {

    public static void main(String[] args) {

        ReporteConsola.imprimirEncabezado(
                "SISTEMA DE REPARTO SPEEDFAST - Semana 3",
                "Polimorfismo, clase abstracta e interfaces en operacion");

        ControladorDeEnvios controlador = new ControladorDeEnvios();

        // 1. RESERVA ----------------------------------------------------
        // Primera interaccion del cliente: el pedido entra al sistema y queda
        // reservado, todavia sin repartidor.
        ReporteConsola.imprimirSeccion("1. RESERVA DE PEDIDOS");
        reservarPedidosDelDia(controlador);

        // 2. ASIGNACION AUTOMATICA -------------------------------------
        // Una sola linea recorre pedidos de tres tipos distintos. Cada uno
        // ejecuta SU version de asignarRepartidor(): eso es sobrescritura.
        ReporteConsola.imprimirSeccion("2. ASIGNACION AUTOMATICA DE REPARTIDORES");
        controlador.asignarRepartidoresAutomaticamente();

        // 3. ASIGNACION MANUAL -----------------------------------------
        // Mismo nombre de metodo, distinta lista de parametros: sobrecarga.
        ReporteConsola.imprimirSeccion("3. ASIGNACION MANUAL DEL OPERADOR");

        Pedido expressEnEspera = controlador.buscarPedido("P-004");
        System.out.println("   P-004 quedo sin repartidor automatico, se asigna a mano:");
        System.out.println("   -> " + expressEnEspera.asignarRepartidor("Rodrigo Lagos", "moto"));

        Pedido comida = controlador.buscarPedido("P-001");
        System.out.println("   El restaurante pide un repartidor conocido para P-001:");
        System.out.println("   -> " + comida.asignarRepartidor("Fernanda Vidal"));

        // 4. TIEMPOS ESTIMADOS -----------------------------------------
        ReporteConsola.imprimirSeccion("4. TIEMPO ESTIMADO DE ENTREGA POR TIPO");
        ReporteConsola.imprimirTablaComparativa(controlador.getPedidos());
        ReporteConsola.imprimirExtremos(controlador.getPedidos());

        // 5. DETALLE DE CADA PEDIDO ------------------------------------
        // mostrarResumen() esta implementado una sola vez en la clase
        // abstracta, pero cada pedido lo llena con sus propios valores.
        ReporteConsola.imprimirSeccion("5. DETALLE DE LOS PEDIDOS");
        for (Pedido pedido : controlador.getPedidos()) {
            pedido.mostrarResumen();
            System.out.println();
        }

        // 6. CANCELACION -----------------------------------------------
        ReporteConsola.imprimirSeccion("6. CANCELACION DE UN ENVIO");
        controlador.cancelar("P-002", "el cliente ya no se encuentra en el domicilio");

        // 7. DESPACHO ---------------------------------------------------
        ReporteConsola.imprimirSeccion("7. DESPACHO DEL LOTE");
        controlador.despachar("verificar la direccion antes de tocar el timbre");

        // 8. CANCELACION RECHAZADA -------------------------------------
        // Se intenta cancelar algo que ya va en ruta: el sistema lo impide.
        ReporteConsola.imprimirSeccion("8. INTENTO DE CANCELAR UN PEDIDO EN RUTA");
        controlador.cancelar("P-003", "el cliente se arrepintio");

        // 9. HISTORIAL --------------------------------------------------
        // Un mismo arreglo guarda un Pedido y el Controlador: no comparten
        // herencia, solo la interfaz Rastreable. Aun asi se recorren igual.
        ReporteConsola.imprimirSeccion("9. HISTORIAL DE ENTREGAS");
        Rastreable[] rastreables = {
                controlador,
                controlador.buscarPedido("P-004")
        };
        for (Rastreable rastreable : rastreables) {
            rastreable.verHistorial();
            System.out.println();
        }

        ReporteConsola.imprimirCierre(controlador.getTotalPedidos());
    }

    /**
     * Reserva los pedidos de la jornada. Se incluyen dos compras express para
     * mostrar el recargo sobre los 5 km y el caso sin repartidor disponible.
     *
     * @param controlador controlador donde se reservan los pedidos
     */
    private static void reservarPedidosDelDia(ControladorDeEnvios controlador) {
        controlador.reservar(new PedidoComida(
                "P-001", "Av. Providencia 1234, Santiago", 3.5,
                "Sushi Kai", true));

        controlador.reservar(new PedidoEncomienda(
                "P-002", "Calle Los Aromos 456, Maipu", 8.0,
                25.5, "Caja reforzada"));

        controlador.reservar(new PedidoExpress(
                "P-003", "Pasaje El Roble 789, La Florida", 2.0,
                "Farmacia Central", true));

        controlador.reservar(new PedidoExpress(
                "P-004", "Av. Vicuna Mackenna 1500, Nunoa", 7.5,
                "Supermercado Lider", false));
    }
}
