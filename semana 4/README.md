# SpeedFast — Entregas simultáneas con hilos

**Asignatura:** Desarrollo Orientado a Objetos II (PRY2203) — Duoc UC
**Experiencia 1 — Semana 4:** Ejecutando tareas en paralelo con hilos en Java

## Descripción

Cuarta etapa del sistema de reparto de **SpeedFast**. Las semanas anteriores dejaron modelado
*qué* es un pedido y *quién* lo lleva; esta semana modela *cuándo*: varios repartidores salen a
la calle **al mismo tiempo**, cada uno como un hilo independiente que recorre su propia ruta,
simula el traslado con pausas aleatorias e informa su avance por consola.

Sobre esa base se agregan las dos cosas que la pauta evalúa junto con la concurrencia: los datos
de la jornada **entran desde un archivo** de texto y el resultado **sale a otro archivo**, y todos
los puntos donde algo puede fallar están cubiertos para que el programa nunca se caiga a medias.

## Estructura del proyecto

```
semana 4/
├── datos/
│   ├── pedidos.txt              Entrada: pedidos y rutas de la jornada
│   └── entregas.txt             Salida: informe generado por el programa
├── src/com/speedfast/
│   ├── Main.java                Arma el escenario y ejecuta la simulación
│   ├── contrato/                Interfaces reutilizadas de la semana 3
│   │   ├── Despachable.java
│   │   ├── Cancelable.java
│   │   └── Rastreable.java
│   ├── excepcion/
│   │   └── PedidoInvalidoException.java   Error propio de la lectura de datos
│   ├── modelo/
│   │   ├── Pedido.java          Clase abstracta base
│   │   ├── PedidoComida.java
│   │   ├── PedidoEncomienda.java
│   │   ├── PedidoExpress.java
│   │   ├── EstadoPedido.java    Enum de estados (ahora incluye ENTREGADO)
│   │   └── Repartidor.java      Runnable: la ruta que ejecuta cada hilo
│   ├── reporte/
│   │   ├── ReporteConsola.java  Formato de la salida por pantalla
│   │   ├── ConsolaSegura.java   Impresión sincronizada entre hilos
│   │   └── ReporteArchivo.java  Escritura del informe final
│   └── servicio/
│       ├── CargadorDePedidos.java     Lectura y validación del archivo
│       ├── SimuladorDeEntregas.java   ExecutorService y espera de las rutas
│       ├── MonitorDeEntregas.java     Marcador compartido entre hilos
│       └── ControladorDeEnvios.java   Coordinación de la jornada (semana 3)
├── semana 4.iml
└── README.md
```

## Qué se reutilizó y qué cambió

| Elemento | Origen | Cambio en la semana 4 |
|----------|--------|-----------------------|
| `Pedido` abstracta, subclases, interfaces | Semanas 2 y 3 | Se conservan intactas en su lógica de negocio |
| `EstadoPedido` | Semana 3 | Se agrega `ENTREGADO`: con hilos, el pedido ya no termina al salir a ruta |
| `Pedido` | Semana 3 | Nueva sobrecarga `asignarRepartidor(Repartidor)` y método `registrarEntrega(long)` |
| `Repartidor` | Semana 3 | Ahora implementa `Runnable` y tiene su propia lista de pedidos |
| `ControladorDeEnvios` | Semana 3 | Se usa tal cual para reservar y cancelar |

## Concurrencia

### Repartidor como `Runnable`

`Repartidor` implementa `Runnable` en lugar de extender `Thread` porque lo que se modela es
**el trabajo** ("recorrer mi ruta"), no un tipo especial de hilo. Separarlos deja que sea el
`ExecutorService` quien decida en qué hilo corre cada ruta, y mantiene libre la única herencia
que Java permite.

Dentro de una ruta las entregas son secuenciales —un repartidor no puede estar en dos lugares a
la vez—; lo que ocurre en paralelo es una ruta respecto de las otras.

### Por qué no hay condiciones de carrera

