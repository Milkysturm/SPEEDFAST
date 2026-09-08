# SpeedFast — Sincronización del acceso a la zona de carga

**Asignatura:** Desarrollo Orientado a Objetos II (PRY2203) — Duoc UC
**Experiencia 2 — Semana 5:** Sincronizando procesos en sistemas concurrentes

## Descripción

Quinta etapa del sistema de SpeedFast. La semana anterior varios repartidores entregaban en
paralelo, pero **cada uno tenía su propia lista**: los hilos corrían al mismo tiempo sin tocar
nunca los mismos datos, así que en rigor no había nada que sincronizar.

Esta semana el escenario cambia: todos los repartidores retiran pedidos de una **misma zona de
carga**. Ese recurso compartido es donde aparecen las condiciones de carrera de verdad —dos
repartidores llevándose el mismo pedido, pedidos que desaparecen sin que nadie los reparta— y es
lo que el proyecto resuelve y, sobre todo, **demuestra**.

## Estructura del proyecto

```
semana 5/
├── src/com/speedfast/
│   ├── Main.java                      Arma los escenarios y pide los reportes
│   ├── modelo/
│   │   ├── Pedido.java                id, direccionEntrega, estado, setEstado sobrecargado
│   │   ├── EstadoPedido.java          PENDIENTE / EN_REPARTO / ENTREGADO
│   │   └── Repartidor.java            Runnable: retira, entrega y avisa
│   ├── zona/
│   │   ├── FuenteDePedidos.java       Contrato común de las dos zonas
│   │   ├── ZonaDeCarga.java           Recurso compartido protegido con synchronized
│   │   ├── ZonaDeCargaInsegura.java   La misma zona SIN proteger (solo demostración)
│   │   └── RegistroDeIncidencias.java Testigo thread-safe que detecta los errores
│   ├── simulacion/
│   │   └── SimulacionDeDespacho.java  ExecutorService y espera de los turnos
│   └── reporte/
│       ├── ConsolaSegura.java         Impresión sincronizada con marca de tiempo
│       └── ReporteConsola.java        Formato de los reportes
├── test/com/speedfast/                Pruebas unitarias (JUnit 5)
│   ├── modelo/PedidoTest.java
│   ├── zona/ZonaDeCargaTest.java
│   ├── zona/ZonaDeCargaConcurrenciaTest.java
│   └── simulacion/DespachoIntegracionTest.java
├── semana 5.iml
└── README.md
```

## El problema, en concreto

Retirar un pedido son tres pasos: mirar si queda alguno, sacarlo de la lista y marcarlo
`EN_REPARTO`. Sin sincronización, entre un paso y otro puede colarse otro hilo:

```
Juan:   ¿queda algo? sí, el pedido #1 ────┐
Camila: ¿queda algo? sí, el pedido #1 ────┤ los dos ven el mismo
Juan:   saca el primero → #1              │
Camila: saca el primero → #2              │ ¡pero entrega el #1 que ya tenía en la mano!
                                          └─ el #2 desaparece sin repartirse
```

El resultado son **retiros duplicados** (un cliente recibe dos veces) y **pedidos perdidos** (otro
no recibe nunca). Aunque cada operación de la lista fuera segura por separado, el problema
seguiría: lo que hay que volver indivisible es la secuencia completa.

## La solución

`ZonaDeCarga` concentra toda la sincronización del proyecto. Esa concentración es deliberada: si
la protección estuviera repartida entre varias clases, bastaría olvidar un solo lugar para reabrir
el problema.

| Decisión | Por qué |
|----------|---------|
| `synchronized` sobre los métodos de la zona | Vuelve indivisible la secuencia consultar → extraer → marcar, no solo cada paso |
| `wait()` cuando no hay pedidos | Un repartidor sin trabajo se duerme en vez de consultar en un ciclo infinito quemando CPU. `wait()` además libera el candado, así otro hilo sí puede entrar a dejar un pedido |
| `while` y no `if` alrededor del `wait()` | `notifyAll()` despierta a todos y solo uno se queda con el pedido; los demás deben volver a dormirse. Java admite además despertares espurios |
| `notifyAll()` y no `notify()` | `notify()` despierta a uno cualquiera; si ese no es al que le tocaba continuar, el aviso se pierde y el resto sigue durmiendo con trabajo disponible |
| `cerrarRecepcion()` | Sin un aviso de "no llegan más pedidos", los que esperan quedarían dormidos para siempre: un bloqueo permanente |
| `Repartidor` no sincroniza nada | Pedir el pedido de forma segura es responsabilidad de la zona. Gracias a eso la misma clase sirve para las dos versiones y la comparación mide solo la sincronización |

