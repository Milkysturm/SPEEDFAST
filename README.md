# SpeedFast — Sistema de reparto

**Asignatura:** Desarrollo Orientado a Objetos II (PRY2203)
**Experiencia 1 — Semana 2:** Definiendo una clase abstracta y su jerarquía

## Descripción

Sistema de consola para **SpeedFast**, empresa de reparto a domicilio con tres tipos de servicio:
comida (restaurantes), encomiendas (documentos o paquetes) y compras express (supermercado o farmacia).

Cada tipo de pedido estima su tiempo de entrega con una fórmula distinta. Esa diferencia se resuelve
con una **clase abstracta** `Pedido` que declara el método `calcularTiempoEntrega()` sin implementarlo,
obligando a cada subclase a definir su propia lógica.

## Estructura del proyecto

```
SpeedFast/
├── src/
│   └── com/speedfast/
│       ├── Main.java                   Clase de prueba
│       ├── modelo/
│       │   ├── Pedido.java             Clase ABSTRACTA base
│       │   ├── PedidoComida.java       Subclase — 15 min + 2 min/km
│       │   ├── PedidoEncomienda.java   Subclase — 20 min + 1,5 min/km
│       │   └── PedidoExpress.java      Subclase — 10 min, +5 min sobre 5 km
│       └── reporte/
│           └── ReporteConsola.java     Formato de la salida por consola
├── SpeedFast.iml
├── .gitignore
└── README.md
```

## Jerarquía de clases

```
                  Pedido  (abstracta)
     idPedido, direccionEntrega, distanciaKm
     mostrarResumen()          <- implementado
     calcularTiempoLineal()    <- implementado (reutilizable)
     calcularTiempoEntrega()   <- abstracto
     getTipoEntrega()          <- abstracto
     getFactorDuracion()       <- abstracto
     asignarRepartidor()       <- abstracto
                        │
     ┌──────────────────┼──────────────────┐
PedidoComida    PedidoEncomienda     PedidoExpress
```

## Por qué una clase abstracta

`Pedido` no se puede instanciar: no existe "un pedido" sin tipo. Reúne lo común a todos
—identificador, dirección y distancia— e implementa **una sola vez** lo que no cambia entre tipos:

- `mostrarResumen()`, porque el formato del resumen es siempre el mismo.
- `calcularTiempoLineal(base, minutosPorKm)`, la fórmula "tiempo base + valor por kilómetro" que
  comparten comida y encomienda. Vive en la clase base justamente para no repetirla en cada subclase.

Lo que sí cambia queda declarado como abstracto, de modo que el compilador **obliga** a cada subclase
a resolverlo:

| Método abstracto | Qué aporta cada subclase |
|---|---|
| `calcularTiempoEntrega()` | La fórmula de tiempo propia de su servicio |
| `getTipoEntrega()` | El nombre del tipo de entrega |
| `getFactorDuracion()` | El factor que explica su duración |
| `asignarRepartidor()` | El criterio con que se elige al repartidor |

## Reglas de cálculo

| Clase | Fórmula | Ejemplo |
|---|---|---|
| `PedidoComida` | 15 min + 2 min por km | 3,5 km → 22 min |
| `PedidoEncomienda` | 20 min + 1,5 min por km, ajustado a entero | 8,0 km → 32 min |
| `PedidoExpress` | 10 min base; +5 min si supera los 5 km | 2,0 km → 10 min / 7,5 km → 15 min |

**Sobre el ajuste a entero:** se usa `Math.round()`, es decir, redondeo al minuto más cercano en vez
de truncamiento. Una encomienda a 7 km da 30,5 minutos y se informa como 31, porque en una estimación
de entrega conviene no prometer menos tiempo del real.

**Sobre el umbral de los 5 km:** la condición es estrictamente mayor. Una compra express a exactamente
5,0 km no paga recargo; a 5,1 km sí.

## Separación de responsabilidades

- **`modelo`** calcula y devuelve datos. `calcularTiempoEntrega()` retorna un `int`, no imprime.
- **`reporte`** concentra el formato de la salida en `ReporteConsola`.
- **`Main`** crea los objetos, recorre el arreglo llamando a `mostrarResumen()` y pide el reporte.

