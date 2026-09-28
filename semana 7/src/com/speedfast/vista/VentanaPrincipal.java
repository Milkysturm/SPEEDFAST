package com.speedfast.vista;

import com.speedfast.control.ControladorDePedidos;
import com.speedfast.dao.ConexionBD;
import com.speedfast.modelo.EstadoPedido;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;

/**
 * Ventana principal del sistema.
 *
 * Es la puerta de entrada de la aplicacion: muestra los botones que abren el
 * resto de las ventanas y una barra inferior con el resumen de los pedidos,
 * que se actualiza sola cuando los datos cambian.
 *
 * Al abrirse carga desde la base de datos los pedidos y repartidores ya
 * guardados, de modo que la aplicacion parte con la informacion real.
 *
 * @author Olga Rivas
 * @version 7.0
 */
public final class VentanaPrincipal extends JFrame {

    /** Version de la clase, requerida por ser serializable. */
    private static final long serialVersionUID = 1L;

    /** Controlador con los datos compartidos por todas las ventanas. */
    private final transient ControladorDePedidos controlador;

    /** Etiqueta con el resumen de pedidos. */
    private final JLabel resumen = new JLabel();

    /** Ventana de registro, se crea la primera vez que se abre. */
    private VentanaRegistroPedido ventanaRegistro;

    /** Ventana de listado, se crea la primera vez que se abre. */
    private VentanaListaPedidos ventanaLista;

    /** Ventana de entregas, se crea la primera vez que se abre. */
    private VentanaEntregas ventanaEntregas;

    /** Ventana de registro de repartidores, se crea la primera vez. */
    private VentanaRegistroRepartidor ventanaRepartidor;

    /**
     * Construye la ventana principal y carga los datos de la base.
     */
    public VentanaPrincipal() {
        this.controlador = new ControladorDePedidos();

        setTitle("SpeedFast - Sistema de gestion de entregas");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setSize(520, 320);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(crearTitulo(), BorderLayout.NORTH);
        add(crearPanelBotones(), BorderLayout.CENTER);
        add(crearBarraResumen(), BorderLayout.SOUTH);

        // Cada vez que cambian los datos se actualiza el resumen, siempre en
        // el hilo grafico porque el aviso puede venir de un hilo de reparto.
        controlador.agregarObservador(() -> SwingUtilities.invokeLater(this::actualizarResumen));

        cargarDatos();
        actualizarResumen();

        cerrarOrdenadamente();
        setVisible(true);
    }

    /**
     * Lee de la base de datos lo que ya estaba guardado.
     */
    private void cargarDatos() {
        try {
            controlador.cargarDesdeBaseDatos();
        } catch (SQLException e) {
            avisarErrorDeBase("No se pudieron leer los datos guardados.", e);
        }
    }

    /**
     * Crea el titulo que encabeza la ventana.
     *
     * @return etiqueta del titulo
     */
    private JLabel crearTitulo() {
        JLabel titulo = new JLabel("SpeedFast - Gestion de pedidos y entregas");
        titulo.setBorder(BorderFactory.createEmptyBorder(12, 14, 6, 14));
        return titulo;
    }

    /**
     * Crea el panel central con los botones de la aplicacion.
     *
     * @return panel con los botones
     */
    private JPanel crearPanelBotones() {
        JPanel panel = new JPanel(new GridLayout(4, 1, 0, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 30, 16, 30));

        JButton botonRegistrar = new JButton("Registrar pedido");
        botonRegistrar.addActionListener(e -> abrirRegistro());

        JButton botonListar = new JButton("Listar pedidos");
        botonListar.addActionListener(e -> abrirLista());

        JButton botonEntregas = new JButton("Asignar repartidor / Iniciar entrega");
        botonEntregas.addActionListener(e -> abrirEntregas());

        JButton botonRepartidor = new JButton("Registrar repartidor");
        botonRepartidor.addActionListener(e -> abrirRegistroRepartidor());

        panel.add(botonRegistrar);
        panel.add(botonListar);
        panel.add(botonEntregas);
        panel.add(botonRepartidor);
        return panel;
    }

    /**
     * Crea la barra inferior con el resumen de pedidos.
     *
     * @return panel del resumen
     */
    private JPanel crearBarraResumen() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(8, 14, 10, 14));
        panel.add(resumen, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Actualiza el texto del resumen con los totales actuales.
     */
    private void actualizarResumen() {
        resumen.setText("Registrados: " + controlador.getTotalPedidos()
                + "   |   Por asignar: " + controlador.contarPorEstado(EstadoPedido.RESERVADO)
                + "   |   En ruta: " + controlador.contarPorEstado(EstadoPedido.DESPACHADO)
                + "   |   Entregados: " + controlador.contarPorEstado(EstadoPedido.ENTREGADO));
    }

    /**
     * Muestra un aviso cuando falla una operacion contra la base de datos.
     *
     * @param queFallo descripcion de lo que se estaba haciendo
     * @param error    excepcion recibida
     */
    private void avisarErrorDeBase(String queFallo, SQLException error) {
        JOptionPane.showMessageDialog(this,
                queFallo + "\n\nDireccion: " + ConexionBD.getUrl()
                        + "\nDetalle: " + error.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
    }

    // ------------------------------------------------------------------
    // Navegacion
    // ------------------------------------------------------------------

    /**
     * Abre el formulario de registro de pedidos.
     *
     * Cada ventana se crea una sola vez y despues solo se muestra: al
     * cerrarla se oculta en lugar de destruirse, asi conserva su contenido y
     * no queda registrada dos veces como observadora del controlador.
     */
    private void abrirRegistro() {
        if (ventanaRegistro == null) {
            ventanaRegistro = new VentanaRegistroPedido(controlador);
        }
        ventanaRegistro.setVisible(true);
        ventanaRegistro.toFront();
    }

    /**
     * Abre la ventana con el listado de pedidos.
     */
    private void abrirLista() {
        if (ventanaLista == null) {
            ventanaLista = new VentanaListaPedidos(controlador);
        }
        ventanaLista.setVisible(true);
        ventanaLista.toFront();
    }

    /**
     * Abre la ventana de asignacion y simulacion de entregas.
     */
    private void abrirEntregas() {
        if (ventanaEntregas == null) {
            ventanaEntregas = new VentanaEntregas(controlador);
        }
        ventanaEntregas.setVisible(true);
        ventanaEntregas.toFront();
    }

    /**
     * Abre el formulario de registro de repartidores.
     */
    private void abrirRegistroRepartidor() {
        if (ventanaRepartidor == null) {
            ventanaRepartidor = new VentanaRegistroRepartidor(controlador);
        }
        ventanaRepartidor.setVisible(true);
        ventanaRepartidor.toFront();
    }

    /**
     * Al cerrar la ventana principal detiene las entregas en curso antes de
     * terminar, para no dejar hilos vivos.
     */
    private void cerrarOrdenadamente() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                controlador.detenerEntregas();
                dispose();
                System.exit(0);
            }
        });
    }

    /**
     * @return controlador con los datos del sistema
     */
    public ControladorDePedidos getControlador() {
        return controlador;
    }
}
