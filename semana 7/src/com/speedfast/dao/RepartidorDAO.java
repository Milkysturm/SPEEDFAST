package com.speedfast.dao;

import com.speedfast.modelo.Repartidor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Operaciones de base de datos sobre la tabla repartidor.
 *
 * @author Olga Rivas
 * @version 7.0
 */
public class RepartidorDAO {

    /** Consulta que trae todos los repartidores. */
    private static final String SQL_LISTAR =
            "SELECT id, nombre, vehiculo FROM repartidor ORDER BY nombre";

    /** Insercion de un repartidor nuevo. */
    private static final String SQL_INSERTAR =
            "INSERT INTO repartidor (nombre, vehiculo) VALUES (?, ?)";

    /**
     * Devuelve todos los repartidores guardados en la base de datos.
     *
     * Recorre el ResultSet armando un Repartidor por cada fila.
     *
     * @return lista de repartidores, vacia si no hay ninguno
     * @throws SQLException si falla la consulta
     */
    public List<Repartidor> listarTodos() throws SQLException {
        List<Repartidor> repartidores = new ArrayList<>();

        // try-with-resources: la conexion, la sentencia y el ResultSet se
        // cierran solos al salir del bloque, incluso si se lanza una excepcion.
        try (Connection conexion = ConexionBD.conectar();
             PreparedStatement sentencia = conexion.prepareStatement(SQL_LISTAR);
             ResultSet filas = sentencia.executeQuery()) {

            while (filas.next()) {
                repartidores.add(construirDesde(filas));
            }
        }
        return repartidores;
    }

    /**
     * Guarda un repartidor nuevo y le asigna el id que genero la base.
     *
     * @param repartidor repartidor a guardar
     * @throws SQLException             si falla la insercion
     * @throws IllegalArgumentException si el repartidor es nulo o no tiene nombre
     */
    public void guardar(Repartidor repartidor) throws SQLException {
        if (repartidor == null || repartidor.getNombre() == null
                || repartidor.getNombre().isBlank()) {
            throw new IllegalArgumentException("El repartidor debe tener un nombre.");
        }

        try (Connection conexion = ConexionBD.conectar();
             PreparedStatement sentencia = conexion.prepareStatement(
                     SQL_INSERTAR, Statement.RETURN_GENERATED_KEYS)) {

            sentencia.setString(1, repartidor.getNombre().trim());
            sentencia.setString(2, repartidor.getVehiculo());
            sentencia.executeUpdate();

            // La base genera el id sola (AUTO_INCREMENT); aca se recupera para
            // que el objeto en memoria quede con el mismo numero.
            try (ResultSet generado = sentencia.getGeneratedKeys()) {
                if (generado.next()) {
                    repartidor.setId(generado.getInt(1));
                }
            }
        }
    }

    /**
     * Arma un Repartidor con los datos de la fila actual del ResultSet.
     *
     * @param filas ResultSet posicionado en una fila
     * @return el repartidor de esa fila
     * @throws SQLException si falla la lectura de alguna columna
     */
    private Repartidor construirDesde(ResultSet filas) throws SQLException {
        String vehiculo = filas.getString("vehiculo");
        Repartidor repartidor = (vehiculo == null)
                ? new Repartidor(filas.getString("nombre"))
                : new Repartidor(filas.getString("nombre"), vehiculo);
        repartidor.setId(filas.getInt("id"));
        return repartidor;
    }
}
