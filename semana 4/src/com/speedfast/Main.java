package com.speedfast;

import com.speedfast.modelo.EstadoPedido;
import com.speedfast.modelo.Pedido;
import com.speedfast.modelo.PedidoComida;
import com.speedfast.modelo.PedidoEncomienda;
import com.speedfast.modelo.PedidoExpress;
import com.speedfast.modelo.Repartidor;
import com.speedfast.reporte.ReporteArchivo;
import com.speedfast.reporte.ReporteConsola;
import com.speedfast.servicio.CargadorDePedidos;
import com.speedfast.servicio.ControladorDeEnvios;
import com.speedfast.servicio.MonitorDeEntregas;
import com.speedfast.servicio.SimuladorDeEntregas;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Simulacion del sistema de reparto SpeedFast.
 *
 * Semana 4: varios repartidores entregando al mismo tiempo, cada uno en su
 * propio hilo.
 *
 * Main no contiene reglas de negocio: arma el escenario y va pidiendo
 * operaciones. Quien calcula un tiempo son las subclases de {@link Pedido};
 * quien recorre una ruta es {@link Repartidor}; quien pone a correr los hilos
 * es {@link SimuladorDeEntregas}; quien da formato a la salida son las clases
 * de reporte.
 *
 * El escenario se lee de {@code datos/pedidos.txt}. Si ese archivo no esta
 * disponible, Main arma en codigo un escenario equivalente para que la
 * simulacion pueda demostrarse igual.
 *
 * Admite dos argumentos opcionales: la ruta del archivo de pedidos y la del
 * informe de salida.
 *
 * @author Olga Rivas
 * @version 4.0
 */
public class Main {

    /** Archivo de entrada por defecto. */
    private static final String ARCHIVO_ENTRADA = "datos/pedidos.txt";

    /** Nombre del informe que se genera al final. */
    private static final String ARCHIVO_SALIDA = "entregas.txt";

    /** Pedido que se cancela antes de salir, para ver como reacciona su hilo. */
    private static final String PEDIDO_A_CANCELAR = "P-106";

    /** Minimos que exige la actividad: 3 repartidores con 2 pedidos cada uno. */
    private static final int MINIMO_REPARTIDORES = 3;
    private static final int MINIMO_PEDIDOS_POR_RUTA = 2;

