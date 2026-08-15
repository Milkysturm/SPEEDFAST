package com.speedfast;

/**
 * Clase base de la jerarquía de pedidos de SpeedFast.
 *
 * Define el estado común a todo pedido (encapsulado) y el comportamiento
 * genérico de asignación de repartidor, que las subclases especializan
 * mediante SOBRESCRITURA (override).
 *
 * Además declara dos versiones del método asignarRepartidor, lo que
 * constituye una SOBRECARGA (overload):
 *   - asignarRepartidor()               -> sin parámetros
 *   - asignarRepartidor(String nombre)  -> recibe el nombre del repartidor
 *
 * @author Olga Rivas
 * @version 1.0
 */
public class Pedido {

    // ==== Atributos encapsulados (private) ====
    private String idPedido;
    private String direccionEntrega;
    private String tipoPedido;

    /**
     * Constructor completo de la clase base.
     *
     * @param idPedido         identificador único del pedido
     * @param direccionEntrega dirección donde se entrega el pedido
     * @param tipoPedido       tipo o categoría del pedido
     */
    public Pedido(String idPedido, String direccionEntrega, String tipoPedido) {
        this.idPedido = idPedido;
        this.direccionEntrega = direccionEntrega;
        this.tipoPedido = tipoPedido;
    }

    // ==== Métodos de acceso (getters y setters) ====
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

    public String getTipoPedido() {
        return tipoPedido;
    }

    public void setTipoPedido(String tipoPedido) {
        this.tipoPedido = tipoPedido;
    }

    /**
     * Comportamiento GENÉRICO de asignación de repartidor.
     * Sirve como contrato para la herencia: cada subclase lo sobrescribe
     * con la lógica propia de su tipo de pedido.
     */
    public void asignarRepartidor() {
        System.out.println("[" + tipoPedido + "] Asignando repartidor...");
        System.out.println("   -> Buscando un repartidor disponible en la zona de "
                + direccionEntrega + "...");
        System.out.println("   -> Sin criterios especiales para este pedido (Pedido N° "
                + idPedido + ").");
    }

    /**
     * Versión SOBRECARGADA: recibe el nombre del repartidor ya seleccionado.
     * Misma funcionalidad, distinta firma.
     *
     * @param nombreRepartidor nombre del repartidor asignado al pedido
     */
    public void asignarRepartidor(String nombreRepartidor) {
        System.out.println("[" + tipoPedido + "] Asignando repartidor...");
        System.out.println("   -> Sin validaciones adicionales para este tipo de pedido.");
        System.out.println("   -> Pedido asignado a " + nombreRepartidor);
    }

    /**
     * Representación en texto del pedido, útil para depuración e informes.
     */
    @Override
    public String toString() {
        return "Pedido N° " + idPedido + " | Tipo: " + tipoPedido
                + " | Dirección: " + direccionEntrega;
    }
}
