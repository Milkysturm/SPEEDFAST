package com.speedfast.servicio;

import com.speedfast.contrato.Cancelable;
import com.speedfast.contrato.Despachable;
import com.speedfast.contrato.Rastreable;
import com.speedfast.modelo.EstadoPedido;
import com.speedfast.modelo.Pedido;

import java.util.ArrayList;
import java.util.List;

/**
 * Coordina la operacion diaria de SpeedFast.
 *
 * Implementa las mismas tres interfaces que Pedido, pero con otro alcance:
 * mientras un pedido se despacha, se cancela y se rastrea a si mismo, el
 * controlador lo hace sobre el lote completo de la jornada. Esa es la ventaja
 * de trabajar con interfaces: dos clases sin relacion de herencia entre si
 * comparten el mismo vocabulario y pueden usarse de forma intercambiable
 * donde solo interese "algo despachable".
 *
 * El controlador no sabe como se calcula un tiempo ni como se elige un
 * repartidor: eso vive en la jerarquia de Pedido. Aca solo se registra,
 * se ordena y se lleva la cuenta.
 *
 * @author Olga Rivas
 * @version 3.0
 */
public class ControladorDeEnvios implements Despachable, Cancelable, Rastreable {

    /** Pedidos ingresados al sistema durante la jornada. */
    private final List<Pedido> pedidos;

    /** Pedidos que efectivamente salieron a ruta. */
    private final List<Pedido> entregasRealizadas;

    /** Bitacora global de la operacion. */
    private final List<String> historial;

    /** Crea un controlador con la jornada vacia. */
    public ControladorDeEnvios() {
        this.pedidos = new ArrayList<>();
        this.entregasRealizadas = new ArrayList<>();
        this.historial = new ArrayList<>();
    }

    // ------------------------------------------------------------------
    // Gestion de la jornada
    // ------------------------------------------------------------------

    /**
     * Reserva un pedido: es la primera interaccion del cliente con el sistema.
     * El pedido queda ingresado en la jornada, en estado RESERVADO y todavia
     * sin repartidor.
     *
     * @param pedido pedido a reservar
     * @return true si la reserva se registro; false si el pedido es null o ya
     *         estaba reservado
     */
    public boolean reservar(Pedido pedido) {
        if (pedido == null || pedidos.contains(pedido)) {
            System.out.println("   Reserva rechazada: pedido nulo o ya reservado.");
            return false;
        }
        pedidos.add(pedido);
        anotar("Reserva de " + pedido.getTipoEntrega() + " " + pedido.getIdPedido()
                + " con destino a " + pedido.getDireccionEntrega());
        System.out.printf("   %-8s %-16s reservado para %s%n",
                pedido.getIdPedido(), pedido.getTipoEntrega(),
                pedido.getDireccionEntrega());
        return true;
    }

    /**
     * Busca un pedido por su identificador.
     *
     * @param idPedido identificador buscado
     * @return el pedido encontrado, o null si no existe
     */
    public Pedido buscarPedido(String idPedido) {
        for (Pedido pedido : pedidos) {
            if (pedido.getIdPedido().equalsIgnoreCase(idPedido)) {
                return pedido;
            }
        }
        return null;
    }

    /**
     * Asigna repartidor automaticamente a todos los pedidos que aun no tienen
     * uno. La llamada es identica para los tres tipos: es Java, en tiempo de
     * ejecucion, quien decide que version de asignarRepartidor() ejecutar.
     */
    public void asignarRepartidoresAutomaticamente() {
        for (Pedido pedido : pedidos) {
            if (pedido.getEstado() == EstadoPedido.RESERVADO) {
                String resultado = pedido.asignarRepartidor();
                System.out.println("   " + pedido.getIdPedido() + " -> " + resultado);
                anotar("Asignacion automatica en " + pedido.getIdPedido()
                        + ": " + resultado);
            }
        }
    }

    // ------------------------------------------------------------------
    // Despachable
    // ------------------------------------------------------------------

    /**
     * Despacha todos los pedidos que ya tienen repartidor asignado.
     *
     * @return true si al menos un pedido salio a ruta
     */
    @Override
    public boolean despachar() {
        int despachados = 0;
        for (Pedido pedido : pedidos) {
            if (pedido.getEstado() == EstadoPedido.ASIGNADO && pedido.despachar()) {
                entregasRealizadas.add(pedido);
                despachados++;
                System.out.println("   " + pedido.getIdPedido() + " en ruta con "
                        + pedido.getNombreRepartidor());
            } else if (pedido.getEstado() != EstadoPedido.DESPACHADO) {
                System.out.println("   " + pedido.getIdPedido() + " no se despacha ("
                        + pedido.getEstado().getDescripcion() + ")");
            }
        }
        anotar("Despacho del lote: " + despachados + " pedido(s) en ruta.");
        return despachados > 0;
    }

