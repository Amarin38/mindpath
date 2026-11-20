#!/usr/bin/env bash
# Carga datos de ejemplo vía API, mostrando qué crea.
#   ./samples.sh     (BASE=... para otra URL; RITMO=segundos entre ítems, default 0.4)
set -euo pipefail
BASE="${BASE:-http://192.168.68.81:81/api/v1}"
RITMO="${RITMO:-0.4}"
B=$'\e[1m'; D=$'\e[2m'; G=$'\e[32m'; C=$'\e[36m'; Y=$'\e[33m'; R=$'\e[31m'; N=$'\e[0m'

post() { # post <ruta> <json> [etiqueta]  -> imprime el id; la etiqueta va a stderr
  local resp code body
  resp=$(curl -s -w '\n%{http_code}' -X POST "$BASE$1" -H 'Content-Type: application/json' -d "$2") || resp=$'\n000'
  code="${resp##*$'\n'}"; body="${resp%$'\n'*}"
  if [[ ! "$code" =~ ^2 ]]; then
    echo "${R}FALLÓ POST $1 → HTTP $code${N}" >&2
    echo "${D}$body${N}" >&2
    case "$code" in
      409) echo "${Y}Ya existe: hay datos de una carga anterior. Borrá la base y volvé a correr.${N}" >&2 ;;
      000|502|503|504) echo "${Y}La API no responde (¿reconstruyendo? revisá docker compose ps y el proxy).${N}" >&2 ;;
    esac
    exit 1
  fi
  [ -z "${3:-}" ] || { echo "  ${G}✔${N} $3" >&2; sleep "$RITMO"; }
  echo "$body" | jq -r '.id'
}
put() { curl -sf -X PUT "$BASE$1" >/dev/null || { echo "${R}FALLÓ PUT $1${N}" >&2; exit 1; }; }
titulo() { echo; echo "${B}${C}$1${N}"; }

if curl -sf "$BASE/usuarios" | jq -e '.[] | select(.email=="ana.lopez@mail.com")' >/dev/null; then
  echo "${Y}Los datos de ejemplo ya están cargados. Para recargarlos, borrá la base primero.${N}"
  exit 0
fi

echo "${B}Cargando datos de ejemplo en MindPath${N}"

# ---------- Docentes ----------
titulo "Docentes"
D1=$(post /docentes '{"email":"laura.gomez@mindpath.edu","password":"password1","nombre":"Laura","apellido":"Gómez","especialidad":"Programación"}' "Laura Gómez · Programación")
D2=$(post /docentes '{"email":"carlos.ruiz@mindpath.edu","password":"password2","nombre":"Carlos","apellido":"Ruiz","especialidad":"Bases de datos"}' "Carlos Ruiz · Bases de datos")

# ---------- Alumnos ----------
titulo "Alumnos"
A1=$(post /alumnos '{"email":"ana.lopez@mail.com","password":"password1","nombre":"Ana","apellido":"López","legajo":"1001"}' "Ana López · legajo 1001")
A2=$(post /alumnos '{"email":"luis.perez@mail.com","password":"password2","nombre":"Luis","apellido":"Pérez","legajo":"1002"}' "Luis Pérez · legajo 1002")
A3=$(post /alumnos '{"email":"sofia.diaz@mail.com","password":"password3","nombre":"Sofía","apellido":"Díaz","legajo":"1003"}' "Sofía Díaz · legajo 1003")
A4=$(post /alumnos '{"email":"mati.sosa@mail.com","password":"password4","nombre":"Matías","apellido":"Sosa","legajo":"1004"}' "Matías Sosa · legajo 1004")

# ---------- Cursos ----------
titulo "Cursos"
C1=$(post "/cursos?docenteId=$D1" '{"titulo":"Java desde cero","descripcion":"Fundamentos de Java y programación orientada a objetos."}' "Java desde cero (Laura)")
C2=$(post "/cursos?docenteId=$D1" '{"titulo":"Spring Boot","descripcion":"APIs REST con Spring Boot y Spring Data."}' "Spring Boot (Laura)")
C3=$(post "/cursos?docenteId=$D2" '{"titulo":"MongoDB","descripcion":"Modelado y consultas en bases documentales."}' "MongoDB (Carlos)")

