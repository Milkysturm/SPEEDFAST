# SpeedFast — Sistema de asignación de repartidores

**Asignatura:** Desarrollo Orientado a Objetos II (PRY2203)
**Experiencia 1 — Semana 1:** Explorando la sobrecarga y sobreescritura en clases derivadas

## Descripción

Sistema de consola para **SpeedFast**, empresa de reparto a domicilio con tres tipos de servicio:
comida (restaurantes), encomiendas (documentos o paquetes) y compras express (supermercado o farmacia).

Cada tipo de pedido aplica un criterio distinto de asignación de repartidor. Esa diferencia se
resuelve con **polimorfismo**: un mismo método `asignarRepartidor()` se comporta según el tipo real
del objeto.

## Estructura del proyecto

```
SpeedFast/
├── src/
│   └── com/speedfast/
│       ├── Main.java                  Clase de prueba
│       └── modelo/
│           ├── Pedido.java            Clase base
│           ├── PedidoComida.java      Subclase — mochila térmica
│           ├── PedidoEncomienda.java  Subclase — peso y embalaje
│           └── PedidoExpress.java     Subclase — cercanía y disponibilidad
├── SpeedFast.iml
├── .gitignore
└── README.md
```

## Jerarquía de clases

```
                    Pedido
     (idPedido, direccionEntrega, tipoPedido)
                       │
      ┌────────────────┼────────────────┐
PedidoComida    PedidoEncomienda   PedidoExpress
```

## Conceptos aplicados

### Encapsulamiento

Todos los atributos son `private` y se acceden mediante *getters* y *setters*. Cada clase incluye un
constructor completo que inicializa su estado; las subclases invocan `super(...)` para delegar la
inicialización de los atributos heredados.

### Sobreescritura (override)

Las tres subclases redefinen `asignarRepartidor()` con la lógica de su criterio:

| Clase | Criterio de asignación |
|---|---|
| `PedidoComida` | Requiere repartidor con mochila térmica |
| `PedidoEncomienda` | Valida peso (límite 20 kg) y embalaje |
| `PedidoExpress` | Repartidor más cercano con disponibilidad inmediata |

### Sobrecarga (overload)

Cada clase define además `asignarRepartidor(String nombreRepartidor)`, misma funcionalidad con una
firma distinta. Esta versión recibe el repartidor ya elegido e imprime la **validación** propia del
tipo de pedido (mochila térmica, peso, disponibilidad), confirmando o rechazando la asignación.

### Polimorfismo

En `Main` los objetos se almacenan en un arreglo de tipo `Pedido[]`. Al recorrerlo y llamar
`pedido.asignarRepartidor()`, Java resuelve en tiempo de ejecución cuál implementación ejecutar
según el tipo real de cada objeto.

## Ejecución

**Desde IntelliJ IDEA:** abrir la carpeta `SpeedFast`, marcar `src` como *Sources Root* si no lo
está, y ejecutar `Main`.

**Desde terminal:**

```bash
javac -d out $(find src -name "*.java")
java -cp out com.speedfast.Main
```

## Salida esperada

```
==========================================================
        SISTEMA DE REPARTO SPEEDFAST - Semana 1
     Sobrecarga y sobreescritura en clases derivadas
==========================================================

--- 1. SOBREESCRITURA: asignarRepartidor() ---

[P-001] PEDIDO DE COMIDA - Sushi Kai
    Destino: Av. Providencia 1234, Santiago
    Criterio: se busca repartidor con MOCHILA TERMICA disponible.

[P-002] ENCOMIENDA - embalaje: Caja reforzada
    Destino: Calle Los Aromos 456, Maipu
    Peso declarado: 25.5 kg.
    Criterio: supera los 20.0 kg, se requiere repartidor con vehiculo de carga.

[P-003] COMPRA EXPRESS - Farmacia Central
    Destino: Pasaje El Roble 789, La Florida
    Repartidor mas cercano a 1.2 km del local.
    Criterio: cuenta con disponibilidad inmediata, se asigna de inmediato.

[P-004] Buscando un repartidor disponible para el pedido de tipo Generico.
    Destino: Camino El Alba 321, Las Condes
    Sin criterios especiales de asignacion.

--- 2. SOBRECARGA: asignarRepartidor(String nombreRepartidor) ---

[P-001] PEDIDO DE COMIDA - Sushi Kai
    Repartidor asignado: Camila Soto
    Validacion: Camila Soto debe portar mochila termica. Asignacion CONFIRMADA.
    Entrega en: Av. Providencia 1234, Santiago

[P-002] ENCOMIENDA - embalaje: Caja reforzada
    Repartidor asignado: Diego Fuentes
    Validacion de peso: 25.5 kg.
    Resultado: Diego Fuentes debe utilizar vehiculo de carga. Asignacion CONFIRMADA con restriccion.
    Entrega en: Calle Los Aromos 456, Maipu

[P-003] COMPRA EXPRESS - Farmacia Central
    Repartidor asignado: Marcela Rojas (a 1.2 km).
    Validacion: disponibilidad inmediata confirmada. Asignacion CONFIRMADA.
    Entrega en: Pasaje El Roble 789, La Florida

[P-004] Repartidor asignado: Ignacio Perez.
    Destino: Camino El Alba 321, Las Condes
    Validacion: sin requisitos adicionales para el tipo Generico.

--- 3. CASOS CON VALIDACION NEGATIVA ---

[P-005] COMPRA EXPRESS - Supermercado Lider
    Repartidor asignado: Tomas Vega (a 0.8 km).
    Validacion: Tomas Vega no esta disponible ahora. Asignacion RECHAZADA, se reasignara.
    Entrega en: Av. Vicuna Mackenna 1500, Nunoa

[P-006] PEDIDO DE COMIDA - Heladeria Nevada
    Destino: Los Leones 200, Providencia
    Criterio: se busca cualquier repartidor disponible (no requiere mochila termica).

==========================================================
        Fin de la ejecucion - 6 pedidos procesados
==========================================================
```

## Entorno

- IntelliJ IDEA
- Java 17 (compatible desde Java 8)
