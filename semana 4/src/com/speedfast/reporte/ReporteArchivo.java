package com.speedfast.reporte;

import com.speedfast.modelo.EstadoPedido;
import com.speedfast.modelo.Pedido;
import com.speedfast.modelo.Repartidor;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * SALIDA DE DATOS del sistema.
 *
 * Escribe en un archivo de texto el informe de la jornada: cuanto entrego
 * cada repartidor, que paso con cada pedido y que problemas se detectaron al
 * leer los datos de entrada.
 *
 * La consola sirve para ver la simulacion mientras ocurre, pero se pierde al
 * cerrar el programa; el informe queda. Por eso el sistema entrega las dos
 * cosas y no solo la salida por pantalla.
 *
 * @author Olga Rivas
 * @version 4.0
 */
public final class ReporteArchivo {

    /** Linea separadora del informe. */
    private static final String LINEA = "============================================================";

    /** Formato de la fecha que encabeza el informe. */
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    /** Clase de utilidad: no se instancia. */
    private ReporteArchivo() {
    }

    /**
     * Escribe el informe completo de la jornada.
     *
     * Se usa try-with-resources para que el archivo quede cerrado si o si,
     * incluso si la escritura falla a la mitad.
     *
     * @param destino      archivo donde se escribe el informe
     * @param repartidores repartidores que participaron de la simulacion
     * @param pedidos      todos los pedidos cargados
     * @param incidencias  problemas detectados al leer el archivo de entrada
     * @throws IOException si el archivo no se puede crear o escribir
     */
    public static void escribir(Path destino, List<Repartidor> repartidores,
                                List<Pedido> pedidos, List<String> incidencias) throws IOException {

        Path carpeta = destino.getParent();
        if (carpeta != null) {
            Files.createDirectories(carpeta);
        }

        try (BufferedWriter salida = Files.newBufferedWriter(destino, StandardCharsets.UTF_8)) {
            escribirEncabezado(salida);
            escribirResumen(salida, repartidores, pedidos);
            escribirDetallePorRepartidor(salida, repartidores);
            escribirPendientes(salida, pedidos);
            escribirIncidencias(salida, incidencias);

            salida.write(LINEA);
            salida.newLine();
            salida.write("  Fin del informe");
            salida.newLine();
            salida.write(LINEA);
            salida.newLine();
        }
    }

    /**
     * Escribe el titulo y la fecha de generacion.
     *
     * @param salida escritor del archivo
     * @throws IOException si falla la escritura
     */
    private static void escribirEncabezado(BufferedWriter salida) throws IOException {
        salida.write(LINEA);
        salida.newLine();
        salida.write("  SPEEDFAST - INFORME DE ENTREGAS (Semana 4)");
        salida.newLine();
        salida.write("  Generado el " + LocalDateTime.now().format(FORMATO_FECHA));
        salida.newLine();
        salida.write(LINEA);
        salida.newLine();
        salida.newLine();
    }

    /**
     * Escribe los totales de la jornada.
     *
     * @param salida       escritor del archivo
     * @param repartidores repartidores que participaron
     * @param pedidos      pedidos cargados
     * @throws IOException si falla la escritura
     */
    private static void escribirResumen(BufferedWriter salida, List<Repartidor> repartidores,
                                        List<Pedido> pedidos) throws IOException {
        int entregados = contarPorEstado(pedidos, EstadoPedido.ENTREGADO);
        long tiempoTotal = 0;
        for (Repartidor repartidor : repartidores) {
            tiempoTotal += repartidor.getTiempoRutaMs();
        }

        salida.write("RESUMEN DE LA JORNADA");
        salida.newLine();
        salida.write(String.format(Locale.US, "  %-32s %d", "Repartidores en ruta", repartidores.size()));
        salida.newLine();
        salida.write(String.format(Locale.US, "  %-32s %d", "Pedidos cargados", pedidos.size()));
        salida.newLine();
        salida.write(String.format(Locale.US, "  %-32s %d", "Entregas completadas", entregados));
        salida.newLine();
        salida.write(String.format(Locale.US, "  %-32s %d", "Pedidos sin entregar",
                pedidos.size() - entregados));
        salida.newLine();
        salida.write(String.format(Locale.US, "  %-32s %d ms", "Tiempo acumulado en ruta", tiempoTotal));
        salida.newLine();
        salida.newLine();
    }

