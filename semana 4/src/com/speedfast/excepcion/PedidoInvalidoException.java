package com.speedfast.excepcion;

/**
 * Se lanza cuando una linea del archivo de pedidos no se puede convertir en
 * un objeto Pedido: le faltan campos, el tipo no existe o algun dato no tiene
 * el formato esperado.
 *
 * Es una excepcion propia y comprobada (extiende Exception) porque leer un
 * archivo escrito a mano es justamente el punto donde se esperan errores: al
 * obligar a capturarla, el compilador impide que una linea mala pase inadvertida
 * y termine cayendo mas adelante como un NullPointerException sin explicacion.
 *
 * @author Olga Rivas
 * @version 4.0
 */
public class PedidoInvalidoException extends Exception {

    /** Version de la clase, requerida por ser serializable como toda Exception. */
    private static final long serialVersionUID = 1L;

    /** Numero de linea del archivo donde se detecto el problema. */
    private final int numeroLinea;

    /**
     * @param numeroLinea linea del archivo que no se pudo interpretar
     * @param mensaje     explicacion de lo que esta mal
     */
    public PedidoInvalidoException(int numeroLinea, String mensaje) {
        super("Linea " + numeroLinea + ": " + mensaje);
        this.numeroLinea = numeroLinea;
    }

    /**
     * @param numeroLinea linea del archivo que no se pudo interpretar
     * @param mensaje     explicacion de lo que esta mal
     * @param causa       excepcion original que provoco el problema
     */
    public PedidoInvalidoException(int numeroLinea, String mensaje, Throwable causa) {
        super("Linea " + numeroLinea + ": " + mensaje, causa);
        this.numeroLinea = numeroLinea;
    }

    public int getNumeroLinea() {
        return numeroLinea;
    }
}
