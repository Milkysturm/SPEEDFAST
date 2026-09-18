package com.speedfast.vista;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;

/**
 * Colores, tipografias y componentes comunes de la interfaz.
 *
 * Tener el estilo en un solo lugar mantiene las cuatro ventanas parecidas
 * entre si y evita repetir los mismos ajustes en cada una.
 *
 * @author Olga Rivas
 * @version 6.0
 */
public final class Estilos {

    /** Azul corporativo, usado en los encabezados. */
    public static final Color AZUL = new Color(0x1F3A5F);

    /** Naranjo de acento, usado en el boton principal de cada ventana. */
    public static final Color NARANJO = new Color(0xE0722F);

    /** Gris muy claro del fondo de las ventanas. */
    public static final Color FONDO = new Color(0xF2F4F7);

    /** Gris de los textos secundarios. */
    public static final Color GRIS = new Color(0x5B6470);

    /** Tipografia de los titulos. */
    public static final Font TITULO = new Font("SansSerif", Font.BOLD, 20);

    /** Tipografia de los subtitulos y textos secundarios. */
    public static final Font SUBTITULO = new Font("SansSerif", Font.PLAIN, 12);

    /** Tipografia general de la interfaz. */
    public static final Font NORMAL = new Font("SansSerif", Font.PLAIN, 13);

    /** Tipografia de las etiquetas de los formularios. */
    public static final Font ETIQUETA = new Font("SansSerif", Font.BOLD, 13);

    /** Tipografia de ancho fijo, para el area de avance. */
    public static final Font MONO = new Font("Monospaced", Font.PLAIN, 12);

    /** Constructor privado: es una clase de utilidad y no se instancia. */
    private Estilos() {
    }

    /**
     * Aplica el aspecto Nimbus si esta disponible, para que la aplicacion se
     * vea igual en cualquier sistema operativo.
     */
    public static void aplicarAspecto() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    return;
                }
            }
        } catch (Exception e) {
            // Si no se puede cambiar el aspecto se usa el que traiga el sistema.
        }
    }

    /**
     * Construye la franja azul con el titulo que encabeza cada ventana.
     *
     * @param titulo    titulo principal
     * @param subtitulo linea descriptiva bajo el titulo
     * @return panel listo para agregar al norte de un BorderLayout
     */
    public static JPanel encabezado(String titulo, String subtitulo) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(AZUL);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        JLabel etiquetaTitulo = new JLabel(titulo);
        etiquetaTitulo.setFont(TITULO);
        etiquetaTitulo.setForeground(Color.WHITE);

        JLabel etiquetaSubtitulo = new JLabel(subtitulo);
        etiquetaSubtitulo.setFont(SUBTITULO);
        etiquetaSubtitulo.setForeground(new Color(0xC7D3E3));

        panel.add(etiquetaTitulo, BorderLayout.NORTH);
        panel.add(etiquetaSubtitulo, BorderLayout.SOUTH);
        return panel;
    }

    /**
     * Crea un boton con el estilo comun de la aplicacion.
     *
     * @param texto     texto del boton
     * @param destacado true para el boton principal de la ventana
     * @return el boton configurado
     */
    public static JButton boton(String texto, boolean destacado) {
        // Se sobrescribe setEnabled para que el boton destacado tambien se
        // agrise al deshabilitarse: al fijarle un color de fondo propio, el
        // aspecto del sistema ya no puede hacerlo por su cuenta.
        JButton boton = new JButton(texto) {
            private static final long serialVersionUID = 1L;

            @Override
            public void setEnabled(boolean habilitado) {
                super.setEnabled(habilitado);
                if (destacado) {
                    setBackground(habilitado ? NARANJO : new Color(0xC3C9D0));
                }
            }
        };
        boton.setFont(new Font("SansSerif", Font.BOLD, 13));
        boton.setFocusPainted(false);
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        boton.setPreferredSize(new Dimension(190, 38));

        if (destacado) {
            boton.setBackground(NARANJO);
            boton.setForeground(Color.WHITE);
            boton.setOpaque(true);
            boton.setBorderPainted(false);
        }
        return boton;
    }

    /**
     * Devuelve un margen interior uniforme.
     *
     * @param espacio pixeles de margen
     * @return borde vacio del tamano indicado
     */
    public static Border margen(int espacio) {
        return BorderFactory.createEmptyBorder(espacio, espacio, espacio, espacio);
    }

    /**
     * Crea una etiqueta de formulario.
     *
     * @param texto texto de la etiqueta
     * @return la etiqueta configurada
     */
    public static JLabel etiqueta(String texto) {
        JLabel etiqueta = new JLabel(texto);
        etiqueta.setFont(ETIQUETA);
        return etiqueta;
    }

    /**
     * Deja una tabla de pedidos con el aspecto y los anchos de columna
     * comunes, para que las dos ventanas que la muestran se vean iguales.
     *
     * @param tabla tabla a configurar
     */
    public static void configurarTabla(JTable tabla) {
        tabla.setFont(NORMAL);
        tabla.setRowHeight(26);
        tabla.setGridColor(new Color(0xE1E5EA));
        tabla.setSelectionBackground(new Color(0xCBDDF2));
        tabla.setSelectionForeground(new Color(0x1B2430));
        tabla.getTableHeader().setFont(ETIQUETA);
        tabla.getTableHeader().setReorderingAllowed(false);

        // Permite ordenar haciendo clic en el encabezado.
        tabla.setAutoCreateRowSorter(true);

        // Pinta cada fila segun el estado del pedido.
        tabla.setDefaultRenderer(Object.class, new RenderEstadoPedido());

        int[] anchos = {70, 115, 235, 90, 175, 185, 85};
        for (int columna = 0; columna < anchos.length
                && columna < tabla.getColumnModel().getColumnCount(); columna++) {
            tabla.getColumnModel().getColumn(columna).setPreferredWidth(anchos[columna]);
        }
    }
}
