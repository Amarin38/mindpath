---
title: "Trabajo Práctico -- MindPath"
subtitle: "Documentación del TP - Entrega primera etapa"
author: "Agustín Marino"
---

# Diagrama de clases
\begin{landscape}

![Diagrama de clases](diagrama.png){ width=100% }

\end{landscape}

***
# Modelo de datos en MongoDB

### Coleccion `usuario`
 - `Usuario` `-->` `Docente`
```json
{
  "_id": "u1",
  "tipo": "DOCENTE",
  "email": "mgarcia@mindpath.edu",
  "passwordHash": "$2a$10$N9qo8uLOickgx2ZMRZo...",
  "nombre": "Marcela",
  "apellido": "García",
  "fechaAlta": "2026-03-01T10:00:00Z",
  "especialidad": "Matemática"
}
```

 - `Usuario` `-->` `Alumno`
```json
{
  "_id": "u2",
  "tipo": "ALUMNO",
  "email": "jperez@mindpath.edu",
  "passwordHash": "$2a$10$7Kx1pQrZ8vNmLc3Yd...",
  "nombre": "Juan",
  "apellido": "Pérez",
  "fechaAlta": "2026-03-05T14:30:00Z",
  "legajo": "A-10432"
}
```

### Coleccion `cursos`
 - `Curso` `-->` `Tema` (Embebida) 
 - `Tema` `-->` `Recurso` (Embebida)
 - `Tema` `-->` `Tema` (Referencia)
```json
{
  "_id": "c1",
  "docenteId": "u1",
  "titulo": "Álgebra Lineal I",
  "descripcion": "Introducción a vectores, matrices y sistemas lineales.",
  "fechaCreacion": "2026-03-01T10:15:00Z",
  "temas": [
    {
      "_id": "t1",
      "nombre": "Vectores en R²",
      "descripcion": "Operaciones básicas con vectores en el plano.",
      "dificultad": "BAJA",
      "orden": 1,
      "prerrequisitos": [],
      "recursos": [
        {
          "_id": "r1",
          "tipo": "VIDEO",
          "titulo": "Suma y resta de vectores",
          "url": "https://cdn.mindpath.edu/vid/vectores-01.mp4",
          "orden": 1
        },
        {
          "_id": "r2",
          "tipo": "PDF",
          "titulo": "Apunte teórico",
          "url": "https://cdn.mindpath.edu/pdf/vectores.pdf",
          "orden": 2
        }
      ]
    },
    {
      "_id": "t2",
      "nombre": "Matrices",
      "descripcion": "Definición, tipos y operaciones matriciales.",
      "dificultad": "MEDIA",
      "orden": 2,
      "prerrequisitos": ["t1"],
      "recursos": [
        {
          "_id": "r3",
          "tipo": "VIDEO",
          "titulo": "Producto de matrices",
          "url": "https://cdn.mindpath.edu/vid/matrices-01.mp4",
          "orden": 1
        }
      ]
    }
  ]
}
```

<br><br>
<br><br>

### Coleccion `evaluacion` 
 - `Evaluacion` `-->` `Respuesta` (Embebida)
``` json
{
  "_id": "e1",
  "alumnoId": "u2",
  "temaId": "t1",
  "fechaInicio": "2026-03-15T11:00:00Z",
  "fechaEntrega": "2026-03-15T11:23:00Z",
  "estado": "FINALIZADA",
  "puntajeTotal": 25.0,
  "aprobada": true,
  "respuestas": [
    {
      "_id": "rp1",
      "preguntaId": "p1",
      "contenido": "5",
      "correcta": true,
      "puntajeObtenido": 10.0,
      "tiempoSegundos": 42,
      "corregidaAutomaticamente": true
    },
    {
      "_id": "rp2",
      "preguntaId": "p2",
      "contenido": "Es la diagonal del paralelogramo que forman.",
      "correcta": true,
      "puntajeObtenido": 15.0,
      "tiempoSegundos": 380,
      "corregidaAutomaticamente": true
    }
  ]
}
```

### Coleccion `alumno`
 - `Alumno` `-->` `ProgresoTema` (Referencia)\
Se actualiza con frecuencia con cada evaluación.
``` json
{
  "_id": "pg1",
  "alumnoId": "u2",
  "temaId": "t1",
  "nivelDominio": 0.83,
  "intentos": 2,
  "fechaUltimaEvaluacion": "2026-03-15T11:23:00Z",
  "mejorPuntaje": 25.0,
  "desbloqueado": true
}
```
 - `Alumno` `-->` `Inscripcion` (Referencia)
``` json
{
  "_id": "i1",
  "alumnoId": "u2",
  "cursoId": "c1",
  "fechaInscripcion": "2026-03-10T09:00:00Z",
  "fechaBaja": null,
  "estado": "ACTIVA"
}
```
### Coleccion `pregunta`
- `Pregunta` `-->` `PreguntaOpcionMultiple` y `Tema` `-->` `Pregunta` (Referencia)
``` json
{
  "_id": "p1",
  "tipo": "OPCION_MULTIPLE",
  "temaId": "t1",
  "enunciado": "¿Cuál es el módulo del vector (3, 4)?",
  "dificultad": "BAJA",
  "puntaje": 10.0,
  "tasaAcierto": 0.78,
  "opciones": ["5", "7", "12", "25"],
  "opcionCorrecta": 0
}
```
- `Pregunta` `-->` `PreguntaAbierta` y `Tema` `-->` `Pregunta` (Referencia)
``` json
{
  "_id": "p2",
  "tipo": "ABIERTA",
  "temaId": "t1",
  "enunciado": "Explicá qué representa geométricamente la suma de dos vectores.",
  "dificultad": "MEDIA",
  "puntaje": 20.0,
  "tasaAcierto": 0.45,
  "respuestaEsperada": "La diagonal del paralelogramo formado por ambos vectores.",
  "palabrasClave": ["paralelogramo", "diagonal", "regla del triángulo"]
}
```

