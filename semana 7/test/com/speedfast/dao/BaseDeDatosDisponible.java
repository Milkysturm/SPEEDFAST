package com.speedfast.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Apoyo de las pruebas que necesitan la base de datos.
 *
 * Las pruebas de los DAO solo tienen sentido con MySQL encendido. Esta clase
 * permite preguntar si esta disponible, para omitirlas en vez de darlas por
 * falladas cuando se ejecutan en un computador sin la base levantada.
 *
 * @author Olga Rivas
 * @version 7.0
 */
final class BaseDeDatosDisponible {

    /** Constructor privado: es una clase de utilidad y no se instancia. */
    private BaseDeDatosDisponible() {
    }

    /**
     * Indica si la base de datos responde.
     *
     * @return true si se pudo conectar
     */
    static boolean hay() {
        try {
            ConexionBD.comprobarConexion();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Deja las tablas vacias, para que cada prueba parta de lo mismo.
     *
     * Se borra primero entrega porque sus llaves foraneas apuntan a las otras
     * dos tablas, y los repartidores se vuelven a cargar porque el sistema los
     * necesita para asignar.
     *
     * @throws SQLException si falla alguna sentencia
     */
    static void limpiar() throws SQLException {
        try (Connection conexion = ConexionBD.conectar();
             Statement sentencia = conexion.createStatement()) {

            sentencia.executeUpdate("DELETE FROM entrega");
            sentencia.executeUpdate("DELETE FROM pedido");
            sentencia.executeUpdate("DELETE FROM repartidor");
            sentencia.executeUpdate("INSERT INTO repartidor (nombre, vehiculo) VALUES"
                    + " ('Camila Soto', 'moto con mochila termica'),"
                    + " ('Diego Fuentes', 'furgon de carga'),"
                    + " ('Luis Vera', 'bicicleta electrica')");
        }
    }
}
