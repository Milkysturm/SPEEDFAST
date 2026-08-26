package com.speedfast.modelo;

/**
 * Pedido proveniente de un restaurante.
 *
 * Regla de tiempo: 15 minutos de preparacion en cocina mas 2 minutos por cada
 * kilometro recorrido.
 *
 * @author Olga
 * @version 2.0
 */
public class PedidoComida extends Pedido {

    /** Minutos fijos de preparacion en el restaurante. */
    private static final int TIEMPO_BASE = 15;

    /** Minutos que se suman por cada kilometro de recorrido. */
    private static final int MINUTOS_POR_KM = 2;

    /** Nombre del restaurante de origen. */
    private String restaurante;

    /** Indica si el pedido requiere mantener la comida caliente. */
    private boolean requiereMochilaTermica;

    /**
     * Constructor completo.
     *
     * @param idPedido               identificador del pedido
     * @param direccionEntrega       direccion de entrega
     * @param distanciaKm            distancia del recorrido en kilometros
     * @param restaurante            restaurante de origen
     * @param requiereMochilaTermica true si necesita mochila termica
     */
    public PedidoComida(String idPedido, String direccionEntrega, double distanciaKm,
                        String restaurante, boolean requiereMochilaTermica) {
        super(idPedido, direccionEntrega, distanciaKm);
        this.restaurante = restaurante;
        this.requiereMochilaTermica = requiereMochilaTermica;
    }

    /**
     * Tiempo estimado = 15 min de preparacion + 2 min por kilometro.
     *
     * @return tiempo estimado en minutos
     */
    @Override
    public int calcularTiempoEntrega() {
        return calcularTiempoLineal(TIEMPO_BASE, MINUTOS_POR_KM);
    }

    @Override
    public String getTipoEntrega() {
        return "Comida";
    }

    @Override
    public String getFactorDuracion() {
        return "Preparacion en cocina (" + TIEMPO_BASE + " min) mas "
                + MINUTOS_POR_KM + " min por km";
    }

    /**
     * La comida se asigna a un repartidor con mochila termica cuando debe
     * llegar caliente.
     */
    @Override
    public String asignarRepartidor() {
        if (requiereMochilaTermica) {
            return "Repartidor con MOCHILA TERMICA";
        }
        return "Cualquier repartidor disponible";
    }

    @Override
    protected String getDetalleServicio() {
        return "Restaurante " + restaurante;
    }

    public String getRestaurante() {
        return restaurante;
    }

    public void setRestaurante(String restaurante) {
        this.restaurante = restaurante;
    }

    public boolean isRequiereMochilaTermica() {
        return requiereMochilaTermica;
    }

    public void setRequiereMochilaTermica(boolean requiereMochilaTermica) {
        this.requiereMochilaTermica = requiereMochilaTermica;
    }
}
