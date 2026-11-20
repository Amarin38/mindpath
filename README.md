# MindPath

API REST de una plataforma e-learning adaptativa. Los alumnos se inscriben a cursos, rinden evaluaciones por tema y el sistema calcula su **nivel de dominio**. La corrección de las evaluaciones se hace de forma asíncrona en un microservicio, conectado por Kafka.

**Stack:** Java 25 · Spring Boot 4.1.1 · Maven · MongoDB Atlas · Kafka (KRaft) · Docker Compose · springdoc (Swagger)

## Arquitectura

```
                 ┌──────────────┐  evaluacion.entregada   ┌────────────────┐
  Cliente ──────▶│     API      │ ──────────────────────▶ │  Calificación  │
  (Swagger)      │    :8090     │                         │     :8091      │
                 │              │ ◀────────────────────── │   (sin BD)     │
                 └──────┬───────┘  evaluacion.corregida   └────────────────┘
                        │                    ▲
                        ▼                    │
                  MongoDB Atlas        Kafka (KRaft)
```

| Componente | Responsabilidad |
|---|---|
| **API** (`/`) | Cursos, temas, preguntas, inscripciones, evaluaciones y progreso. Persiste en MongoDB. |
| **Calificación** (`/calificacion`) | Servicio sin estado ni base de datos. Corrige las respuestas y calcula el nivel de dominio. |
| **Kafka** | Comunicación asíncrona entre ambos. Tópicos `evaluacion.entregada` y `evaluacion.corregida`, con el id de la evaluación como clave. |
| **gateway (nginx)** | Proxy externo que publica la API en el puerto 81 (alias de red `mindpath`). |

### Flujo de una evaluación

1. `POST /evaluaciones` inicia la evaluación (`EN_PROCESO`) y el alumno responde con `POST /evaluaciones/{id}/respuestas`.
2. `POST /evaluaciones/{id}/entrega` la pasa a `ENTREGADO`, publica `evaluacion.entregada` y responde de inmediato.
3. Calificación consume el evento, corrige y publica `evaluacion.corregida`.
4. La API consume el resultado, pasa la evaluación a `FINALIZADA` y actualiza el progreso del alumno.

La aplicación del resultado es idempotente: solo se aplica si la evaluación está en `ENTREGADO`, así que un mensaje duplicado se ignora (entrega *at-least-once*).

### Reglas de negocio

- Para iniciar una evaluación el alumno necesita: inscripción activa, tema desbloqueado (prerrequisitos aprobados), ninguna evaluación en curso en ese tema, tema no aprobado y menos de 3 intentos.
- Las preguntas de un tema deben sumar al menos 10 puntos.
- Una evaluación se aprueba con el 70 % del puntaje máximo.
- **Nivel de dominio:** el primer intento es el porcentaje obtenido; los siguientes dan `0.4 × nuevo + 0.6 × anterior`.
- **Categoría:** `BAJO` (< 0.4), `MEDIO` (< 0.7), `ALTO` (≥ 0.7).
- Las preguntas pueden ser de opción múltiple o abiertas (se acierta si aparecen todas las palabras clave).

### Estructura

```
src/main/java/com/mindpath/api/
  usuario/  curso/  pregunta/  inscripcion/  evaluacion/  progreso/  common/  config/
calificacion/        microservicio de corrección (propio pom y Dockerfile)
tests/sistema.sh     pruebas de sistema end-to-end
samples.sh           datos de ejemplo
demo.sh              demo en vivo
mindpath.bashrc      comandos mp-*
```

## Uso

### Configuración

Crear un `.env` en la raíz con las credenciales de MongoDB Atlas:

```
MONGODB_USER=...
MONGODB_PASSWD=...
MONGODB_HOST=...
```

### Levantar

```bash
docker compose up -d --build
```

La red externa `proxy` (la del gateway) debe existir. Swagger de la API: `http://<host>:81/swagger-ui.html`. El de Calificación está en `http://<host>:8091/swagger-ui.html`.

### Endpoints principales

| Recurso | Endpoints |
|---|---|
| Usuarios | `POST /alumnos`, `POST /docentes`, `GET /usuarios`, `GET /usuarios/{id}` |
| Cursos y temas | `/cursos`, `/cursos/{cursoId}/temas`, `PUT .../temas/{temaId}/prerrequisitos/{id}`, `POST .../temas/{temaId}/recursos` |
| Preguntas | `/cursos/{cursoId}/temas/{temaId}/preguntas` |
| Inscripciones | `/inscripciones` |
| Evaluaciones | `POST /evaluaciones`, `GET /evaluaciones/{id}/preguntas`, `POST /evaluaciones/{id}/respuestas`, `POST /evaluaciones/{id}/entrega` |
| Progreso | `GET /alumnos/{alumnoId}/progresos` |
| Calificación | `POST /api/v1/calificaciones` (puerto 8091) |

El detalle completo está en Swagger. Los errores se devuelven como `ProblemDetail` (400, 404, 409).

### Comandos `mp-*`

Instalar en `~/.bashrc` (usa `MP_DIR`, por defecto `~/repos/mindpath`):

```bash
cat mindpath.bashrc >> ~/.bashrc && source ~/.bashrc
```

| Comando | Qué hace |
|---|---|
| `mp-logs` | Requests HTTP (`[REQ]`), eventos Kafka (`[EVT]`), warnings y errores de la API y de Calificación. |
| `mp-kafka` | Mensajes de los tópicos (`--from-beginning` para el historial). |
| `mp-test [unit\|sistema\|todo]` | Tests unitarios (Maven en Docker) y/o pruebas de sistema. |
| `mp-demo aprobado\|reintento\|agotado` | Demo en tmux: demo a la izquierda, `mp-kafka` y `mp-logs` a la derecha. |

### Datos de ejemplo

```bash
./samples.sh
```

Carga 2 docentes, 4 alumnos, 3 cursos, 7 temas con prerrequisitos, recursos, preguntas e inscripciones (cada tema suma 10 puntos). Requiere `curl` y `jq`.

### Demo

Cada escenario usa un alumno y un curso distintos, y necesita al alumno sin intentos previos en el tema.

| Escenario | Alumno · Curso | Qué muestra |
|---|---|---|
| `aprobado` | Matías · MongoDB | Aprueba al primer intento; no puede repetir el tema. |
| `reintento` | Luis · Java desde cero | Desaprueba, reintenta y aprueba; compara los dos intentos. |
| `agotado` | Sofía · Spring Boot | Desaprueba 3 veces; el cuarto intento es rechazado. |

La demo carga los samples, espera un Enter y avanza por pasos. `AUTO=1` quita las pausas y `RITMO=2` la hace más lenta.

El layout de tmux se ajusta con `MP_DEMO_COLS` (default 95) y `MP_KAFKA_ROWS` (default 14).

### Tests

```bash
mp-test unit      # JUnit + Mockito, sin infraestructura
mp-test sistema   # tests/sistema.sh, necesita el stack levantado
```

Para volver a cargar los samples hay que borrar antes la base de datos.
