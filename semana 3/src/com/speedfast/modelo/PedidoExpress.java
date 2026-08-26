package com.speedfast.modelo;

/**
 * Compra express en supermercado o farmacia.
 *
 * Regla de tiempo: 10 minutos base, porque el repartidor compra en el local.
 * Si la distancia supera los 5 km se agregan 5 minutos extra.
 *
 * A diferencia de los otros dos tipos, aca el tiempo no crece de forma
 * continua con la distancia sino por tramos, asi que no usa la formula
 * lineal de la clase base.
 *
 * @author Olga
 * @version 2.0
 */
public class PedidoExpress extends Pedido {

    /** Minutos fijos de compra en el local. */
    private static final int TIEMPO_BASE = 10;

    /** Distancia a partir de la cual se aplica el recargo. */
    private static final int DISTANCIA_RECARGO_KM = 5;

    /** Minutos adicionales cuando se supera la distancia de recargo. */
    private static final int RECARGO_MINUTOS = 5;

    /** Local desde donde se realiza la compra. */
    private String local;

    /** Indica si hay un repartidor disponible de inmediato. */
    private boolean disponibilidadInmediata;

    /**
     * Constructor completo.
     *
     * @param idPedido                identificador del pedido
     * @param direccionEntrega        direccion de entrega
     * @param distanciaKm             distancia del recorrido en kilometros
     * @param local                   supermercado o farmacia de origen
     * @param disponibilidadInmediata true si hay repartidor disponible ahora
     */
    public PedidoExpress(String idPedido, String direccionEntrega, double distanciaKm,
                         String local, boolean disponibilidadInmediata) {
        super(idPedido, direccionEntrega, distanciaKm);
        this.local = local;
        this.disponibilidadInmediata = disponibilidadInmediata;
    }

    /**
     * Tiempo estimado = 10 min base, mas 5 min extra si la distancia supera
     * los 5 km.
     *
     * @return tiempo estimado en minutos
     */
    @Override
    public int calcularTiempoEntrega() {
        if (getDistanciaKm() > DISTANCIA_RECARGO_KM) {
            return TIEMPO_BASE + RECARGO_MINUTOS;
        }
        return TIEMPO_BASE;
    }

    @Override
    public String getTipoEntrega() {
        return "Compra Express";
    }

    @Override
    public String getFactorDuracion() {
        if (getDistanciaKm() > DISTANCIA_RECARGO_KM) {
            return "Compra en local (" + TIEMPO_BASE + " min) mas recargo de "
                    + RECARGO_MINUTOS + " min por superar los " + DISTANCIA_RECARGO_KM + " km";
        }
        return "Compra en local (" + TIEMPO_BASE + " min), sin recargo por distancia";
    }

    /**
     * Asignacion automatica: la compra express solo se asigna si hay un
     * repartidor cercano libre en ese momento. Si no lo hay, el pedido queda
     * a la espera y a proposito NO se le asigna a nadie.
     *
     * @return mensaje que describe el resultado de la asignacion
     */
    @Override
    public String asignarRepartidor() {
        if (disponibilidadInmediata) {
            return registrarAsignacion(
                    new Repartidor("Valentina Cortes", "bicicleta electrica"),
                    "repartidor cercano disponible de inmediato");
        }
        String aviso = "Sin disponibilidad inmediata: el pedido queda en espera.";
        registrarEvento(aviso);
        return aviso;
    }

    /**
     * Las compras express salen antes que el resto de los pedidos.
     *
     * Sobrescribe {@link Pedido#despachar()} para agregar la marca de
     * prioridad, pero reutiliza con super la validacion de la clase base en
     * lugar de repetirla.
     *
     * @return true si el pedido quedo despachado
     */
    @Override
    public boolean despachar() {
        boolean resultado = super.despachar();
        if (resultado) {
            registrarEvento("Marcado como PRIORITARIO: las compras express "
                    + "salen antes que el resto de los pedidos.");
        }
        return resultado;
    }

    @Override
    protected String getDetalleServicio() {
        return "Compra en " + local;
    }

    public String getLocal() {
        return local;
    }

    public void setLocal(String local) {
        this.local = local;
    }

    public boolean isDisponibilidadInmediata() {
        return disponibilidadInmediata;
    }

    public void setDisponibilidadInmediata(boolean disponibilidadInmediata) {
        this.disponibilidadInmediata = disponibilidadInmediata;
    }
}