    /**
     * Escribe, por cada repartidor, el detalle de los pedidos de su ruta.
     *
     * @param salida       escritor del archivo
     * @param repartidores repartidores que participaron
     * @throws IOException si falla la escritura
     */
    private static void escribirDetallePorRepartidor(BufferedWriter salida,
                                                     List<Repartidor> repartidores) throws IOException {
        salida.write("DETALLE POR REPARTIDOR");
        salida.newLine();

        for (Repartidor repartidor : repartidores) {
            salida.newLine();
            salida.write("  " + repartidor.getNombre() + " - " + repartidor.getVehiculo());
            salida.newLine();
            salida.write("  " + repartidor.getEntregasCompletadas() + " de "
                    + repartidor.getTotalPedidos() + " entrega(s), "
                    + repartidor.getTiempoRutaMs() + " ms en ruta.");
            salida.newLine();

            for (Pedido pedido : repartidor.getPedidosAsignados()) {
                salida.write(String.format(Locale.US,
                        "     %-8s %-16s %6.1f km  estimado %3d min  real %5d ms  %s",
                        pedido.getIdPedido(),
                        pedido.getTipoEntrega(),
                        pedido.getDistanciaKm(),
                        pedido.calcularTiempoEntrega(),
                        pedido.getDuracionRealMs(),
                        pedido.getEstado().getDescripcion()));
                salida.newLine();
            }
        }
        salida.newLine();
    }

    /**
     * Escribe la lista de pedidos que no llegaron a destino.
     *
     * @param salida  escritor del archivo
     * @param pedidos pedidos cargados
     * @throws IOException si falla la escritura
     */
    private static void escribirPendientes(BufferedWriter salida, List<Pedido> pedidos)
            throws IOException {
        salida.write("PEDIDOS QUE NO SE ENTREGARON");
        salida.newLine();

        int pendientes = 0;
        for (Pedido pedido : pedidos) {
            if (pedido.getEstado() != EstadoPedido.ENTREGADO) {
                salida.write("  - " + pedido.getIdPedido() + " | " + pedido.getTipoEntrega()
                        + " | " + pedido.getEstado().getDescripcion());
                salida.newLine();
                pendientes++;
            }
        }
        if (pendientes == 0) {
            salida.write("  (ninguno: todas las entregas se completaron)");
            salida.newLine();
        }
        salida.newLine();
    }

    /**
     * Escribe los problemas detectados al leer el archivo de entrada.
     *
     * @param salida      escritor del archivo
     * @param incidencias mensajes de error de la carga
     * @throws IOException si falla la escritura
     */
    private static void escribirIncidencias(BufferedWriter salida, List<String> incidencias)
            throws IOException {
        salida.write("INCIDENCIAS AL LEER EL ARCHIVO DE PEDIDOS");
        salida.newLine();

        if (incidencias.isEmpty()) {
            salida.write("  (ninguna: todas las lineas se pudieron interpretar)");
            salida.newLine();
        } else {
            for (String incidencia : incidencias) {
                salida.write("  - " + incidencia);
                salida.newLine();
            }
        }
        salida.newLine();
    }

    /**
     * Cuenta cuantos pedidos estan en un estado determinado.
     *
     * @param pedidos pedidos a revisar
     * @param estado  estado buscado
     * @return cantidad de coincidencias
     */
    private static int contarPorEstado(List<Pedido> pedidos, EstadoPedido estado) {
        int total = 0;
        for (Pedido pedido : pedidos) {
            if (pedido.getEstado() == estado) {
                total++;
            }
        }
        return total;
    }
}
