package com.speedfast.vista;

import com.speedfast.control.ControladorDePedidos;
import com.speedfast.modelo.Comuna;
import com.speedfast.modelo.Pedido;
import com.speedfast.modelo.PedidoComida;
import com.speedfast.modelo.PedidoEncomienda;
import com.speedfast.modelo.PedidoExpress;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;

/**
 * Formulario de registro de pedidos.
 *
 * Pide los datos comunes a todo pedido -identificador, calle, comuna, tipo y
 * distancia- y, segun el tipo elegido en el combo, muestra los campos propios
 * de ese tipo mediante un CardLayout. Al elegir la comuna se propone la
 * distancia correspondiente. Al guardar valida todos los campos y solo
 * entonces crea el pedido y lo agrega al controlador.
 *
 * @author Olga Rivas
 * @version 6.0
 */
public final class VentanaRegistroPedido extends JFrame {

    /** Version de la clase, requerida por ser serializable. */
    private static final long serialVersionUID = 1L;

    /** Distancia maxima aceptada, en kilometros. */
    private static final double DISTANCIA_MAXIMA_KM = 500;

    /** Peso maximo aceptado, en kilogramos. */
    private static final double PESO_MAXIMO_KG = 1000;

    /** Nombres de los tipos que se muestran en el combo. */
    private static final String COMIDA = "Comida";
    private static final String ENCOMIENDA = "Encomienda";
    private static final String EXPRESS = "Compra Express";

    /** Controlador donde se registran los pedidos. */
    private final transient ControladorDePedidos controlador;

    /** Campos comunes a todos los tipos. */
    private final JTextField campoId = new JTextField(18);
    private final JTextField campoCalle = new JTextField(18);
    private final JComboBox<Comuna> comboComuna = new JComboBox<>(Comuna.values());
    private final JComboBox<String> comboTipo = new JComboBox<>(new String[]{COMIDA, ENCOMIENDA, EXPRESS});
    private final JTextField campoDistancia = new JTextField(18);

    /** Campos propios del pedido de comida. */
    private final JTextField campoRestaurante = new JTextField(18);
    private final JCheckBox checkMochila = new JCheckBox("Requiere mochila termica");

    /** Campos propios de la encomienda. */
    private final JTextField campoPeso = new JTextField(18);
    private final JTextField campoEmbalaje = new JTextField(18);

    /** Campos propios de la compra express. */
    private final JTextField campoLocal = new JTextField(18);
    private final JCheckBox checkDisponibilidad = new JCheckBox("Repartidor disponible de inmediato");

    /** Panel que cambia de contenido segun el tipo elegido. */
    private final JPanel panelEspecifico = new JPanel(new CardLayout());

    /**
     * Construye el formulario de registro.
     *
     * @param controlador controlador donde se agregaran los pedidos
     */
    public VentanaRegistroPedido(ControladorDePedidos controlador) {
        this.controlador = controlador;

        setTitle("SpeedFast - Registrar pedido");
        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        setSize(470, 350);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(crearFormulario(), BorderLayout.CENTER);
        add(crearPanelBotones(), BorderLayout.SOUTH);

        comboTipo.addActionListener(e -> mostrarCamposDelTipo());
        comboComuna.addActionListener(e -> proponerDistancia());
        mostrarCamposDelTipo();
        campoId.setText(controlador.sugerirIdPedido());
        proponerDistancia();
    }

    /**
     * Escribe en el campo de distancia la que corresponde a la comuna
     * elegida. El valor queda editable, por si el destino esta mas lejos o
     * mas cerca de lo habitual dentro de la misma comuna.
     */
    private void proponerDistancia() {
        Comuna comuna = (Comuna) comboComuna.getSelectedItem();
        if (comuna != null) {
            campoDistancia.setText(String.valueOf(comuna.getDistanciaKm()));
        }
    }

