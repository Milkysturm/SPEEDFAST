package com.speedfast.vista;

import com.speedfast.control.ControladorDePedidos;
import com.speedfast.modelo.EstadoPedido;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Ventana principal del sistema.
 *
 * Es la puerta de entrada de la aplicacion: muestra los tres botones que
 * abren el resto de las ventanas y una barra inferior con el resumen de los
 * pedidos, que se actualiza sola cuando los datos cambian.
 *
 * Usa BorderLayout: el encabezado arriba, los botones al centro en un
 * GridLayout de una columna, y el resumen abajo.
 *
 * @author Olga Rivas
 * @version 6.0
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

    /**
     * Construye la ventana principal y crea el controlador del sistema.
     */
    public VentanaPrincipal() {
        this.controlador = new ControladorDePedidos();

        setTitle("SpeedFast - Sistema de gestion de entregas");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setSize(560, 420);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(Estilos.encabezado("SpeedFast",
                "Gestion de pedidos y entregas a domicilio"), BorderLayout.NORTH);
        add(crearPanelBotones(), BorderLayout.CENTER);
        add(crearBarraResumen(), BorderLayout.SOUTH);

        // Cada vez que cambian los datos se actualiza el resumen, siempre en
        // el hilo grafico porque el aviso puede venir de un hilo de reparto.
        controlador.agregarObservador(() -> SwingUtilities.invokeLater(this::actualizarResumen));
        actualizarResumen();

        cerrarOrdenadamente();
        setVisible(true);
    }

    /**
     * Crea el panel central con los tres botones de la aplicacion.
     *
     * @return panel con los botones
     */
    private JPanel crearPanelBotones() {
        JPanel panel = new JPanel(new GridLayout(3, 1, 0, 14));
        panel.setBackground(Estilos.FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(28, 60, 28, 60));

        JButton botonRegistrar = Estilos.boton("Registrar pedido", true);
        botonRegistrar.addActionListener(e -> abrirRegistro());

        JButton botonListar = Estilos.boton("Listar pedidos", false);
        botonListar.addActionListener(e -> abrirLista());

        JButton botonEntregas = Estilos.boton("Asignar repartidor / Iniciar entrega", false);
        botonEntregas.addActionListener(e -> abrirEntregas());

        panel.add(botonRegistrar);
        panel.add(botonListar);
        panel.add(botonEntregas);
        return panel;
    }

    /**
     * Crea la barra inferior con el resumen de pedidos.
     *
     * @return panel del resumen
     */
    private JPanel crearBarraResumen() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xD8DDE4)),
                Estilos.margen(10)));

        resumen.setFont(Estilos.NORMAL);
        resumen.setForeground(Estilos.GRIS);
        panel.add(resumen, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Actualiza el texto del resumen con los totales actuales.
     */
    private void actualizarResumen() {
        resumen.setText("Pedidos registrados: " + controlador.getTotalPedidos()
                + "     Por asignar: " + controlador.contarPorEstado(EstadoPedido.RESERVADO)
                + "     En ruta: " + controlador.contarPorEstado(EstadoPedido.DESPACHADO)
                + "     Entregados: " + controlador.contarPorEstado(EstadoPedido.ENTREGADO));
    }

    // ------------------------------------------------------------------
    // Navegacion
    // ------------------------------------------------------------------

    /**
     * Abre la ventana de registro de pedidos.
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
