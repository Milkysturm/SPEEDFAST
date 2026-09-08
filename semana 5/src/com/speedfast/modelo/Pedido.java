package com.speedfast.modelo;

/**
 * Encomienda que llega a la zona de carga y que un repartidor debe entregar.
 *
 * Guarda su identificador, la direccion de destino, el estado en que se
 * encuentra y el nombre del repartidor que lo retiro.
 *
 * @author Olga Rivas
 * @version 5.0
 */
public class Pedido {

    /** Identificador del pedido. */
    private int id;

    /** Direccion donde debe entregarse. */
    private String direccionEntrega;

    /** Situacion actual del pedido. */
    private EstadoPedido estado;

    /** Nombre del repartidor que lo retiro, o null si sigue en la zona. */
    private String repartidorAsignado;

    /**
     * Crea un pedido en estado PENDIENTE y sin repartidor asignado.
     *
     * @param id               identificador del pedido, positivo
     * @param direccionEntrega direccion de entrega, no vacia
     * @throws IllegalArgumentException si el id no es positivo o la direccion viene vacia
     */
    public Pedido(int id, String direccionEntrega) {
        if (id <= 0) {
            throw new IllegalArgumentException("El id del pedido debe ser positivo: " + id);
        }
        if (direccionEntrega == null || direccionEntrega.isBlank()) {
            throw new IllegalArgumentException("El pedido " + id + " necesita una direccion de entrega.");
        }
        this.id = id;
        this.direccionEntrega = direccionEntrega;
        this.estado = EstadoPedido.PENDIENTE;
        this.repartidorAsignado = null;
    }

    // ------------------------------------------------------------------
    // Getters y setters
    // ------------------------------------------------------------------

    /**
     * @return identificador del pedido
     */
    public int getId() {
        return id;
    }

    /**
     * @param id nuevo identificador del pedido
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return direccion de entrega
     */
    public String getDireccionEntrega() {
        return direccionEntrega;
    }

    /**
     * @param direccionEntrega nueva direccion de entrega
     */
    public void setDireccionEntrega(String direccionEntrega) {
        this.direccionEntrega = direccionEntrega;
    }

    /**
     * @return estado actual del pedido
     */
    public EstadoPedido getEstado() {
        return estado;
    }

    /**
     * Actualiza el estado del pedido.
     *
     * @param nuevoEstado estado al que pasa el pedido
     * @throws IllegalArgumentException si el estado es nulo
     */
    public void setEstado(EstadoPedido nuevoEstado) {
        if (nuevoEstado == null) {
            throw new IllegalArgumentException("El estado del pedido " + id + " no puede ser nulo.");
        }
        this.estado = nuevoEstado;
    }

    /**
     * Actualiza el estado a partir de su nombre en texto. Es una sobrecarga
     * del metodo anterior: convierte el texto al enum y valida que exista.
     *
     * @param nuevoEstado nombre del estado: PENDIENTE, EN_REPARTO o ENTREGADO
     * @throws IllegalArgumentException si el texto no corresponde a un estado
     */
    public void setEstado(String nuevoEstado) {
        setEstado(EstadoPedido.desdeTexto(nuevoEstado));
    }

    /**
     * @return nombre del repartidor que retiro el pedido, o null si nadie lo hizo
     */
    public String getRepartidorAsignado() {
        return repartidorAsignado;
    }

    /**
     * @param repartidorAsignado nombre del repartidor que retiro el pedido
     */
    public void setRepartidorAsignado(String repartidorAsignado) {
        this.repartidorAsignado = repartidorAsignado;
    }

    /**
     * Indica si el pedido todavia espera en la zona de carga.
     *
     * @return true si el estado es PENDIENTE
     */
    public boolean estaPendiente() {
        return estado == EstadoPedido.PENDIENTE;
    }

    /**
     * Indica si el pedido ya llego a destino.
     *
     * @return true si el estado es ENTREGADO
     */
    public boolean estaEntregado() {
        return estado == EstadoPedido.ENTREGADO;
    }

    /**
     * Devuelve el pedido en una linea, con su id, estado, destino y responsable.
     *
     * @return descripcion del pedido
     */
    @Override
    public String toString() {
        String responsable = (repartidorAsignado == null) ? "sin repartidor" : repartidorAsignado;
        return "Pedido #" + id + " [" + estado + "] -> " + direccionEntrega
                + " (" + responsable + ")";
    }
}