    /**
     * Al volver a mostrar la ventana propone un identificador libre, salvo
     * que haya uno escrito a mano que todavia sirva.
     *
     * @param visible true para mostrar la ventana
     */
    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            String actual = campoId.getText().trim();
            if (actual.isEmpty() || controlador.existeId(actual)) {
                campoId.setText(controlador.sugerirIdPedido());
            }
        }
        super.setVisible(visible);
    }

    /**
     * Arma el formulario con los campos comunes y el panel que cambia.
     *
     * @return panel del formulario
     */
    private JPanel crearFormulario() {
        JPanel campos = new JPanel(new GridLayout(5, 2, 8, 8));
        campos.setBorder(BorderFactory.createEmptyBorder(14, 14, 8, 14));

        campos.add(new JLabel("ID del pedido:"));
        campos.add(campoId);
        campos.add(new JLabel("Calle y numero:"));
        campos.add(campoCalle);
        campos.add(new JLabel("Comuna:"));
        campos.add(comboComuna);
        campos.add(new JLabel("Tipo de pedido:"));
        campos.add(comboTipo);
        campos.add(new JLabel("Distancia (km):"));
        campos.add(campoDistancia);

        armarPanelEspecifico();

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(campos, BorderLayout.NORTH);
        panel.add(panelEspecifico, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Prepara las tres tarjetas del CardLayout, una por tipo de pedido.
     */
    private void armarPanelEspecifico() {
        panelEspecifico.setBorder(BorderFactory.createTitledBorder("Datos del tipo de pedido"));
        panelEspecifico.add(tarjeta("Restaurante:", campoRestaurante, checkMochila), COMIDA);
        panelEspecifico.add(tarjeta("Peso (kg):", campoPeso, "Tipo de embalaje:", campoEmbalaje), ENCOMIENDA);
        panelEspecifico.add(tarjeta("Local:", campoLocal, checkDisponibilidad), EXPRESS);
    }

    /**
     * Crea una tarjeta con un campo de texto y una casilla de verificacion.
     *
     * @param texto   etiqueta del campo
     * @param campo   campo de texto
     * @param casilla casilla de verificacion
     * @return panel de la tarjeta
     */
    private JPanel tarjeta(String texto, JTextField campo, JCheckBox casilla) {
        JPanel filaCasilla = new JPanel(new GridLayout(1, 2, 8, 0));
        filaCasilla.add(new JLabel(""));
        filaCasilla.add(casilla);

        JPanel contenido = new JPanel(new GridLayout(2, 1, 0, 6));
        contenido.add(fila(texto, campo));
        contenido.add(filaCasilla);
        return enCaja(contenido);
    }

    /**
     * Crea una tarjeta con dos campos de texto.
     *
     * @param texto1 etiqueta del primer campo
     * @param campo1 primer campo
     * @param texto2 etiqueta del segundo campo
     * @param campo2 segundo campo
     * @return panel de la tarjeta
     */
    private JPanel tarjeta(String texto1, JTextField campo1, String texto2, JTextField campo2) {
        JPanel contenido = new JPanel(new GridLayout(2, 1, 0, 6));
        contenido.add(fila(texto1, campo1));
        contenido.add(fila(texto2, campo2));
        return enCaja(contenido);
    }

    /**
     * Crea una fila con su etiqueta a la izquierda y su campo a la derecha.
     *
     * @param texto etiqueta de la fila
     * @param campo campo de texto
     * @return panel de la fila
     */
    private JPanel fila(String texto, JTextField campo) {
        JPanel panel = new JPanel(new GridLayout(1, 2, 8, 0));
        panel.add(new JLabel(texto));
        panel.add(campo);
        return panel;
    }

    /**
     * Deja el contenido de una tarjeta pegado arriba, para que los campos
     * conserven su alto natural en vez de estirarse.
     *
     * @param contenido campos de la tarjeta
     * @return panel de la tarjeta
     */
    private JPanel enCaja(JPanel contenido) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panel.add(contenido, BorderLayout.NORTH);
        return panel;
    }

    /**
     * Muestra la tarjeta que corresponde al tipo seleccionado en el combo.
     */
    private void mostrarCamposDelTipo() {
        CardLayout distribucion = (CardLayout) panelEspecifico.getLayout();
        distribucion.show(panelEspecifico, (String) comboTipo.getSelectedItem());
    }

    /**
     * Crea la fila de botones inferior.
     *
     * @return panel con los botones
     */
    private JPanel crearPanelBotones() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));

        JButton botonLimpiar = new JButton("Limpiar");
        botonLimpiar.addActionListener(e -> limpiarFormulario());

        JButton botonGuardar = new JButton("Guardar pedido");
        botonGuardar.addActionListener(e -> guardar());

        panel.add(botonLimpiar);
        panel.add(botonGuardar);
        return panel;
    }

    // ------------------------------------------------------------------
    // Guardado y validacion
    // ------------------------------------------------------------------

    /**
     * Valida los datos y, si estan correctos, crea el pedido y lo registra.
     */
    private void guardar() {
        String id = campoId.getText().trim();
        String calle = campoCalle.getText().trim();
        Comuna comuna = (Comuna) comboComuna.getSelectedItem();

        if (id.isEmpty()) {
            avisar("Debes indicar el ID del pedido.", campoId);
            return;
        }
        if (id.contains(" ")) {
            avisar("El ID no puede llevar espacios. Por ejemplo: P-010", campoId);
            return;
        }
        if (controlador.existeId(id)) {
            avisar("Ya existe un pedido con el ID " + id + ".", campoId);
            return;
        }
        if (calle.isEmpty()) {
            avisar("Debes indicar la calle y el numero.", campoCalle);
            return;
        }
        if (comuna == null) {
            avisar("Debes elegir la comuna de entrega.", comboComuna);
            return;
        }

        // La direccion que se guarda en el pedido junta las dos partes.
        String direccion = calle + ", " + comuna.getNombre();

        double distancia = leerNumero(campoDistancia, "la distancia");
        if (Double.isNaN(distancia)) {
            return;
        }
        if (distancia < 0) {
            avisar("La distancia no puede ser negativa.", campoDistancia);
            return;
        }
        if (distancia > DISTANCIA_MAXIMA_KM) {
            avisar("La distancia no puede superar los " + (int) DISTANCIA_MAXIMA_KM + " km.",
                    campoDistancia);
            return;
        }

        Pedido pedido = crearPedido(id, direccion, distancia);
        if (pedido == null) {
            return;
        }

        try {
            controlador.agregarPedido(pedido);
        } catch (IllegalArgumentException e) {
            avisar(e.getMessage(), campoId);
            return;
        }

        JOptionPane.showMessageDialog(this,
                "Pedido " + id + " registrado correctamente.\nTipo: " + pedido.getTipoEntrega()
                        + "\nTiempo estimado: " + pedido.calcularTiempoEntrega() + " min",
                "Pedido registrado", JOptionPane.INFORMATION_MESSAGE);

        limpiarFormulario();
    }

    /**
     * Crea el pedido de la subclase que corresponde al tipo elegido.
     *
     * @param id        identificador del pedido
     * @param direccion direccion de entrega
     * @param distancia distancia en kilometros
     * @return el pedido creado, o null si falta algun dato del tipo
     */
    private Pedido crearPedido(String id, String direccion, double distancia) {
        String tipo = (String) comboTipo.getSelectedItem();

        if (COMIDA.equals(tipo)) {
            String restaurante = campoRestaurante.getText().trim();
            if (restaurante.isEmpty()) {
                avisar("Debes indicar el restaurante de origen.", campoRestaurante);
                return null;
            }
            return new PedidoComida(id, direccion, distancia, restaurante, checkMochila.isSelected());
        }

        if (ENCOMIENDA.equals(tipo)) {
            double peso = leerNumero(campoPeso, "el peso");
            if (Double.isNaN(peso)) {
                return null;
            }
            if (peso <= 0) {
                avisar("El peso debe ser mayor que cero.", campoPeso);
                return null;
            }
            if (peso > PESO_MAXIMO_KG) {
                avisar("El peso no puede superar los " + (int) PESO_MAXIMO_KG + " kg.", campoPeso);
                return null;
            }
            String embalaje = campoEmbalaje.getText().trim();
            if (embalaje.isEmpty()) {
                avisar("Debes indicar el tipo de embalaje.", campoEmbalaje);
                return null;
            }
            return new PedidoEncomienda(id, direccion, distancia, peso, embalaje);
        }

        String local = campoLocal.getText().trim();
        if (local.isEmpty()) {
            avisar("Debes indicar el local de la compra.", campoLocal);
            return null;
        }
        return new PedidoExpress(id, direccion, distancia, local, checkDisponibilidad.isSelected());
    }

    /**
     * Lee un numero desde un campo de texto.
     *
     * @param campo    campo que contiene el numero
     * @param queCampo nombre del campo para el mensaje de error
     * @return el valor leido, o NaN si el texto no es un numero valido
     */
    private double leerNumero(JTextField campo, String queCampo) {
        String texto = campo.getText().trim().replace(",", ".");
        if (texto.isEmpty()) {
            avisar("Debes indicar " + queCampo + ".", campo);
            return Double.NaN;
        }
        try {
            double valor = Double.parseDouble(texto);
            // parseDouble tambien acepta "Infinity" y "NaN", que no son
            // valores validos para este formulario y dejarian la tabla con
            // tiempos absurdos. isFinite los descarta.
            if (!Double.isFinite(valor)) {
                avisar("El valor de " + queCampo + " no es un numero valido.", campo);
                return Double.NaN;
            }
            return valor;
        } catch (NumberFormatException e) {
            avisar("El valor de " + queCampo + " debe ser un numero. Recibido: '" + texto + "'", campo);
            return Double.NaN;
        }
    }

    /**
     * Muestra un aviso de validacion y deja el foco en el campo con problema.
     *
     * @param mensaje texto a mostrar
     * @param campo   campo que debe corregirse
     */
    private void avisar(String mensaje, JComponent campo) {
        JOptionPane.showMessageDialog(this, mensaje, "Revisa los datos",
                JOptionPane.WARNING_MESSAGE);
        campo.requestFocusInWindow();
    }

    /**
     * Deja el formulario listo para un pedido nuevo, con el siguiente
     * identificador ya sugerido.
     */
    private void limpiarFormulario() {
        campoId.setText(controlador.sugerirIdPedido());
        campoCalle.setText("");
        comboComuna.setSelectedIndex(0);
        proponerDistancia();
        campoRestaurante.setText("");
        campoPeso.setText("");
        campoEmbalaje.setText("");
        campoLocal.setText("");
        checkMochila.setSelected(false);
        checkDisponibilidad.setSelected(false);
        comboTipo.setSelectedIndex(0);
        campoId.requestFocusInWindow();
    }
}