Un detalle que vale la pena notar: el pedido se marca `EN_REPARTO` **dentro** del bloque
sincronizado. Así ningún otro hilo puede llegar a verlo en un estado intermedio.

## La demostración

El programa corre la **misma jornada dos veces**, con los mismos 12 pedidos y los mismos tres
repartidores. Lo único que cambia es la zona de carga:

```
   INDICADOR                         SIN SINCRONIZAR   SINCRONIZADA
   ----------------------------------------------------------------
   Pedidos cargados                               12             12
   Pedidos distintos retirados                    10             12
   Entregados correctamente                       10             12
   Retiros duplicados                              2              0
   Incidencias totales                             4              0
```

Quien detecta los errores es `RegistroDeIncidencias`, construido para ser confiable **incluso
cuando la zona que vigila no lo es**: usa `ConcurrentHashMap.putIfAbsent()`, que es atómico, de
modo que si dos hilos anotan el mismo pedido en el mismo instante, uno de los dos recibe sí o sí
el valor que dejó el otro. Con un `HashMap` normal los dos podrían creerse los primeros y el
retiro duplicado pasaría inadvertido.

**Sobre la reproducibilidad:** `ZonaDeCargaInsegura` mantiene abierta unos milisegundos la ventana
entre consultar y extraer. Eso no inventa un error que no existiría —la ventana está presente en
cualquier código no sincronizado—, solo la agranda lo suficiente para que se vea en cada corrida
en vez de una de cada tantas. Sin esa pausa el fallo aparece de forma intermitente, y esa
intermitencia es justamente lo más peligroso de las condiciones de carrera: el programa "funciona"
en las pruebas y falla en producción.

El tercer escenario carga cuatro pedidos y **espera a que la zona quede vacía y haya al menos un
repartidor dormido** antes de hacer llegar tres más. Esa espera consulta el estado real
(`getRepartidoresEnEspera()`) en lugar de dormir un tiempo fijo: con un `sleep` a ojo, a veces los
repartidores seguían trabajando y el escenario no demostraba nada.

La zona además avisa por consola cuando alguien se queda esperando, así el mecanismo deja de ser
invisible:

```
[ 1.10 s] [Repartidor - Diego] Entregado pedido #103. Estado: ENTREGADO (601 ms de viaje)
[ 1.10 s] [Zona de carga] no quedan pedidos disponibles: pool-3-thread-2 queda a la espera (1 en espera).
[ 1.11 s] >>> Llega un camion con 3 pedido(s) mas a la zona de carga.
[ 1.11 s] [Repartidor - Diego] Retirando pedido #105... Destino: Camino El Alba 321, Las Condes
```

## Pruebas unitarias

En la retroalimentación de la Sumativa 1 quedó anotado que el siguiente paso era **verificar el
comportamiento con pruebas unitarias en vez de leyendo la salida por consola**. En un proyecto
concurrente eso no es solo prolijidad: una condición de carrera puede no manifestarse en la
corrida que uno mira, así que la consola es directamente insuficiente como evidencia.

Las pruebas afirman invariantes que deben cumplirse **siempre**, repitiendo el experimento:

| Prueba | Qué asegura |
|--------|-------------|
| `PedidoTest` | El pedido nace PENDIENTE, rechaza id o dirección inválidos, y `setEstado(String)` valida igual que la versión con enum |
| `ZonaDeCargaTest` | Orden de llegada, cambio de estado al retirar, que no se acepten pedidos con la recepción cerrada, y que la espera **despierte** y **no se bloquee** (con timeout, así un deadlock hace fallar la prueba en vez de colgarla) |
| `ZonaDeCargaConcurrenciaTest` | Con 200 pedidos y 6 hilos, repetido 20 veces: cada pedido sale exactamente una vez y ninguno se pierde |
| `DespachoIntegracionTest` | La jornada completa con repartidores reales termina sin incidencias, y los pedidos tardíos también se entregan |

