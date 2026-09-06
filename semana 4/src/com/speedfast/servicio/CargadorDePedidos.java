package com.speedfast.servicio;

import com.speedfast.excepcion.PedidoInvalidoException;
import com.speedfast.modelo.Pedido;
import com.speedfast.modelo.PedidoComida;
import com.speedfast.modelo.PedidoEncomienda;
import com.speedfast.modelo.PedidoExpress;
import com.speedfast.modelo.Repartidor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ENTRADA DE DATOS del sistema.
 *
 * Lee el archivo de texto con los pedidos de la jornada y arma con el las
 * rutas: un {@link Repartidor} por cada nombre distinto que aparezca, con
 * todos sus pedidos ya asignados.
 *
 * Que los datos vengan de un archivo y no escritos dentro de Main es lo que
 * permite cambiar el escenario de la simulacion sin recompilar, y es tambien
 * lo que obliga a manejar en serio los errores: una linea escrita a mano puede
 * traer un numero mal puesto, un tipo inexistente o campos de menos. Ninguno
 * de esos casos puede detener el programa, asi que la linea se descarta, el
 * problema queda anotado y la carga continua.
 *
 * Formato de cada linea (campos separados por punto y coma):
 *
 * <pre>
 * TIPO;ID;DIRECCION;DISTANCIA_KM;REPARTIDOR;VEHICULO;DATO_1;DATO_2
 * </pre>
 *
 * donde DATO_1 y DATO_2 dependen del tipo:
 *
 * <ul>
 *   <li>COMIDA      : restaurante ; requiere mochila termica (true/false)</li>
 *   <li>ENCOMIENDA  : peso en kg  ; tipo de embalaje</li>
 *   <li>EXPRESS     : local       ; disponibilidad inmediata (true/false)</li>
 * </ul>
 *
 * Las lineas en blanco y las que empiezan con # se ignoran.
 *
 * @author Olga Rivas
 * @version 4.0
 */
public class CargadorDePedidos {

    /** Separador de campos del archivo. */
    private static final String SEPARADOR = ";";

    /** Cantidad exacta de campos que debe tener una linea valida. */
    private static final int CAMPOS_ESPERADOS = 8;

    /** Problemas encontrados durante la lectura, para informarlos despues. */
    private final List<String> incidencias;

    /** Pedidos cargados correctamente, en el orden del archivo. */
    private final List<Pedido> pedidos;

    /** Identificadores ya vistos, para no aceptar pedidos repetidos. */
    private final Set<String> idsUsados;

    /** Crea un cargador sin datos leidos. */
    public CargadorDePedidos() {
        this.incidencias = new ArrayList<>();
        this.pedidos = new ArrayList<>();
        this.idsUsados = new HashSet<>();
    }

    /**
     * Lee el archivo indicado y construye las rutas de los repartidores.
     *
     * @param archivo ruta del archivo de pedidos
     * @return lista de repartidores con sus pedidos ya asignados
     * @throws IOException si el archivo no existe o no se puede leer
     */
    public List<Repartidor> cargar(Path archivo) throws IOException {
        incidencias.clear();
        pedidos.clear();
        idsUsados.clear();

        // LinkedHashMap para que los repartidores queden en el mismo orden en
        // que aparecen en el archivo y la salida sea siempre comparable.
        Map<String, Repartidor> rutas = new LinkedHashMap<>();
        List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);

        for (int i = 0; i < lineas.size(); i++) {
            String linea = lineas.get(i).trim();
            int numeroLinea = i + 1;

            if (linea.isEmpty() || linea.startsWith("#")) {
                continue;
            }

            try {
                procesarLinea(linea, numeroLinea, rutas);
            } catch (PedidoInvalidoException e) {
                // El archivo lo escribe una persona: una linea mala se informa
                // y se salta, pero la jornada se carga igual.
                incidencias.add(e.getMessage());
            }
        }

