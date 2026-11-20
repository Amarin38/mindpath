#!/usr/bin/env bash
set -uo pipefail

BASE="${BASE:-http://192.168.68.81:81/api/v1}"
CAL="${CAL:-http://192.168.68.81:8091/api/v1}"
ESPERA_KAFKA="${ESPERA_KAFKA:-30}"   # segundos máximos esperando la corrección asíncrona

B=$'\e[1m'; D=$'\e[2m'; R=$'\e[31m'; G=$'\e[32m'; C=$'\e[36m'; N=$'\e[0m'
PASS=0; FAIL=0; CODE=""; BODY=""; ESTADO=""; EID=""

req() { # req METODO ruta [json]   -> CODE y BODY
  local resp
  resp=$(curl -s -w '\n%{http_code}' -X "$1" "$BASE$2" -H 'Content-Type: application/json' ${3:+-d "$3"}) || resp=$'\n000'
  CODE="${resp##*$'\n'}"; BODY="${resp%$'\n'*}"
}
creal() { # igual que req pero contra el microservicio
  local resp
  resp=$(curl -s -w '\n%{http_code}' -X POST "$CAL$1" -H 'Content-Type: application/json' -d "$2") || resp=$'\n000'
  CODE="${resp##*$'\n'}"; BODY="${resp%$'\n'*}"
}
id() { echo "$BODY" | jq -r '.id'; }
campo() { echo "$BODY" | jq -r "$1"; }

ok()   { PASS=$((PASS+1)); printf '  %s✔%s %s\n' "$G" "$N" "$1"; }
fail() { FAIL=$((FAIL+1)); printf '  %s✘ %s%s\n      %s%s%s\n' "$R" "$1" "$N" "$D" "${2:-}" "$N"; }
eq()   { if [ "$2" = "$3" ]; then ok "$1"; else fail "$1" "esperado: $2 · obtenido: $3  ${BODY:0:160}"; fi; }
contiene() { if [[ "$3" == *"$2"* ]]; then ok "$1"; else fail "$1" "no contiene \"$2\": $3"; fi; }
seccion() { echo; echo "${B}${C}$1${N}"; }
fatal() { echo "${R}$1${N}"; exit 2; }

command -v jq >/dev/null || fatal "Falta jq (sudo apt install jq)."

esperar_finalizada() { # esperar_finalizada <evaluacionId> -> deja ESTADO
  local i
  for i in $(seq 1 $((ESPERA_KAFKA * 2))); do
    req GET "/evaluaciones/$1"; ESTADO=$(campo .estado)
    [ "$ESTADO" = "FINALIZADA" ] && return 0
    sleep 0.5
  done
  return 1
}

responder_todo() { # responder_todo <evaluacionId> <bien|mal>
  local p pid tipo resp
  while read -r p; do
    pid=$(echo "$p" | jq -r .id); tipo=$(echo "$p" | jq -r .tipo)
    if [ "$2" = "bien" ]; then
      if [ "$tipo" = "ABIERTA" ]; then resp=$(echo "$p" | jq -r '.palabrasClave | join(" ")'); else resp=$(echo "$p" | jq -r .opcionCorrecta); fi
    else
      if [ "$tipo" = "ABIERTA" ]; then resp="nada"; else resp="9"; fi
    fi
    req POST "/evaluaciones/$1/respuestas" "{\"preguntaId\":\"$pid\",\"contenido\":\"$resp\",\"tiempoSegundos\":5}"
    [ "$CODE" = "201" ] || fail "responder $pid" "HTTP $CODE ${BODY:0:120}"
  done < <(echo "$PREGUNTAS" | jq -c '.[]')
}

intento() { # intento <alumnoId> <bien|mal>  -> deja EID, ESTADO; devuelve 1 si no pudo iniciar
  req POST /evaluaciones "{\"alumnoId\":\"$1\",\"cursoId\":\"$CURSO\",\"temaId\":\"$T1\"}"
  [ "$CODE" = "201" ] || return 1
  EID=$(id); responder_todo "$EID" "$2"
  req POST "/evaluaciones/$EID/entrega"
  esperar_finalizada "$EID"
}

S=$RANDOM$RANDOM
echo "${B}Pruebas de sistema MindPath${N}  ${D}($BASE · sufijo $S)${N}"

