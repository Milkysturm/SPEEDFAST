package com.speedfast.vista;

import com.speedfast.modelo.EstadoPedido;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;

/**
 * Pinta cada fila de la tabla segun el estado del pedido.
 *
 * Recibe la tabla y la fila que se esta dibujando, consulta el estado en el
 * modelo y devuelve la celda con el color correspondiente. Cuando la fila
 * esta seleccionada se respetan los colores de seleccion.
 *
 * @author Olga Rivas
 * @version 6.0
 */
public class RenderEstadoPedido extends DefaultTableCellRenderer {

    /** Version de la clase, requerida por ser serializable. */
    private static final long serialVersionUID = 1L;

    /** Fondo de los pedidos entregados. */
    private static final Color FONDO_ENTREGADO = new Color(0xE7F5EC);

    /** Texto de los pedidos entregados. */
    private static final Color TEXTO_ENTREGADO = new Color(0x1E6B3A);

    /** Fondo de los pedidos en ruta. */
    private static final Color FONDO_EN_RUTA = new Color(0xE4EEFA);

    /** Texto de los pedidos en ruta. */
    private static final Color TEXTO_EN_RUTA = new Color(0x14508C);

    /** Fondo de los pedidos ya asignados pero sin salir. */
    private static final Color FONDO_ASIGNADO = new Color(0xFDF3E3);

    /** Texto de los pedidos ya asignados pero sin salir. */
    private static final Color TEXTO_ASIGNADO = new Color(0x8A5A12);

    /** Fondo de los pedidos cancelados. */
    private static final Color FONDO_CANCELADO = new Color(0xFBE9E9);

    /** Texto de los pedidos cancelados. */
    private static final Color TEXTO_CANCELADO = new Color(0x9B2C2C);

    /**
     * Devuelve la celda ya coloreada segun el estado de su pedido.
     *
     * @param tabla        tabla que se esta dibujando
     * @param valor        contenido de la celda
     * @param seleccionada true si la fila esta seleccionada
     * @param conFoco      true si la celda tiene el foco
     * @param fila         fila en coordenadas de la vista
     * @param columna      columna en coordenadas de la vista
     * @return el componente que dibuja la celda
     */
    @Override
    public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionada,
                                                   boolean conFoco, int fila, int columna) {
        Component celda = super.getTableCellRendererComponent(tabla, valor, seleccionada,
                conFoco, fila, columna);

        // Los numeros se alinean a la derecha, como es habitual en una tabla.
        setHorizontalAlignment(valor instanceof Number ? SwingConstants.RIGHT : SwingConstants.LEFT);

        if (seleccionada) {
            return celda;
        }

        // Sin estado especial la celda usa los colores propios de la tabla,
        // para que se vea como la dibuja el sistema.
        celda.setBackground(tabla.getBackground());
        celda.setForeground(tabla.getForeground());

        if (!(tabla.getModel() instanceof ModeloTablaPedidos)) {
            return celda;
        }

        // La fila que llega es la de la vista; con la tabla ordenada o
        // filtrada no coincide con la del modelo, asi que hay que convertirla.
        ModeloTablaPedidos modelo = (ModeloTablaPedidos) tabla.getModel();
        EstadoPedido estado = modelo.getEstadoEn(tabla.convertRowIndexToModel(fila));
        if (estado == null) {
            return celda;
        }

        switch (estado) {
            case ENTREGADO:
                celda.setBackground(FONDO_ENTREGADO);
                celda.setForeground(TEXTO_ENTREGADO);
                break;
            case DESPACHADO:
                celda.setBackground(FONDO_EN_RUTA);
                celda.setForeground(TEXTO_EN_RUTA);
                break;
            case ASIGNADO:
                celda.setBackground(FONDO_ASIGNADO);
                celda.setForeground(TEXTO_ASIGNADO);
                break;
            case CANCELADO:
                celda.setBackground(FONDO_CANCELADO);
                celda.setForeground(TEXTO_CANCELADO);
                break;
            default:
                break;
        }
        return celda;
    }
}
