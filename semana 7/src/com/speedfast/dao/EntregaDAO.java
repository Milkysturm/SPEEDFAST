package com.speedfast.dao;

import com.speedfast.modelo.Entrega;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Operaciones de base de datos sobre la tabla entrega.
 *
 * Es la tabla que relaciona pedidos con repartidores, asi que cada fila
 * guardada deja registrado quien entrego que pedido y cuando.
 *
 * @author Olga Rivas
 * @version 7.0
 */
public class EntregaDAO {

    /** Insercion de una entrega nueva. */
    private static final String SQL_INSERTAR =
            "INSERT INTO entrega (id_pedido, id_repartidor, fecha, hora) VALUES (?, ?, ?, ?)";

    /** Consulta de todas las entregas, de la mas reciente a la mas antigua. */
    private static final String SQL_LISTAR =
            "SELECT id, id_pedido, id_repartidor, fecha, hora"
            + " FROM entrega ORDER BY fecha DESC, hora DESC";

    /**
     * Guarda una entrega y le asigna el id que genero MySQL.
     *
     * La base rechaza la fila si el pedido o el repartidor no existen: esa es
     * la comprobacion que hacen las llaves foraneas.
     *
     * @param entrega entrega a guardar
     * @throws SQLException             si falla la insercion
     * @throws IllegalArgumentException si la entrega es nula o le faltan datos
     */
    public void guardar(Entrega entrega) throws SQLException {
        if (entrega == null) {
            throw new IllegalArgumentException("La entrega no puede ser nula.");
        }
        if (entrega.getIdPedido() <= 0 || entrega.getIdRepartidor() <= 0) {
            throw new IllegalArgumentException(
                    "La entrega necesita un pedido y un repartidor ya guardados.");
        }
        if (entrega.getFecha() == null || entrega.getHora() == null) {
            throw new IllegalArgumentException("La entrega necesita fecha y hora.");
        }

        try (Connection conexion = ConexionBD.conectar();
             PreparedStatement sentencia = conexion.prepareStatement(
                     SQL_INSERTAR, Statement.RETURN_GENERATED_KEYS)) {

            sentencia.setInt(1, entrega.getIdPedido());
            sentencia.setInt(2, entrega.getIdRepartidor());
            sentencia.setDate(3, java.sql.Date.valueOf(entrega.getFecha()));
            sentencia.setTime(4, java.sql.Time.valueOf(entrega.getHora()));
            sentencia.executeUpdate();

            try (ResultSet generado = sentencia.getGeneratedKeys()) {
                if (generado.next()) {
                    entrega.setId(generado.getInt(1));
                }
            }
        }
    }

    /**
     * Devuelve todas las entregas registradas.
     *
     * @return lista de entregas, vacia si no hay ninguna
     * @throws SQLException si falla la consulta
     */
    public List<Entrega> listarTodas() throws SQLException {
        List<Entrega> entregas = new ArrayList<>();

        try (Connection conexion = ConexionBD.conectar();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_LISTAR);
             ResultSet filas = sentencia.executeQuery()) {

            while (filas.next()) {
                Entrega entrega = new Entrega(
                        filas.getInt("id_pedido"),
                        filas.getInt("id_repartidor"),
                        filas.getDate("fecha").toLocalDate(),
                        filas.getTime("hora").toLocalTime());
                entrega.setId(filas.getInt("id"));
                entregas.add(entrega);
            }
        }
        return entregas;
    }
}
