package com.speedfast.vista;

import com.speedfast.modelo.EstadoPedido;
import com.speedfast.modelo.Pedido;
import com.speedfast.modelo.PedidoComida;
import com.speedfast.modelo.PedidoEncomienda;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.TableRowSorter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del modelo que alimenta las tablas: columnas, carga de filas,
 * celdas no editables y correspondencia entre fila y pedido.
 *
 * Se prueba el modelo y no la ventana: DefaultTableModel es una clase de
 * datos y no necesita que haya una pantalla para funcionar.
 *
 * @author Olga Rivas
 * @version 6.0
 */
class ModeloTablaPedidosTest {

    /** Modelo nuevo antes de cada prueba. */
    private ModeloTablaPedidos modelo;

    /** Pedidos de ejemplo que se cargan en la tabla. */
    private List<Pedido> pedidos;

    /**
     * Prepara un modelo vacio y dos pedidos de distinto tipo.
     */
    @BeforeEach
    void prepararModelo() {
        modelo = new ModeloTablaPedidos();
        pedidos = new ArrayList<>(Arrays.asList(
                new PedidoComida("P-001", "Av. Siempre Viva 742, Nunoa", 4.0, "Sushi Kai", true),
                new PedidoEncomienda("P-002", "Los Aromos 456, Maipu", 8.0, 12.5, "Caja")));
    }

    @Test
    @DisplayName("El modelo parte vacio y con las siete columnas definidas")
    void parteVacioConSusColumnas() {
        assertEquals(0, modelo.getRowCount());
        assertEquals(7, modelo.getColumnCount());
        assertEquals("ID", modelo.getColumnName(0));
        assertEquals("Estado", modelo.getColumnName(4));
    }

    @Test
    @DisplayName("La distancia y el tiempo son numeros, para que el orden no sea alfabetico")
    void laDistanciaYElTiempoSonNumeros() {
        assertEquals(Double.class, modelo.getColumnClass(ModeloTablaPedidos.COLUMNA_DISTANCIA));
        assertEquals(Integer.class, modelo.getColumnClass(ModeloTablaPedidos.COLUMNA_TIEMPO));
        assertEquals(String.class, modelo.getColumnClass(0));
    }

    @Test
    @DisplayName("Ordenar por distancia compara valores, no texto")
    void elOrdenPorDistanciaEsNumerico() {
        pedidos.add(new PedidoComida("P-003", "Camino Largo 9, Maipu", 12.5, "Pizza Uno", false));
        modelo.cargar(pedidos);

        TableRowSorter<ModeloTablaPedidos> ordenador = new TableRowSorter<>(modelo);
        ordenador.setSortKeys(List.of(
                new RowSorter.SortKey(ModeloTablaPedidos.COLUMNA_DISTANCIA, SortOrder.ASCENDING)));
        ordenador.sort();

        // Como texto, "12.5" quedaria antes que "4.0" y que "8.0".
        assertEquals("P-001", modelo.getIdEn(ordenador.convertRowIndexToModel(0)));
        assertEquals("P-002", modelo.getIdEn(ordenador.convertRowIndexToModel(1)));
        assertEquals("P-003", modelo.getIdEn(ordenador.convertRowIndexToModel(2)));
    }

    @Test
    @DisplayName("Cargar deja una fila por pedido y en el mismo orden")
    void cargaUnaFilaPorPedido() {
        modelo.cargar(pedidos);

        assertEquals(2, modelo.getRowCount());
        assertEquals("P-001", modelo.getValueAt(0, 0));
        assertEquals("P-002", modelo.getValueAt(1, 0));
    }

    @Test
    @DisplayName("Cada fila muestra los datos del pedido que le corresponde")
    void cadaFilaMuestraSusDatos() {
        modelo.cargar(pedidos);

        assertEquals("Av. Siempre Viva 742, Nunoa", modelo.getValueAt(0, 2));
        assertEquals(4.0, (Double) modelo.getValueAt(0, 3), 0.001);
        assertEquals("Reservado, sin repartidor", modelo.getValueAt(0, 4));
        assertEquals("sin asignar", modelo.getValueAt(0, 5));
    }

    @Test
    @DisplayName("Volver a cargar reemplaza las filas, no las acumula")
    void cargarReemplazaLasFilas() {
        modelo.cargar(pedidos);
        modelo.cargar(pedidos);

        assertEquals(2, modelo.getRowCount(), "la tabla no debe duplicar los pedidos");
    }

    @Test
    @DisplayName("Cargar una lista vacia deja la tabla sin filas")
    void cargarListaVaciaDejaLaTablaVacia() {
        modelo.cargar(pedidos);
        modelo.cargar(new ArrayList<>());

        assertEquals(0, modelo.getRowCount());
        assertNull(modelo.getIdEn(0));
        assertNull(modelo.getEstadoEn(0));
    }

    @Test
    @DisplayName("Ninguna celda de la tabla es editable")
    void ningunaCeldaEsEditable() {
        modelo.cargar(pedidos);

        for (int fila = 0; fila < modelo.getRowCount(); fila++) {
            for (int columna = 0; columna < modelo.getColumnCount(); columna++) {
                assertFalse(modelo.isCellEditable(fila, columna),
                        "la celda " + fila + "," + columna + " no debe poder editarse");
            }
        }
    }

    @Test
    @DisplayName("El modelo recuerda el ID y el estado de cada fila")
    void recuerdaIdYEstadoDeCadaFila() {
        pedidos.get(1).cancelar("prueba");
        modelo.cargar(pedidos);

        assertEquals("P-001", modelo.getIdEn(0));
        assertEquals(EstadoPedido.RESERVADO, modelo.getEstadoEn(0));
        assertEquals(EstadoPedido.CANCELADO, modelo.getEstadoEn(1));
    }

    @Test
    @DisplayName("Consultar una fila que no existe devuelve null en vez de fallar")
    void filaFueraDeRangoDevuelveNull() {
        modelo.cargar(pedidos);

        assertNull(modelo.getIdEn(-1));
        assertNull(modelo.getIdEn(99));
        assertNull(modelo.getEstadoEn(99));
    }

    @Test
    @DisplayName("buscarFila encuentra el pedido y devuelve -1 si no esta")
    void buscaLaFilaDeUnPedido() {
        modelo.cargar(pedidos);

        assertEquals(0, modelo.buscarFila("P-001"));
        assertEquals(1, modelo.buscarFila("P-002"));
        assertEquals(-1, modelo.buscarFila("P-999"));
    }
}
