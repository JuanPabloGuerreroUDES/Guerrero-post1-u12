# Sistema de Solicitudes de Reembolso de Gastos Corporativos

**Post-contenido 1 — Unidad 12 · Patrones de Diseño de Software**
**Autor:** Juan Pablo Guerrero Hernández

Proyecto integrador de cierre de la materia. El área de Finanzas de una empresa mediana reemplaza su hoja de cálculo compartida por un sistema propio para registrar, validar, calcular y notificar solicitudes de reembolso de gastos.

Este README es el documento de diseño del proyecto: primero se analiza la especificación y se propone el diseño, y solo después se implementa.

---

## 1. Diseño Propuesto

> Sección escrita **antes** de programar cualquier patrón. En este punto el repositorio solo contiene el `pom.xml` y el modelo de dominio (`SolicitudReembolso`, `CategoriaGasto`, `EstadoSolicitud`).

Para cada requisito se identifica **qué es lo que varía**, qué patrón aísla esa variación y se aplica el *test de explicabilidad* de la Guía (sección 1.2): si no puedo explicar en una oración por qué uso el patrón, probablemente no lo necesito.

| Requisito | Qué varía | Patrón propuesto | Justificación en una oración |
|-----------|-----------|------------------|------------------------------|
| **RF1** — Validación en varias etapas | La **cantidad y el orden de las verificaciones** que debe pasar una solicitud | **Chain of Responsibility** | Cada verificación es un eslabón que aprueba o corta el flujo sin conocer a los demás, de modo que agregar el "límite anual acumulado" es crear un eslabón nuevo y engancharlo, sin tocar los tres existentes. |
| **RF2** — Cálculo distinto por categoría | La **fórmula** con la que se calcula el monto aprobado | **Strategy** | Las tres fórmulas son algoritmos intercambiables detrás de una misma operación (`calcular`), así que agregar la categoría "bienestar" es agregar una clase y no editar un `switch` que ya funciona. |
| **RF3** — Notificación simultánea | El **conjunto de interesados** y **la reacción de cada uno** ante un cambio de estado | **Observer** | El servicio solo anuncia "esta solicitud cambió de estado" y cada interesado decide por su cuenta qué hacer, por lo que sumar el panel de métricas no modifica ni al servicio ni a los otros interesados. |

### RF1 — Validación en varias etapas → Chain of Responsibility

- **Punto de variabilidad:** hoy son tres verificaciones (tope por categoría, documentos soporte, duplicados); Finanzas ya anunció una cuarta (límite anual acumulado). Lo que cambia con el tiempo es *cuántas verificaciones hay y en qué orden corren*, no la solicitud ni el servicio.
- **Por qué este patrón:** las verificaciones son independientes entre sí y el flujo debe detenerse en el primer rechazo. Chain of Responsibility modela exactamente eso: cada eslabón decide y, si aprueba, delega en el siguiente.
- **Por qué no otro:**
  - Tres `if` seguidos en el servicio funcionan hoy, pero cada verificación nueva obligaría a modificar el servicio (viola el requisito de "sin modificar las verificaciones existentes" en la práctica, porque todas viven en el mismo método).
  - *Template Method* fija el número de pasos en la clase base; aquí justamente el número de pasos es lo que varía.
  - *Specification* componible (`and`/`or`/`not`) sería más de lo necesario: no hay ningún requisito que pida combinar reglas con lógica booleana, solo ejecutarlas en secuencia.

### RF2 — Cálculo distinto por categoría → Strategy

- **Punto de variabilidad:** la fórmula del monto aprobado. Viáticos: tope diario × días de viaje. Representación: monto completo hasta un tope mensual. Capacitación: 80 % del monto hasta un tope anual. Finanzas planea categorías nuevas ("bienestar"), cada una con su fórmula.
- **Por qué este patrón:** todas las fórmulas responden a la misma pregunta ("¿cuánto se aprueba para esta solicitud?") con la misma entrada y la misma salida; solo cambia el algoritmo. Eso es la definición de Strategy.
- **Por qué no otro:**
  - Un `switch` sobre `CategoriaGasto` concentra todas las fórmulas en un método que hay que reabrir con cada categoría nueva.
  - *Template Method* no aplica porque las fórmulas no comparten un esqueleto común de pasos: una multiplica por días, otra aplica un porcentaje, otra solo recorta.
  - *Factory Method / Abstract Factory* resuelven creación de objetos, y aquí el problema no es crear nada sino elegir un comportamiento.

