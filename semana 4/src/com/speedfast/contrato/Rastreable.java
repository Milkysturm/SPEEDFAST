package com.speedfast.contrato;

/**
 * Contrato de todo elemento que lleva registro de lo que le fue ocurriendo
 * y puede mostrarlo.
 *
 * Cada Pedido rastrea su propia bitacora; el ControladorDeEnvios rastrea el
 * historial global de la operacion. Misma capacidad, dos alcances distintos.
 *
 * @author Olga Rivas
 * @version 3.0
 */
public interface Rastreable {

    /** Muestra por consola el registro de eventos del elemento. */
    void verHistorial();
}
