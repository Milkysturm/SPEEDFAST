package com.speedfast;

/**
 * Pedido de compra express (supermercado o farmacia).
 * Criterio de asignación: debe tomarlo el repartidor más cercano que tenga
 * disponibilidad inmediata.
 */
public class PedidoExpress extends Pedido {

    /** Distancia del repartidor más cercano, en kilómetros. */
    private double distanciaRepartidorKm;

    /** Indica si ese repartidor puede tomar el pedido de inmediato. */
    private boolean disponibilidadInmediata;

    /**
     * Constructor completo. Fija el tipo de pedido en la clase base.
     *
     * @param idPedido                identificador único del pedido
     * @param direccionEntrega        dirección de entrega
     * @param distanciaRepartidorKm   distancia del repartidor más cercano (km)
     * @param disponibilidadInmediata true si puede salir de inmediato
     */
    public PedidoExpress(String idPedido, String direccionEntrega,
                         double distanciaRepartidorKm, boolean disponibilidadInmediata) {
        super(idPedido, direccionEntrega, "Pedido Express");
        this.distanciaRepartidorKm = distanciaRepartidorKm;
        this.disponibilidadInmediata = disponibilidadInmediata;
    }

    public double getDistanciaRepartidorKm() {
        return distanciaRepartidorKm;
    }

    public void setDistanciaRepartidorKm(double distanciaRepartidorKm) {
        this.distanciaRepartidorKm = distanciaRepartidorKm;
    }

    public boolean isDisponibilidadInmediata() {
        return disponibilidadInmediata;
    }

    public void setDisponibilidadInmediata(boolean disponibilidadInmediata) {
        this.disponibilidadInmediata = disponibilidadInmediata;
    }

    /**
     * SOBRESCRITURA del método genérico: lógica propia de la compra express.
     */
    @Override
    public void asignarRepartidor() {
        System.out.println("[" + getTipoPedido() + "] Asignando repartidor...");
        if (disponibilidadInmediata) {
            System.out.println("   -> Repartidor más cercano con disponibilidad inmediata encontrado a "
                    + distanciaRepartidorKm + " km.");
            System.out.println("   -> Ruta optimizada hacia " + getDireccionEntrega() + ".");
        } else {
            System.out.println("   -> No hay repartidores con disponibilidad inmediata. Reintentando...");
        }
    }

    /**
     * SOBRECARGA sobrescrita: confirma cercanía y disponibilidad del
     * repartidor recibido por parámetro.
     *
     * @param nombreRepartidor nombre del repartidor asignado
     */
    @Override
    public void asignarRepartidor(String nombreRepartidor) {
        System.out.println("[" + getTipoPedido() + "] Asignando repartidor...");
        System.out.println("   -> Repartidor más cercano con disponibilidad inmediata encontrado.");
        if (disponibilidadInmediata) {
            System.out.println("   -> " + nombreRepartidor + " se encuentra a "
                    + distanciaRepartidorKm + " km del punto de retiro.");
            System.out.println("   -> Pedido asignado a " + nombreRepartidor);
        } else {
            System.out.println("   -> " + nombreRepartidor
                    + " no está disponible de inmediato. Se buscará otro repartidor.");
        }
    }
}
