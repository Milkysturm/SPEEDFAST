package com.speedfast.modelo;

/**
 * Clase abstracta base de la jerarquia de pedidos de SpeedFast.
 *
 * Reune el estado y el comportamiento comun a todo pedido, y deja abierto
 * a las subclases aquello que cambia segun el tipo de entrega:
 *
 *  - {@link #calcularTiempoEntrega()} : cada tipo aplica su propia formula.
 *  - {@link #getTipoEntrega()}        : nombre del tipo de entrega.
 *  - {@link #getFactorDuracion()}     : factor que afecta la duracion.
 *  - {@link #asignarRepartidor()}     : criterio para elegir al repartidor.
 *
 * No se puede instanciar directamente: no existe "un pedido" sin tipo.
 *
 * @author Olga
 * @version 2.0
 */
public abstract class Pedido {

    /** Identificador unico del pedido. */
    private String idPedido;

    /** Direccion donde se debe entregar el pedido. */
    private String direccionEntrega;

    /** Distancia en kilometros entre el origen y la direccion de entrega. */
    private double distanciaKm;

    /**
     * Constructor comun a toda la jerarquia.
     *
     * @param idPedido         identificador del pedido
     * @param direccionEntrega direccion de entrega
     * @param distanciaKm      distancia del recorrido en kilometros, no negativa
     * @throws IllegalArgumentException si la distancia es negativa
     */
    public Pedido(String idPedido, String direccionEntrega, double distanciaKm) {
        this.idPedido = idPedido;
        this.direccionEntrega = direccionEntrega;
        this.distanciaKm = validarDistancia(distanciaKm);
    }

    /**
     * Valida que la distancia no sea negativa, para evitar que un dato
     * erroneo produzca tiempos de entrega negativos.
     *
     * @param distanciaKm distancia a validar
     * @return la misma distancia si es valida
     * @throws IllegalArgumentException si la distancia es negativa
     */
    private static double validarDistancia(double distanciaKm) {
        if (distanciaKm < 0) {
            throw new IllegalArgumentException(
                    "La distancia no puede ser negativa: " + distanciaKm);
        }
        return distanciaKm;
    }

    // ------------------------------------------------------------------
    // Contrato que deben cumplir las subclases
    // ------------------------------------------------------------------

    /**
     * Calcula el tiempo estimado de entrega en minutos.
     * Cada subclase aplica la formula que corresponde a su tipo de servicio.
     *
     * @return tiempo estimado en minutos
     */
    public abstract int calcularTiempoEntrega();

    /**
     * Nombre del tipo de entrega, usado en los reportes.
     *
     * @return descripcion del tipo de entrega
     */
    public abstract String getTipoEntrega();

    /**
     * Factor propio del tipo de pedido que afecta la duracion de la entrega.
     *
     * @return descripcion del factor que incide en el tiempo
     */
    public abstract String getFactorDuracion();

    /**
     * Criterio con el que se elige al repartidor para este pedido.
     * Cada tipo de servicio exige condiciones distintas.
     *
     * @return descripcion del criterio de asignacion
     */
    public abstract String asignarRepartidor();

    // ------------------------------------------------------------------
    // Comportamiento comun implementado en la clase abstracta
    // ------------------------------------------------------------------

    /**
     * Imprime los datos basicos del pedido.
     *
     * Se implementa una sola vez aca porque el formato del resumen es igual
     * para todos los pedidos; lo que varia son los valores que aportan los
     * metodos abstractos.
     */
    public void mostrarResumen() {
        System.out.println("Pedido " + idPedido + "  [" + getTipoEntrega() + "]");
        System.out.println("   Direccion de entrega : " + direccionEntrega);
        System.out.println("   Distancia            : " + distanciaKm + " km");
        System.out.println("   Detalle del servicio : " + getDetalleServicio());
        System.out.println("   Factor de duracion   : " + getFactorDuracion());
        System.out.println("   Asignacion           : " + asignarRepartidor());
        System.out.println("   Tiempo estimado      : " + calcularTiempoEntrega() + " min");
    }

    /**
     * Formula lineal reutilizable: un tiempo base fijo mas un valor por
     * kilometro recorrido, ajustado a un numero entero de minutos.
     *
     * Vive en la clase abstracta justamente para no repetir el mismo calculo
     * en cada subclase que lo necesite.
     *
     * @param tiempoBase    minutos fijos del servicio
     * @param minutosPorKm  minutos que se suman por cada kilometro
     * @return tiempo estimado en minutos
     */
    protected int calcularTiempoLineal(int tiempoBase, double minutosPorKm) {
        double tiempo = tiempoBase + (minutosPorKm * distanciaKm);
        return (int) Math.round(tiempo);
    }

    /**
     * Descripcion breve de los datos propios de cada subclase.
     * Se define aca con un valor por defecto para que las subclases solo la
     * sobrescriban si tienen algo que aportar.
     *
     * @return detalle especifico del pedido
     */
    protected String getDetalleServicio() {
        return "Sin datos adicionales";
    }

    // ------------------------------------------------------------------
    // Getters y setters (encapsulamiento)
    // ------------------------------------------------------------------

    public String getIdPedido() {
        return idPedido;
    }

    public void setIdPedido(String idPedido) {
        this.idPedido = idPedido;
    }

    public String getDireccionEntrega() {
        return direccionEntrega;
    }

    public void setDireccionEntrega(String direccionEntrega) {
        this.direccionEntrega = direccionEntrega;
    }

    public double getDistanciaKm() {
        return distanciaKm;
    }

    /**
     * Asigna la distancia validando que no sea negativa.
     *
     * @param distanciaKm distancia en kilometros, mayor o igual a cero
     * @throws IllegalArgumentException si la distancia es negativa
     */
    public void setDistanciaKm(double distanciaKm) {
        this.distanciaKm = validarDistancia(distanciaKm);
    }

    @Override
    public String toString() {
        return getTipoEntrega() + " " + idPedido + " (" + distanciaKm + " km, "
                + calcularTiempoEntrega() + " min)";
    }
}
