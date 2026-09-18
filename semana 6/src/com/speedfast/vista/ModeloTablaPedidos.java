package com.speedfast.vista;

import com.speedfast.modelo.EstadoPedido;
import com.speedfast.modelo.Pedido;

import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de datos de las tablas de pedidos.
 *
 * Extiende DefaultTableModel, que es el modelo que pide la actividad, y le
 * agrega tres cosas: las celdas no se pueden editar a mano, sabe llenarse a
 * partir de una lista de pedidos, y recuerda el estado y el id de cada fila
 * para que el render pueda colorearla y las ventanas puedan identificarla.
 *
 * @author Olga Rivas
 * @version 6.0
 */
public class ModeloTablaPedidos extends DefaultTableModel {

    /** Version de la clase, requerida por ser serializable. */
    private static final long serialVersionUID = 1L;

    /** Posicion de la columna que guarda la distancia. */
    public static final int COLUMNA_DISTANCIA = 3;

    /** Posicion de la columna que guarda el tiempo estimado. */
    public static final int COLUMNA_TIEMPO = 6;

    /** Titulos de las columnas. La unidad va en el titulo, no en la celda. */
    private static final String[] COLUMNAS = {
            "ID", "Tipo", "Direccion", "Distancia (km)", "Estado", "Repartidor", "Tiempo (min)"
    };

    /** Estado del pedido de cada fila, en el mismo orden que la tabla. */
    private final ArrayList<EstadoPedido> estados = new ArrayList<>();

    /** Identificador del pedido de cada fila, en el mismo orden que la tabla. */
    private final ArrayList<String> identificadores = new ArrayList<>();

    /**
     * Crea el modelo con las columnas definidas y sin filas.
     */
    public ModeloTablaPedidos() {
        super(COLUMNAS, 0);
    }

    /**
     * Impide editar las celdas: la tabla es solo para mostrar informacion.
     *
     * @param fila    fila consultada
     * @param columna columna consultada
     * @return siempre false
     */
    @Override
    public boolean isCellEditable(int fila, int columna) {
        return false;
    }

    /**
     * Indica el tipo de dato de cada columna.
     *
     * La distancia y el tiempo se guardan como numeros, no como texto, para
     * que al ordenar por esas columnas se comparen sus valores y no sus
     * letras: como texto, "12.5" quedaria antes que "3.5".
     *
     * @param columna columna consultada
     * @return la clase de los valores de esa columna
     */
    @Override
    public Class<?> getColumnClass(int columna) {
        if (columna == COLUMNA_DISTANCIA) {
            return Double.class;
        }
        if (columna == COLUMNA_TIEMPO) {
            return Integer.class;
        }
        return String.class;
    }

    /**
     * Vacia la tabla y la vuelve a llenar con los pedidos indicados.
     *
     * @param pedidos pedidos a mostrar
     */
    public void cargar(List<Pedido> pedidos) {
        setRowCount(0);
        estados.clear();
        identificadores.clear();

        for (Pedido pedido : pedidos) {
            addRow(new Object[]{
                    pedido.getIdPedido(),
                    pedido.getTipoEntrega(),
                    pedido.getDireccionEntrega(),
                    pedido.getDistanciaKm(),
                    pedido.getEstado().getDescripcion(),
                    pedido.getNombreRepartidor(),
                    pedido.calcularTiempoEntrega()
            });
            estados.add(pedido.getEstado());
            identificadores.add(pedido.getIdPedido());
        }
    }

    /**
     * Devuelve el estado del pedido de una fila.
     *
     * @param fila fila en coordenadas del modelo
     * @return el estado, o null si la fila no existe
     */
    public EstadoPedido getEstadoEn(int fila) {
        if (fila < 0 || fila >= estados.size()) {
            return null;
        }
        return estados.get(fila);
    }

    /**
     * Devuelve el identificador del pedido de una fila.
     *
     * @param fila fila en coordenadas del modelo
     * @return el identificador, o null si la fila no existe
     */
    public String getIdEn(int fila) {
        if (fila < 0 || fila >= identificadores.size()) {
            return null;
        }
        return identificadores.get(fila);
    }

    /**
     * Busca en que fila esta un pedido.
     *
     * @param idPedido identificador buscado
     * @return la fila en coordenadas del modelo, o -1 si no esta
     */
    public int buscarFila(String idPedido) {
        return identificadores.indexOf(idPedido);
    }
}
