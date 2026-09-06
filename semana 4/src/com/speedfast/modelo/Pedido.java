package com.speedfast.modelo;

import com.speedfast.contrato.Cancelable;
import com.speedfast.contrato.Despachable;
import com.speedfast.contrato.Rastreable;

import java.util.ArrayList;
import java.util.List;

/**
 * Clase abstracta base de la jerarquia de pedidos de SpeedFast.
 *
 * Reune el estado y el comportamiento comun a todo pedido, y deja abierto
 * a las subclases aquello que cambia segun el tipo de entrega:
 *
 *  - {@link #calcularTiempoEntrega()} : cada tipo aplica su propia formula.
 *  - {@link #getTipoEntrega()}        : nombre del tipo de entrega.
 *  - {@link #getFactorDuracion()}     : factor que afecta la duracion.
 *  - {@link #asignarRepartidor()}     : criterio para elegir al repartidor.
 *
 * Implementa las tres interfaces del sistema porque todo pedido, sea del tipo
 * que sea, se despacha, se puede cancelar y deja rastro de lo que le ocurrio.
 * El "como" lo resuelve aca una sola vez; las subclases solo lo ajustan si su
 * negocio lo exige.
 *
 * No se puede instanciar directamente: no existe "un pedido" sin tipo.
 *
 * Semana 4: un pedido pertenece a la ruta de un unico repartidor, de modo que
 * su estado lo modifica siempre el mismo hilo. Por eso no necesita sincronizar
 * nada; los resultados se leen recien cuando todas las rutas terminaron.
 *
 * @author Olga Rivas
 * @version 4.0
 */
public abstract class Pedido implements Despachable, Cancelable, Rastreable {

    /** Identificador unico del pedido. */
    private String idPedido;

    /** Direccion donde se debe entregar el pedido. */
    private String direccionEntrega;

    /** Distancia en kilometros entre el origen y la direccion de entrega. */
    private double distanciaKm;

    /** Repartidor a cargo. Es null mientras el pedido no ha sido asignado. */
    private Repartidor repartidor;

    /** Situacion actual del pedido dentro del flujo de entrega. */
    private EstadoPedido estado;

    /** Registro de todo lo que le fue ocurriendo al pedido. */
    private final List<String> bitacora;

    /**
     * Duracion real de la entrega simulada, en milisegundos.
     * Vale cero mientras el pedido no haya sido entregado.
     */
    private long duracionRealMs;

    /**
     * Constructor comun a toda la jerarquia.
     *
     * @param idPedido         identificador del pedido
     * @param direccionEntrega direccion de entrega
     * @param distanciaKm      distancia del recorrido en kilometros, no negativa
     * @throws IllegalArgumentException si la distancia es negativa
     */
    public Pedido(String idPedido, String direccionEntrega, double distanciaKm) {
        this.idPedido = idPedido;
        this.direccionEntrega = direccionEntrega;
        this.distanciaKm = validarDistancia(distanciaKm);
        this.estado = EstadoPedido.RESERVADO;
        this.bitacora = new ArrayList<>();
        registrarEvento("Pedido reservado con destino a " + direccionEntrega);
    }

    /**
     * Valida que la distancia no sea negativa, para evitar que un dato
     * erroneo produzca tiempos de entrega negativos.
     *
     * @param distanciaKm distancia a validar
     * @return la misma distancia si es valida
     * @throws IllegalArgumentException si la distancia es negativa
     */
    private static double validarDistancia(double distanciaKm) {
        if (distanciaKm < 0) {
            throw new IllegalArgumentException(
                    "La distancia no puede ser negativa: " + distanciaKm);
        }
        return distanciaKm;
    }

    // ------------------------------------------------------------------
    // Contrato que deben cumplir las subclases (sobrescritura)
    // ------------------------------------------------------------------

    /**
     * Calcula el tiempo estimado de entrega en minutos.
     * Cada subclase aplica la formula que corresponde a su tipo de servicio.
     *
     * @return tiempo estimado en minutos
     */
    public abstract int calcularTiempoEntrega();

    /**
     * Nombre del tipo de entrega, usado en los reportes.
     *
     * @return descripcion del tipo de entrega
     */
    public abstract String getTipoEntrega();

