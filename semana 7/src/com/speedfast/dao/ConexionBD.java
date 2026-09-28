package com.speedfast.dao;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Entrega las conexiones a la base de datos speedfast_db.
 *
 * Los datos de conexion se leen del archivo db.properties si existe, y si no
 * se usan los valores por defecto. Asi la contrasena no queda escrita dentro
 * del codigo y quien ejecute el proyecto en otro computador solo cambia ese
 * archivo, sin recompilar.
 *
 * @author Olga Rivas
 * @version 7.0
 */
public final class ConexionBD {

    /** Archivo con los datos de conexion. */
    private static final String ARCHIVO = "db.properties";

    /** Direccion de la base de datos si no hay archivo de configuracion. */
    private static final String URL_POR_DEFECTO = "jdbc:mysql://localhost:3306/speedfast_db";

    /** Usuario de la base de datos si no hay archivo de configuracion. */
    private static final String USUARIO_POR_DEFECTO = "root";

    /** Contrasena si no hay archivo de configuracion. */
    private static final String CLAVE_POR_DEFECTO = "";

    /** Datos de conexion, leidos una sola vez. */
    private static Properties configuracion;

    /** Constructor privado: es una clase de utilidad y no se instancia. */
    private ConexionBD() {
    }

    /**
     * Abre una conexion nueva con la base de datos.
     *
     * Quien la pide es responsable de cerrarla. En este proyecto todas las
     * consultas usan try-with-resources, que la cierra sola al terminar el
     * bloque, incluso si se lanza una excepcion.
     *
     * @return conexion abierta
     * @throws SQLException si no se puede conectar
     */
    public static Connection conectar() throws SQLException {
        Properties datos = getConfiguracion();
        return DriverManager.getConnection(
                datos.getProperty("url", URL_POR_DEFECTO),
                datos.getProperty("usuario", USUARIO_POR_DEFECTO),
                datos.getProperty("clave", CLAVE_POR_DEFECTO));
    }

    /**
     * Comprueba que la base de datos responda. Se llama al iniciar la
     * aplicacion para avisar del problema antes de abrir las ventanas.
     *
     * @throws SQLException si no se puede conectar
     */
    public static void comprobarConexion() throws SQLException {
        try (Connection conexion = conectar()) {
            if (!conexion.isValid(5)) {
                throw new SQLException("La conexion se abrio pero no responde.");
            }
        }
    }

    /**
     * @return direccion de la base de datos que se esta usando
     */
    public static String getUrl() {
        return getConfiguracion().getProperty("url", URL_POR_DEFECTO);
    }

    /**
     * Lee db.properties la primera vez que se necesita.
     *
     * Se busca primero como archivo en la carpeta desde donde se ejecuta y
     * despues dentro del proyecto compilado, para que funcione tanto al
     * ejecutar desde IntelliJ como desde la terminal.
     *
     * @return datos de conexion, vacios si no hay archivo
     */
    private static synchronized Properties getConfiguracion() {
        if (configuracion != null) {
            return configuracion;
        }
        configuracion = new Properties();

        Path archivo = Path.of(ARCHIVO);
        if (Files.isReadable(archivo)) {
            try (InputStream entrada = Files.newInputStream(archivo)) {
                configuracion.load(entrada);
                return configuracion;
            } catch (IOException e) {
                // Si el archivo existe pero no se puede leer se siguen usando
                // los valores por defecto, en vez de impedir que arranque.
                System.err.println("No se pudo leer " + ARCHIVO + ": " + e.getMessage());
            }
        }

        try (InputStream entrada = ConexionBD.class.getClassLoader().getResourceAsStream(ARCHIVO)) {
            if (entrada != null) {
                configuracion.load(entrada);
            }
        } catch (IOException e) {
            System.err.println("No se pudo leer " + ARCHIVO + ": " + e.getMessage());
        }
        return configuracion;
    }
}
