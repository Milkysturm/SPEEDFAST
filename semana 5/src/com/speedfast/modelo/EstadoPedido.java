package com.speedfast.modelo;

import java.util.Locale;

/**
 * Estados por los que pasa un pedido: PENDIENTE, EN_REPARTO y ENTREGADO.
 *
 * @author Olga Rivas
 * @version 5.0
 */
public enum EstadoPedido {

    /** El pedido esta en la zona de carga, esperando que alguien lo retire. */
    PENDIENTE("Pendiente en zona de carga"),

    /** Un repartidor lo retiro y va en camino al cliente. */
    EN_REPARTO("En reparto"),

    /** El pedido llego a destino. */
    ENTREGADO("Entregado");

    /** Texto legible para mostrar en los reportes. */
    private final String descripcion;

    /**
     * Constructor del enum.
     *
     * @param descripcion texto legible del estado
     */
    EstadoPedido(String descripcion) {
        this.descripcion = descripcion;
    }

    /**
     * Devuelve el texto legible del estado.
     *
     * @return descripcion del estado
     */
    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Convierte un texto en el estado correspondiente, sin distinguir
     * mayusculas ni espacios sobrantes.
     *
     * @param texto nombre del estado
     * @return el estado correspondiente
     * @throws IllegalArgumentException si el texto es nulo o no es un estado valido
     */
    public static EstadoPedido desdeTexto(String texto) {
        if (texto == null) {
            throw new IllegalArgumentException("El estado no puede ser nulo.");
        }
        try {
            // Locale.ROOT para que la conversion no dependa del idioma del equipo.
            return EstadoPedido.valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado desconocido: '" + texto
                    + "'. Los validos son PENDIENTE, EN_REPARTO y ENTREGADO.", e);
        }
    }
}