# ------------------------------------------------------------------
seccion "1. Disponibilidad"
req GET /cursos;                eq "La API responde (GET /cursos)" 200 "$CODE"
creal /calificaciones '{}';     eq "El microservicio responde (400 ante un body vacío)" 400 "$CODE"
[ "$CODE" = "000" ] && fatal "El microservicio no responde en $CAL."

# ------------------------------------------------------------------
seccion "2. Usuarios: validación y unicidad"
req POST /alumnos "{\"email\":\"corta$S@t.com\",\"password\":\"123\",\"nombre\":\"A\",\"apellido\":\"B\",\"legajo\":\"1\"}"
eq "Contraseña de menos de 8 caracteres → 400" 400 "$CODE"
req POST /alumnos "{\"email\":\"no-es-email\",\"password\":\"password1\",\"nombre\":\"A\",\"apellido\":\"B\",\"legajo\":\"1\"}"
eq "Email inválido → 400" 400 "$CODE"

req POST /docentes "{\"email\":\"doc$S@t.com\",\"password\":\"password1\",\"nombre\":\"Doc\",\"apellido\":\"Test\",\"especialidad\":\"QA\"}"
eq "Alta de docente → 201" 201 "$CODE"; DOC=$(id)
req POST /alumnos "{\"email\":\"a1$S@t.com\",\"password\":\"password1\",\"nombre\":\"Aprueba\",\"apellido\":\"Test\",\"legajo\":\"A$S\"}"
eq "Alta de alumno → 201" 201 "$CODE"; A1=$(id)
req POST /alumnos "{\"email\":\"a1$S@t.com\",\"password\":\"password1\",\"nombre\":\"Otro\",\"apellido\":\"Test\",\"legajo\":\"B$S\"}"
eq "Email duplicado → 409" 409 "$CODE"
req POST /alumnos "{\"email\":\"a2$S@t.com\",\"password\":\"password1\",\"nombre\":\"Desaprueba\",\"apellido\":\"Test\",\"legajo\":\"C$S\"}"; A2=$(id)
req POST /alumnos "{\"email\":\"a3$S@t.com\",\"password\":\"password1\",\"nombre\":\"SinInscribir\",\"apellido\":\"Test\",\"legajo\":\"D$S\"}"; A3=$(id)

# ------------------------------------------------------------------
seccion "3. Curso, temas y preguntas"
req POST "/cursos?docenteId=$DOC" "{\"titulo\":\"Curso de prueba $S\",\"descripcion\":\"Datos de la prueba de sistema\"}"
eq "Alta de curso → 201" 201 "$CODE"; CURSO=$(id)
req POST "/cursos?docenteId=no-existe" '{"titulo":"X","descripcion":"Y"}'
eq "Curso con docente inexistente → 404" 404 "$CODE"
req POST "/cursos/$CURSO/temas" '{"nombre":"Tema 1","descripcion":"base","dificultad":"BAJA"}'
eq "Alta de tema → 201" 201 "$CODE"; T1=$(id)
req POST "/cursos/$CURSO/temas" '{"nombre":"Tema 2","descripcion":"avanzado","dificultad":"MEDIA"}'; T2=$(id)
req POST "/cursos/$CURSO/temas" '{"nombre":"Tema corto","descripcion":"pocos puntos","dificultad":"BAJA"}'; T3=$(id)
req POST "/cursos/$CURSO/temas" '{"nombre":"","dificultad":"BAJA"}'
eq "Tema sin nombre → 400" 400 "$CODE"

req PUT "/cursos/$CURSO/temas/$T2/prerrequisitos/$T1"
eq "Tema 2 requiere Tema 1 → 204" 204 "$CODE"
req PUT "/cursos/$CURSO/temas/$T1/prerrequisitos/$T2"
eq "Dependencia circular → 400" 400 "$CODE"