    /**
     * Factor propio del tipo de pedido que afecta la duracion de la entrega.
     *
     * @return descripcion del factor que incide en el tiempo
     */
    public abstract String getFactorDuracion();

    /**
     * Asignacion AUTOMATICA del repartidor.
     *
     * Cada subclase decide a quien elegir segun su propia regla de negocio
     * (mochila termica, vehiculo de carga, cercania) y deja al elegido
     * guardado en el pedido mediante {@link #registrarAsignacion}.
     *
     * @return mensaje que describe la asignacion realizada
     */
    public abstract String asignarRepartidor();

    // ------------------------------------------------------------------
    // Asignacion manual (sobrecarga del metodo anterior)
    // ------------------------------------------------------------------

    /**
     * Asignacion MANUAL: el operador indica el nombre y el sistema deja el
     * vehiculo pendiente de confirmar.
     *
     * Es una sobrecarga de {@link #asignarRepartidor()}: mismo nombre, misma
     * finalidad, distinta lista de parametros. Se implementa una sola vez en
     * la clase abstracta porque una asignacion a dedo no depende del tipo de
     * pedido.
     *
     * @param nombre nombre del repartidor elegido por el operador
     * @return mensaje que describe la asignacion realizada
     */
    public String asignarRepartidor(String nombre) {
        return registrarAsignacion(new Repartidor(nombre),
                "asignacion manual del operador");
    }

    /**
     * Asignacion MANUAL indicando ademas el vehiculo.
     *
     * Segunda sobrecarga del mismo metodo, para cuando el operador si conoce
     * el vehiculo con el que saldra el repartidor.
     *
     * @param nombre   nombre del repartidor elegido por el operador
     * @param vehiculo vehiculo con el que realizara la entrega
     * @return mensaje que describe la asignacion realizada
     */
    public String asignarRepartidor(String nombre, String vehiculo) {
        return registrarAsignacion(new Repartidor(nombre, vehiculo),
                "asignacion manual del operador con vehiculo indicado");
    }

    /**
     * Asignacion a una RUTA: el pedido queda a cargo de un repartidor que ya
     * existe, en lugar de crear uno nuevo a partir de su nombre.
     *
     * Es la sobrecarga que necesita la semana 4, porque ahora el repartidor
     * es ademas la tarea que corre en un hilo: el pedido tiene que apuntar a
     * ese mismo objeto y no a una copia con el mismo nombre.
     *
     * @param repartidor repartidor que se hara cargo de la entrega
     * @return mensaje que describe la asignacion realizada
     */
    public String asignarRepartidor(Repartidor repartidor) {
        if (repartidor == null) {
            String aviso = "No se puede asignar un repartidor nulo al pedido " + idPedido + ".";
            registrarEvento(aviso);
            return aviso;
        }
        return registrarAsignacion(repartidor, "incorporado a la ruta de " + repartidor.getNombre());
    }

    /**
     * Guarda al repartidor en el pedido, actualiza el estado y deja el hecho
     * anotado en la bitacora.
     *
     * Existe para que las tres formas de asignar (automatica y las dos
     * manuales) compartan exactamente el mismo efecto y no se repita codigo.
     *
     * @param repartidor repartidor que queda a cargo
     * @param criterio   razon por la que fue elegido
     * @return mensaje que describe la asignacion realizada
     */
    protected String registrarAsignacion(Repartidor repartidor, String criterio) {
        if (estado == EstadoPedido.CANCELADO) {
            String aviso = "No se puede asignar repartidor: el pedido "
                    + idPedido + " esta cancelado.";
            registrarEvento(aviso);
            return aviso;
        }
        this.repartidor = repartidor;
        this.estado = EstadoPedido.ASIGNADO;
        String mensaje = repartidor + " - " + criterio;
        registrarEvento("Repartidor asignado: " + mensaje);
        return mensaje;
    }

    // ------------------------------------------------------------------
    // Despachable
    // ------------------------------------------------------------------