Así, cambiar el formato de la salida no obliga a tocar la lógica de negocio.

## Atributos propios de cada subclase

Cada subclase agrega sus propios atributos, que influyen tanto en el detalle que muestra el resumen
como en el criterio de asignación del repartidor:

| Clase | Atributos propios | Criterio de asignación |
|---|---|---|
| `PedidoComida` | `restaurante`, `requiereMochilaTermica` | Repartidor con mochila térmica |
| `PedidoEncomienda` | `pesoKg`, `tipoEmbalaje` | Vehículo de carga sobre 20 kg |
| `PedidoExpress` | `local`, `disponibilidadInmediata` | Repartidor con disponibilidad inmediata |

## Validación

El constructor de `Pedido` rechaza distancias negativas con `IllegalArgumentException`, para que un
dato erróneo no produzca tiempos de entrega negativos. La misma validación se aplica en
`setDistanciaKm()`.

## Polimorfismo

En `Main` los cuatro pedidos se guardan en un arreglo de tipo `Pedido[]`. Al recorrerlo, Java
resuelve en tiempo de ejecución qué implementación de `calcularTiempoEntrega()` corresponde a cada
objeto, sin que el código del reporte necesite conocer los tipos concretos.

## Ejecución

**Desde IntelliJ IDEA:** abrir la carpeta `SpeedFast` y ejecutar `Main`. Si el IDE no reconoce las
fuentes, marcar `src` como *Sources Root*.

**Desde terminal:**

```bash
javac -d out $(find src -name "*.java")
java -cp out com.speedfast.Main
```

## Salida esperada

```
============================================================
  SISTEMA DE REPARTO SPEEDFAST - Semana 2
  Clase abstracta Pedido y calculo de tiempo de entrega
============================================================

DETALLE DE LOS PEDIDOS

Pedido P-001  [Comida]
   Direccion de entrega : Av. Providencia 1234, Santiago
   Distancia            : 3.5 km
   Detalle del servicio : Restaurante Sushi Kai
   Factor de duracion   : Preparacion en cocina (15 min) mas 2 min por km
   Asignacion           : Repartidor con MOCHILA TERMICA
   Tiempo estimado      : 22 min

Pedido P-002  [Encomienda]
   Direccion de entrega : Calle Los Aromos 456, Maipu
   Distancia            : 8.0 km
   Detalle del servicio : 25.5 kg en Caja reforzada
   Factor de duracion   : Retiro y revision de embalaje (20 min) mas 1.5 min por km
   Asignacion           : Repartidor con VEHICULO DE CARGA (supera 20.0 kg)
   Tiempo estimado      : 32 min

Pedido P-003  [Compra Express]
   Direccion de entrega : Pasaje El Roble 789, La Florida
   Distancia            : 2.0 km
   Detalle del servicio : Compra en Farmacia Central
   Factor de duracion   : Compra en local (10 min), sin recargo por distancia
   Asignacion           : Repartidor cercano, asignacion inmediata
   Tiempo estimado      : 10 min

Pedido P-004  [Compra Express]
   Direccion de entrega : Av. Vicuna Mackenna 1500, Nunoa
   Distancia            : 7.5 km
   Detalle del servicio : Compra en Supermercado Lider
   Factor de duracion   : Compra en local (10 min) mas recargo de 5 min por superar los 5 km
   Asignacion           : Sin disponibilidad inmediata, queda en espera
   Tiempo estimado      : 15 min


COMPARATIVA DE TIEMPOS ESTIMADOS

PEDIDO   TIPO              DISTANCIA     TIEMPO
------------------------------------------------
P-001    Comida               3.5 km     22 min
P-002    Encomienda           8.0 km     32 min
P-003    Compra Express       2.0 km     10 min
P-004    Compra Express       7.5 km     15 min

Entrega mas rapida : P-003 (Compra Express) con 10 min.
Entrega mas lenta  : P-002 (Encomienda) con 32 min.
Diferencia entre ambas: 22 min.

============================================================
  Fin del reporte - 4 pedidos procesados
============================================================
```

## Entorno

- IntelliJ IDEA
- Java 17 (compatible desde Java 8)
