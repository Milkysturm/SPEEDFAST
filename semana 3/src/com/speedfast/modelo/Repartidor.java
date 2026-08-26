package com.speedfast.modelo;

/**
 * Persona que realiza la entrega, con el vehiculo con el que trabaja.
 *
 * Se modela como clase propia y no como un String dentro de Pedido porque
 * un repartidor tiene mas de un dato relevante (nombre y vehiculo) y porque
 * manana podria crecer sin obligar a tocar la jerarquia de pedidos.
 *
 * @author Olga Rivas
 * @version 3.0
 */
public class Repartidor {

    /** Nombre de la persona que reparte. */
    private String nombre;

    /** Vehiculo con el que realiza el reparto. */
    private String vehiculo;

    /**
     * Constructor completo.
     *
     * @param nombre   nombre del repartidor
     * @param vehiculo vehiculo asignado
     */
    public Repartidor(String nombre, String vehiculo) {
        this.nombre = nombre;
        this.vehiculo = vehiculo;
    }

    /**
     * Constructor usado en la asignacion manual, cuando el operador indica
     * solo el nombre y el vehiculo aun no se conoce.
     *
     * @param nombre nombre del repartidor
     */
    public Repartidor(String nombre) {
        this(nombre, "vehiculo por confirmar");
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getVehiculo() {
        return vehiculo;
    }

    public void setVehiculo(String vehiculo) {
        this.vehiculo = vehiculo;
    }

    @Override
    public String toString() {
        return nombre + " (" + vehiculo + ")";
    }
}
