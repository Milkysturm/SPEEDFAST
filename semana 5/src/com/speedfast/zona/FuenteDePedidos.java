package com.speedfast.zona;

import com.speedfast.modelo.Pedido;

/**
 * Contrato de todo lugar del que un repartidor puede sacar pedidos.
 *
 * Lo implementan las dos versiones de la zona de carga, la sincronizada y la
 * insegura, de modo que la clase Repartidor funciona igual con cualquiera de
 * las dos.
 *
 * @author Olga Rivas
 * @version 5.0
 */
public interface FuenteDePedidos {

    /**
     * Deja un pedido disponible para ser retirado.
     *
     * @param pedido pedido que llega a la zona
     */
    void agregarPedido(Pedido pedido);

    /**
     * Entrega un pedido a quien lo pide, y solo a uno.
     *
     * @return el pedido retirado, o null si ya no queda nada por repartir
     */
    Pedido retirarPedido();

    /**
     * Avisa que no llegaran mas pedidos, para que quien este esperando pueda
     * terminar su turno.
     */
    void cerrarRecepcion();

    /**
     * @return cantidad de pedidos que aun esperan en la zona
     */
    int getPedidosPendientes();
}
