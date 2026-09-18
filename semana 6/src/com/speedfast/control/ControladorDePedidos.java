package com.speedfast.control;

import com.speedfast.contrato.EscuchaDeEntregas;
import com.speedfast.modelo.EstadoPedido;
import com.speedfast.modelo.Pedido;
import com.speedfast.modelo.PedidoComida;
import com.speedfast.modelo.PedidoEncomienda;
import com.speedfast.modelo.PedidoExpress;
import com.speedfast.modelo.Repartidor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Guarda los datos del sistema en memoria y coordina las operaciones que
 * piden las ventanas.
 *
 * Es el punto unico donde viven la lista de pedidos y la de repartidores, de
 * modo que las tres ventanas trabajan sobre los mismos datos. Cuando algo
 * cambia avisa a sus observadores, que es como la tabla se entera de que hay
 * un pedido nuevo.
 *
 * No contiene codigo de Swing a proposito: las ventanas son las que se
 * encargan de actualizarse en el hilo grafico.
 *
 * @author Olga Rivas
 * @version 6.0
 */
public class ControladorDePedidos {

    /** Pedidos registrados en el sistema. */
    private final List<Pedido> pedidos = new ArrayList<>();

    /** Repartidores disponibles. */
    private final List<Repartidor> repartidores = new ArrayList<>();

    /** Acciones que se ejecutan cuando los datos cambian. */
    private final List<Runnable> observadores = new ArrayList<>();

    /** Pool de hilos de la simulacion en curso, o null si no hay ninguna. */
    private ExecutorService pool;

    /**
     * Crea el controlador con los repartidores de la empresa y algunos
     * pedidos de ejemplo, para que la aplicacion no parta vacia.
     */
    public ControladorDePedidos() {
        repartidores.add(new Repartidor("Camila Soto", "moto con mochila termica"));
        repartidores.add(new Repartidor("Diego Fuentes", "furgon de carga"));
        repartidores.add(new Repartidor("Luis Vera", "bicicleta electrica"));

        pedidos.add(new PedidoComida("P-001", "Av. Providencia 1234, Santiago", 3.5,
                "Sushi Kai", true));
        pedidos.add(new PedidoEncomienda("P-002", "Calle Los Aromos 456, Maipu", 8.0,
                25.5, "Caja reforzada"));
        pedidos.add(new PedidoExpress("P-003", "Pasaje El Roble 789, La Florida", 2.0,
                "Farmacia Central", true));
    }

    // ------------------------------------------------------------------
    // Pedidos
    // ------------------------------------------------------------------

    /**
     * Agrega un pedido a la lista en memoria y avisa del cambio.
     *
     * @param pedido pedido a registrar
     * @throws IllegalArgumentException si el pedido es nulo o el id ya existe
     */
    public void agregarPedido(Pedido pedido) {
        if (pedido == null) {
            throw new IllegalArgumentException("El pedido no puede ser nulo.");
        }
        if (existeId(pedido.getIdPedido())) {
            throw new IllegalArgumentException("Ya existe un pedido con el id "
                    + pedido.getIdPedido() + ".");
        }
        pedidos.add(pedido);
        notificarCambio();
    }

    /**
     * Indica si ya hay un pedido registrado con ese identificador.
     *
     * @param id identificador a buscar
     * @return true si el id ya esta en uso
     */
    public boolean existeId(String id) {
        return buscarPorId(id) != null;
    }

    /**
     * Busca un pedido por su identificador.
     *
     * @param id identificador buscado
     * @return el pedido encontrado, o null si no existe
     */
    public Pedido buscarPorId(String id) {
        if (id == null) {
            return null;
        }
        for (Pedido pedido : pedidos) {
            if (pedido.getIdPedido().equalsIgnoreCase(id.trim())) {
                return pedido;
            }
        }
        return null;
    }

    /**
     * Propone el siguiente identificador libre con el formato P-000.
     *
     * Recorre los ids que siguen ese formato, se queda con el numero mas alto
     * y devuelve el siguiente. Asi el formulario puede sugerirlo y la usuaria
     * no tiene que inventarlo.
     *
     * @return identificador sugerido para el proximo pedido
     */
    public String sugerirIdPedido() {
        int mayor = 0;
        for (Pedido pedido : pedidos) {
            String id = pedido.getIdPedido();
            if (id != null && id.toUpperCase().startsWith("P-")) {
                try {
                    mayor = Math.max(mayor, Integer.parseInt(id.substring(2).trim()));
                } catch (NumberFormatException e) {
                    // Un id que no termina en numero simplemente no cuenta.
                }
            }
        }
        return String.format("P-%03d", mayor + 1);
    }

    /**
     * @return copia de todos los pedidos registrados
     */
    public List<Pedido> getPedidos() {
        return new ArrayList<>(pedidos);
    }