### RF3 — Notificación simultánea → Observer

- **Punto de variabilidad:** quiénes se enteran de un cambio de estado y cómo reacciona cada uno. El empleado recibe siempre el resultado, Contabilidad solo reacciona a las aprobaciones, Auditoría registra todo. Finanzas ya mencionó un cuarto interesado (panel de métricas).
- **Por qué este patrón:** hay un único emisor (el cambio de estado) y varios receptores con reacciones distintas e independientes. Con Observer, el filtro "solo me interesan las aprobaciones" vive dentro de Contabilidad y no en el servicio.
- **Por qué no otro:**
  - Llamadas directas desde el servicio a cada notificador lo acoplan a los tres y obligan a editarlo por cada interesado nuevo.
  - *Mediator* coordina comunicación en varias direcciones entre colegas; aquí la comunicación es de uno a muchos y en un solo sentido.
  - Una cadena (*Chain of Responsibility*) no sirve porque en una cadena un eslabón puede cortar el flujo, y el requisito exige que **todos** se enteren.

### Por qué tres patrones distintos y no uno solo

Cada patrón responde a una forma de variación diferente, y por eso no son intercambiables entre sí:

| Patrón | Cuántos participan por solicitud | ¿Puede uno detener a los demás? |
|--------|----------------------------------|---------------------------------|
| Chain of Responsibility (RF1) | Todos, en orden, hasta el primer rechazo | Sí — es el objetivo |
| Strategy (RF2) | Exactamente uno, elegido por la categoría | No aplica |
| Observer (RF3) | Todos, siempre | No — todos deben enterarse |

### Estructura de paquetes prevista

```
src/main/java/com/empresa/reembolsos/
├── ReembolsosApplication.java
├── dominio/            modelo plano, sin Spring ni JPA
├── procesamiento/
│   ├── validacion/     RF1 — Chain of Responsibility
│   ├── calculo/        RF2 — Strategy
│   └── notificacion/   RF3 — Observer
├── aplicacion/         servicio que coordina los tres patrones
├── infraestructura/    repositorio JPA
└── adaptadores/rest/   controlador REST
```

---

## 2. Decisiones de diseño

### Decisión 1 — Orden de la cadena de validadores

**Decisión:** `documentos → monto → duplicados`. El eslabón más caro va **de último**.

```java
documentos.siguiente(monto).siguiente(duplicados);
```

**Criterio de eficiencia.** Los tres eslabones no cuestan lo mismo:

| Eslabón | Qué hace | Costo |
|---------|----------|-------|
| `ValidadorDocumentosSoporte` | Lee un `boolean` de la solicitud | O(1) |
| `ValidadorMontoMaximo` | Busca un tope en un mapa y compara dos `BigDecimal` | O(1) |
| `ValidadorDuplicados` | Recorre la lista de solicitudes pendientes del empleado | O(n) |

Como la cadena se corta en el primer rechazo, conviene que las verificaciones baratas filtren primero: una solicitud sin documentos o con el monto pasado de tope se rechaza sin llegar a recorrer la lista de pendientes.

**Qué se gana y qué se pierde con cada orden:**

| | Duplicados **al final** (elegido) | Duplicados **al inicio** |
|---|---|---|
| Se gana | Las solicitudes mal diligenciadas (el error más común) se rechazan en O(1). El motivo que ve el empleado es el más fácil de corregir por su cuenta: adjuntar el soporte o ajustar el monto. | El empleado se entera de inmediato de que ya tiene una solicitud en curso y no pierde tiempo corrigiendo una que igual sería rechazada. |
| Se pierde | Un empleado puede corregir documentos y monto, y solo entonces enterarse de que la solicitud era un duplicado. | Se recorre la lista de pendientes para **todas** las solicitudes, incluso las que iban a caer por una verificación trivial. |

