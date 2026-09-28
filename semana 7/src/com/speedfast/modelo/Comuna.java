package com.speedfast.modelo;

/**
 * Comunas de Santiago donde reparte SpeedFast, con la distancia aproximada
 * desde la central de la empresa.
 *
 * Las distancias son valores referenciales fijos, pensados para que el
 * formulario proponga un numero razonable en vez de dejar que se escriba
 * cualquiera. En un sistema real vendrian de un servicio de geolocalizacion.
 *
 * @author Olga Rivas
 * @version 6.0
 */
public enum Comuna {

    ESTACION_CENTRAL("Estacion Central", 2.0),
    SANTIAGO_CENTRO("Santiago Centro", 3.5),
    INDEPENDENCIA("Independencia", 4.5),
    PROVIDENCIA("Providencia", 5.0),
    RECOLETA("Recoleta", 5.5),
    NUNOA("Nunoa", 6.5),
    SAN_MIGUEL("San Miguel", 7.5),
    MACUL("Macul", 8.5),
    LA_REINA("La Reina", 11.0),
    LAS_CONDES("Las Condes", 12.0),
    LA_FLORIDA("La Florida", 12.5),
    PENALOLEN("Penalolen", 13.0),
    PUDAHUEL("Pudahuel", 13.5),
    VITACURA("Vitacura", 13.5),
    MAIPU("Maipu", 14.0),
    QUILICURA("Quilicura", 15.5),
    SAN_BERNARDO("San Bernardo", 18.0),
    PUENTE_ALTO("Puente Alto", 20.0);

    /** Nombre de la comuna tal como se muestra en la interfaz. */
    private final String nombre;

    /** Distancia aproximada desde la central, en kilometros. */
    private final double distanciaKm;

    /**
     * Constructor del enum.
     *
     * @param nombre      nombre de la comuna
     * @param distanciaKm distancia aproximada desde la central
     */
    Comuna(String nombre, double distanciaKm) {
        this.nombre = nombre;
        this.distanciaKm = distanciaKm;
    }

    /**
     * @return nombre de la comuna
     */
    public String getNombre() {
        return nombre;
    }

    /**
     * @return distancia aproximada desde la central, en kilometros
     */
    public double getDistanciaKm() {
        return distanciaKm;
    }

    /**
     * Devuelve el nombre, que es lo que muestra el combo de la interfaz.
     *
     * @return nombre de la comuna
     */
    @Override
    public String toString() {
        return nombre;
    }
}