    /**
     * Despacha el lote dejando una observacion comun para toda la jornada.
     *
     * Sobrecarga de {@link #despachar()}.
     *
     * @param observacion indicacion general para los repartidores
     * @return true si al menos un pedido salio a ruta
     */
    public boolean despachar(String observacion) {
        boolean resultado = despachar();
        anotar("Observacion general del despacho: " + observacion);
        return resultado;
    }

    // ------------------------------------------------------------------
    // Cancelable
    // ------------------------------------------------------------------

    /**
     * Cancela todos los pedidos que todavia no salieron a ruta, por ejemplo
     * al cerrar la operacion del dia.
     *
     * @return true si se cancelo al menos un pedido
     */
    @Override
    public boolean cancelar() {
        int cancelados = 0;
        for (Pedido pedido : pedidos) {
            if (pedido.cancelar()) {
                cancelados++;
            }
        }
        anotar("Cierre de operacion: " + cancelados + " pedido(s) cancelado(s).");
        return cancelados > 0;
    }

    /**
     * Cancela un pedido puntual identificado por su ID.
     *
     * Sobrecarga de {@link #cancelar()}.
     *
     * @param idPedido identificador del pedido a cancelar
     * @param motivo   razon de la cancelacion
     * @return true si el pedido existia y quedo cancelado
     */
    public boolean cancelar(String idPedido, String motivo) {
        Pedido pedido = buscarPedido(idPedido);
        if (pedido == null) {
            System.out.println("   No existe el pedido " + idPedido);
            anotar("Intento de cancelar un pedido inexistente: " + idPedido);
            return false;
        }
        boolean resultado = pedido.cancelar(motivo);
        if (resultado) {
            entregasRealizadas.remove(pedido);
            System.out.println("   " + idPedido + " cancelado. Motivo: " + motivo);
            anotar("Cancelado " + idPedido + " por: " + motivo);
        } else {
            System.out.println("   " + idPedido + " no se puede cancelar ("
                    + pedido.getEstado().getDescripcion() + ")");
            anotar("Cancelacion rechazada en " + idPedido);
        }
        return resultado;
    }

    // ------------------------------------------------------------------
    // Rastreable
    // ------------------------------------------------------------------

    /** Muestra el historial global de la operacion y las entregas realizadas. */
    @Override
    public void verHistorial() {
        System.out.println("Movimientos de la jornada:");
        for (int i = 0; i < historial.size(); i++) {
            System.out.println("   " + (i + 1) + ". " + historial.get(i));
        }

        System.out.println();
        System.out.println("Entregas despachadas: " + entregasRealizadas.size());
        if (entregasRealizadas.isEmpty()) {
            System.out.println("   (ninguna)");
            return;
        }
        for (Pedido pedido : entregasRealizadas) {
            System.out.println("   - " + pedido.getIdPedido() + " | "
                    + pedido.getTipoEntrega() + " | "
                    + pedido.getNombreRepartidor() + " | "
                    + pedido.calcularTiempoEntrega() + " min");
        }
    }

    /**
     * Muestra el historial de un pedido concreto.
     *
     * Sobrecarga de {@link #verHistorial()}: el controlador delega en la
     * bitacora que cada pedido lleva de si mismo.
     *
     * @param idPedido identificador del pedido a consultar
     */
    public void verHistorial(String idPedido) {
        Pedido pedido = buscarPedido(idPedido);
        if (pedido == null) {
            System.out.println("No existe el pedido " + idPedido);
            return;
        }
        pedido.verHistorial();
    }

    /**
     * Anota un hecho en la bitacora global.
     *
     * @param evento descripcion de lo ocurrido
     */
    private void anotar(String evento) {
        historial.add(evento);
    }

    // ------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------

    /**
     * @return copia de los pedidos ingresados
     */
    public List<Pedido> getPedidos() {
        return new ArrayList<>(pedidos);
    }

    /**
     * @return copia de las entregas que salieron a ruta
     */
    public List<Pedido> getEntregasRealizadas() {
        return new ArrayList<>(entregasRealizadas);
    }

    /**
     * @return cantidad de pedidos ingresados en la jornada
     */
    public int getTotalPedidos() {
        return pedidos.size();
    }
}