| Elemento | Quién lo toca | Cómo se resuelve |
|----------|---------------|------------------|
| Los pedidos de una ruta | Un solo hilo (su repartidor) | Confinamiento: un pedido pertenece a una única ruta, así que nadie compite por él |
| Estado final de los pedidos | El hilo principal, al terminar | `awaitTermination()` actúa como punto de sincronización: lo escrito por los hilos ya es visible |
| Salida por consola | Todos los hilos | `ConsolaSegura.imprimir()` es `synchronized`, así ningún mensaje sale partido |
| Contadores globales | Todos los hilos | `AtomicInteger` en `MonitorDeEntregas`: `i++` no es atómico y perdería entregas |
| Bitácora de rutas cerradas | Todos los hilos | `CopyOnWriteArrayList`, y `registrarRuta()` es `synchronized`: los campos atómicos por separado no bastan, hay que volver indivisible el conjunto «tomar número de orden + escribir la línea» |
| Números aleatorios | Cada hilo | `ThreadLocalRandom`, en vez de un `Random` compartido con contención |

### Por qué `ExecutorService` y no `Thread` sueltos

- El pool administra los hilos y permite fijar cuántos corren a la vez.
- Devuelve un `Future` por tarea: una excepción lanzada **dentro** de un hilo queda guardada ahí
  y se pierde en silencio si nadie la consulta. `SimuladorDeEntregas.revisarFallas()` la revisa.
- `shutdown()` + `awaitTermination()` garantizan que la simulación termina recién cuando todas
  las rutas terminaron, con un tope de espera para no quedar colgado. Si ese tope se alcanza,
  `shutdownNow()` interrumpe pero **vuelve de inmediato**, así que hay una segunda espera antes
  de leer resultados: de lo contrario el informe podría mostrar datos a medio escribir.

## Manejo de excepciones

| Punto crítico | Excepción | Cómo se maneja |
|---------------|-----------|----------------|
| Archivo de pedidos ausente o ilegible | `IOException` / `NoSuchFileException` | Se avisa con la ruta buscada y se arma el escenario de respaldo definido en `Main`, para que la simulación se pueda demostrar igual |
| Ruta pasada por argumento mal formada | `InvalidPathException` | Se avisa y se usa la ruta por defecto |
| Id de pedido repetido en el archivo | `PedidoInvalidoException` | Se descarta la línea: dos pedidos con el mismo id romperían las búsquedas por id |
| Distancia o peso mal escritos | `NumberFormatException` | Se traduce a `PedidoInvalidoException` con el número de línea |
| Distancia negativa | `IllegalArgumentException` (la lanza `Pedido`) | Igual: la línea se descarta y la carga sigue |
| Tipo inexistente o campos faltantes | `PedidoInvalidoException` | Se descarta solo esa línea |
| Pausa del hilo interrumpida | `InterruptedException` | Se restaura la marca con `Thread.currentThread().interrupt()` y la ruta se corta ordenadamente |
| Falla dentro de una ruta | `RuntimeException` | Se informa y el repartidor continúa con el siguiente pedido |
| Error propagado por un hilo | `ExecutionException` | Se detecta al consultar el `Future` |
| Tarea rechazada por el pool | `RejectedExecutionException` | Se informa y las rutas en curso se cierran ordenadamente |
| Escritura del informe | `IOException` | Se avisa, pero no invalida la simulación ya ocurrida |

El archivo `datos/pedidos.txt` incluye **cinco líneas defectuosas a propósito** al final, para
que la ejecución demuestre que cada error se informa, se descarta esa línea y el programa sigue.

## Entrada y salida de datos

**Entrada** — `datos/pedidos.txt`, campos separados por `;`:

```
TIPO;ID;DIRECCION;DISTANCIA_KM;REPARTIDOR;VEHICULO;DATO_1;DATO_2
```

| Tipo | `DATO_1` | `DATO_2` |
|------|----------|----------|
| `COMIDA` | restaurante | requiere mochila térmica (`true`/`false`) |
| `ENCOMIENDA` | peso en kg | tipo de embalaje |
| `EXPRESS` | local | disponibilidad inmediata (`true`/`false`) |