    public static void main(String[] args) {

        ReporteConsola.imprimirEncabezado(
                "SISTEMA DE REPARTO SPEEDFAST - Semana 4",
                "Entregas simultaneas con Runnable y ExecutorService");

        // 1. ENTRADA DE DATOS -------------------------------------------
        // Los pedidos y las rutas se leen de un archivo de texto, no estan
        // escritos dentro del programa.
        ReporteConsola.imprimirSeccion("1. LECTURA DEL ARCHIVO DE PEDIDOS");

        Path entrada = CargadorDePedidos.ubicar(
                aTexto(args, 0, ARCHIVO_ENTRADA));

        CargadorDePedidos cargador = new CargadorDePedidos();
        List<Repartidor> repartidores;
        List<Pedido> pedidos;
        List<String> incidencias = new ArrayList<>();

        try {
            repartidores = cargador.cargar(entrada);
            pedidos = cargador.getPedidos();
            incidencias = cargador.getIncidencias();

            System.out.println("   Archivo leido  : " + entrada);
            System.out.println("   Pedidos validos: " + pedidos.size());
            System.out.println("   Repartidores   : " + repartidores.size());
            informarIncidencias(incidencias);

        } catch (IOException e) {
            // Que falte el archivo no puede dejar la actividad sin demostrar:
            // se avisa con claridad y se sigue con el escenario de respaldo.
            System.out.println("   No se pudo leer el archivo de pedidos:");
            System.out.println("   " + entrada.toAbsolutePath());
            System.out.println("   Detalle: " + e.getClass().getSimpleName());
            repartidores = new ArrayList<>();
            pedidos = new ArrayList<>();
        }

        if (pedidos.isEmpty()) {
            System.out.println();
            System.out.println("   No hay pedidos utilizables en el archivo.");
            System.out.println("   Se arma el escenario de respaldo definido en Main.");
            repartidores = escenarioPorDefecto();
            pedidos = pedidosDe(repartidores);
            System.out.println("   Repartidores creados: " + repartidores.size()
                    + " | Pedidos creados: " + pedidos.size());
        }

        revisarMinimosDeLaPauta(repartidores);

        // 2. REGISTRO EN EL CONTROLADOR ---------------------------------
        // Se reutiliza el controlador de la semana 3 para llevar la jornada.
        ReporteConsola.imprimirSeccion("2. REGISTRO DE LA JORNADA");

        ControladorDeEnvios controlador = new ControladorDeEnvios();
        for (Pedido pedido : pedidos) {
            controlador.reservar(pedido);
        }

        // 3. RUTAS ARMADAS ----------------------------------------------
        ReporteConsola.imprimirSeccion("3. RUTAS ASIGNADAS");
        mostrarRutas(repartidores);

        // 4. TIEMPOS ESTIMADOS ------------------------------------------
        // Sigue funcionando el polimorfismo de las semanas anteriores: una
        // sola llamada y cada tipo de pedido calcula su tiempo a su manera.
        ReporteConsola.imprimirSeccion("4. TIEMPO ESTIMADO POR TIPO DE PEDIDO");
        ReporteConsola.imprimirTablaComparativa(pedidos);
        ReporteConsola.imprimirExtremos(pedidos);

        // 5. CANCELACION ANTES DE SALIR ---------------------------------
        // Sirve para comprobar que el hilo del repartidor detecta el cambio
        // de estado y omite ese pedido en vez de caerse.
        ReporteConsola.imprimirSeccion("5. CANCELACION ANTES DE INICIAR LAS RUTAS");
        controlador.cancelar(elegirPedidoACancelar(controlador, pedidos),
                "el cliente anulo la compra");

        // 6. SIMULACION CONCURRENTE -------------------------------------
        ReporteConsola.imprimirSeccion("6. SIMULACION: TODAS LAS RUTAS AL MISMO TIEMPO");

        SimuladorDeEntregas simulador = new SimuladorDeEntregas(repartidores);
        long inicio = System.currentTimeMillis();
        boolean completa = simulador.ejecutar();
        long duracionReal = System.currentTimeMillis() - inicio;

        // 7. RESULTADO DE LA SIMULACION ---------------------------------
        ReporteConsola.imprimirSeccion("7. RESULTADO DE LA SIMULACION");
        mostrarResultado(simulador.getMonitor(), repartidores, duracionReal, completa);

        // 8. ESTADO FINAL DE CADA PEDIDO --------------------------------
        ReporteConsola.imprimirSeccion("8. ESTADO FINAL DE LOS PEDIDOS");
        mostrarEstadoFinal(pedidos);

        // 9. DETALLE Y BITACORA -----------------------------------------
        // mostrarResumen() vive en la clase abstracta y verHistorial() viene
        // de la interfaz Rastreable: los dos siguen funcionando sobre los
        // pedidos que acaban de pasar por los hilos.
        ReporteConsola.imprimirSeccion("9. DETALLE Y BITACORA DE PEDIDOS SELECCIONADOS");
        mostrarDetalleDeUnPedidoPorTipo(pedidos);

        // 10. SALIDA DE DATOS -------------------------------------------
        ReporteConsola.imprimirSeccion("10. INFORME ESCRITO EN ARCHIVO");

        Path carpeta = entrada.getParent();
        Path salida = (args.length > 1)
                ? Path.of(aTexto(args, 1, ARCHIVO_SALIDA))
                : (carpeta == null ? Path.of(ARCHIVO_SALIDA) : carpeta.resolve(ARCHIVO_SALIDA));

        try {
            ReporteArchivo.escribir(salida, repartidores, pedidos, incidencias);
            System.out.println("   Informe generado en: " + salida);
        } catch (IOException e) {
            // Que falle la escritura del informe no puede invalidar la
            // simulacion, que ya ocurrio: se avisa y el programa termina bien.
            System.out.println("   No se pudo escribir el informe en " + salida);
            System.out.println("   Detalle: " + e.getMessage());
        }

        ReporteConsola.imprimirCierre(controlador.getTotalPedidos());
    }

    // ------------------------------------------------------------------
    // Escenario de respaldo, instanciado directamente en Main
    // ------------------------------------------------------------------

