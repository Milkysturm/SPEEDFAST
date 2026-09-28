package com.speedfast.contrato;

/**
 * Contrato de todo elemento que puede ser cancelado antes de completarse.
 *
 * La cancelacion es una responsabilidad distinta del despacho: hay objetos
 * que podrian despacharse sin poder cancelarse. Mantenerlas en interfaces
 * separadas evita obligar a una clase a implementar comportamiento que no
 * necesita.
 *
 * @author Olga Rivas
 * @version 3.0
 */
public interface Cancelable {

    /**
     * Cancela el elemento.
     *
     * @return true si la cancelacion se pudo realizar, false si el estado
     *         actual ya no lo permite
     */
    boolean cancelar();
}
