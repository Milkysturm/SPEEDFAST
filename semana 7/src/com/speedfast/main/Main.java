package com.speedfast.main;

import com.speedfast.dao.ConexionBD;
import com.speedfast.vista.VentanaPrincipal;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.sql.SQLException;

/**
 * Punto de entrada de la aplicacion.
 *
 * Antes de abrir las ventanas comprueba que la base de datos responda, porque
 * sin ella la aplicacion no tiene nada que mostrar ni donde guardar. Si no
 * conecta, avisa indicando a que direccion intento conectarse.
 *
 * La creacion de la interfaz se hace con SwingUtilities.invokeLater porque los
 * componentes de Swing deben construirse y usarse en el hilo grafico (Event
 * Dispatch Thread), no en el hilo main.
 *
 * @author Olga Rivas
 * @version 7.0
 */
public class Main {

    /**
     * Inicia la aplicacion.
     *
     * @param args no se utilizan
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            if (hayBaseDeDatos()) {
                new VentanaPrincipal();
            }
        });
    }

    /**
     * Comprueba la conexion y avisa si no hay base de datos disponible.
     *
     * @return true si se puede trabajar
     */
    private static boolean hayBaseDeDatos() {
        try {
            ConexionBD.comprobarConexion();
            return true;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "No se pudo conectar con la base de datos.\n\n"
                            + "Direccion: " + ConexionBD.getUrl() + "\n"
                            + "Detalle: " + e.getMessage() + "\n\n"
                            + "Revisa que el servidor MySQL este encendido y que los datos\n"
                            + "de db.properties sean los correctos.",
                    "Sin conexion a la base de datos", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }
}