## Decisiones de modelado

| Relación (UML) | Tipo | Decisión |
|---|---|---|
| Curso → Tema | Composición | Embebida |
| Tema → Recurso | Composición | Embebida |
| Evaluacion → Respuesta | Composición | Embebida |
| Alumno → ProgresoTema | Composición | Referencia |
| Alumno → Inscripcion | Composición | Referencia |
| Tema → Pregunta | Asociación | Referencia |
| Docente → Curso | Asociación | Referencia |
| Tema → Tema (Requiere) | Asociación reflexiva | Array de IDs |
| Usuario → Alumno / Docente | Herencia | Discriminador |
| Pregunta → OpcionMultiple / Abierta | Herencia | Discriminador |

## Colecciones

| Colección | Contenido | Documentos embebidos |
|---|---|---|
| `usuarios` | Docentes y alumnos | — |
| `cursos` | Cursos con su estructura de contenidos | `temas`, y dentro de cada tema `recursos` |
| `inscripciones` | Relación alumno-curso con su estado | — |
| `preguntas` | Banco de preguntas de ambos tipos | — |
| `evaluaciones` | Intentos de evaluación de los alumnos | `respuestas` |
| `progresoTema` | Estado de avance por alumno y tema | — |

## Índices

| Colección | Índice | Tipo |
|---|---|---|
| `usuarios` | `email` | Único |
| `usuarios` | `legajo` | Único, sparse |
| `cursos` | `docenteId` | Simple |
| `inscripciones` | `alumnoId + cursoId` | Único parcial (`estado: "ACTIVA"`) |
| `inscripciones` | `cursoId` | Simple |
| `preguntas` | `temaId + dificultad` | Compuesto | 
| `evaluaciones` | `alumnoId + temaId` | Compuesto |
| `progresoTema` | `alumnoId + temaId` | Único | 

***
# Tecnologías a utilizar
### Backend
- Lenguaje: `Java 25 (LTS)`
- Framework: `Spring Boot 4.1.1`
- Gestión de dependencias: `Maven`
- Acceso a datos: `Spring Data MongoDB`
- Testing: `JUnit 6`, `Mockito`,  `Testcontainers`

_Justificación_: Elegí Java porque es un lenguaje con el que me siento cómodo y del que quiero seguir aprendiendo, usando su versión LTS más reciente para contar con soporte extendido y evitar problemas de estabilidad. Spring Boot ofrece todo lo necesario para construir una API REST completa —inyección de dependencias, gestión de configuración, seguridad y una integración directa con MongoDB a través de Spring Data—, lo que me permite concentrarme en la lógica del dominio en lugar de en la infraestructura. Para la gestión de dependencias opté por Maven por su simplicidad y su configuración declarativa; Gradle habría sido una alternativa válida y más flexible, pero su curva de aprendizaje agregaba complejidad innecesaria al proyecto. En cuanto al testing, las tres herramientas cubren niveles distintos: JUnit provee el framework de ejecución y las aserciones, Mockito permite aislar unidades reemplazando sus dependencias, y Testcontainers habilita pruebas de integración contra una instancia real de MongoDB en Docker, algo necesario porque las bases embebidas no reproducen fielmente el comportamiento del motor.

### Base de datos
- Motor: `MongoDB 8`
- Entorno: `MongoDB Atlas` (nube)

### Frontend
- Lenguaje: `JavaScript`
- Framework: `React 19`
- Build tool: `Vite`

_Justificación_: Elegí JavaScript por ser el lenguaje nativo del navegador; TypeScript habría aportado tipado estático, pero sumaba una capa de configuración y aprendizaje que no era prioritaria en este proyecto. Opté por React porque tiene una curva de aprendizaje más accesible que otros frameworks del ecosistema y, al mismo tiempo, ofrece la capacidad suficiente para construir una interfaz de la complejidad que requiere el sistema. Para el empaquetado utilicé Vite en lugar de Create React App, que se encuentra discontinuado, porque ya viene preconfigurado y listo para usar.

### Control de versiones
- `Git` / `GitHub`

***
# Tipos de Testing y Módulos a testear
### Módulos a testear
| Módulo | Tipo de test | Qué se testea |
|---|---|---|
| Corrección de preguntas | Unit test | `corregir()` en opción múltiple y abierta |
| Cálculo de evaluación | Unit test | Puntaje total y condición de aprobación |
| Validación de prerrequisitos | Unit test | Que no se pueda declarar una dependencia circular entre temas |
| Desbloqueo de temas | Unit test | `estaDesbloqueadoPara()` según el progreso del alumno |
| Progreso del alumno | Unit test | Actualización de intentos y mejor puntaje |
| Ciclo de vida de la evaluación | State transition test | Transiciones válidas e inválidas de `estado` |
| Ciclo de vida de la inscripción | State transition test | Alta, baja y reinscripción |
| Persistencia con herencia | Integration test | Mapeo del discriminador `tipo` |
| Documentos embebidos | Integration test | Cursos con temas y recursos anidados |
| Restricciones de unicidad | Integration test | Índice parcial de inscripciones |
| Flujo de inscripción | System test | Alta y baja vía API |
| Flujo de evaluación | System test | Iniciar, responder, finalizar y actualizar progreso |
| Autorización por rol | System test | Restricción de operaciones según tipo de usuario |
***