    /**
     * Pone el pedido en ruta. Solo procede si ya tiene repartidor y no fue
     * cancelado.
     *
     * @return true si el pedido quedo despachado
     */
    @Override
    public boolean despachar() {
        if (estado == EstadoPedido.CANCELADO) {
            registrarEvento("Despacho rechazado: el pedido esta cancelado.");
            return false;
        }
        if (repartidor == null) {
            registrarEvento("Despacho rechazado: no hay repartidor asignado.");
            return false;
        }
        if (estado == EstadoPedido.DESPACHADO) {
            registrarEvento("Despacho omitido: el pedido ya iba en ruta.");
            return false;
        }
        estado = EstadoPedido.DESPACHADO;
        registrarEvento("Despachado con " + repartidor.getNombre()
                + ", tiempo estimado " + calcularTiempoEntrega() + " min.");
        return true;
    }

    /**
     * Despacha el pedido agregando una observacion del operador.
     *
     * Sobrecarga de {@link #despachar()}: reutiliza toda su validacion y solo
     * suma la nota a la bitacora.
     *
     * @param observacion indicacion adicional para el repartidor
     * @return true si el pedido quedo despachado
     */
    public boolean despachar(String observacion) {
        boolean resultado = despachar();
        if (resultado) {
            registrarEvento("Observacion del despacho: " + observacion);
        }
        return resultado;
    }

    /**
     * Cierra la entrega: el repartidor llego al destino.
     *
     * Solo tiene sentido sobre un pedido que va en ruta, asi que cualquier
     * otro estado se rechaza dejando la razon anotada en la bitacora en lugar
     * de lanzar una excepcion que cortaria el hilo del repartidor.
     *
     * @param duracionMs tiempo real que tomo la entrega simulada
     * @return true si el pedido quedo marcado como entregado
     */
    public boolean registrarEntrega(long duracionMs) {
        if (estado != EstadoPedido.DESPACHADO) {
            registrarEvento("Entrega rechazada: el pedido no va en ruta ("
                    + estado.getDescripcion() + ").");
            return false;
        }
        this.duracionRealMs = Math.max(0, duracionMs);
        this.estado = EstadoPedido.ENTREGADO;
        registrarEvento("Entregado por " + getNombreRepartidor()
                + " tras " + this.duracionRealMs + " ms de traslado simulado.");
        return true;
    }

    // ------------------------------------------------------------------
    // Cancelable
    // ------------------------------------------------------------------

    /**
     * Cancela el pedido. No se permite cancelar algo que ya va en ruta.
     *
     * @return true si el pedido quedo cancelado
     */
    @Override
    public boolean cancelar() {
        if (estado == EstadoPedido.DESPACHADO) {
            registrarEvento("Cancelacion rechazada: el pedido ya va en ruta.");
            return false;
        }
        if (estado == EstadoPedido.CANCELADO) {
            registrarEvento("Cancelacion omitida: el pedido ya estaba cancelado.");
            return false;
        }
        estado = EstadoPedido.CANCELADO;
        repartidor = null;
        registrarEvento("Pedido cancelado y repartidor liberado.");
        return true;
    }

    /**
     * Cancela el pedido dejando registrado el motivo.
     *
     * Sobrecarga de {@link #cancelar()}.
     *
     * @param motivo razon de la cancelacion
     * @return true si el pedido quedo cancelado
     */
    public boolean cancelar(String motivo) {
        boolean resultado = cancelar();
        if (resultado) {
            registrarEvento("Motivo de la cancelacion: " + motivo);
        }
        return resultado;
    }

    // ------------------------------------------------------------------
    // Rastreable
    // ------------------------------------------------------------------

    /** Muestra por consola la bitacora completa de este pedido. */
    @Override
    public void verHistorial() {
        System.out.println("Historial del pedido " + idPedido
                + " [" + getTipoEntrega() + "]");
        if (bitacora.isEmpty()) {
            System.out.println("   (sin movimientos registrados)");
            return;
        }
        for (int i = 0; i < bitacora.size(); i++) {
            System.out.println("   " + (i + 1) + ". " + bitacora.get(i));
        }
    }

