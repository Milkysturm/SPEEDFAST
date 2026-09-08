package com.speedfast.modelo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de la clase Pedido: creacion, validacion de los datos de entrada y
 * cambio de estado con las dos versiones de setEstado.
 *
 * @author Olga Rivas
 * @version 5.0
 */
class PedidoTest {

    @Test
    @DisplayName("Un pedido recien creado esta PENDIENTE y sin repartidor")
    void pedidoNuevoEstaPendiente() {
        Pedido pedido = new Pedido(1, "Santiago Centro");

        assertEquals(1, pedido.getId());
        assertEquals("Santiago Centro", pedido.getDireccionEntrega());
        assertEquals(EstadoPedido.PENDIENTE, pedido.getEstado());
        assertTrue(pedido.estaPendiente());
        assertFalse(pedido.estaEntregado());
        assertEquals(null, pedido.getRepartidorAsignado());
    }

    @Test
    @DisplayName("No se puede crear un pedido con id no positivo")
    void idInvalidoEsRechazado() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new Pedido(0, "Providencia"));

        assertTrue(error.getMessage().contains("positivo"));
    }

    @Test
    @DisplayName("No se puede crear un pedido sin direccion de entrega")
    void direccionVaciaEsRechazada() {
        assertThrows(IllegalArgumentException.class, () -> new Pedido(1, "   "));
        assertThrows(IllegalArgumentException.class, () -> new Pedido(1, null));
    }

    @Test
    @DisplayName("setEstado acepta el enum y tambien su nombre en texto")
    void setEstadoAceptaEnumYTexto() {
        Pedido pedido = new Pedido(7, "Nunoa");

        pedido.setEstado(EstadoPedido.EN_REPARTO);
        assertEquals(EstadoPedido.EN_REPARTO, pedido.getEstado());

        pedido.setEstado("entregado");
        assertEquals(EstadoPedido.ENTREGADO, pedido.getEstado());
        assertTrue(pedido.estaEntregado());
    }

    @Test
    @DisplayName("Un estado escrito mal se detecta al momento, no despues")
    void estadoInvalidoEsRechazado() {
        Pedido pedido = new Pedido(7, "Nunoa");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> pedido.setEstado("ENTREGDO"));

        assertTrue(error.getMessage().contains("ENTREGDO"));
        assertEquals(EstadoPedido.PENDIENTE, pedido.getEstado(),
                "un estado invalido no debe dejar el pedido a medias");
    }

    @Test
    @DisplayName("El estado nulo tampoco pasa")
    void estadoNuloEsRechazado() {
        Pedido pedido = new Pedido(7, "Nunoa");

        assertThrows(IllegalArgumentException.class, () -> pedido.setEstado((EstadoPedido) null));
        assertThrows(IllegalArgumentException.class, () -> pedido.setEstado((String) null));
    }

    @Test
    @DisplayName("toString muestra id, estado, destino y responsable")
    void toStringEsInformativo() {
        Pedido pedido = new Pedido(3, "Recoleta");
        pedido.setRepartidorAsignado("Juan");

        String texto = pedido.toString();

        assertTrue(texto.contains("#3"));
        assertTrue(texto.contains("PENDIENTE"));
        assertTrue(texto.contains("Recoleta"));
        assertTrue(texto.contains("Juan"));
    }
}