        return new ArrayList<>(rutas.values());
    }

    /**
     * Convierte una linea del archivo en un pedido y lo suma a la ruta del
     * repartidor correspondiente.
     *
     * @param linea       contenido de la linea
     * @param numeroLinea posicion de la linea en el archivo
     * @param rutas       repartidores construidos hasta el momento
     * @throws PedidoInvalidoException si la linea no se puede interpretar
     */
    private void procesarLinea(String linea, int numeroLinea, Map<String, Repartidor> rutas)
            throws PedidoInvalidoException {

        // El -1 conserva los campos vacios del final, que si no desaparecerian
        // y una linea incompleta pasaria como valida.
        String[] campos = linea.split(SEPARADOR, -1);

        if (campos.length != CAMPOS_ESPERADOS) {
            throw new PedidoInvalidoException(numeroLinea,
                    "se esperaban " + CAMPOS_ESPERADOS + " campos y llegaron " + campos.length);
        }

        String tipo = campos[0].trim().toUpperCase();
        String idPedido = campos[1].trim();
        String direccion = campos[2].trim();
        double distanciaKm = leerNumero(campos[3], numeroLinea, "la distancia");
        String nombreRepartidor = campos[4].trim();
        String vehiculo = campos[5].trim();
        String dato1 = campos[6].trim();
        String dato2 = campos[7].trim();

        if (idPedido.isEmpty() || nombreRepartidor.isEmpty()) {
            throw new PedidoInvalidoException(numeroLinea,
                    "el id del pedido y el nombre del repartidor no pueden ir vacios");
        }

        // Un id repetido dejaria dos pedidos distintos respondiendo al mismo
        // nombre, y las busquedas por id encontrarian siempre el primero.
        if (!idsUsados.add(idPedido.toUpperCase())) {
            throw new PedidoInvalidoException(numeroLinea,
                    "el id '" + idPedido + "' ya fue usado en una linea anterior");
        }

        Pedido pedido = construirPedido(tipo, idPedido, direccion, distanciaKm,
                dato1, dato2, numeroLinea);

        Repartidor repartidor = rutas.computeIfAbsent(nombreRepartidor,
                nombre -> new Repartidor(nombre, vehiculo.isEmpty() ? "vehiculo por confirmar" : vehiculo));

        // El repartidor sale con un solo vehiculo, el de su primera linea. Si
        // otra linea dice algo distinto se avisa, en vez de perder el dato en
        // silencio y que despues nadie entienda por que no se aplico.
        if (!vehiculo.isEmpty() && !vehiculo.equals(repartidor.getVehiculo())) {
            incidencias.add("Linea " + numeroLinea + ": se mantiene el vehiculo '"
                    + repartidor.getVehiculo() + "' de " + repartidor.getNombre()
                    + " y se ignora '" + vehiculo + "'.");
        }

        repartidor.asignarPedido(pedido);
        pedidos.add(pedido);
    }

    /**
     * Crea el pedido del tipo indicado.
     *
     * @param tipo        palabra que identifica el tipo de pedido
     * @param idPedido    identificador del pedido
     * @param direccion   direccion de entrega
     * @param distanciaKm distancia del recorrido
     * @param dato1       primer campo especifico del tipo
     * @param dato2       segundo campo especifico del tipo
     * @param numeroLinea linea del archivo, para poder informar el error
     * @return el pedido construido
     * @throws PedidoInvalidoException si el tipo no existe o los datos propios
     *                                 del tipo estan mal formados
     */
    private Pedido construirPedido(String tipo, String idPedido, String direccion,
                                   double distanciaKm, String dato1, String dato2,
                                   int numeroLinea) throws PedidoInvalidoException {
        try {
            switch (tipo) {
                case "COMIDA":
                    return new PedidoComida(idPedido, direccion, distanciaKm,
                            dato1, leerBoolean(dato2, numeroLinea, "la mochila termica"));
                case "ENCOMIENDA":
                    return new PedidoEncomienda(idPedido, direccion, distanciaKm,
                            leerNumero(dato1, numeroLinea, "el peso"), dato2);
                case "EXPRESS":
                    return new PedidoExpress(idPedido, direccion, distanciaKm,
                            dato1, leerBoolean(dato2, numeroLinea, "la disponibilidad"));
                default:
                    throw new PedidoInvalidoException(numeroLinea,
                            "tipo de pedido desconocido: '" + tipo + "'");
            }
        } catch (IllegalArgumentException e) {
            // La propia clase Pedido rechaza, por ejemplo, una distancia
            // negativa. Se traduce a la excepcion del cargador para que el
            // mensaje diga en que linea del archivo estaba el dato malo.
            throw new PedidoInvalidoException(numeroLinea, e.getMessage(), e);
        }
    }

    /**
     * Convierte un texto en numero.
     *
     * @param texto       valor leido del archivo
     * @param numeroLinea linea del archivo
     * @param queCampo    nombre del campo, para el mensaje de error
     * @return el valor convertido
     * @throws PedidoInvalidoException si el texto no es un numero
     */
    private double leerNumero(String texto, int numeroLinea, String queCampo)
            throws PedidoInvalidoException {
        try {
            return Double.parseDouble(texto.trim());
        } catch (NumberFormatException e) {
            throw new PedidoInvalidoException(numeroLinea,
                    queCampo + " no es un numero valido: '" + texto.trim() + "'", e);
        }
    }

    /**
     * Convierte un texto en true o false, sin aceptar cualquier otra cosa.
     *
     * Boolean.parseBoolean devolveria false ante cualquier palabra que no sea
     * "true", y un dato mal escrito pasaria como valido sin que nadie se entere.
     *
     * @param texto       valor leido del archivo
     * @param numeroLinea linea del archivo
     * @param queCampo    nombre del campo, para el mensaje de error
     * @return el valor convertido
     * @throws PedidoInvalidoException si el texto no es true ni false
     */
    private boolean leerBoolean(String texto, int numeroLinea, String queCampo)
            throws PedidoInvalidoException {
        String valor = texto.trim().toLowerCase();
        if ("true".equals(valor)) {
            return true;
        }
        if ("false".equals(valor)) {
            return false;
        }
        throw new PedidoInvalidoException(numeroLinea,
                queCampo + " debe ser true o false, y llego: '" + texto.trim() + "'");
    }

    /**
     * Busca el archivo de datos tanto si el programa se ejecuta desde la
     * carpeta del proyecto como desde la raiz del repositorio, que es el
     * tropiezo mas comun al abrir el proyecto en otro computador.
     *
     * @param rutaRelativa ruta indicada por el usuario
     * @return la primera ruta existente encontrada, o la original si ninguna existe
     */
    public static Path ubicar(String rutaRelativa) {
        Path directa = Path.of(rutaRelativa);
        if (Files.exists(directa)) {
            return directa;
        }
        Path desdeRaiz = Path.of("semana 4").resolve(rutaRelativa);
        if (Files.exists(desdeRaiz)) {
            return desdeRaiz;
        }
        return directa;
    }

    /**
     * @return copia de los problemas detectados durante la lectura
     */
    public List<String> getIncidencias() {
        return new ArrayList<>(incidencias);
    }

    /**
     * @return copia de los pedidos cargados correctamente
     */
    public List<Pedido> getPedidos() {
        return new ArrayList<>(pedidos);
    }
}