Las líneas en blanco y las que empiezan con `#` se ignoran. Cada nombre distinto de repartidor
genera una ruta, y los pedidos se le van sumando en el orden del archivo.

**Salida** — `datos/entregas.txt`: resumen de la jornada, detalle por repartidor, pedidos no
entregados e incidencias detectadas en la lectura.

## Cómo ejecutar

**Desde IntelliJ IDEA:** abrir la carpeta `semana 4`, marcar `src` como *Sources Root* si no lo
está, y ejecutar `Main`. El directorio de trabajo debe ser la carpeta del proyecto para que
encuentre `datos/pedidos.txt`.

**Desde terminal:**

```bash
cd "semana 4"
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp out com.speedfast.Main
```

Opcionalmente se pueden indicar otras rutas:

```bash
java -cp out com.speedfast.Main datos/pedidos.txt datos/entregas.txt
```

## Salida por consola (fragmento)

```
6. SIMULACION: TODAS LAS RUTAS AL MISMO TIEMPO

[  0.00 s] [Repartidor: Camila Soto] inicia su ruta con 3 pedido(s) en moto con mochila termica. (hilo pool-1-thread-1)
[  0.00 s] [Repartidor: Diego Fuentes] inicia su ruta con 2 pedido(s) en furgon de carga. (hilo pool-1-thread-2)
[  0.00 s] [Repartidor: Luis Vera] inicia su ruta con 3 pedido(s) en bicicleta electrica. (hilo pool-1-thread-3)
[  0.01 s] [Repartidor: Camila Soto] Entregando PedidoComida #P-101 (3.5 km, estimado 22 min)...
[  0.01 s] [Repartidor: Diego Fuentes] Entregando PedidoEncomienda #P-102 (8.0 km, estimado 32 min)...
[  0.01 s] [Repartidor: Luis Vera] Entregando PedidoExpress #P-103 (2.0 km, estimado 10 min)...
[  0.64 s] [Repartidor: Luis Vera] Pedido #P-103 entregado. (612 ms de traslado simulado)
[  0.64 s] [Repartidor: Luis Vera] omite el pedido #P-106: fue cancelado antes de salir.
...
[  2.71 s] [Repartidor: Camila Soto] termino su ruta: 3 de 3 entrega(s) completada(s) en 2688 ms.

7. RESULTADO DE LA SIMULACION

   Rutas terminadas     : 3 de 3
   Entregas completadas : 7

   Trabajo total de las rutas   : 5357 ms
   Duracion real en paralelo    : 2725 ms
   Mejora por trabajar en paralelo: 2.0 veces
```

La marca de tiempo al inicio de cada línea es lo que hace visible el paralelismo: los tres
repartidores arrancan en el mismo instante y sus mensajes se intercalan. El contraste final entre
el trabajo acumulado y la duración real es la medida concreta de la mejora de eficiencia.

## Decisiones de diseño

- **`Repartidor` crece en vez de duplicarse.** La pauta pide una clase `Repartidor` con nombre y
  lista de pedidos; ya existía una con nombre y vehículo, usada por `Pedido`. Crear una segunda
  habría dejado dos conceptos con el mismo nombre, así que se amplió la existente.
- **La consola es el único recurso realmente compartido.** El diseño evita compartir estado antes
  de tener que sincronizarlo: es más simple y más difícil de romper que repartir `synchronized`.
- **Los errores de datos no lanzan, informan.** Dentro de `run()` una excepción no controlada
  mataría la ruta completa en silencio; por eso los problemas de un pedido se anotan y la ruta
  continúa.
- **Los resultados se leen recién cuando no queda ningún hilo escribiendo**, tanto en el final
  normal como cuando la simulación se corta por tiempo.
- **`Main` también sabe armar el escenario sin el archivo.** El caso pide leer los datos de
  entrada, pero si el archivo falta la demostración de concurrencia no puede desaparecer: hay un
  escenario de respaldo instanciado en código con los tres repartidores y sus rutas.

## Entorno

- IntelliJ IDEA
- Java 17

---

**Autora:** Olga Rivas