Entre `documentos` y `monto` el costo es el mismo; se dejó `documentos` primero porque es el rechazo más frecuente y el más simple de explicar.

**Límite de esta decisión.** El orden solo ahorra el *recorrido* de la lista, no la *consulta* a la base de datos: el servicio carga los pendientes antes de invocar la cadena, porque así `ValidadorDuplicados` no necesita conocer el repositorio. Se aceptó ese costo a cambio de mantener `procesamiento.validacion` sin dependencias de infraestructura. Si la consulta llegara a pesar, la carga de pendientes se podría volver perezosa (`Supplier<List<...>>`) sin cambiar el orden de la cadena.

### Decisión 2 — Mecanismo de selección de la calculadora

**Decisión:** un mapa `CategoriaGasto → CalculadoraReembolso` construido a partir de la `List<CalculadoraReembolso>` que Spring inyecta con todos los `@Component` registrados. Se descartó el `switch` manual.

```java
public SelectorCalculadora(List<CalculadoraReembolso> lista) {
    this.calculadoras = lista.stream()
        .collect(Collectors.toMap(CalculadoraReembolso::getCategoria, Function.identity()));
}
```

**Justificación contra el RF2.** El requisito dice que Finanzas agregará categorías "sin modificar el cálculo de las categorías existentes". Comparando qué hay que tocar para agregar la categoría "bienestar":

| | Mapa desde `List` inyectada (elegido) | `switch` sobre `CategoriaGasto` |
|---|---|---|
| Archivos **nuevos** | `CalculadoraBienestar.java` | `CalculadoraBienestar.java` |
| Archivos **modificados** del cálculo | Ninguno | `SelectorCalculadora` (un `case` más) |
| Si se olvida registrar la categoría | No se puede olvidar: el `@Component` se registra solo | Compila igual y falla en tiempo de ejecución con la primera solicitud |

Solo la primera opción cumple el requisito sin tocar una línea ya escrita del paquete `calculo`: cada calculadora declara su propia categoría con `getCategoria()` y el selector no conoce ninguna implementación concreta. Es el principio abierto/cerrado aplicado donde el requisito lo pide de forma explícita, no por anticipación.

**Costos aceptados:**

- El valor nuevo en el enum `CategoriaGasto` sí hay que agregarlo; eso es dominio, no cálculo, y es inevitable en ambas opciones.
- La relación categoría → calculadora ya no se lee en un solo lugar; hay que buscar las implementaciones de la interfaz. Con tres clases en un paquete es un costo menor.
- Si dos calculadoras declaran la misma categoría, `Collectors.toMap` lanza `IllegalStateException` y la aplicación **no arranca**. Se considera una ventaja: el error aparece al desplegar y no con la solicitud de un empleado.
- Una categoría nueva también necesita su tope en `ValidadorMontoMaximo` (RF1). Eso es una línea en el mapa de topes de la validación; las fórmulas de las categorías existentes, que es lo que protege el RF2, no se tocan.

### Decisión 3 — Capas simples o Hexagonal