    /**
     * Devuelve los pedidos que estan en un estado determinado.
     *
     * @param estado estado buscado
     * @return copia de los pedidos que coinciden
     */
    public List<Pedido> getPedidosPorEstado(EstadoPedido estado) {
        List<Pedido> filtrados = new ArrayList<>();
        for (Pedido pedido : pedidos) {
            if (pedido.getEstado() == estado) {
                filtrados.add(pedido);
            }
        }
        return filtrados;
    }

    /**
     * @return cantidad de pedidos registrados
     */
    public int getTotalPedidos() {
        return pedidos.size();
    }

    /**
     * Cuenta los pedidos que estan en un estado determinado.
     *
     * @param estado estado buscado
     * @return cantidad de coincidencias
     */
    public int contarPorEstado(EstadoPedido estado) {
        return getPedidosPorEstado(estado).size();
    }

    // ------------------------------------------------------------------
    // Repartidores y entregas
    // ------------------------------------------------------------------

    /**
     * @return copia de los repartidores disponibles
     */
    public List<Repartidor> getRepartidores() {
        return new ArrayList<>(repartidores);
    }

    /**
     * Asigna un pedido a un repartidor.
     *
     * @param pedido     pedido a asignar
     * @param repartidor repartidor que se hara cargo
     * @return true si la asignacion se realizo
     */
    public boolean asignarRepartidor(Pedido pedido, Repartidor repartidor) {
        if (pedido == null || repartidor == null) {
            return false;
        }
        if (pedido.getEstado() != EstadoPedido.RESERVADO) {
            return false;
        }
        boolean asignado = repartidor.asignarPedido(pedido);
        if (asignado) {
            notificarCambio();
        }
        return asignado;
    }

    /**
     * Cancela un pedido y lo saca de la ruta del repartidor que lo tuviera.
     *
     * La decision de si el pedido se puede cancelar la toma el propio pedido:
     * uno que ya va en ruta la rechaza.
     *
     * @param pedido pedido a cancelar
     * @param motivo razon de la cancelacion
     * @return true si el pedido quedo cancelado
     */
    public boolean cancelarPedido(Pedido pedido, String motivo) {
        if (pedido == null) {
            return false;
        }
        boolean cancelado = pedido.cancelar(motivo);
        if (cancelado) {
            for (Repartidor repartidor : repartidores) {
                repartidor.quitarPedido(pedido);
            }
            notificarCambio();
        }
        return cancelado;
    }

    /**
     * Lanza las entregas de todos los repartidores que tengan pedidos
     * asignados, cada uno en su propio hilo.
     *
     * El metodo vuelve de inmediato: los hilos siguen trabajando en segundo
     * plano e informan su avance al escucha. Es lo que permite que la ventana
     * no se congele mientras dura la simulacion.
     *
     * @param escucha objeto que recibira los avisos de avance
     * @return cantidad de repartidores que salieron a ruta
     */
    public int iniciarEntregas(EscuchaDeEntregas escucha) {
        List<Repartidor> conTrabajo = new ArrayList<>();
        for (Repartidor repartidor : repartidores) {
            repartidor.limpiarEntregados();
            if (repartidor.getTotalPedidos() > 0) {
                conTrabajo.add(repartidor);
            }
        }

        if (conTrabajo.isEmpty()) {
            return 0;
        }

        pool = Executors.newFixedThreadPool(conTrabajo.size());
        for (Repartidor repartidor : conTrabajo) {
            repartidor.setEscucha(escucha);
            pool.execute(repartidor);
        }
        // No se aceptan mas tareas, pero las enviadas se ejecutan completas.
        pool.shutdown();

        return conTrabajo.size();
    }

    /**
     * Indica si hay una simulacion todavia en curso.
     *
     * @return true si quedan hilos trabajando
     */
    public boolean hayEntregasEnCurso() {
        return pool != null && !pool.isTerminated();
    }

    /**
     * Detiene la simulacion en curso, si la hay. Se llama al cerrar la
     * aplicacion para no dejar hilos vivos.
     */
    public void detenerEntregas() {
        if (pool != null) {
            pool.shutdownNow();
        }
    }

    // ------------------------------------------------------------------
    // Observadores
    // ------------------------------------------------------------------

    /**
     * Registra una accion que se ejecutara cada vez que los datos cambien.
     *
     * Las ventanas la usan para refrescar sus tablas y contadores.
     *
     * @param observador accion a ejecutar ante un cambio
     */
    public void agregarObservador(Runnable observador) {
        if (observador != null) {
            observadores.add(observador);
        }
    }

    /**
     * Avisa a todos los observadores que los datos cambiaron.
     *
     * Puede llamarse desde un hilo de reparto, asi que cada observador es
     * responsable de actualizar la interfaz en el hilo grafico.
     */
    public void notificarCambio() {
        for (Runnable observador : new ArrayList<>(observadores)) {
            observador.run();
        }
    }
}
