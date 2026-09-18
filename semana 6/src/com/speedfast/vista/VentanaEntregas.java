package com.speedfast.vista;

import com.speedfast.contrato.EscuchaDeEntregas;
import com.speedfast.control.ControladorDePedidos;
import com.speedfast.modelo.EstadoPedido;
import com.speedfast.modelo.Pedido;
import com.speedfast.modelo.Repartidor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Ventana de asignacion de repartidores y simulacion de entregas.
 *
 * Permite elegir un pedido de la tabla, asignarle un repartidor y lanzar la
 * simulacion. Cada repartidor entrega en su propio hilo, y esta ventana va
 * mostrando el avance.
 *
 * Implementa EscuchaDeEntregas para recibir los avisos de los repartidores.
 * Como esos avisos llegan desde los hilos de reparto y no desde el hilo
 * grafico, cada actualizacion se envia con SwingUtilities.invokeLater: en
 * Swing los componentes solo pueden tocarse desde el Event Dispatch Thread.
 *
 * @author Olga Rivas
 * @version 6.0
 */
public final class VentanaEntregas extends JFrame implements EscuchaDeEntregas {

    /** Version de la clase, requerida por ser serializable. */
    private static final long serialVersionUID = 1L;

    /** Formato de la hora que encabeza cada linea del avance. */
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Controlador con los datos del sistema. */
    private final transient ControladorDePedidos controlador;

    /** Modelo que alimenta la tabla de pedidos. */
    private final ModeloTablaPedidos modelo = new ModeloTablaPedidos();

    /** Tabla donde se elige el pedido a asignar. */
    private final JTable tabla = new JTable(modelo);

    /** Combo con los repartidores disponibles. */
    private final JComboBox<Repartidor> comboRepartidores = new JComboBox<>();

    /** Area donde se muestra el avance de la simulacion. */
    private final JTextArea avance = new JTextArea();

    /** Boton que lanza la simulacion. */
    private final JButton botonIniciar = Estilos.boton("Iniciar entregas", true);

    /** Boton que asigna el repartidor elegido al pedido seleccionado. */
    private final JButton botonAsignar = Estilos.boton("Asignar al pedido seleccionado", false);

    /** Etiqueta con el estado de la simulacion. */
    private final JLabel estado = new JLabel("Sin entregas en curso");

    /** Rutas que todavia no terminan. */
    private int rutasPendientes;

    /**
     * Construye la ventana de entregas.
     *
     * @param controlador controlador con los pedidos y repartidores
     */
    public VentanaEntregas(ControladorDePedidos controlador) {
        this.controlador = controlador;

        setTitle("SpeedFast - Asignar repartidor e iniciar entregas");
        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        setSize(1000, 620);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        for (Repartidor repartidor : controlador.getRepartidores()) {
            comboRepartidores.addItem(repartidor);
        }

        add(Estilos.encabezado("Asignar repartidor e iniciar entregas",
                "Elige un pedido, asignale un repartidor y lanza la simulacion"), BorderLayout.NORTH);
        add(crearCentro(), BorderLayout.CENTER);
        add(crearBarraInferior(), BorderLayout.SOUTH);

        controlador.agregarObservador(() -> SwingUtilities.invokeLater(this::refrescar));
        refrescar();
        registrar("Listo. Asigna repartidores y presiona Iniciar entregas.");
    }

