package com.speedfast.contrato;

import com.speedfast.modelo.Pedido;

/**
 * Contrato de quien quiere enterarse del avance de las entregas.
 *
 * Lo implementa la ventana que muestra la simulacion. Gracias a esta interfaz
 * el repartidor informa lo que va haciendo sin saber nada de Swing, y la
 * ventana decide como mostrarlo.
 *
 * @author Olga Rivas
 * @version 6.0
 */
public interface EscuchaDeEntregas {

    /**
     * Avisa que un pedido cambio de estado.
     *
     * @param pedido  pedido afectado
     * @param mensaje descripcion de lo ocurrido
     */
    void pedidoActualizado(Pedido pedido, String mensaje);

    /**
     * Avisa que un repartidor termino su ruta.
     *
     * @param nombreRepartidor nombre del repartidor
     * @param entregas         cantidad de entregas que completo
     */
    void rutaTerminada(String nombreRepartidor, int entregas);
}