P="/cursos/$CURSO/temas/$T1/preguntas"
req POST "$P" '{"tipo":"OPCION_MULTIPLE","enunciado":"¿Opción 1?","dificultad":"BAJA","puntaje":3,"opciones":["a","b","c"],"opcionCorrecta":1}'
eq "Pregunta de opción múltiple → 201" 201 "$CODE"
req POST "$P" '{"tipo":"OPCION_MULTIPLE","enunciado":"¿Opción 0?","dificultad":"BAJA","puntaje":3,"opciones":["a","b"],"opcionCorrecta":0}'
req POST "$P" '{"tipo":"ABIERTA","enunciado":"Definí alfa y beta","dificultad":"MEDIA","puntaje":4,"respuestaEsperada":"alfa beta","palabrasClave":["alfa","beta"]}'
eq "Pregunta abierta → 201" 201 "$CODE"
req POST "$P" '{"tipo":"OPCION_MULTIPLE","enunciado":"inválida","dificultad":"BAJA","puntaje":3,"opciones":["a"],"opcionCorrecta":0}'
eq "Opción múltiple con una sola opción → 400" 400 "$CODE"
req POST "/cursos/$CURSO/temas/$T2/preguntas" '{"tipo":"OPCION_MULTIPLE","enunciado":"T2","dificultad":"BAJA","puntaje":10,"opciones":["a","b"],"opcionCorrecta":0}'
req POST "/cursos/$CURSO/temas/$T3/preguntas" '{"tipo":"OPCION_MULTIPLE","enunciado":"corta","dificultad":"BAJA","puntaje":5,"opciones":["a","b"],"opcionCorrecta":0}'
req GET "$P"; PREGUNTAS="$BODY"
eq "El tema 1 tiene 3 preguntas (puntaje total 10)" "3" "$(echo "$PREGUNTAS" | jq length)"

req POST /inscripciones "{\"alumnoId\":\"$A1\",\"cursoId\":\"$CURSO\"}"
eq "Inscripción de alumno 1 → 201" 201 "$CODE"
req POST /inscripciones "{\"alumnoId\":\"$A1\",\"cursoId\":\"$CURSO\"}"
eq "Inscripción activa duplicada → 409" 409 "$CODE"
req POST /inscripciones "{\"alumnoId\":\"$A2\",\"cursoId\":\"$CURSO\"}"

# ------------------------------------------------------------------
seccion "4. Reglas para iniciar una evaluación"
req POST /evaluaciones "{\"alumnoId\":\"$A3\",\"cursoId\":\"$CURSO\",\"temaId\":\"$T1\"}"
eq "Alumno sin inscripción → 409" 409 "$CODE"
req POST /evaluaciones "{\"alumnoId\":\"$A1\",\"cursoId\":\"$CURSO\",\"temaId\":\"$T2\"}"
eq "Tema bloqueado por prerrequisito → 409" 409 "$CODE"
req POST /evaluaciones "{\"alumnoId\":\"$A1\",\"cursoId\":\"$CURSO\",\"temaId\":\"$T3\"}"
eq "Tema con menos de 10 puntos → 409" 409 "$CODE"
contiene "El mensaje explica el mínimo de puntos" "al menos 10" "$(campo .detail)"

# ------------------------------------------------------------------
seccion "5. Flujo aprobado (todo bien) con Kafka"
req POST /evaluaciones "{\"alumnoId\":\"$A1\",\"cursoId\":\"$CURSO\",\"temaId\":\"$T1\"}"
eq "Iniciar evaluación → 201" 201 "$CODE"; EID=$(id)
eq "Estado inicial EN_PROCESO" EN_PROCESO "$(campo .estado)"
req POST /evaluaciones "{\"alumnoId\":\"$A1\",\"cursoId\":\"$CURSO\",\"temaId\":\"$T1\"}"
eq "Segunda evaluación mientras hay una en curso → 409" 409 "$CODE"

P0=$(echo "$PREGUNTAS" | jq -c '.[0]'); PID1=$(echo "$P0" | jq -r .id)
R0=$(echo "$P0" | jq -r 'if .tipo=="ABIERTA" then (.palabrasClave|join(" ")) else (.opcionCorrecta|tostring) end')
req POST "/evaluaciones/$EID/respuestas" "{\"preguntaId\":\"$PID1\",\"contenido\":\"$R0\",\"tiempoSegundos\":5}"
eq "Responder una pregunta → 201" 201 "$CODE"
req POST "/evaluaciones/$EID/respuestas" "{\"preguntaId\":\"$PID1\",\"contenido\":\"$R0\",\"tiempoSegundos\":5}"
eq "Responder dos veces la misma pregunta → 409" 409 "$CODE"
req POST "/evaluaciones/$EID/respuestas" '{"preguntaId":"no-existe","contenido":"1","tiempoSegundos":5}'
eq "Responder una pregunta inexistente → 404" 404 "$CODE"

# completar las que faltan
RESTO=$(echo "$PREGUNTAS" | jq -c '.[1:]')
SAVE="$PREGUNTAS"; PREGUNTAS="$RESTO"; responder_todo "$EID" bien; PREGUNTAS="$SAVE"

