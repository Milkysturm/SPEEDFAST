package com.speedfast.vista;

import com.speedfast.control.ControladorDePedidos;
import com.speedfast.modelo.Repartidor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;

/**
 * Formulario para registrar un repartidor nuevo en la base de datos.
 *
 * Pide el nombre y el vehiculo, valida que el nombre no venga vacio y guarda
 * la fila en la tabla repartidor. El repartidor queda disponible de inmediato
 * en el combo de la ventana de entregas.
 *
 * @author Olga Rivas
 * @version 7.0
 */
public final class VentanaRegistroRepartidor extends JFrame {

    /** Version de la clase, requerida por ser serializable. */
    private static final long serialVersionUID = 1L;

    /** Controlador donde se registran los repartidores. */
    private final transient ControladorDePedidos controlador;

    /** Nombre del repartidor. */
    private final JTextField campoNombre = new JTextField(18);

    /** Vehiculo con el que reparte. */
    private final JTextField campoVehiculo = new JTextField(18);

    /**
     * Construye el formulario.
     *
     * @param controlador controlador donde se agregaran los repartidores
     */
    public VentanaRegistroRepartidor(ControladorDePedidos controlador) {
        this.controlador = controlador;

        setTitle("SpeedFast - Registrar repartidor");
        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        setSize(400, 180);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(crearFormulario(), BorderLayout.CENTER);
        add(crearPanelBotones(), BorderLayout.SOUTH);
    }

    /**
     * Arma el formulario con sus dos campos.
     *
     * @return panel del formulario
     */
    private JPanel crearFormulario() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 14, 8, 14));

        panel.add(new JLabel("Nombre:"));
        panel.add(campoNombre);
        panel.add(new JLabel("Vehiculo:"));
        panel.add(campoVehiculo);
        return panel;
    }

    /**
     * Crea la fila de botones inferior.
     *
     * @return panel con los botones
     */
    private JPanel crearPanelBotones() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));

        JButton botonLimpiar = new JButton("Limpiar");
        botonLimpiar.addActionListener(e -> limpiar());

        JButton botonGuardar = new JButton("Guardar repartidor");
        botonGuardar.addActionListener(e -> guardar());

        panel.add(botonLimpiar);
        panel.add(botonGuardar);
        return panel;
    }

    /**
     * Valida los datos y guarda el repartidor en la base de datos.
     */
    private void guardar() {
        String nombre = campoNombre.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debes indicar el nombre del repartidor.",
                    "Revisa los datos", JOptionPane.WARNING_MESSAGE);
            campoNombre.requestFocusInWindow();
            return;
        }

        String vehiculo = campoVehiculo.getText().trim();
        Repartidor repartidor = vehiculo.isEmpty()
                ? new Repartidor(nombre)
                : new Repartidor(nombre, vehiculo);

        try {
            controlador.agregarRepartidor(repartidor);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "No se pudo guardar el repartidor en la base de datos.\n\nDetalle: "
                            + e.getMessage(),
                    "Error de base de datos", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(this,
                "Repartidor guardado con el id " + repartidor.getId() + ".\n" + repartidor,
                "Repartidor registrado", JOptionPane.INFORMATION_MESSAGE);
        limpiar();
    }

    /**
     * Deja el formulario listo para un repartidor nuevo.
     */
    private void limpiar() {
        campoNombre.setText("");
        campoVehiculo.setText("");
        campoNombre.requestFocusInWindow();
    }
}
