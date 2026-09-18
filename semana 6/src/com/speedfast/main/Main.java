package com.speedfast.main;

import com.speedfast.vista.Estilos;
import com.speedfast.vista.VentanaPrincipal;

import javax.swing.SwingUtilities;

/**
 * Punto de entrada de la aplicacion.
 *
 * Aplica el aspecto visual y abre la ventana principal. La creacion de la
 * interfaz se hace con SwingUtilities.invokeLater porque los componentes de
 * Swing deben construirse y usarse en el hilo grafico (Event Dispatch
 * Thread), no en el hilo main.
 *
 * @author Olga Rivas
 * @version 6.0
 */
public class Main {

    /**
     * Inicia la aplicacion.
     *
     * @param args no se utilizan
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Estilos.aplicarAspecto();
            new VentanaPrincipal();
        });
    }
}
