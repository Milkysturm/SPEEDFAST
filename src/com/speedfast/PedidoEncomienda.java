package com.speedfast;

/**
 * Pedido de encomienda (documentos o paquetes).
 * Criterio de asignación: requiere validar el peso del paquete y que el
 * embalaje sea seguro antes de entregarlo a un repartidor.
 */
public class PedidoEncomienda extends Pedido {

    /** Peso del paquete en kilogramos. */
    private double pesoKg;

    /** Indica si el paquete cuenta con embalaje adecuado. */
    private boolean embalajeSeguro;

    /** Peso máximo permitido por repartidor en moto (kg). */
    private static final double PESO_MAXIMO_KG = 20.0;

    /**
     * Constructor completo. Fija el tipo de pedido en la clase base.
     *
     * @param idPedido         identificador único del pedido
     * @param direccionEntrega dirección de entrega
     * @param pesoKg           peso del paquete en kilogramos
     * @param embalajeSeguro   true si el embalaje cumple el estándar
     */
    public PedidoEncomienda(String idPedido, String direccionEntrega,
                            double pesoKg, boolean embalajeSeguro) {
        super(idPedido, direccionEntrega, "Pedido Encomienda");
        this.pesoKg = pesoKg;
        this.embalajeSeguro = embalajeSeguro;
    }

    public double getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(double pesoKg) {
        this.pesoKg = pesoKg;
    }

    public boolean isEmbalajeSeguro() {
        return embalajeSeguro;
    }

    public void setEmbalajeSeguro(boolean embalajeSeguro) {
        this.embalajeSeguro = embalajeSeguro;
    }

    /**
     * Valida en conjunto el peso y el embalaje del paquete.
     *
     * @return true si la encomienda está en condiciones de ser despachada
     */
    private boolean validarPaquete() {
        return pesoKg <= PESO_MAXIMO_KG && embalajeSeguro;
    }

    /**
     * SOBRESCRITURA del método genérico: lógica propia de la encomienda.
     */
    @Override
    public void asignarRepartidor() {
        System.out.println("[" + getTipoPedido() + "] Asignando repartidor...");
        System.out.println("   -> Validando peso (" + pesoKg + " kg) y embalaje... "
                + (validarPaquete() ? "OK" : "RECHAZADO"));
        if (validarPaquete()) {
            System.out.println("   -> Encomienda apta para despacho. Buscando repartidor disponible.");
        } else {
            System.out.println("   -> La encomienda excede los " + PESO_MAXIMO_KG
                    + " kg o el embalaje no es seguro.");
        }
    }

    /**
     * SOBRECARGA sobrescrita: valida peso y embalaje antes de confirmar
     * al repartidor recibido por parámetro.
     *
     * @param nombreRepartidor nombre del repartidor asignado
     */
    @Override
    public void asignarRepartidor(String nombreRepartidor) {
        System.out.println("[" + getTipoPedido() + "] Asignando repartidor...");
        System.out.println("   -> Validando peso (" + pesoKg + " kg) y embalaje... "
                + (validarPaquete() ? "OK" : "RECHAZADO"));
        if (validarPaquete()) {
            System.out.println("   -> Pedido asignado a " + nombreRepartidor);
        } else {
            System.out.println("   -> No se puede asignar a " + nombreRepartidor
                    + ": la encomienda no cumple las condiciones de despacho.");
        }
    }
}
