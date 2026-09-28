package com.speedfast.control;

import com.speedfast.contrato.EscuchaDeEntregas;
import com.speedfast.dao.EntregaDAO;
import com.speedfast.dao.PedidoDAO;
import com.speedfast.dao.RepartidorDAO;
import com.speedfast.modelo.Entrega;
import com.speedfast.modelo.EstadoPedido;
import com.speedfast.modelo.Pedido;
import com.speedfast.modelo.Repartidor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Coordina las operaciones que piden las ventanas y mantiene los datos
 * sincronizados con la base de datos.
 *
 * A diferencia de la semana 6, las listas ya no son el lugar donde viven los
 * datos: son una copia en memoria de lo que hay en MySQL, para que la interfaz
 * no tenga que consultar la base cada vez que dibuja una fila. Todo cambio se
 * guarda primero en la base y despues se refleja en la copia.
 *
 * No contiene codigo de Swing a proposito: las ventanas son las que se
 * encargan de actualizarse en el hilo grafico.
 *
 * @author Olga Rivas
 * @version 7.0
 */
public class ControladorDePedidos {

    /** Acceso a la tabla pedido. */
    private final PedidoDAO pedidoDAO = new PedidoDAO();

    /** Acceso a la tabla repartidor. */
    private final RepartidorDAO repartidorDAO = new RepartidorDAO();

    /** Acceso a la tabla entrega. */
    private final EntregaDAO entregaDAO = new EntregaDAO();

    /** Copia en memoria de los pedidos guardados. */
    private final List<Pedido> pedidos = new ArrayList<>();

    /** Copia en memoria de los repartidores guardados. */
    private final List<Repartidor> repartidores = new ArrayList<>();

    /** Acciones que se ejecutan cuando los datos cambian. */
    private final List<Runnable> observadores = new ArrayList<>();

    /** Pool de hilos de la simulacion en curso, o null si no hay ninguna. */
    private ExecutorService pool;

    // ------------------------------------------------------------------
    // Carga inicial
    // ------------------------------------------------------------------

    /**
     * Lee de la base de datos todos los pedidos y repartidores.
     *
     * Se llama al abrir la aplicacion y cada vez que se aprieta Refrescar, de
     * modo que la interfaz siempre muestre lo que hay guardado.
     *
     * @throws SQLException si falla alguna consulta
     */
    public void cargarDesdeBaseDatos() throws SQLException {
        List<Pedido> pedidosGuardados = pedidoDAO.listarTodos();
        List<Repartidor> repartidoresGuardados = repartidorDAO.listarTodos();

        // Se reemplaza el contenido recien cuando las dos consultas salieron
        // bien: si fallara la segunda, la interfaz se quedaria a medio camino.
        pedidos.clear();
        pedidos.addAll(pedidosGuardados);
        repartidores.clear();
        repartidores.addAll(repartidoresGuardados);

        notificarCambio();
    }

    // ------------------------------------------------------------------
    // Pedidos
    // ------------------------------------------------------------------

    /**
     * Guarda un pedido en la base de datos y lo agrega a la lista en memoria.
     *
     * @param pedido pedido a registrar
     * @throws SQLException             si falla la insercion
     * @throws IllegalArgumentException si el pedido es nulo o el codigo ya existe
     */
    public void agregarPedido(Pedido pedido) throws SQLException {
        if (pedido == null) {
            throw new IllegalArgumentException("El pedido no puede ser nulo.");
        }
        if (existeId(pedido.getIdPedido())) {
            throw new IllegalArgumentException("Ya existe un pedido con el id "
                    + pedido.getIdPedido() + ".");
        }

        // Primero la base: si el INSERT falla, el pedido no queda en memoria
        // y la interfaz no muestra algo que en realidad no se guardo.
        pedidoDAO.guardar(pedido);
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
     * Busca un pedido por su identificador visible.
     *
     * @param id identificador buscado
     * @return el pedido encontrado, o null si no existe
     */
    public Pedido buscarPorId(String id) {
        if (id == null) {
            return null;
        }
        for (Pedido pedido : pedidos) {
            if (pedido.getIdPedido() != null
                    && pedido.getIdPedido().equalsIgnoreCase(id.trim())) {
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
    // Repartidores
    // ------------------------------------------------------------------

    /**
     * @return copia de los repartidores disponibles
     */
    public List<Repartidor> getRepartidores() {
        return new ArrayList<>(repartidores);
    }

    /**
     * Guarda un repartidor nuevo en la base de datos.
     *
     * @param repartidor repartidor a registrar
     * @throws SQLException             si falla la insercion
     * @throws IllegalArgumentException si le falta el nombre
     */
    public void agregarRepartidor(Repartidor repartidor) throws SQLException {
        repartidorDAO.guardar(repartidor);
        repartidores.add(repartidor);
        notificarCambio();
    }

    /**
     * Asigna un pedido a un repartidor y guarda el cambio de estado.
     *
     * @param pedido     pedido a asignar
     * @param repartidor repartidor que se hara cargo
     * @return true si la asignacion se realizo
     * @throws SQLException si falla la actualizacion en la base
     */
    public boolean asignarRepartidor(Pedido pedido, Repartidor repartidor) throws SQLException {
        if (pedido == null || repartidor == null) {
            return false;
        }
        if (pedido.getEstado() != EstadoPedido.RESERVADO) {
            return false;
        }
        if (!repartidor.asignarPedido(pedido)) {
            return false;
        }

        pedidoDAO.actualizarEstado(pedido);
        notificarCambio();
        return true;
    }

    /**
     * Cancela un pedido, lo saca de la ruta del repartidor que lo tuviera y
     * guarda el cambio en la base.
     *
     * La decision de si el pedido se puede cancelar la toma el propio pedido:
     * uno que ya va en ruta la rechaza.
     *
     * @param pedido pedido a cancelar
     * @param motivo razon de la cancelacion
     * @return true si el pedido quedo cancelado
     * @throws SQLException si falla la actualizacion en la base
     */
    public boolean cancelarPedido(Pedido pedido, String motivo) throws SQLException {
        if (pedido == null || !pedido.cancelar(motivo)) {
            return false;
        }

        for (Repartidor repartidor : repartidores) {
            repartidor.quitarPedido(pedido);
        }
        pedidoDAO.actualizarEstado(pedido);
        notificarCambio();
        return true;
    }

    // ------------------------------------------------------------------
    // Entregas
    // ------------------------------------------------------------------

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
     * Guarda en la base el estado actual de un pedido y, si acaba de ser
     * entregado, registra la entrega en la tabla correspondiente.
     *
     * La llaman los avisos que vienen de los hilos de reparto, para que lo que
     * ocurre durante la simulacion tambien quede guardado.
     *
     * @param pedido pedido que cambio de estado
     * @throws SQLException si falla la actualizacion o la insercion
     */
    public void sincronizarConBase(Pedido pedido) throws SQLException {
        if (pedido == null || pedido.getId() == 0) {
            return;
        }

        pedidoDAO.actualizarEstado(pedido);

        Repartidor repartidor = pedido.getRepartidor();
        if (pedido.getEstado() == EstadoPedido.ENTREGADO
                && repartidor != null && repartidor.getId() > 0) {
            entregaDAO.guardar(Entrega.ahora(pedido.getId(), repartidor.getId()));
        }
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
