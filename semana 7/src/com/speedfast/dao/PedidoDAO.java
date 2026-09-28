package com.speedfast.dao;

import com.speedfast.modelo.EstadoPedido;
import com.speedfast.modelo.Pedido;
import com.speedfast.modelo.PedidoComida;
import com.speedfast.modelo.PedidoEncomienda;
import com.speedfast.modelo.PedidoExpress;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Operaciones de base de datos sobre la tabla pedido.
 *
 * Traduce entre la jerarquia de pedidos del sistema y las columnas de la
 * tabla: el tipo se guarda como texto (COMIDA, ENCOMIENDA, EXPRESS) y al leer
 * decide que subclase construir.
 *
 * @author Olga Rivas
 * @version 7.0
 */
public class PedidoDAO {

    /** Nombres de tipo que acepta la columna tipo. */
    private static final String TIPO_COMIDA = "COMIDA";
    private static final String TIPO_ENCOMIENDA = "ENCOMIENDA";
    private static final String TIPO_EXPRESS = "EXPRESS";

    /** Insercion de un pedido nuevo. */
    private static final String SQL_INSERTAR =
            "INSERT INTO pedido (direccion, tipo, estado, codigo, distancia_km, restaurante,"
            + " peso_kg, embalaje, local_compra, prioritario, estado_detalle)"
            + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    /** Consulta que trae todos los pedidos. */
    private static final String SQL_LISTAR =
            "SELECT id, direccion, tipo, estado, codigo, distancia_km, restaurante,"
            + " peso_kg, embalaje, local_compra, prioritario, estado_detalle"
            + " FROM pedido ORDER BY id";

    /** Actualizacion del estado de un pedido. */
    private static final String SQL_ACTUALIZAR_ESTADO =
            "UPDATE pedido SET estado = ?, estado_detalle = ? WHERE id = ?";

    /**
     * Guarda un pedido en la base de datos y le asigna el id que genero MySQL.
     *
     * @param pedido pedido a guardar
     * @throws SQLException             si falla la insercion
     * @throws IllegalArgumentException si el pedido es nulo
     */
    public void guardar(Pedido pedido) throws SQLException {
        if (pedido == null) {
            throw new IllegalArgumentException("El pedido no puede ser nulo.");
        }

        // try-with-resources: la conexion y la sentencia se cierran solas al
        // salir del bloque, incluso si se lanza una excepcion.
        try (Connection conexion = ConexionBD.conectar();
             PreparedStatement sentencia = conexion.prepareStatement(
                     SQL_INSERTAR, Statement.RETURN_GENERATED_KEYS)) {

            // PreparedStatement pone cada dato en su parametro, asi que el
            // texto que escriba la usuaria nunca se mezcla con la consulta.
            sentencia.setString(1, pedido.getDireccionEntrega());
            sentencia.setString(2, tipoDe(pedido));
            sentencia.setString(3, estadoDe(pedido.getEstado()));
            sentencia.setString(4, pedido.getIdPedido());
            sentencia.setDouble(5, pedido.getDistanciaKm());
            sentencia.setString(6, valorSiEs(pedido, PedidoComida.class));
            aplicarPeso(sentencia, pedido);
            sentencia.setString(8, (pedido instanceof PedidoEncomienda)
                    ? ((PedidoEncomienda) pedido).getTipoEmbalaje() : null);
            sentencia.setString(9, (pedido instanceof PedidoExpress)
                    ? ((PedidoExpress) pedido).getLocal() : null);
            aplicarPrioritario(sentencia, pedido);
            sentencia.setString(11, pedido.getEstado().name());

            sentencia.executeUpdate();

            try (ResultSet generado = sentencia.getGeneratedKeys()) {
                if (generado.next()) {
                    pedido.setId(generado.getInt(1));
                }
            }
        }
    }