    /**
     * Anota un hecho en la bitacora del pedido.
     *
     * Se declara final para que ninguna subclase pueda cambiar la forma en que
     * se registra: el constructor de la clase base ya la usa, y sobrescribirla
     * dejaria al objeto anotando eventos antes de estar completamente creado.
     *
     * @param evento descripcion de lo ocurrido
     */
    protected final void registrarEvento(String evento) {
        bitacora.add(evento);
    }

    // ------------------------------------------------------------------
    // Comportamiento comun implementado en la clase abstracta
    // ------------------------------------------------------------------

    /**
     * Imprime los datos basicos del pedido.
     *
     * Se implementa una sola vez aca porque el formato del resumen es igual
     * para todos los pedidos; lo que varia son los valores que aportan los
     * metodos abstractos.
     */
    public void mostrarResumen() {
        System.out.println("Pedido " + idPedido + "  [" + getTipoEntrega() + "]");
        System.out.println("   Direccion de entrega : " + direccionEntrega);
        System.out.println("   Distancia            : " + distanciaKm + " km");
        System.out.println("   Detalle del servicio : " + getDetalleServicio());
        System.out.println("   Factor de duracion   : " + getFactorDuracion());
        System.out.println("   Repartidor           : " + getNombreRepartidor());
        System.out.println("   Estado               : " + estado.getDescripcion());
        System.out.println("   Tiempo estimado      : " + calcularTiempoEntrega() + " min");
    }

    /**
     * Formula lineal reutilizable: un tiempo base fijo mas un valor por
     * kilometro recorrido, ajustado a un numero entero de minutos.
     *
     * Vive en la clase abstracta justamente para no repetir el mismo calculo
     * en cada subclase que lo necesite.
     *
     * @param tiempoBase    minutos fijos del servicio
     * @param minutosPorKm  minutos que se suman por cada kilometro
     * @return tiempo estimado en minutos
     */
    protected int calcularTiempoLineal(int tiempoBase, double minutosPorKm) {
        double tiempo = tiempoBase + (minutosPorKm * distanciaKm);
        return (int) Math.round(tiempo);
    }

    /**
     * Descripcion breve de los datos propios de cada subclase.
     * Se define aca con un valor por defecto para que las subclases solo la
     * sobrescriban si tienen algo que aportar.
     *
     * @return detalle especifico del pedido
     */
    protected String getDetalleServicio() {
        return "Sin datos adicionales";
    }

    // ------------------------------------------------------------------
    // Getters y setters (encapsulamiento)
    // ------------------------------------------------------------------

    public String getIdPedido() {
        return idPedido;
    }

    public void setIdPedido(String idPedido) {
        this.idPedido = idPedido;
    }

    public String getDireccionEntrega() {
        return direccionEntrega;
    }

    public void setDireccionEntrega(String direccionEntrega) {
        this.direccionEntrega = direccionEntrega;
    }

    public double getDistanciaKm() {
        return distanciaKm;
    }

    /**
     * Asigna la distancia validando que no sea negativa.
     *
     * @param distanciaKm distancia en kilometros, mayor o igual a cero
     * @throws IllegalArgumentException si la distancia es negativa
     */
    public void setDistanciaKm(double distanciaKm) {
        this.distanciaKm = validarDistancia(distanciaKm);
    }

    public Repartidor getRepartidor() {
        return repartidor;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    /**
     * @return duracion real de la entrega en milisegundos, o cero si aun no
     *         ha sido entregado
     */
    public long getDuracionRealMs() {
        return duracionRealMs;
    }

    /**
     * Nombre legible del repartidor, o un aviso si aun no hay ninguno.
     *
     * @return nombre del repartidor o "sin asignar"
     */
    public String getNombreRepartidor() {
        return (repartidor == null) ? "sin asignar" : repartidor.toString();
    }

    /**
     * Copia de la bitacora, para que quien la consulte no pueda modificarla.
     *
     * @return lista con los eventos registrados
     */
    public List<String> getBitacora() {
        return new ArrayList<>(bitacora);
    }

    @Override
    public String toString() {
        return getTipoEntrega() + " " + idPedido + " (" + distanciaKm + " km, "
                + calcularTiempoEntrega() + " min) - " + estado.getDescripcion();
    }
}
