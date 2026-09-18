package com.speedfast.vista;

import com.speedfast.control.ControladorDePedidos;
import com.speedfast.modelo.EstadoPedido;
import com.speedfast.modelo.Pedido;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;

/**
 * Ventana que muestra los pedidos registrados en una tabla.
 *
 * La tabla se arma con un ModeloTablaPedidos, que extiende DefaultTableModel.
 * Se puede ordenar haciendo clic en el encabezado y filtrar por estado con el
 * combo de arriba. Ademas de refrescarse con el boton, se actualiza sola
 * cuando se registra un pedido nuevo o cuando un repartidor cambia el estado
 * de uno.
 *
 * @author Olga Rivas
 * @version 6.0
 */
public final class VentanaListaPedidos extends JFrame {

    /** Version de la clase, requerida por ser serializable. */
    private static final long serialVersionUID = 1L;

    /** Texto de la primera opcion del filtro. */
    private static final String TODOS = "Todos los pedidos";

    /** Controlador con los datos del sistema. */
    private final transient ControladorDePedidos controlador;

    /** Modelo que alimenta la tabla. */
    private final ModeloTablaPedidos modelo = new ModeloTablaPedidos();

    /** Tabla donde se muestran y se seleccionan los pedidos. */
    private final JTable tabla = new JTable(modelo);

    /** Combo que filtra la tabla por estado. */
    private final JComboBox<String> comboFiltro = new JComboBox<>();

    /** Etiqueta con la cantidad de pedidos mostrados. */
    private final JLabel contador = new JLabel();

    /**
     * Construye la ventana del listado.
     *
     * @param controlador controlador con los pedidos a mostrar
     */
    public VentanaListaPedidos(ControladorDePedidos controlador) {
        this.controlador = controlador;

        setTitle("SpeedFast - Listado de pedidos");
        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        setSize(1000, 460);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(Estilos.encabezado("Pedidos registrados",
                "Estado actual de todos los pedidos del sistema"), BorderLayout.NORTH);
        add(crearCentro(), BorderLayout.CENTER);
        add(crearBarraInferior(), BorderLayout.SOUTH);

        // La tabla se refresca sola ante cualquier cambio en los datos.
        controlador.agregarObservador(() -> SwingUtilities.invokeLater(this::refrescar));
        refrescar();
    }

    /**
     * Crea la zona central: el filtro arriba y la tabla debajo.
     *
     * @return panel central
     */
    private JPanel crearCentro() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Estilos.FONDO);
        panel.add(crearBarraFiltro(), BorderLayout.NORTH);

        Estilos.configurarTabla(tabla);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane contenedor = new JScrollPane(tabla);
        contenedor.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
        contenedor.getViewport().setBackground(Color.WHITE);
        panel.add(contenedor, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Crea la fila con el combo de filtro por estado.
     *
     * @return panel del filtro
     */
    private JPanel crearBarraFiltro() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 12));
        panel.setBackground(Estilos.FONDO);

        comboFiltro.addItem(TODOS);
        for (EstadoPedido estado : EstadoPedido.values()) {
            comboFiltro.addItem(estado.getDescripcion());
        }
        comboFiltro.setFont(Estilos.NORMAL);
        comboFiltro.addActionListener(e -> refrescar());

        panel.add(Estilos.etiqueta("Mostrar:"));
        panel.add(comboFiltro);
        return panel;
    }

    /**
     * Crea la barra inferior con el contador y el boton de refrescar.
     *
     * @return panel inferior
     */
    private JPanel crearBarraInferior() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xD8DDE4)));

        contador.setFont(Estilos.NORMAL);
        contador.setForeground(Estilos.GRIS);
        contador.setBorder(Estilos.margen(12));

        JButton botonCancelar = Estilos.boton("Cancelar pedido", false);
        botonCancelar.setPreferredSize(null);
        botonCancelar.addActionListener(e -> cancelarSeleccionado());

        JButton botonRefrescar = Estilos.boton("Refrescar", false);
        botonRefrescar.setPreferredSize(null);
        botonRefrescar.addActionListener(e -> refrescar());

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        derecha.setBackground(Color.WHITE);
        derecha.add(botonCancelar);
        derecha.add(botonRefrescar);

        panel.add(contador, BorderLayout.WEST);
        panel.add(derecha, BorderLayout.EAST);
        return panel;
    }

    /**
     * Cancela el pedido seleccionado, pidiendo confirmacion y motivo.
     *
     * Quien decide si el pedido se puede cancelar es el modelo: un pedido que
     * ya va en ruta rechaza la cancelacion y la ventana solo avisa.
     */
    private void cancelarSeleccionado() {
        int filaVista = tabla.getSelectedRow();
        if (filaVista < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona primero un pedido en la tabla.",
                    "Falta seleccionar", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String id = modelo.getIdEn(tabla.convertRowIndexToModel(filaVista));
        Pedido pedido = controlador.buscarPorId(id);
        if (pedido == null) {
            return;
        }

        if (pedido.getEstado() == EstadoPedido.CANCELADO) {
            JOptionPane.showMessageDialog(this, "El pedido " + id + " ya estaba cancelado.",
                    "Nada que hacer", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int respuesta = JOptionPane.showConfirmDialog(this,
                "Vas a cancelar el pedido " + id + " con destino a\n" + pedido.getDireccionEntrega()
                        + "\n\nEsta accion no se puede deshacer. Continuar?",
                "Confirmar cancelacion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (respuesta != JOptionPane.YES_OPTION) {
            return;
        }

        String motivo = JOptionPane.showInputDialog(this,
                "Motivo de la cancelacion:", "Cancelar pedido " + id, JOptionPane.QUESTION_MESSAGE);
        if (motivo == null) {
            return;
        }
        if (motivo.isBlank()) {
            motivo = "sin motivo indicado";
        }

        if (controlador.cancelarPedido(pedido, motivo)) {
            JOptionPane.showMessageDialog(this,
                    "Pedido " + id + " cancelado.\nMotivo: " + motivo,
                    "Pedido cancelado", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                    "El pedido " + id + " ya va en ruta y no se puede cancelar.",
                    "No se puede cancelar", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Vuelve a cargar la tabla aplicando el filtro elegido.
     */
    private void refrescar() {
        int opcion = comboFiltro.getSelectedIndex();
        if (opcion <= 0) {
            modelo.cargar(controlador.getPedidos());
            contador.setText("Mostrando " + modelo.getRowCount() + " pedido(s)");
            return;
        }

        EstadoPedido estado = EstadoPedido.values()[opcion - 1];
        modelo.cargar(controlador.getPedidosPorEstado(estado));
        contador.setText("Mostrando " + modelo.getRowCount() + " de "
                + controlador.getTotalPedidos() + " pedido(s)");
    }
}
