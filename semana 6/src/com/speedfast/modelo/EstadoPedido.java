package com.speedfast.modelo;

/**
 * Estados por los que puede pasar un pedido dentro del sistema.
 *
 * Se usa un enum en lugar de un String suelto porque el conjunto de estados
 * es cerrado y conocido: asi el compilador impide escribir un estado que no
 * existe y las comparaciones no dependen de como se escriba la palabra.
 *
 * En la semana 4 se agrega ENTREGADO, porque con hilos el pedido ya no
 * termina su ciclo al salir a ruta: el repartidor tarda un tiempo real en
 * completarlo y recien ahi la entrega queda cerrada.
 *
 * @author Olga Rivas
 * @version 4.0
 */
public enum EstadoPedido {

    /** El cliente reservo el pedido, pero todavia no tiene repartidor. */
    RESERVADO("Reservado, sin repartidor"),

    /** Ya tiene un repartidor asignado y espera salir a ruta. */
    ASIGNADO("Asignado, listo para despachar"),

    /** El pedido va en camino al cliente. */
    DESPACHADO("En ruta"),

    /** El repartidor completo la entrega. */
    ENTREGADO("Entregado"),

    /** El pedido fue anulado y no se entregara. */
    CANCELADO("Cancelado");

    /** Texto legible para mostrar en los reportes. */
    private final String descripcion;

    EstadoPedido(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
