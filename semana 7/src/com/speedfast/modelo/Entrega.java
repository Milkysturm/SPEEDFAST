package com.speedfast.modelo;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Entrega realizada: relaciona un pedido con el repartidor que lo llevo, y
 * deja registrada la fecha y la hora.
 *
 * Es la clase que corresponde a la tabla entrega de la base de datos. Guarda
 * los identificadores de la base (numeros) y no los objetos completos, que es
 * como estan escritas las llaves foraneas.
 *
 * @author Olga Rivas
 * @version 7.0
 */
public class Entrega {

    /** Identificador en la base de datos. Vale cero mientras no se guarda. */
    private int id;

    /** Identificador del pedido entregado. */
    private int idPedido;

    /** Identificador del repartidor que lo entrego. */
    private int idRepartidor;

    /** Fecha de la entrega. */
    private LocalDate fecha;

    /** Hora de la entrega. */
    private LocalTime hora;

    /**
     * Crea una entrega con su fecha y hora.
     *
     * @param idPedido     identificador del pedido
     * @param idRepartidor identificador del repartidor
     * @param fecha        fecha de la entrega
     * @param hora         hora de la entrega
     */
    public Entrega(int idPedido, int idRepartidor, LocalDate fecha, LocalTime hora) {
        this.idPedido = idPedido;
        this.idRepartidor = idRepartidor;
        this.fecha = fecha;
        this.hora = hora;
    }

    /**
     * Crea una entrega con la fecha y la hora de este momento.
     *
     * @param idPedido     identificador del pedido
     * @param idRepartidor identificador del repartidor
     * @return entrega lista para guardar
     */
    public static Entrega ahora(int idPedido, int idRepartidor) {
        return new Entrega(idPedido, idRepartidor, LocalDate.now(), LocalTime.now().withNano(0));
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIdPedido() {
        return idPedido;
    }

    public void setIdPedido(int idPedido) {
        this.idPedido = idPedido;
    }

    public int getIdRepartidor() {
        return idRepartidor;
    }

    public void setIdRepartidor(int idRepartidor) {
        this.idRepartidor = idRepartidor;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public LocalTime getHora() {
        return hora;
    }

    public void setHora(LocalTime hora) {
        this.hora = hora;
    }

    @Override
    public String toString() {
        return "Entrega " + id + ": pedido " + idPedido + " por repartidor "
                + idRepartidor + " el " + fecha + " a las " + hora;
    }
}
