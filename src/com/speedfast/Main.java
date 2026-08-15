package com.speedfast;

/**
 * Clase principal del sistema de reparto SpeedFast.
 *
 * Demuestra el uso de polimorfismo:
 *   1. Sobrescritura: cada subclase responde distinto al mismo mensaje.
 *   2. Sobrecarga: el mismo método admite dos firmas diferentes.
 *   3. Polimorfismo en tiempo de ejecución: un arreglo de tipo Pedido
 *      almacena objetos de distintas subclases y Java resuelve en ejecución
 *      qué implementación invocar.
 */
public class Main {

    public static void main(String[] args) {

        System.out.println("=================================================");
        System.out.println("      SPEEDFAST - SISTEMA DE REPARTO A DOMICILIO ");
        System.out.println("=================================================");

        // ---- Instanciación de un objeto de cada subclase ----
        PedidoComida pedidoComida =
                new PedidoComida("1001", "Av. Providencia 1234", true);

        PedidoEncomienda pedidoEncomienda =
                new PedidoEncomienda("1002", "Calle Los Aromos 567", 8.5, true);

        PedidoExpress pedidoExpress =
                new PedidoExpress("1003", "Pasaje Las Rosas 89", 1.2, true);

        // =================================================================
        // 1) MÉTODOS SOBRESCRITOS (misma firma, comportamiento distinto)
        // =================================================================
        System.out.println("\n--- 1. MÉTODOS SOBRESCRITOS: asignarRepartidor() ---\n");

        pedidoComida.asignarRepartidor();
        System.out.println();
        pedidoEncomienda.asignarRepartidor();
        System.out.println();
        pedidoExpress.asignarRepartidor();

        // =================================================================
        // 2) MÉTODOS SOBRECARGADOS (reciben el nombre del repartidor)
        // =================================================================
        System.out.println("\n--- 2. MÉTODOS SOBRECARGADOS: asignarRepartidor(String) ---\n");

        pedidoComida.asignarRepartidor("Juan Pérez");
        System.out.println();
        pedidoEncomienda.asignarRepartidor("Camila Soto");
        System.out.println();
        pedidoExpress.asignarRepartidor("Luis Díaz");

        // =================================================================
        // 3) POLIMORFISMO EN TIEMPO DE EJECUCIÓN
        //    Las referencias son de tipo Pedido (clase base), pero cada
        //    objeto ejecuta la versión de SU propia clase.
        // =================================================================
        System.out.println("\n--- 3. POLIMORFISMO: arreglo de tipo Pedido ---\n");

        Pedido[] pedidos = { pedidoComida, pedidoEncomienda, pedidoExpress };
        String[] repartidores = { "Juan Pérez", "Camila Soto", "Luis Díaz" };

        for (int i = 0; i < pedidos.length; i++) {
            System.out.println(pedidos[i]);            // usa toString()
            pedidos[i].asignarRepartidor(repartidores[i]);
            System.out.println();
        }

        // =================================================================
        // 4) CASOS QUE NO CUMPLEN LAS VALIDACIONES
        //    Evidencia de que la lógica de cada subclase realmente valida.
        // =================================================================
        System.out.println("--- 4. CASOS CON VALIDACIÓN FALLIDA ---\n");

        PedidoComida comidaSinMochila =
                new PedidoComida("1004", "Av. Matta 4321", false);
        comidaSinMochila.asignarRepartidor("Pedro Rojas");

        System.out.println();

        PedidoEncomienda encomiendaPesada =
                new PedidoEncomienda("1005", "Camino El Alba 900", 32.0, false);
        encomiendaPesada.asignarRepartidor("Ana Muñoz");

        System.out.println("\n=================================================");
        System.out.println("        PROCESO DE ASIGNACIÓN FINALIZADO         ");
        System.out.println("=================================================");
    }
}