    /**
     * Devuelve todos los pedidos guardados en la base de datos.
     *
     * @return lista de pedidos, vacia si no hay ninguno
     * @throws SQLException si falla la consulta
     */
    public List<Pedido> listarTodos() throws SQLException {
        List<Pedido> pedidos = new ArrayList<>();

        try (Connection conexion = ConexionBD.conectar();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_LISTAR);
             ResultSet filas = sentencia.executeQuery()) {

            while (filas.next()) {
                pedidos.add(construirDesde(filas));
            }
        }
        return pedidos;
    }

    /**
     * Guarda en la base el estado que tiene el pedido en memoria.
     *
     * @param pedido pedido cuyo estado cambio
     * @throws SQLException si falla la actualizacion
     */
    public void actualizarEstado(Pedido pedido) throws SQLException {
        if (pedido == null || pedido.getId() == 0) {
            return;
        }

        try (Connection conexion = ConexionBD.conectar();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_ACTUALIZAR_ESTADO)) {

            sentencia.setString(1, estadoDe(pedido.getEstado()));
            sentencia.setString(2, pedido.getEstado().name());
            sentencia.setInt(3, pedido.getId());
            sentencia.executeUpdate();
        }
    }

    // ------------------------------------------------------------------
    // Traduccion entre el sistema y las columnas
    // ------------------------------------------------------------------

    /**
     * Nombre de tipo que corresponde a la subclase del pedido.
     *
     * @param pedido pedido a clasificar
     * @return COMIDA, ENCOMIENDA o EXPRESS
     */
    private String tipoDe(Pedido pedido) {
        if (pedido instanceof PedidoComida) {
            return TIPO_COMIDA;
        }
        if (pedido instanceof PedidoEncomienda) {
            return TIPO_ENCOMIENDA;
        }
        return TIPO_EXPRESS;
    }

    /**
     * Traduce el estado del sistema al que acepta la columna estado.
     *
     * El modelo de la actividad define tres estados y el sistema usa cinco,
     * asi que los dos que faltan se agrupan: un pedido reservado y uno ya
     * asignado siguen estando pendientes de salir.
     *
     * @param estado estado del pedido en el sistema
     * @return PENDIENTE, EN_REPARTO, ENTREGADO o CANCELADO
     */
    private String estadoDe(EstadoPedido estado) {
        switch (estado) {
            case DESPACHADO:
                return "EN_REPARTO";
            case ENTREGADO:
                return "ENTREGADO";
            case CANCELADO:
                return "CANCELADO";
            default:
                return "PENDIENTE";
        }
    }

    /**
     * Recupera el estado exacto del sistema a partir de lo guardado.
     *
     * Usa la columna estado_detalle si existe; si no, traduce desde el estado
     * del modelo de la actividad.
     *
     * @param detalle contenido de estado_detalle, puede ser null
     * @param estado  contenido de estado
     * @return estado del sistema
     */
    private EstadoPedido estadoDesde(String detalle, String estado) {
        if (detalle != null) {
            try {
                return EstadoPedido.valueOf(detalle);
            } catch (IllegalArgumentException e) {
                // Un valor que no corresponde a ningun estado se ignora y se
                // usa la columna estado, que si esta acotada por la base.
            }
        }
        if ("EN_REPARTO".equals(estado)) {
            return EstadoPedido.DESPACHADO;
        }
        if ("ENTREGADO".equals(estado)) {
            return EstadoPedido.ENTREGADO;
        }
        if ("CANCELADO".equals(estado)) {
            return EstadoPedido.CANCELADO;
        }
        return EstadoPedido.RESERVADO;
    }

    /**
     * Arma el pedido que corresponde a la fila actual del ResultSet.
     *
     * @param filas ResultSet posicionado en una fila
     * @return el pedido de esa fila, de la subclase que indique la columna tipo
     * @throws SQLException si falla la lectura de alguna columna
     */
    private Pedido construirDesde(ResultSet filas) throws SQLException {
        String codigo = filas.getString("codigo");
        String direccion = filas.getString("direccion");
        double distancia = filas.getDouble("distancia_km");
        boolean prioritario = filas.getBoolean("prioritario");

        Pedido pedido;
        switch (filas.getString("tipo")) {
            case TIPO_COMIDA:
                pedido = new PedidoComida(codigo, direccion, distancia,
                        textoOPorDefecto(filas.getString("restaurante"), "Sin indicar"), prioritario);
                break;
            case TIPO_ENCOMIENDA:
                pedido = new PedidoEncomienda(codigo, direccion, distancia,
                        Math.max(0.1, filas.getDouble("peso_kg")),
                        textoOPorDefecto(filas.getString("embalaje"), "Sin indicar"));
                break;
            default:
                pedido = new PedidoExpress(codigo, direccion, distancia,
                        textoOPorDefecto(filas.getString("local_compra"), "Sin indicar"), prioritario);
                break;
        }

        pedido.setId(filas.getInt("id"));

        // El modelo relaciona pedido y repartidor solo a traves de la tabla
        // entrega, asi que una asignacion que todavia no produjo una entrega no
        // queda guardada en ninguna parte. Al recuperar el pedido no se sabe
        // quien lo tenia, y un pedido asignado sin repartidor no se podria ni
        // reasignar ni despachar: por eso vuelve como pendiente, que es lo que
        // de verdad representa la fila guardada.
        EstadoPedido estado = estadoDesde(
                filas.getString("estado_detalle"), filas.getString("estado"));
        if (estado == EstadoPedido.ASIGNADO || estado == EstadoPedido.DESPACHADO) {
            estado = EstadoPedido.RESERVADO;
        }
        pedido.restaurarEstado(estado);
        return pedido;
    }

    /**
     * Pone el peso en la sentencia, o NULL si el pedido no es una encomienda.
     *
     * @param sentencia sentencia que se esta preparando
     * @param pedido    pedido que se esta guardando
     * @throws SQLException si falla al asignar el parametro
     */
    private void aplicarPeso(PreparedStatement sentencia, Pedido pedido) throws SQLException {
        if (pedido instanceof PedidoEncomienda) {
            sentencia.setDouble(7, ((PedidoEncomienda) pedido).getPesoKg());
        } else {
            sentencia.setNull(7, java.sql.Types.DECIMAL);
        }
    }

    /**
     * Devuelve el restaurante si el pedido es de comida, y null si no.
     *
     * @param pedido pedido que se esta guardando
     * @param tipo   subclase que se espera
     * @return el dato propio de esa subclase, o null
     */
    private String valorSiEs(Pedido pedido, Class<PedidoComida> tipo) {
        return tipo.isInstance(pedido) ? tipo.cast(pedido).getRestaurante() : null;
    }

    /**
     * Pone la marca de atencion preferente: mochila termica en los pedidos de
     * comida, repartidor disponible de inmediato en las compras express. En
     * una encomienda no aplica, asi que se guarda NULL y no false, que seria
     * decir que se pregunto y la respuesta fue que no.
     *
     * @param sentencia sentencia que se esta preparando
     * @param pedido    pedido que se esta guardando
     * @throws SQLException si falla al asignar el parametro
     */
    private void aplicarPrioritario(PreparedStatement sentencia, Pedido pedido) throws SQLException {
        if (pedido instanceof PedidoComida) {
            sentencia.setBoolean(10, ((PedidoComida) pedido).isRequiereMochilaTermica());
        } else if (pedido instanceof PedidoExpress) {
            sentencia.setBoolean(10, ((PedidoExpress) pedido).isDisponibilidadInmediata());
        } else {
            sentencia.setNull(10, java.sql.Types.BOOLEAN);
        }
    }

    /**
     * Devuelve el texto, o uno de reemplazo si viene vacio desde la base.
     *
     * @param texto        valor leido
     * @param porDefecto   valor a usar si el anterior es nulo o vacio
     * @return texto utilizable
     */
    private String textoOPorDefecto(String texto, String porDefecto) {
        return (texto == null || texto.isBlank()) ? porDefecto : texto;
    }
}