    /**
     * Arma en codigo el mismo escenario que trae el archivo: tres repartidores
     * con dos o mas pedidos cada uno, que es el minimo que pide la actividad.
     *
     * @return repartidores con sus rutas listas para salir
     */
    private static List<Repartidor> escenarioPorDefecto() {
        Repartidor camila = new Repartidor("Camila Soto", "moto con mochila termica");
        camila.asignarPedido(new PedidoComida("P-101", "Av. Providencia 1234, Santiago",
                3.5, "Sushi Kai", true));
        camila.asignarPedido(new PedidoComida("P-104", "Av. Irarrazaval 3400, Nunoa",
                6.2, "Pizzeria Napoli", true));
        camila.asignarPedido(new PedidoExpress("P-107", "Los Leones 200, Providencia",
                1.8, "Farmacia Salcobrand", true));

        Repartidor diego = new Repartidor("Diego Fuentes", "furgon de carga");
        diego.asignarPedido(new PedidoEncomienda("P-102", "Calle Los Aromos 456, Maipu",
                8.0, 25.5, "Caja reforzada"));
        diego.asignarPedido(new PedidoEncomienda("P-105", "Camino El Alba 321, Las Condes",
                15.4, 4.2, "Sobre acolchado"));

        Repartidor luis = new Repartidor("Luis Vera", "bicicleta electrica");
        luis.asignarPedido(new PedidoExpress("P-103", "Pasaje El Roble 789, La Florida",
                2.0, "Farmacia Central", true));
        luis.asignarPedido(new PedidoExpress("P-106", "Av. Vicuna Mackenna 1500, Nunoa",
                7.5, "Supermercado Lider", true));
        luis.asignarPedido(new PedidoComida("P-108", "Gran Avenida 5600, San Miguel",
                9.1, "Heladeria Nevada", false));

        List<Repartidor> repartidores = new ArrayList<>();
        repartidores.add(camila);
        repartidores.add(diego);
        repartidores.add(luis);
        return repartidores;
    }

    /**
     * Reune en una sola lista los pedidos de todas las rutas.
     *
     * @param repartidores repartidores con sus rutas armadas
     * @return todos los pedidos de la jornada
     */
    private static List<Pedido> pedidosDe(List<Repartidor> repartidores) {
        List<Pedido> pedidos = new ArrayList<>();
        for (Repartidor repartidor : repartidores) {
            pedidos.addAll(repartidor.getPedidosAsignados());
        }
        return pedidos;
    }

    // ------------------------------------------------------------------
    // Apoyo a la lectura de datos
    // ------------------------------------------------------------------

    /**
     * Devuelve el argumento indicado, o el valor por defecto si no viene o si
     * no puede usarse como ruta.
     *
     * @param args        argumentos recibidos por el programa
     * @param indice      posicion del argumento buscado
     * @param porDefecto  valor a usar si el argumento no sirve
     * @return texto de la ruta a utilizar
     */
    private static String aTexto(String[] args, int indice, String porDefecto) {
        if (args.length <= indice || args[indice].isBlank()) {
            return porDefecto;
        }
        try {
            Path.of(args[indice]);
            return args[indice];
        } catch (InvalidPathException e) {
            System.out.println("   La ruta indicada no es valida: " + args[indice]);
            System.out.println("   Se usara " + porDefecto + " en su lugar.");
            return porDefecto;
        }
    }

    /**
     * Muestra las lineas del archivo que hubo que descartar.
     *
     * @param incidencias problemas detectados durante la lectura
     */
    private static void informarIncidencias(List<String> incidencias) {
        if (incidencias.isEmpty()) {
            return;
        }
        System.out.println();
        System.out.println("   Lineas con problemas (" + incidencias.size()
                + "). El programa continua igual:");
        for (String incidencia : incidencias) {
            System.out.println("      - " + incidencia);
        }
    }

    /**
     * Avisa si el escenario cargado no alcanza los minimos que pide la
     * actividad, para que el aviso salga en pantalla y no pase inadvertido.
     *
     * @param repartidores repartidores que van a salir a ruta
     */
    private static void revisarMinimosDeLaPauta(List<Repartidor> repartidores) {
        if (repartidores.size() < MINIMO_REPARTIDORES) {
            System.out.println();
            System.out.println("   AVISO: hay " + repartidores.size() + " repartidor(es) y la "
                    + "actividad pide al menos " + MINIMO_REPARTIDORES + ".");
        }
        for (Repartidor repartidor : repartidores) {
            if (repartidor.getTotalPedidos() < MINIMO_PEDIDOS_POR_RUTA) {
                System.out.println("   AVISO: " + repartidor.getNombre() + " tiene solo "
                        + repartidor.getTotalPedidos() + " pedido(s) y se piden al menos "
                        + MINIMO_PEDIDOS_POR_RUTA + ".");
            }
        }
    }

    // ------------------------------------------------------------------
    // Presentacion
    // ------------------------------------------------------------------

    /**
     * Muestra que pedidos le tocan a cada repartidor antes de salir a ruta.
     *
     * @param repartidores repartidores con sus rutas armadas
     */
    private static void mostrarRutas(List<Repartidor> repartidores) {
        for (Repartidor repartidor : repartidores) {
            System.out.println("   " + repartidor.getNombre()
                    + " - " + repartidor.getVehiculo()
                    + " (" + repartidor.getTotalPedidos() + " pedidos)");
            for (Pedido pedido : repartidor.getPedidosAsignados()) {
                System.out.printf("      %-8s %-16s %5.1f km  ->  %s%n",
                        pedido.getIdPedido(),
                        pedido.getTipoEntrega(),
                        pedido.getDistanciaKm(),
                        pedido.getDireccionEntrega());
            }
        }
    }