Hay una prueba deliberadamente inusual: `laZonaSinSincronizarFalla()`. Es el **control del
experimento** — si la versión sin proteger también pasara todas las invariantes, la sincronización
no estaría demostrando nada.

Las pruebas de concurrencia usan consumidores mínimos en vez de la clase `Repartidor`: lo que está
bajo prueba es la zona, y sin las pausas de la entrega simulada el ciclo se repite miles de veces
por segundo, que es lo que hace aflorar las carreras. Todos los hilos arrancan a la vez con un
`CountDownLatch`, porque si partieran escalonados el primero podría vaciar la zona antes de que el
segundo empiece y no habría competencia que medir.

**Para ejecutarlas en IntelliJ:** abrir cualquier clase de `test/` y hacer clic en la flecha verde.
La primera vez IntelliJ ofrece *"Add 'JUnit5' to classpath"* — aceptar; el IDE trae las librerías
incluidas y no hay que descargar nada. El programa principal compila y corre igual sin esto: las
pruebas están en una carpeta de fuentes aparte.

## Cómo ejecutar

**Desde IntelliJ IDEA:** abrir la carpeta `semana 5` y ejecutar `Main`.

**Desde terminal:**

```bash
cd "semana 5"
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp out com.speedfast.Main
```

## Salida por consola (fragmento)

```
1. ZONA DE CARGA SIN SINCRONIZAR (demostracion del problema)

   [Zona de carga inicializada - version SIN proteger]
   Pedido #1 agregado. Destino: Santiago Centro
   ...
   [ 0.01 s] [Repartidor - Juan] comienza su turno. (hilo pool-1-thread-1)
   [ 0.01 s] [Repartidor - Camila] comienza su turno. (hilo pool-1-thread-2)
   [ 0.02 s] [Repartidor - Juan] Retirando pedido #1... Destino: Santiago Centro
   [ 0.02 s] [Repartidor - Camila] Retirando pedido #1... Destino: Santiago Centro
   [ 0.02 s] [Repartidor - Camila] ATENCION: el pedido #1 ya habia sido retirado por otro repartidor.
   ...
   Incidencias de concurrencia detectadas (4):
      - RETIRO DUPLICADO: el pedido #1 ya lo habia retirado Juan y tambien lo tomo Camila
      - PEDIDO PERDIDO: el pedido #2 desaparecio de la zona sin que nadie lo retirara.
```

La marca de tiempo y el nombre del hilo en cada línea son lo que permite verificar en la consola
que los repartidores trabajan de verdad en paralelo.

## Decisiones de diseño

- **Una interfaz común para las dos zonas.** `FuenteDePedidos` existe para que `Repartidor` sea
  exactamente el mismo código en ambas corridas. Sin ella, la comparación no probaría nada: podría
  argumentarse que la diferencia vino del repartidor y no de la sincronización.
- **El testigo es más robusto que lo que vigila.** `RegistroDeIncidencias` usa estructuras atómicas
  precisamente porque tiene que dar un veredicto confiable sobre un sistema que está fallando.
- **La firma de `retirarPedido()` se mantiene tal como la pide el enunciado**, sin declarar
  `InterruptedException`. La interrupción se atiende adentro devolviendo `null`, que en este diseño
  significa "no hay más trabajo para ti".
- **Doce pedidos y no cinco.** El mínimo de la actividad son cinco, pero con tan pocos la
  competencia por la zona casi no ocurre y el escenario sin sincronizar no alcanzaría a mostrar el
  problema.
- **Los objetos `Pedido` se crean de nuevo en cada escenario.** Un pedido ya entregado no puede
  volver a repartirse, y la zona lo rechaza explícitamente: solo acepta pedidos `PENDIENTE`. Antes
  corregía el estado en silencio, lo que escondía el error en lugar de mostrarlo.
- **El pool se cierra en un `finally`.** Los hilos de un pool fijo no son *daemon*: si una excepción
  inesperada escapara sin cerrarlo, la JVM se quedaría viva para siempre con los repartidores
  dormidos. Por la misma razón `Future.get()` lleva tiempo máximo — un `get()` sin límite justo
  después de haber decidido no esperar más anularía el guardarraíl.

## Entorno

- IntelliJ IDEA
- Java 17
- JUnit 5 (incluido en IntelliJ) para las pruebas

---

**Autora:** Olga Rivas