Documentada en [`docs/adr/ADR-001.md`](docs/adr/ADR-001.md) y resumida en la [sección 3](#3-adr-001--resumen-de-la-decisión-arquitectónica).

### Decisión 4 — Por qué la Regla 1 no prohíbe todo `org.springframework`

**Decisión:** la Regla 1 de ArchUnit prohíbe que `procesamiento` dependa de `..infraestructura..`, `..adaptadores..`, `org.springframework.data..` y `jakarta.persistence..`, pero **permite** las anotaciones de estereotipo (`@Component`).

**Justificación.** Lo que la regla protege no es la "pureza" del paquete sino una propiedad concreta: que los tres patrones se puedan entender, probar y cambiar sin base de datos y sin web. Las dos clases de dependencia afectan esa propiedad de forma muy distinta:

| | Anotación de estereotipo (`@Component`) | Acceso a datos o capa web (`JpaRepository`, `EntityManager`, `@Entity`, controladores) |
|---|---|---|
| Qué es | Metadato: le dice al contenedor "regístrame" | Capacidad: le permite a la clase hacer entrada/salida |
| ¿Cambia lo que la clase hace? | No. La clase se comporta igual con o sin Spring | Sí. La clase pasa a consultar o escribir fuera de sí misma |
| ¿Se puede probar con `new`? | Sí — así lo hacen las pruebas unitarias de este proyecto (`new ValidadorDuplicados()`, `new CalculadoraViaje()`) | No sin simular el repositorio o levantar una base de datos |
| ¿Invierte la dirección de dependencias? | No | Sí: `procesamiento` empezaría a conocer a `infraestructura` |

Prohibir cualquier import de `org.springframework` habría tenido un costo real y ningún beneficio:

1. **Rompería la Decisión 2.** Sin `@Component`, cada validador, calculadora y notificador tendría que registrarse a mano en una clase `@Configuration` con un método `@Bean` por cada uno. Agregar la categoría "bienestar" o el observador del panel de métricas volvería a exigir modificar un archivo existente, justo lo que RF2 y RF3 piden evitar.
2. **Castigaría algo que no hace daño.** Una regla que marca como violación un `@Component` inofensivo termina desactivada o llena de excepciones, y entonces deja de detectar lo que sí importa.
3. **Sería acoplamiento solo nominal.** Si algún día se cambiara de framework, quitar una anotación por clase es mecánico; desenredar un `JpaRepository` usado dentro de un validador no lo es.

**Límite de la regla tal como está escrita.** Prohíbe las dependencias que hoy existen en el proyecto y que harían daño (Spring Data, JPA, los paquetes propios de infraestructura y adaptadores). No lista, por ejemplo, `org.springframework.web..` ni `org.springframework.jdbc..`: hoy ninguna clase de `procesamiento` tiene motivo para usarlas y agregar prohibiciones para casos que no han ocurrido sería ruido. Si la integración de Contabilidad (ADR-001) trae un cliente HTTP, ese será el momento de ampliar la regla.

#### Ajustes a las reglas ArchUnit respecto al enunciado

Al ejecutar las reglas contra el código real aparecieron dos falsos positivos. Se corrigió la regla y no el código, porque en ambos casos el código cumplía la intención de la regla:

| Regla | Qué fallaba | Ajuste |
|-------|-------------|--------|
| `controladorSoloUsaServicio` | ArchUnit cuenta como "acceso" que el controlador lea y escriba su propio campo `servicio`, que está en `adaptadores.rest` | Se agregó `..adaptadores.rest..` a los paquetes permitidos |
| `calculadorasImplementanInterfaz` | `SelectorCalculadora` vive en `procesamiento.calculo` pero no es una estrategia, es quien las elige | La regla aplica a las clases cuyo nombre empieza por `Calculadora` |

Además, `@AnalyzeClasses` usa `ImportOption.DoNotIncludeTests` para que las reglas evalúen solo código de producción: las clases de prueba (`CalculadoraViajeTest`, etc.) comparten paquete con las calculadoras y no implementan la interfaz.

---

## 4. Qué se descartó y por qué

Durante el diseño y la implementación aparecieron varias alternativas más sofisticadas que las que quedaron en el código. Para cada una se aplicó la misma pregunta de la Guía (sección 4.2): **¿la variabilidad que la justificaría es real y está documentada hoy, o es especulación sobre un futuro que aún no llegó?**

Ningún descarte es definitivo: es la decisión vigente hoy, y cada uno indica qué señal haría reconsiderarlo.

| # | Alternativa descartada | Principio que la descarta | Origen |
|---|------------------------|---------------------------|--------|
| 1 | Patrón State para el ciclo de vida de la solicitud | YAGNI | Propio |
| 2 | Jerarquía de excepciones, una por validador | Test de explicabilidad | Propio |
| 3 | Entidad JPA separada del dominio + mapper | YAGNI / KISS | Propio |
| 4 | Topes y porcentajes externalizados a configuración | Rule of Three | Propio |
| 5 | `ApplicationEventPublisher` en vez de la lista de observadores | Test de explicabilidad | Referencia del enunciado |
| 6 | Visitor o Abstract Factory para exportar reportes | Rule of Three | Referencia del enunciado |

### 4.1 Patrón State para el ciclo de vida de la solicitud

- **Qué se consideró.** `EstadoSolicitud` tiene cuatro valores y `cambiarEstado` acepta cualquier transición, incluso `APROBADA → PENDIENTE`. Fue tentador modelar cada estado como una clase (`EstadoPendiente`, `EstadoAprobada`, …) con sus transiciones permitidas, que es el caso de libro del patrón State.
- **Por qué se descartó.** State se justifica cuando el **comportamiento** del objeto cambia según su estado. En los tres requisitos la solicitud no hace nada distinto según su estado: solo lo almacena, y quien reacciona al cambio son los observadores. Tampoco hay ningún requisito que defina qué transiciones son válidas; inventarlas sería decidir reglas de negocio que le corresponden a Finanzas. Cuatro clases y una interfaz para envolver un `setEstado` es resolver un problema que el enunciado no plantea (YAGNI).
- **Qué señal haría reconsiderar.** Que Finanzas defina reglas por estado: por ejemplo, que una solicitud en `REQUIERE_CORRECCION` pueda editarse y reenviarse pero una `APROBADA` no, o que rechazar exija un motivo. Incluso entonces, el primer paso sería una tabla de transiciones permitidas en el enum; State solo si además aparece comportamiento distinto por estado.

### 4.2 Jerarquía de excepciones, una por validador

- **Qué se consideró.** Crear `DocumentosFaltantesException`, `MontoExcedidoException` y `SolicitudDuplicadaException` (heredando de una base común) en lugar de una sola `SolicitudInvalidaException` con el motivo como texto.
- **Por qué se descartó.** No pasa el test de explicabilidad: no pude completar la frase "necesito tres tipos de excepción porque…". Una jerarquía se justifica cuando alguien las **captura de forma distinta**, y aquí hay un único `@ExceptionHandler` que responde lo mismo para las tres: HTTP 422 con el motivo. Además iría en contra del RF1: cada verificación nueva obligaría a crear también su excepción, cuando hoy basta con una clase que devuelve `ResultadoValidacion.rechazar("…")`.
- **Qué señal haría reconsiderar.** Que un cliente de la API necesite reaccionar por programa de forma distinta según la causa (por ejemplo, que el frontend abra el cargador de archivos solo cuando faltan documentos). Aun así, lo proporcional sería agregar un código de motivo a `ResultadoValidacion`, no una clase de excepción por validador.

### 4.3 Entidad JPA separada del dominio, con mapper

- **Qué se consideró.** El enunciado pide dos cosas que chocan: un dominio sin imports de JPA y un `JpaRepository<SolicitudReembolso, Long>` usado directamente. La salida "de arquitectura limpia" era crear `SolicitudReembolsoEntity` con las anotaciones, un `SolicitudReembolsoMapper` para convertir en ambos sentidos y un adaptador que envolviera el repositorio.
- **Por qué se descartó.** Son tres clases nuevas para copiar nueve campos idénticos de un objeto a otro, y es exactamente el puerto de persistencia que ADR-001 rechaza por especulativo. Se resolvió con una herramienta más simple que ya trae JPA: el mapeo en `META-INF/orm.xml`. El dominio queda sin un solo import de `jakarta.persistence` y no existe modelo duplicado (KISS).
- **Qué señal haría reconsiderar.** Que el esquema de la base de datos y el modelo de dominio empiecen a divergir (columnas que no corresponden a campos, una solicitud repartida en varias tablas) o que llegue el segundo origen de datos que menciona ADR-001. Mientras tabla y clase sean espejo, el mapper no traduce nada.

### 4.4 Topes y porcentajes externalizados a configuración

- **Qué se consideró.** Los topes (150.000 diario, 2.000.000 mensual, 3.000.000 anual) y el 80 % de capacitación están como constantes en las clases. Se pensó en moverlos a `application.yml` con `@ConfigurationProperties`, o a una tabla administrable, para que Finanzas los cambie sin desplegar.
- **Por qué se descartó.** Nadie ha pedido cambiarlos. El RF2 habla de agregar **categorías con fórmulas nuevas**, que es código en cualquier caso, no de ajustar valores. No se cumple la Rule of Three: no ha habido ni un primer cambio de tope, mucho menos tres. Externalizarlos hoy agrega una clase de propiedades, validación de configuración y una fuente de errores nueva (un tope mal escrito en un archivo que nadie prueba), a cambio de una flexibilidad que nadie solicitó.
- **Qué señal haría reconsiderar.** El primer cambio real de un tope se hace editando la constante. Si Finanzas los ajusta de forma recurrente (por ejemplo, cada año con el incremento del salario mínimo) o pide topes distintos por área o cargo, ahí sí hay variabilidad demostrada y se externalizan.
- **Observación relacionada.** `ValidadorMontoMaximo` y las calculadoras tienen cada uno sus propias constantes de tope y no las comparten. Se dejó así a propósito: responden a requisitos distintos (RF1 es un techo por solicitud, RF2 es la fórmula de la categoría) y unificarlas las acoplaría sin que ningún requisito lo pida. Con los valores del enunciado el techo de la validación es igual o más estricto que el tope de la fórmula, de modo que el recorte de las calculadoras actúa como segunda barrera y se verifica en sus pruebas unitarias.

### 4.5 Spring `ApplicationEventPublisher` en vez de una lista de observadores inyectada *(referencia del enunciado)*

- **Qué se consideró.** Publicar un evento de dominio con `ApplicationEventPublisher` y reaccionar con `@EventListener`, como muestra la Guía (sección 2.3), en lugar de inyectar `List<ObservadorCambioEstado>` en el servicio.
- **Por qué se descartó.** Con tres observadores fijos y conocidos en tiempo de compilación, la indirección de un bus de eventos genérico no aporta desacoplamiento adicional —ya se logra con la interfaz `ObservadorCambioEstado`— y sí dificulta rastrear en el código quién reacciona a qué cambio, el mismo costo de depuración que advierte la Guía sobre Observer con eventos. Con la lista inyectada, "buscar implementaciones" de la interfaz responde la pregunta en el IDE.
- **Qué señal haría reconsiderar.** Que aparezcan observadores que deban ejecutarse de forma asíncrona o después del commit de la transacción (`@TransactionalEventListener`), o emisores distintos del servicio que publiquen el mismo evento. La integración externa de Contabilidad (ADR-001) es la candidata más probable a provocarlo.

### 4.6 Visitor o Abstract Factory para exportar reportes en varios formatos *(referencia del enunciado)*

- **Qué se consideró.** Anticipar que Contabilidad pediría exportar el historial de solicitudes en PDF y Excel además de CSV, y construir desde ahora una jerarquía de exportadores.
- **Por qué se descartó.** El único formato del que se ha hablado es CSV (y la exportación ni siquiera hace parte de los tres requisitos implementados); no hay una segunda solicitud real de otro formato, así que no se cumple la Rule of Three (Guía, sección 4.2). Construir la abstracción ahora sería resolver un problema que todavía no existe, y con un solo formato no hay forma de saber qué tienen en común los exportadores.
- **Qué señal haría reconsiderar.** La llegada del segundo formato justifica extraer una interfaz `ExportadorReporte` (un Strategy simple). Visitor solo si además el reporte debe recorrer estructuras heterogéneas, cosa que ningún requisito sugiere.
