package com.speedfast.zona;

import com.speedfast.modelo.EstadoPedido;
import com.speedfast.modelo.Pedido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;

/**
 * Pruebas del comportamiento basico de ZonaDeCarga: agregar y retirar
 * pedidos, respetar el orden de llegada, rechazar lo que no corresponde y
 * manejar bien la espera cuando la zona esta vacia.
 *
 * @author Olga Rivas
 * @version 5.0
 */
class ZonaDeCargaTest {

    @Test
    @DisplayName("Un pedido agregado queda pendiente y disponible")
    void agregarDejaElPedidoPendiente() {
        ZonaDeCarga zona = new ZonaDeCarga();
        zona.agregarPedido(new Pedido(1, "Santiago Centro"));

        assertEquals(1, zona.getPedidosPendientes());
        assertEquals(1, zona.getTotalRecibidos());
    }

    @Test
    @DisplayName("Retirar entrega el pedido y lo deja EN_REPARTO")
    void retirarCambiaElEstado() {
        ZonaDeCarga zona = new ZonaDeCarga();
        zona.agregarPedido(new Pedido(1, "Santiago Centro"));

        Pedido retirado = zona.retirarPedido();

        assertNotNull(retirado);
        assertEquals(1, retirado.getId());
        assertEquals(EstadoPedido.EN_REPARTO, retirado.getEstado());
        assertEquals(0, zona.getPedidosPendientes());
        assertEquals(1, zona.getTotalRetirados());
    }

    @Test
    @DisplayName("Los pedidos se entregan en el orden en que llegaron")
    void laZonaRespetaElOrdenDeLlegada() {
        ZonaDeCarga zona = new ZonaDeCarga();
        zona.agregarPedido(new Pedido(1, "Santiago Centro"));
        zona.agregarPedido(new Pedido(2, "Providencia"));
        zona.agregarPedido(new Pedido(3, "Nunoa"));

        assertEquals(1, zona.retirarPedido().getId());
        assertEquals(2, zona.retirarPedido().getId());
        assertEquals(3, zona.retirarPedido().getId());
    }

    @Test
    @DisplayName("Con la recepcion cerrada y la zona vacia, retirar devuelve null sin bloquear")
    void zonaCerradaYVaciaDevuelveNull() {
        ZonaDeCarga zona = new ZonaDeCarga();
        zona.cerrarRecepcion();

        // Con tiempo maximo: si la espera estuviera mal escrita, la llamada no
        // volveria nunca y la prueba quedaria colgada en vez de fallar.
        assertTimeoutPreemptively(Duration.ofSeconds(2),
                () -> assertNull(zona.retirarPedido()),
                "retirarPedido no debe quedarse bloqueado cuando ya no llegaran pedidos");
    }

    @Test
    @DisplayName("Con la recepcion cerrada todavia se entrega lo que quedaba dentro")
    void loQueQuedaDentroSeAlcanzaARepartir() {
        ZonaDeCarga zona = new ZonaDeCarga();
        zona.agregarPedido(new Pedido(1, "Santiago Centro"));
        zona.cerrarRecepcion();

        assertNotNull(zona.retirarPedido());
        assertNull(zona.retirarPedido());
    }

    @Test
    @DisplayName("Cerrada la recepcion, no se aceptan pedidos nuevos")
    void noSeAceptanPedidosDespuesDeCerrar() {
        ZonaDeCarga zona = new ZonaDeCarga();
        zona.cerrarRecepcion();

        assertThrows(IllegalStateException.class,
                () -> zona.agregarPedido(new Pedido(1, "Santiago Centro")));
    }

    @Test
    @DisplayName("La zona no acepta pedidos nulos")
    void noSeAceptanPedidosNulos() {
        ZonaDeCarga zona = new ZonaDeCarga();

        assertThrows(IllegalArgumentException.class, () -> zona.agregarPedido(null));
    }

    @Test
    @DisplayName("Un repartidor que espera despierta cuando llega un pedido")
    void laEsperaDespiertaConUnPedidoNuevo() {
        ZonaDeCarga zona = new ZonaDeCarga();

        assertTimeoutPreemptively(Duration.ofSeconds(3), () -> {
            Thread camion = new Thread(() -> {
                try {
                    Thread.sleep(200);
                    zona.agregarPedido(new Pedido(1, "Las Condes"));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            camion.start();

            // Pide un pedido antes de que llegue, asi queda esperando.
            Pedido recibido = zona.retirarPedido();

            assertNotNull(recibido, "el repartidor debia despertar al llegar el pedido");
            assertEquals(1, recibido.getId());
            camion.join();
        });
    }
}