# ---------- Temas ----------
titulo "Temas"
T11=$(post "/cursos/$C1/temas" '{"nombre":"Variables y tipos","descripcion":"Tipos primitivos, operadores y control de flujo.","dificultad":"BAJA"}' "Java · Variables y tipos")
T12=$(post "/cursos/$C1/temas" '{"nombre":"Clases y objetos","descripcion":"Encapsulamiento, herencia y polimorfismo.","dificultad":"MEDIA"}' "Java · Clases y objetos")
T13=$(post "/cursos/$C1/temas" '{"nombre":"Colecciones y streams","descripcion":"List, Map, Set y la API de streams.","dificultad":"ALTA"}' "Java · Colecciones y streams")
T21=$(post "/cursos/$C2/temas" '{"nombre":"Inyección de dependencias","descripcion":"Beans, contexto y configuración.","dificultad":"BAJA"}' "Spring · Inyección de dependencias")
T22=$(post "/cursos/$C2/temas" '{"nombre":"Controladores REST","descripcion":"Mapeo de rutas, DTOs y validación.","dificultad":"MEDIA"}' "Spring · Controladores REST")
T31=$(post "/cursos/$C3/temas" '{"nombre":"Documentos y colecciones","descripcion":"Conceptos básicos de MongoDB.","dificultad":"BAJA"}' "Mongo · Documentos y colecciones")
T32=$(post "/cursos/$C3/temas" '{"nombre":"Agregaciones","descripcion":"Pipeline de agregación.","dificultad":"ALTA"}' "Mongo · Agregaciones")

# Prerrequisitos
titulo "Prerrequisitos"
put "/cursos/$C1/temas/$T12/prerrequisitos/$T11"
echo "  ${G}✔${N} Clases y objetos requiere Variables y tipos"; sleep "$RITMO"
put "/cursos/$C1/temas/$T13/prerrequisitos/$T12"
echo "  ${G}✔${N} Colecciones y streams requiere Clases y objetos"; sleep "$RITMO"
put "/cursos/$C2/temas/$T22/prerrequisitos/$T21"
echo "  ${G}✔${N} Controladores REST requiere Inyección de dependencias"; sleep "$RITMO"
put "/cursos/$C3/temas/$T32/prerrequisitos/$T31"
echo "  ${G}✔${N} Agregaciones requiere Documentos y colecciones"; sleep "$RITMO"

# ---------- Recursos ----------
titulo "Recursos"
post "/cursos/$C1/temas/$T11/recursos" '{"tipo":"VIDEO","titulo":"Introducción a Java","url":"https://example.com/java-intro"}' "Recurso: Introducción a Java" >/dev/null
post "/cursos/$C1/temas/$T11/recursos" '{"tipo":"PDF","titulo":"Guía de tipos de datos","url":"https://example.com/tipos.pdf"}' "Recurso: Guía de tipos de datos" >/dev/null
post "/cursos/$C1/temas/$T12/recursos" '{"tipo":"LINK","titulo":"Documentación oficial de clases","url":"https://docs.oracle.com/javase/tutorial/java/javaOO/"}' "Recurso: Documentación oficial de clases" >/dev/null
post "/cursos/$C2/temas/$T21/recursos" '{"tipo":"PRESENTACION","titulo":"Slides de IoC","url":"https://example.com/ioc.pptx"}' "Recurso: Slides de IoC" >/dev/null
post "/cursos/$C3/temas/$T31/recursos" '{"tipo":"VIDEO","titulo":"Primeros pasos con MongoDB","url":"https://example.com/mongo-intro"}' "Recurso: Primeros pasos con MongoDB" >/dev/null

# ---------- Preguntas ----------
titulo "Preguntas"
om() { # om <curso> <tema> <dificultad> <enunciado> <puntaje> <opcionesJSON> <correcta>
  post "/cursos/$1/temas/$2/preguntas" "{\"tipo\":\"OPCION_MULTIPLE\",\"enunciado\":\"$4\",\"dificultad\":\"$3\",\"puntaje\":$5,\"opciones\":$6,\"opcionCorrecta\":$7}" "[opción múltiple] $4" >/dev/null
}
ab() { # ab <curso> <tema> <dificultad> <enunciado> <puntaje> <respuestaEsperada> <palabrasJSON>
  post "/cursos/$1/temas/$2/preguntas" "{\"tipo\":\"ABIERTA\",\"enunciado\":\"$4\",\"dificultad\":\"$3\",\"puntaje\":$5,\"respuestaEsperada\":\"$6\",\"palabrasClave\":$7}" "[abierta] $4" >/dev/null
}