    /**
     * Elige que pedido cancelar: el previsto, si existe, y si no el ultimo
     * cargado, de modo que la demostracion funcione con cualquier archivo.
     *
     * @param controlador controlador donde estan reservados los pedidos
     * @param pedidos     pedidos de la jornada
     * @return identificador del pedido a cancelar
     */
    private static String elegirPedidoACancelar(ControladorDeEnvios controlador,
                                                List<Pedido> pedidos) {
        if (controlador.buscarPedido(PEDIDO_A_CANCELAR) != null) {
            return PEDIDO_A_CANCELAR;
        }
        return pedidos.get(pedidos.size() - 1).getIdPedido();
    }

    /**
     * Muestra los totales de la simulacion y compara el tiempo que tomo en
     * paralelo con el que habria tomado haciendo una ruta despues de la otra.
     *
     * @param monitor      marcador compartido que actualizaron los hilos
     * @param repartidores repartidores que participaron
     * @param duracionReal milisegundos que tomo la simulacion completa
     * @param completa     true si todas las rutas alcanzaron a terminar
     */
    private static void mostrarResultado(MonitorDeEntregas monitor, List<Repartidor> repartidores,
                                         long duracionReal, boolean completa) {
        long sumaDeRutas = 0;
        for (Repartidor repartidor : repartidores) {
            sumaDeRutas += repartidor.getTiempoRutaMs();
        }

        System.out.println("   Rutas terminadas     : " + monitor.getRutasTerminadas()
                + " de " + repartidores.size());
        System.out.println("   Entregas completadas : " + monitor.getEntregasCompletadas());
        System.out.println("   Simulacion completa  : " + (completa ? "si" : "no"));
        System.out.println();

        System.out.println("   Cierre de cada ruta, en el orden en que fueron terminando:");
        for (String evento : monitor.getBitacora()) {
            System.out.println("      " + evento);
        }
        System.out.println();

        System.out.printf(Locale.US, "   Trabajo total de las rutas     : %d ms%n", sumaDeRutas);
        System.out.printf(Locale.US, "   Duracion real en paralelo      : %d ms%n", duracionReal);

        // La comparacion solo tiene sentido si hubo mas de una ruta corriendo.
        if (duracionReal > 0 && repartidores.size() > 1) {
            System.out.printf(Locale.US, "   Mejora por trabajar en paralelo: %.1f veces%n",
                    (double) sumaDeRutas / duracionReal);
        }
    }

    /**
     * Muestra en que estado quedo cada pedido despues de la simulacion.
     *
     * Se puede leer sin sincronizar nada porque la espera del ExecutorService
     * garantiza que todos los hilos ya terminaron de escribir.
     *
     * @param pedidos pedidos de la jornada
     */
    private static void mostrarEstadoFinal(List<Pedido> pedidos) {
        System.out.printf("   %-8s %-16s %-12s %12s%n", "PEDIDO", "TIPO", "ESTADO", "TIEMPO REAL");
        System.out.println("   --------------------------------------------------------");

        int entregados = 0;
        for (Pedido pedido : pedidos) {
            System.out.printf("   %-8s %-16s %-12s %9d ms%n",
                    pedido.getIdPedido(),
                    pedido.getTipoEntrega(),
                    pedido.getEstado().getDescripcion(),
                    pedido.getDuracionRealMs());
            if (pedido.getEstado() == EstadoPedido.ENTREGADO) {
                entregados++;
            }
        }

        System.out.println();
        System.out.println("   Entregados: " + entregados + " de " + pedidos.size());
    }

    /**
     * Muestra el resumen completo y la bitacora de un pedido de cada tipo.
     *
     * Es la prueba de que mostrarResumen() y verHistorial() siguen operativos
     * despues del trabajo concurrente: el resumen sale de la clase abstracta y
     * la bitacora la fue escribiendo el hilo del repartidor mientras entregaba.
     *
     * @param pedidos pedidos de la jornada
     */
    private static void mostrarDetalleDeUnPedidoPorTipo(List<Pedido> pedidos) {
        List<String> tiposMostrados = new ArrayList<>();

        for (Pedido pedido : pedidos) {
            if (tiposMostrados.contains(pedido.getTipoEntrega())) {
                continue;
            }
            tiposMostrados.add(pedido.getTipoEntrega());

            pedido.mostrarResumen();
            System.out.println();
            pedido.verHistorial();
            System.out.println();
        }
    }
}
