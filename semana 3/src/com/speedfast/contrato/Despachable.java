package com.speedfast.contrato;

/**
 * Contrato de todo elemento del sistema que puede ser despachado, es decir,
 * puesto en ruta hacia su destino.
 *
 * Se declara como interfaz y no como metodo de la clase Pedido porque
 * "despachar" no es exclusivo de un pedido: el ControladorDeEnvios tambien
 * despacha, pero lo hace sobre un lote completo. Al separar el contrato,
 * ambas clases comparten el vocabulario sin compartir la implementacion.
 *
 * @author Olga Rivas
 * @version 3.0
 */
public interface Despachable {

    /**
     * Pone el elemento en ruta.
     *
     * @return true si el despacho se pudo realizar, false si el estado actual
     *         no lo permite (por ejemplo, si ya fue cancelado)
     */
    boolean despachar();
}
