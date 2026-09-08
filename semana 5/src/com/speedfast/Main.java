package com.speedfast;

import com.speedfast.modelo.Pedido;
import com.speedfast.reporte.ConsolaSegura;
import com.speedfast.reporte.ReporteConsola;
import com.speedfast.simulacion.SimulacionDeDespacho;
import com.speedfast.zona.ZonaDeCarga;
import com.speedfast.zona.ZonaDeCargaInsegura;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Simulacion del despacho concurrente de SpeedFast.
 *
 * Semana 5: varios repartidores retiran pedidos de una MISMA zona de carga.
 * A diferencia de la semana anterior, donde cada repartidor tenia su propia
 * lista y por lo tanto no competia con nadie, aca el recurso es compartido y
 * hace falta sincronizar de verdad.
 *
 * El programa corre tres escenarios:
 *
 * <ol>
 *   <li>La misma jornada con una zona SIN sincronizar, para ver el problema.</li>
 *   <li>La misma jornada con la {@link ZonaDeCarga} protegida, para ver que
 *       desaparece.</li>
 *   <li>Una jornada con pedidos que llegan tarde, para ver que los
 *       repartidores esperan sin consumir CPU y despiertan al llegar trabajo.</li>
 * </ol>
 *
 * Main solo arma los escenarios y pide los reportes: la logica de los hilos
 * esta en SimulacionDeDespacho y la sincronizacion en ZonaDeCarga.
 *
 * @author Olga Rivas
 * @version 5.0
 */
public class Main {

    /** Repartidores del turno principal. */
    private static final String[] EQUIPO = {"Juan", "Camila", "Luis"};

    /** Milisegundos que tarda en llegar el camion con los pedidos tardios. */
    private static final long RETRASO_CAMION_MS = 1200;

    public static void main(String[] args) {

        ReporteConsola.imprimirEncabezado(
                "SISTEMA DE DESPACHO SPEEDFAST - Semana 5",
                "Sincronizacion del acceso concurrente a la zona de carga");

        // ---------------------------------------------------------------
        // ESCENARIO 1: sin sincronizar, para mostrar el problema
        // ---------------------------------------------------------------
        ReporteConsola.imprimirSeccion("1. ZONA DE CARGA SIN SINCRONIZAR (demostracion del problema)");
        ConsolaSegura.imprimirSimple("[Zona de carga inicializada - version SIN proteger]");

        SimulacionDeDespacho sinProteger =
                new SimulacionDeDespacho("Zona sin sincronizar", new ZonaDeCargaInsegura());
        sinProteger.cargarPedidos(pedidosDeLaJornada());
        sinProteger.contratarRepartidores(EQUIPO);

        ConsolaSegura.saltoDeLinea();
        ConsolaSegura.reiniciarCronometro();
        sinProteger.ejecutar();
        ReporteConsola.imprimirResultado(sinProteger);

        // ---------------------------------------------------------------
        // ESCENARIO 2: la misma jornada, ahora protegida
        // ---------------------------------------------------------------
        ReporteConsola.imprimirSeccion("2. ZONA DE CARGA SINCRONIZADA (la solucion)");
        ConsolaSegura.imprimirSimple("[Zona de carga inicializada - version protegida con synchronized]");

        SimulacionDeDespacho protegida =
                new SimulacionDeDespacho("Zona sincronizada", new ZonaDeCarga());
        protegida.cargarPedidos(pedidosDeLaJornada());
        protegida.contratarRepartidores(EQUIPO);

        ConsolaSegura.saltoDeLinea();
        ConsolaSegura.reiniciarCronometro();
        protegida.ejecutar();
        ReporteConsola.imprimirResultado(protegida);

        ReporteConsola.imprimirSeccion("3. ESTADO FINAL DE LOS PEDIDOS (zona sincronizada)");
        ReporteConsola.imprimirEstadoFinal(protegida);

        // ---------------------------------------------------------------
        // ESCENARIO 3: pedidos que llegan cuando el turno ya empezo
        // ---------------------------------------------------------------
        ReporteConsola.imprimirSeccion("4. PEDIDOS QUE LLEGAN TARDE (espera con wait / notifyAll)");
        ConsolaSegura.imprimirSimple("[Zona de carga inicializada - la recepcion sigue abierta]");

        SimulacionDeDespacho conLlegadaTardia =
                new SimulacionDeDespacho("Llegada tardia", new ZonaDeCarga());
        conLlegadaTardia.cargarPedidos(pedidosIniciales());
        conLlegadaTardia.contratarRepartidores("Marcela", "Diego");

        ConsolaSegura.saltoDeLinea();
        ConsolaSegura.reiniciarCronometro();
        conLlegadaTardia.ejecutar(pedidosTardios(), RETRASO_CAMION_MS);
        ReporteConsola.imprimirResultado(conLlegadaTardia);

        // ---------------------------------------------------------------
        // COMPARACION Y CIERRE
        // ---------------------------------------------------------------
        ReporteConsola.imprimirSeccion("5. COMPARACION DE LAS DOS ZONAS SOBRE LA MISMA JORNADA");
        ReporteConsola.imprimirComparacion(sinProteger, protegida);

        System.out.println();
        System.out.println("   La unica diferencia entre ambas corridas es la sincronizacion de la");
        System.out.println("   zona de carga: los pedidos, los repartidores y la clase Repartidor");
        System.out.println("   son exactamente los mismos.");

        ReporteConsola.imprimirSeccion("6. VERIFICACION FINAL");
        verificar(protegida, conLlegadaTardia);
    }