req POST "/evaluaciones/$EID/entrega"
eq "Entrega responde de inmediato → 200" 200 "$CODE"
eq "Tras entregar queda ENTREGADO (corrige el microservicio)" ENTREGADO "$(campo .estado)"
if esperar_finalizada "$EID"; then ok "Kafka: la evaluación pasa a FINALIZADA"; else fail "Kafka: la evaluación no se finalizó en ${ESPERA_KAFKA}s" "estado: $ESTADO. Revisá: docker compose logs calificacion api"; fi
eq "Puntaje total 10" "10" "$(campo '(.puntajeTotal // 0)*1')"
eq "Aprobada" true "$(campo .aprobada)"
req GET "/alumnos/$A1/progresos/$T1"
eq "Progreso: nivel de dominio 1.0" "1" "$(campo '(.nivelDominio // 0)*1')"
eq "Progreso: 1 intento" "1" "$(campo .intentos)"
eq "Progreso: categoría de nivel ALTO" ALTO "$(campo .categoriaNivel)"
req POST "/evaluaciones/$EID/entrega"
eq "Entregar una evaluación ya finalizada → 409" 409 "$CODE"

req POST /evaluaciones "{\"alumnoId\":\"$A1\",\"cursoId\":\"$CURSO\",\"temaId\":\"$T1\"}"
eq "Repetir un tema aprobado → 409" 409 "$CODE"
contiene "El mensaje explica que ya aprobó" "aprobó" "$(campo .detail)"
req POST /evaluaciones "{\"alumnoId\":\"$A1\",\"cursoId\":\"$CURSO\",\"temaId\":\"$T2\"}"
eq "Al aprobar el tema 1 se desbloquea el tema 2 → 201" 201 "$CODE"

# ------------------------------------------------------------------
seccion "6. Flujo desaprobado y máximo de 3 intentos"
for n in 1 2 3; do
  if intento "$A2" mal; then
    eq "Intento $n: FINALIZADA" FINALIZADA "$ESTADO"
    req GET "/evaluaciones/$EID"
    eq "Intento $n: desaprobada (0 puntos)" false "$(campo .aprobada)"
  else
    fail "Intento $n: no se pudo completar" "HTTP $CODE ${BODY:0:140}"
  fi
done
req GET "/alumnos/$A2/progresos/$T1"
eq "Progreso: 3 intentos registrados" "3" "$(campo .intentos)"
eq "Progreso: categoría de nivel BAJO" BAJO "$(campo .categoriaNivel)"
req POST /evaluaciones "{\"alumnoId\":\"$A2\",\"cursoId\":\"$CURSO\",\"temaId\":\"$T1\"}"
eq "Cuarto intento → 409" 409 "$CODE"
contiene "El mensaje explica el máximo de intentos" "máximo" "$(campo .detail)"

# ------------------------------------------------------------------
seccion "7. Microservicio de calificación (HTTP directo)"
creal /calificaciones '{"evaluacionId":"x","preguntas":[{"id":"p1","tipo":"OPCION_MULTIPLE","puntaje":5,"opcionCorrecta":2}],"respuestas":[{"preguntaId":"p1","contenido":"2"}]}'
eq "Request válido → 200" 200 "$CODE"
eq "Corrige bien la respuesta" true "$(campo '.respuestas[0].correcta')"
eq "Nivel ALTO con 100% en primer intento" ALTO "$(campo .categoriaNivel)"
creal /calificaciones '{"evaluacionId":"x","nivelDominioActual":0.9,"preguntas":[{"id":"p1","tipo":"OPCION_MULTIPLE","puntaje":5,"opcionCorrecta":2}],"respuestas":[{"preguntaId":"p1","contenido":"0"}]}'
eq "Con nivel previo 0.9 y un error el nivel queda MEDIO (0.54)" MEDIO "$(campo .categoriaNivel)"
creal /calificaciones '{"evaluacionId":"x","preguntas":[],"respuestas":[]}'
eq "Sin preguntas → 400" 400 "$CODE"

# ------------------------------------------------------------------
echo; echo "${B}Resultado${N}: ${G}$PASS ok${N}, $( [ "$FAIL" -eq 0 ] && echo "0 fallos" || echo "${R}$FAIL fallos${N}")"
[ "$FAIL" -eq 0 ]
