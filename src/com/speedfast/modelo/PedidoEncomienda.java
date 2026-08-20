package com.speedfast.modelo;

/**
 * Pedido de documentos o paquetes.
 *
 * Regla de tiempo: 20 minutos base por el retiro y la revision del embalaje,
 * mas 1.5 minutos por kilometro. El resultado se ajusta a un numero entero
 * de minutos.
 *
 * @author Olga
 * @version 2.0
 */
public class PedidoEncomienda extends Pedido {

    /** Minutos fijos de retiro y revision del paquete. */
    private static final int TIEMPO_BASE = 20;

    /** Minutos que se suman por cada kilometro de recorrido. */
    private static final double MINUTOS_POR_KM = 1.5;

    /** Peso maximo permitido para traslado en moto. */
    private static final double PESO_MAXIMO_MOTO = 20.0;

    /** Peso del paquete en kilogramos. */
    private double pesoKg;

    /** Tipo de embalaje (caja, sobre, pallet, etc.). */
    private String tipoEmbalaje;

    /**
     * Constructor completo.
     *
     * @param idPedido         identificador del pedido
     * @param direccionEntrega direccion de entrega
     * @param distanciaKm      distancia del recorrido en kilometros
     * @param pesoKg           peso del paquete en kg
     * @param tipoEmbalaje     descripcion del embalaje
     */
    public PedidoEncomienda(String idPedido, String direccionEntrega, double distanciaKm,
                            double pesoKg, String tipoEmbalaje) {
        super(idPedido, direccionEntrega, distanciaKm);
        this.pesoKg = pesoKg;
        this.tipoEmbalaje = tipoEmbalaje;
    }

    /**
     * Tiempo estimado = 20 min base + 1.5 min por kilometro, ajustado a entero.
     *
     * @return tiempo estimado en minutos
     */
    @Override
    public int calcularTiempoEntrega() {
        return calcularTiempoLineal(TIEMPO_BASE, MINUTOS_POR_KM);
    }

    @Override
    public String getTipoEntrega() {
        return "Encomienda";
    }

    @Override
    public String getFactorDuracion() {
        return "Retiro y revision de embalaje (" + TIEMPO_BASE + " min) mas "
                + MINUTOS_POR_KM + " min por km";
    }

    /**
     * La encomienda se asigna segun el peso: sobre el limite de la moto se
     * necesita un vehiculo de carga.
     */
    @Override
    public String asignarRepartidor() {
        if (pesoKg > PESO_MAXIMO_MOTO) {
            return "Repartidor con VEHICULO DE CARGA (supera " + PESO_MAXIMO_MOTO + " kg)";
        }
        return "Repartidor en moto";
    }

    @Override
    protected String getDetalleServicio() {
        return pesoKg + " kg en " + tipoEmbalaje;
    }

    public double getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(double pesoKg) {
        this.pesoKg = pesoKg;
    }

    public String getTipoEmbalaje() {
        return tipoEmbalaje;
    }

    public void setTipoEmbalaje(String tipoEmbalaje) {
        this.tipoEmbalaje = tipoEmbalaje;
    }
}