    /**
     * Comprueba que los escenarios sincronizados terminaron sin problemas e
     * imprime el mensaje de cierre.
     *
     * @param protegida        escenario con la zona sincronizada
     * @param conLlegadaTardia escenario con pedidos tardios
     */
    private static void verificar(SimulacionDeDespacho protegida,
                                  SimulacionDeDespacho conLlegadaTardia) {
        boolean todoBien = protegida.todoEnOrden() && conLlegadaTardia.todoEnOrden();

        System.out.println("   Zona sincronizada : "
                + protegida.getPedidosEntregados() + " de " + protegida.getTotalPedidos()
                + " entregados, " + protegida.getRegistro().getTotalIncidencias() + " incidencia(s).");
        System.out.println("   Llegada tardia    : "
                + conLlegadaTardia.getPedidosEntregados() + " de " + conLlegadaTardia.getTotalPedidos()
                + " entregados, " + conLlegadaTardia.getRegistro().getTotalIncidencias() + " incidencia(s).");

        if (todoBien) {
            ReporteConsola.imprimirCierre("Todos los pedidos han sido entregados correctamente");
        } else {
            ReporteConsola.imprimirCierre("ATENCION: la verificacion final encontro problemas.");
        }
    }

    // ------------------------------------------------------------------
    // Escenarios de datos
    // ------------------------------------------------------------------

    /**
     * Crea los doce pedidos de la jornada que se usa para comparar las dos
     * zonas de carga. Se generan de nuevo en cada escenario, porque un pedido
     * ya entregado no puede volver a repartirse.
     *
     * @return pedidos recien creados, todos en estado PENDIENTE
     */
    private static List<Pedido> pedidosDeLaJornada() {
        List<String> destinos = Arrays.asList(
                "Santiago Centro", "Providencia", "Nunoa", "Recoleta",
                "Las Condes", "Maipu", "La Florida", "San Miguel",
                "Vitacura", "Independencia", "Macul", "Penalolen");

        List<Pedido> pedidos = new ArrayList<>();
        for (int i = 0; i < destinos.size(); i++) {
            pedidos.add(new Pedido(i + 1, destinos.get(i)));
        }
        return pedidos;
    }

    /**
     * Crea los pedidos con los que arranca el turno del tercer escenario.
     *
     * @return cuatro pedidos iniciales
     */
    private static List<Pedido> pedidosIniciales() {
        return new ArrayList<>(Arrays.asList(
                new Pedido(101, "Av. Matta 800, Santiago"),
                new Pedido(102, "Los Leones 200, Providencia"),
                new Pedido(103, "Gran Avenida 5600, San Miguel"),
                new Pedido(104, "Av. Grecia 900, Nunoa")));
    }

    /**
     * Crea los pedidos que llegan cuando los repartidores ya estan trabajando.
     *
     * @return tres pedidos tardios
     */
    private static List<Pedido> pedidosTardios() {
        return new ArrayList<>(Arrays.asList(
                new Pedido(105, "Camino El Alba 321, Las Condes"),
                new Pedido(106, "Pasaje El Roble 789, La Florida"),
                new Pedido(107, "Av. Vicuna Mackenna 1500, Nunoa")));
    }
}
