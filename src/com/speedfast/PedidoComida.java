package com.speedfast;

/**
 * Pedido de comida proveniente de restaurantes.
 * Criterio de asignación: el repartidor debe contar con mochila térmica
 * para mantener la temperatura de los alimentos.
 */
public class PedidoComida extends Pedido {

    /** Indica si el repartidor asignado cuenta con mochila térmica. */
    private boolean mochilaTermica;

    /**
     * Constructor completo. Fija el tipo de pedido en la clase base.
     *
     * @param idPedido         identificador único del pedido
     * @param direccionEntrega dirección de entrega
     * @param mochilaTermica   true si el repartidor dispone de mochila térmica
     */
    public PedidoComida(String idPedido, String direccionEntrega, boolean mochilaTermica) {
        super(idPedido, direccionEntrega, "Pedido Comida");
        this.mochilaTermica = mochilaTermica;
    }

    public boolean isMochilaTermica() {
        return mochilaTermica;
    }

    public void setMochilaTermica(boolean mochilaTermica) {
        this.mochilaTermica = mochilaTermica;
    }

    /**
     * SOBRESCRITURA del método genérico: lógica propia del pedido de comida.
     */
    @Override
    public void asignarRepartidor() {
        System.out.println("[" + getTipoPedido() + "] Asignando repartidor...");
        System.out.println("   -> Verificando mochila térmica... "
                + (mochilaTermica ? "OK" : "NO DISPONIBLE"));
        if (mochilaTermica) {
            System.out.println("   -> Repartidor habilitado para transportar alimentos calientes.");
        } else {
            System.out.println("   -> Se requiere un repartidor con mochila térmica. Búsqueda en curso.");
        }
    }

    /**
     * SOBRECARGA sobrescrita: valida la mochila térmica antes de confirmar
     * al repartidor recibido por parámetro.
     *
     * @param nombreRepartidor nombre del repartidor asignado
     */
    @Override
    public void asignarRepartidor(String nombreRepartidor) {
        System.out.println("[" + getTipoPedido() + "] Asignando repartidor...");
        System.out.println("   -> Verificando mochila térmica... "
                + (mochilaTermica ? "OK" : "NO DISPONIBLE"));
        if (mochilaTermica) {
            System.out.println("   -> Pedido asignado a " + nombreRepartidor);
        } else {
            System.out.println("   -> " + nombreRepartidor
                    + " no puede tomar el pedido: falta mochila térmica.");
        }
    }
}
