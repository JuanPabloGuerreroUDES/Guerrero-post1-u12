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

**Límite honesto de esta decisión.** El orden solo ahorra el *recorrido* de la lista, no la *consulta* a la base de datos: el servicio carga los pendientes antes de invocar la cadena, porque así `ValidadorDuplicados` no necesita conocer el repositorio. Se aceptó ese costo a cambio de mantener `procesamiento.validacion` sin dependencias de infraestructura. Si la consulta llegara a pesar, la carga de pendientes se podría volver perezosa (`Supplier<List<...>>`) sin cambiar el orden de la cadena.