# Java: Variables y tipos
om $C1 $T11 BAJA "¿Qué tipo almacena números enteros de 32 bits?" 2 '["long","int","double","byte"]' 1
om $C1 $T11 BAJA "¿Cuál es el operador de igualdad en Java?" 2 '["=","==","===","equals"]' 1
om $C1 $T11 MEDIA "¿Qué imprime 7 / 2 con enteros?" 2 '["3.5","3","4","Error"]' 1
ab $C1 $T11 BAJA "Explicá qué es una variable." 4 "Un espacio de memoria con nombre que almacena un valor." '["memoria","valor"]'
# Java: Clases y objetos
om $C1 $T12 BAJA "¿Qué palabra clave crea una instancia?" 2 '["new","this","class","create"]' 0
om $C1 $T12 MEDIA "¿Qué principio oculta el estado interno de una clase?" 2 '["Herencia","Polimorfismo","Encapsulamiento","Abstracción"]' 2
om $C1 $T12 ALTA "¿Qué modificador impide heredar de una clase?" 2 '["static","final","abstract","private"]' 1
ab $C1 $T12 MEDIA "¿Qué es el polimorfismo?" 4 "Capacidad de un objeto de tomar varias formas." '["formas","objeto"]'
# Java: Colecciones
om $C1 $T13 MEDIA "¿Qué colección no admite duplicados?" 2 '["List","Set","ArrayList","Queue"]' 1
om $C1 $T13 ALTA "¿Qué operación de stream transforma cada elemento?" 3 '["filter","map","collect","reduce"]' 1
ab $C1 $T13 ALTA "¿Para qué sirve Collectors.groupingBy?" 5 "Agrupa los elementos de un stream según una clave." '["agrupa","clave"]'
# Spring: Inyección
om $C2 $T21 BAJA "¿Qué anotación marca un bean de servicio?" 3 '["@Service","@Entity","@Document","@Id"]' 0
om $C2 $T21 MEDIA "¿Qué tipo de inyección recomienda Spring?" 3 '["Por campo","Por constructor","Por setter","Manual"]' 1
ab $C2 $T21 BAJA "¿Qué es la inversión de control?" 4 "El contenedor crea y administra las dependencias." '["contenedor","dependencias"]'
# Spring: Controladores
om $C2 $T22 BAJA "¿Qué anotación expone un controlador REST?" 2 '["@RestController","@Service","@Repository","@Bean"]' 0
om $C2 $T22 MEDIA "¿Qué código HTTP corresponde a un recurso creado?" 2 '["200","201","204","404"]' 1
om $C2 $T22 ALTA "¿Qué anotación valida el cuerpo del request?" 2 '["@Valid","@Value","@Scope","@Lazy"]' 0
ab $C2 $T22 MEDIA "¿Para qué sirve un DTO?" 4 "Transporta datos entre capas sin exponer la entidad." '["datos","entidad"]'
# Mongo: Documentos
om $C3 $T31 BAJA "¿En qué formato se guardan los documentos?" 3 '["BSON","XML","CSV","YAML"]' 0
om $C3 $T31 BAJA "¿Cómo se llama el identificador por defecto?" 3 '["_id","id","uuid","pk"]' 0
ab $C3 $T31 MEDIA "¿Qué es una colección?" 4 "Un grupo de documentos, equivalente a una tabla." '["documentos","grupo"]'
# Mongo: Agregaciones
om $C3 $T32 MEDIA "¿Qué etapa filtra documentos en un pipeline?" 2 '["$match","$group","$sort","$limit"]' 0
om $C3 $T32 ALTA "¿Qué etapa agrupa y calcula acumulados?" 3 '["$project","$group","$unwind","$skip"]' 1
ab $C3 $T32 ALTA "¿Qué hace la etapa \$unwind?" 5 "Descompone un arreglo en un documento por elemento." '["arreglo","elemento"]'

# ---------- Inscripciones ----------
titulo "Inscripciones"
post /inscripciones "{\"alumnoId\":\"$A1\",\"cursoId\":\"$C1\"}" "Ana → Java desde cero" >/dev/null
post /inscripciones "{\"alumnoId\":\"$A1\",\"cursoId\":\"$C2\"}" "Ana → Spring Boot" >/dev/null
post /inscripciones "{\"alumnoId\":\"$A2\",\"cursoId\":\"$C1\"}" "Luis → Java desde cero" >/dev/null
post /inscripciones "{\"alumnoId\":\"$A3\",\"cursoId\":\"$C2\"}" "Sofía → Spring Boot" >/dev/null
post /inscripciones "{\"alumnoId\":\"$A3\",\"cursoId\":\"$C3\"}" "Sofía → MongoDB" >/dev/null
post /inscripciones "{\"alumnoId\":\"$A4\",\"cursoId\":\"$C3\"}" "Matías → MongoDB" >/dev/null

echo
echo "${B}Resumen${N}"
printf '  %-14s %s\n' "Docentes"  "$(curl -sf $BASE/usuarios | jq '[.[]|select(.tipo=="DOCENTE")]|length')"
printf '  %-14s %s\n' "Alumnos"   "$(curl -sf $BASE/usuarios | jq '[.[]|select(.tipo=="ALUMNO")]|length')"
printf '  %-14s %s\n' "Cursos"    "$(curl -sf $BASE/cursos | jq length)"
echo "${G}Datos cargados.${N}"