    /**
     * Arma la zona central: la tabla arriba y el avance abajo.
     *
     * @return componente central
     */
    private JSplitPane crearCentro() {
        JPanel arriba = new JPanel(new BorderLayout());
        arriba.setBackground(Estilos.FONDO);
        arriba.add(crearBarraAsignacion(), BorderLayout.NORTH);

        Estilos.configurarTabla(tabla);
        tabla.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        JScrollPane contenedorTabla = new JScrollPane(tabla);
        contenedorTabla.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        arriba.add(contenedorTabla, BorderLayout.CENTER);

        avance.setEditable(false);
        avance.setFont(Estilos.MONO);
        avance.setBackground(new Color(0x1B2430));
        avance.setForeground(new Color(0xD7E3F0));
        avance.setBorder(Estilos.margen(8));

        JScrollPane contenedorAvance = new JScrollPane(avance);
        contenedorAvance.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));

        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT, arriba, contenedorAvance);
        division.setDividerLocation(300);
        division.setBorder(null);
        return division;
    }

    /**
     * Crea la fila con el combo de repartidores y el boton de asignar.
     *
     * @return panel de asignacion
     */
    private JPanel crearBarraAsignacion() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 12));
        panel.setBackground(Estilos.FONDO);

        comboRepartidores.setFont(Estilos.NORMAL);
        botonAsignar.setPreferredSize(null);
        botonAsignar.addActionListener(e -> asignar());

        panel.add(Estilos.etiqueta("Repartidor:"));
        panel.add(comboRepartidores);
        panel.add(botonAsignar);
        return panel;
    }

    /**
     * Crea la barra inferior con el estado y el boton de iniciar.
     *
     * @return panel inferior
     */
    private JPanel crearBarraInferior() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xD8DDE4)));

        estado.setFont(Estilos.NORMAL);
        estado.setForeground(Estilos.GRIS);
        estado.setBorder(Estilos.margen(12));

        botonIniciar.setPreferredSize(null);
        botonIniciar.addActionListener(e -> iniciar());

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        derecha.setBackground(Color.WHITE);
        derecha.add(botonIniciar);

        panel.add(estado, BorderLayout.WEST);
        panel.add(derecha, BorderLayout.EAST);
        return panel;
    }

    // ------------------------------------------------------------------
    // Acciones
    // ------------------------------------------------------------------

    /**
     * Asigna el repartidor elegido al pedido seleccionado en la tabla.
     */
    private void asignar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona primero un pedido en la tabla.",
                    "Falta seleccionar", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String id = modelo.getIdEn(tabla.convertRowIndexToModel(fila));
        Pedido pedido = controlador.buscarPorId(id);
        Repartidor repartidor = (Repartidor) comboRepartidores.getSelectedItem();

        if (pedido == null || repartidor == null) {
            return;
        }
        if (pedido.getEstado() != EstadoPedido.RESERVADO) {
            JOptionPane.showMessageDialog(this,
                    "El pedido " + id + " ya no esta disponible: " + pedido.getEstado().getDescripcion() + ".",
                    "No se puede asignar", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (controlador.asignarRepartidor(pedido, repartidor)) {
            registrar("Pedido " + id + " asignado a " + repartidor.getNombre() + ".");
        }
    }

    /**
     * Lanza la simulacion de entregas en segundo plano.
     */
    private void iniciar() {
        if (controlador.hayEntregasEnCurso()) {
            JOptionPane.showMessageDialog(this, "Ya hay una simulacion en curso.",
                    "Espera un momento", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int rutas = controlador.iniciarEntregas(this);
        if (rutas == 0) {
            JOptionPane.showMessageDialog(this,
                    "Ningun repartidor tiene pedidos asignados.\nAsigna al menos uno antes de iniciar.",
                    "Nada que entregar", JOptionPane.WARNING_MESSAGE);
            return;
        }

        rutasPendientes = rutas;
        botonIniciar.setEnabled(false);
        botonAsignar.setEnabled(false);
        estado.setText("Entregas en curso: " + rutas + " repartidor(es) en ruta");
        registrar("--- Comienza la simulacion con " + rutas + " repartidor(es) ---");
    }

    // ------------------------------------------------------------------
    // EscuchaDeEntregas: llega desde los hilos de reparto
    // ------------------------------------------------------------------

    /**
     * Recibe el aviso de que un pedido cambio de estado.
     *
     * @param pedido  pedido afectado
     * @param mensaje descripcion de lo ocurrido
     */
    @Override
    public void pedidoActualizado(Pedido pedido, String mensaje) {
        SwingUtilities.invokeLater(() -> {
            registrar("Pedido " + pedido.getIdPedido() + ": " + mensaje);
            controlador.notificarCambio();
        });
    }

    /**
     * Recibe el aviso de que un repartidor termino su ruta.
     *
     * @param nombreRepartidor nombre del repartidor
     * @param entregas         cantidad de entregas completadas
     */
    @Override
    public void rutaTerminada(String nombreRepartidor, int entregas) {
        SwingUtilities.invokeLater(() -> {
            registrar(nombreRepartidor + " termino su ruta con " + entregas + " entrega(s).");
            rutasPendientes--;
            if (rutasPendientes <= 0) {
                botonIniciar.setEnabled(true);
                botonAsignar.setEnabled(true);
                estado.setText("Simulacion terminada");
                registrar("--- Todas las rutas terminaron ---");
            }
            controlador.notificarCambio();
        });
    }

    // ------------------------------------------------------------------
    // Apoyo
    // ------------------------------------------------------------------

    /**
     * Vuelve a cargar la tabla conservando el pedido que estaba seleccionado.
     *
     * La seleccion se recuerda por identificador y no por numero de fila,
     * porque con la tabla ordenada la fila puede cambiar de lugar.
     */
    private void refrescar() {
        int filaVista = tabla.getSelectedRow();
        String seleccionado = (filaVista < 0)
                ? null : modelo.getIdEn(tabla.convertRowIndexToModel(filaVista));

        modelo.cargar(controlador.getPedidos());

        if (seleccionado != null) {
            int filaModelo = modelo.buscarFila(seleccionado);
            if (filaModelo >= 0) {
                int nuevaFila = tabla.convertRowIndexToView(filaModelo);
                if (nuevaFila >= 0) {
                    tabla.setRowSelectionInterval(nuevaFila, nuevaFila);
                }
            }
        }
    }

    /**
     * Agrega una linea al area de avance, con la hora por delante.
     *
     * @param mensaje texto a mostrar
     */
    private void registrar(String mensaje) {
        avance.append("[" + LocalTime.now().format(HORA) + "] " + mensaje + "\n");
        avance.setCaretPosition(avance.getDocument().getLength());
    }
}
