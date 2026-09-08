package com.speedfast.zona;

import com.speedfast.modelo.EstadoPedido;
import com.speedfast.modelo.Pedido;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de concurrencia de la zona de carga.
 *
 * Cada prueba lanza varios hilos que vacian la zona al mismo tiempo y
 * comprueba que cada pedido salga exactamente una vez y que no se pierda
 * ninguno. Las pruebas se repiten varias veces con @RepeatedTest, ya que un
 * error de sincronizacion puede no aparecer en una sola corrida.
 *
 * Los hilos de prueba son consumidores minimos y no la clase Repartidor,
 * porque lo que se esta probando es la zona de carga.
 *
 * @author Olga Rivas
 * @version 5.0
 */
class ZonaDeCargaConcurrenciaTest {

    /** Pedidos que se cargan en cada repeticion. */
    private static final int TOTAL_PEDIDOS = 200;

    /** Hilos que compiten por la zona. */
    private static final int TOTAL_HILOS = 6;

    @RepeatedTest(20)
    @DisplayName("Cada pedido lo retira un unico repartidor, en 20 repeticiones")
    void ningunPedidoSaleDosVeces() throws InterruptedException {
        ZonaDeCarga zona = new ZonaDeCarga();
        cargar(zona, TOTAL_PEDIDOS);
        zona.cerrarRecepcion();

        List<Integer> retirados = vaciarEnParalelo(zona, TOTAL_HILOS);

        Set<Integer> distintos = new HashSet<>(retirados);
        assertEquals(TOTAL_PEDIDOS, retirados.size(),
                "la cantidad de retiros debe coincidir con la de pedidos cargados");
        assertEquals(TOTAL_PEDIDOS, distintos.size(),
                "ningun pedido puede haber sido retirado dos veces");
        assertEquals(0, zona.getPedidosPendientes(),
                "la zona debe quedar vacia");
        assertEquals(TOTAL_PEDIDOS, zona.getTotalRetirados());
    }

    @RepeatedTest(10)
    @DisplayName("Todo pedido retirado queda en estado EN_REPARTO")
    void todoPedidoRetiradoQuedaEnReparto() throws InterruptedException {
        ZonaDeCarga zona = new ZonaDeCarga();
        List<Pedido> pedidos = cargar(zona, 60);
        zona.cerrarRecepcion();

        vaciarEnParalelo(zona, 4);

        for (Pedido pedido : pedidos) {
            assertEquals(EstadoPedido.EN_REPARTO, pedido.getEstado(),
                    "el pedido #" + pedido.getId() + " salio de la zona sin cambiar de estado");
        }
    }

    @Test
    @DisplayName("Los pedidos que llegan durante el turno tambien se reparten")
    void losPedidosTardiosTambienSeReparten() throws InterruptedException {
        ZonaDeCarga zona = new ZonaDeCarga();
        cargar(zona, 20);

        ExecutorService pool = Executors.newFixedThreadPool(4);
        ConcurrentLinkedQueue<Integer> retirados = new ConcurrentLinkedQueue<>();
        for (int i = 0; i < 4; i++) {
            pool.execute(() -> consumir(zona, retirados));
        }

        // Espera a que los cuatro consumidores agoten la zona y queden
        // dormidos, para que los pedidos nuevos los despierten.
        long limite = System.currentTimeMillis() + 5000;
        while (zona.getRepartidoresEnEspera() < 4 && System.currentTimeMillis() < limite) {
            Thread.sleep(10);
        }
        assertEquals(4, zona.getRepartidoresEnEspera(),
                "los cuatro consumidores debian quedar dormidos esperando trabajo");

        // Con los consumidores ya dormidos entran 20 pedidos mas.
        for (int i = 21; i <= 40; i++) {
            zona.agregarPedido(new Pedido(i, "Destino " + i));
        }
        zona.cerrarRecepcion();

        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS), "los consumidores debian terminar");

        assertEquals(40, retirados.size());
        assertEquals(40, new HashSet<>(retirados).size(), "hubo pedidos retirados mas de una vez");
    }

    /**
     * Comprueba que la version sin sincronizar si pierde o duplica pedidos.
     *
     * Es el control del experimento: confirma que el problema que resuelve
     * ZonaDeCarga existe de verdad. Se intenta varias veces porque una
     * condicion de carrera no aparece siempre.
     */
    @Test
    @DisplayName("La zona SIN sincronizar si pierde o duplica pedidos (control del experimento)")
    void laZonaSinSincronizarFalla() throws InterruptedException {
        boolean apareceElError = false;

        for (int intento = 1; intento <= 10 && !apareceElError; intento++) {
            ZonaDeCargaInsegura zona = new ZonaDeCargaInsegura();
            for (int i = 1; i <= 40; i++) {
                zona.agregarPedido(new Pedido(i, "Destino " + i));
            }

            List<Integer> retirados = vaciarEnParalelo(zona, 4);
            boolean hayDuplicados = new HashSet<>(retirados).size() != retirados.size();
            boolean faltanPedidos = retirados.size() != 40;

            apareceElError = hayDuplicados || faltanPedidos;
        }

        assertTrue(apareceElError,
                "se esperaba que la version sin sincronizar fallara en alguno de los 10 intentos");
    }

    // ------------------------------------------------------------------
    // Apoyo
    // ------------------------------------------------------------------

    /**
     * Carga la zona con pedidos numerados desde 1.
     *
     * @param zona     zona a llenar
     * @param cantidad cantidad de pedidos
     * @return los pedidos creados
     */
    private List<Pedido> cargar(FuenteDePedidos zona, int cantidad) {
        List<Pedido> pedidos = new ArrayList<>();
        for (int i = 1; i <= cantidad; i++) {
            Pedido pedido = new Pedido(i, "Destino " + i);
            zona.agregarPedido(pedido);
            pedidos.add(pedido);
        }
        return pedidos;
    }

    /**
     * Vacia la zona con varios hilos compitiendo y devuelve los ids retirados.
     *
     * El CountDownLatch hace que todos los hilos arranquen a la vez, para que
     * haya competencia real por los pedidos.
     *
     * @param zona  zona a vaciar
     * @param hilos cantidad de consumidores simultaneos
     * @return ids de los pedidos retirados, en el orden en que salieron
     * @throws InterruptedException si la espera es interrumpida
     */
    private List<Integer> vaciarEnParalelo(FuenteDePedidos zona, int hilos) throws InterruptedException {
        ConcurrentLinkedQueue<Integer> retirados = new ConcurrentLinkedQueue<>();
        CountDownLatch partida = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(hilos);

        for (int i = 0; i < hilos; i++) {
            pool.execute(() -> {
                try {
                    partida.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                consumir(zona, retirados);
            });
        }

        partida.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS),
                "los consumidores quedaron bloqueados: posible deadlock");

        return new ArrayList<>(retirados);
    }

    /**
     * Retira pedidos hasta que no quede ninguno.
     *
     * @param zona      zona de la que se retira
     * @param retirados coleccion donde se anotan los ids retirados
     */
    private void consumir(FuenteDePedidos zona, ConcurrentLinkedQueue<Integer> retirados) {
        while (true) {
            Pedido pedido;
            try {
                pedido = zona.retirarPedido();
            } catch (RuntimeException e) {
                // La version insegura puede lanzar excepciones; para esta
                // prueba eso tambien cuenta como pedido perdido.
                return;
            }
            if (pedido == null) {
                return;
            }
            retirados.add(pedido.getId());
        }
    }
}
