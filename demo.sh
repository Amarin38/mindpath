#!/usr/bin/env bash
set -euo pipefail

BASE="${BASE:-http://192.168.68.81:81/api/v1}"
RITMO="${RITMO:-1.2}"
MAX_INTENTOS=3
export BASE
DIR="$(cd "$(dirname "$0")" && pwd)"

ESC="${1:-}"
case "$ESC" in
  aprobado)  DEF_ALUMNO="mati.sosa@mail.com"; DEF_CURSO="MongoDB";         DEF_TEMA="Documentos y colecciones"
             ETIQUETA="APROBADO DIRECTO"; PLAN="Matías responde todo bien y aprueba al primer intento." ;;
  reintento) DEF_ALUMNO="luis.perez@mail.com"; DEF_CURSO="Java desde cero";   DEF_TEMA="Variables y tipos"
             ETIQUETA="DESAPRUEBA Y LUEGO APRUEBA"; PLAN="Luis desaprueba el primer intento, reintenta y aprueba. Se comparan los dos intentos." ;;
  agotado)   DEF_ALUMNO="sofia.diaz@mail.com"; DEF_CURSO="Spring Boot";       DEF_TEMA="Inyección de dependencias"
             ETIQUETA="3 INTENTOS DESAPROBADOS"; PLAN="Sofía desaprueba 3 veces y el sistema no le deja hacer un 4º intento." ;;
  *) echo "Uso: $0 aprobado|reintento|agotado" >&2; exit 1 ;;
esac
ALUMNO_EMAIL="${ALUMNO_EMAIL:-$DEF_ALUMNO}"
CURSO_TITULO="${CURSO_TITULO:-$DEF_CURSO}"
TEMA_NOMBRE="${TEMA_NOMBRE:-$DEF_TEMA}"

B=$'\e[1m'; D=$'\e[2m'; R=$'\e[31m'; G=$'\e[32m'; Y=$'\e[33m'; C=$'\e[36m'; M=$'\e[35m'; N=$'\e[0m'

CODE=""; BODY=""; PASO_N=0
get()  { curl -sf "$BASE$1" || { echo "${R}FALLÓ GET $1${N}" >&2; exit 1; }; }
call() { # call METODO ruta [json]  -> CODE y BODY, sin abortar
  local resp
  resp=$(curl -s -w '\n%{http_code}' -X "$1" "$BASE$2" -H 'Content-Type: application/json' ${3:+-d "$3"}) || resp=$'\n000'
  CODE="${resp##*$'\n'}"; BODY="${resp%$'\n'*}"
}
post() { # post ruta [json] -> imprime el cuerpo; aborta si no es 2xx
  call POST "$@"
  if [[ ! "$CODE" =~ ^2 ]]; then
    echo "${R}FALLÓ POST $1 → HTTP $CODE${N}" >&2; echo "${D}$BODY${N}" >&2; exit 1
  fi
  echo "$BODY"
}
lento()  { sleep "$RITMO"; }
num()    { awk -v x="$1" 'BEGIN{ if (x==int(x)) printf "%d", x; else printf "%.1f", x }'; }
fila()   { printf '  %-18s %s\n' "$1" "$2"; }   # fila <etiqueta> <valor (puede llevar color)>
linea()  { printf '%s\n' "${D}────────────────────────────────────────────────────────────${N}"; }
enter()  { if [ -z "${AUTO:-}" ]; then read -rp "${D}[ENTER para continuar]${N} " _; fi; }
paso() { # paso <título> <qué pasa>
  PASO_N=$((PASO_N+1))
  echo; linea
  echo "${B}${C}PASO $PASO_N · $1${N}"
  echo "${D}$2${N}"
  enter
}

# ---------------- Fase 0: datos de ejemplo ----------------
clear
echo "${B}${C}FASE 0 · Datos de ejemplo${N}"
echo "${D}Se cargan por la API (pasan por validación; las contraseñas quedan con BCrypt).${N}"
RITMO=0.4 "$DIR/samples.sh"
echo
enter

# ---------------- Presentación ----------------
clear
cat <<BANNER
${B}MindPath · Evaluación adaptativa con Kafka${N}

   ┌─────────┐  1. evaluacion.entregada  ┌───────────────┐
   │   API   │ ────────────────────────▶ │  Calificación │
   │ :8090   │                           │  :8091        │
   │         │ ◀──────────────────────── │  (sin BD)     │
   └─────────┘  2. evaluacion.corregida  └───────────────┘
        │                  ▲
        ▼                  │
     MongoDB             Kafka (KRaft)

${D}La API no corrige: publica la entrega y sigue. El microservicio corrige.
La API aplica el resultado cuando llega.${N}
BANNER

A=$(get /usuarios | jq -r --arg e "$ALUMNO_EMAIL" '.[] | select(.email==$e) | .id')
C_ID=$(get /cursos | jq -r --arg t "$CURSO_TITULO" '.[] | select(.titulo==$t) | .id')
[ -n "$A" ]    || { echo "${R}No existe el alumno $ALUMNO_EMAIL. ¿Corriste samples.sh?${N}" >&2; exit 1; }
[ -n "$C_ID" ] || { echo "${R}No existe el curso \"$CURSO_TITULO\". ¿Corriste samples.sh?${N}" >&2; exit 1; }
T=$(get "/cursos/$C_ID/temas" | jq -r --arg n "$TEMA_NOMBRE" '.[] | select(.nombre==$n) | .id')
[ -n "$T" ]    || { echo "${R}No existe el tema \"$TEMA_NOMBRE\" en ese curso.${N}" >&2; exit 1; }

PREV=$(get "/evaluaciones?alumnoId=$A&temaId=$T" | jq length)
if [ "$PREV" -gt 0 ]; then
  echo
  echo "${R}$ALUMNO_EMAIL ya tiene $PREV intento(s) en \"$TEMA_NOMBRE\".${N}"
  echo "${Y}Este escenario necesita al alumno sin intentos previos: borrá la base y volvé a correr (la Fase 0 la recarga sola).${N}"
  exit 1
fi

PREG=$(get "/cursos/$C_ID/temas/$T/preguntas")      # vista docente: incluye la respuesta correcta
N_PREG=$(echo "$PREG" | jq length)
PMAX=$(echo "$PREG" | jq '[.[].puntaje]|add')

echo
APRUEBA=$(awk -v m="$PMAX" 'BEGIN{printf "%.4g", m*0.7}')
PMAX_T=$(num "$PMAX")
echo "  ${B}${M}ESCENARIO · $ETIQUETA${N}"
echo "  ${D}$PLAN${N}"
echo
fila "Alumno"  "$ALUMNO_EMAIL"
fila "Curso"   "$CURSO_TITULO"
fila "Tema"    "$TEMA_NOMBRE  ($N_PREG preguntas, $PMAX_T puntos)"
fila "Aprueba" "con $APRUEBA de $PMAX_T puntos (70%)"
fila "Intentos" "máximo $MAX_INTENTOS"

# ---------------- Un intento completo ----------------
RES_PT=(); RES_OK=(); RES_NIVEL=(); RES_DIF=(); RES_MS=()

respuesta_para() { # respuesta_para <índice 1..N> <pregunta json> <bien|mal|ultima>
  local i="$1" p="$2" modo="$3" tipo ok mal
  tipo=$(echo "$p" | jq -r .tipo)
  if [ "$tipo" = "ABIERTA" ]; then
    ok=$(echo "$p" | jq -r '.palabrasClave | join(" ")'); mal="no sé"
  else
    ok=$(echo "$p" | jq -r .opcionCorrecta)
    mal=$(( (ok + 1) % $(echo "$p" | jq '.opciones|length') ))
  fi
  case "$modo" in
    bien)   RESP="$ok"; CORRECTA=1 ;;
    mal)    RESP="$mal"; CORRECTA=0 ;;
    ultima) if [ "$i" -eq "$N_PREG" ]; then RESP="$mal"; CORRECTA=0; else RESP="$ok"; CORRECTA=1; fi ;;
  esac
}

intento() { # intento <n> <bien|mal|ultima> <1 detallado | 0 compacto>
  local n="$1" modo="$2" det="$3" E i p pid enun etq T0 ESTADO k MS EVAL txt
  case "$modo" in
    bien)   txt="responde todo bien" ;;
    mal)    txt="responde todo mal" ;;
    ultima) txt="responde mal la última pregunta" ;;
  esac

  if [ "$det" = 1 ]; then
    paso "Intento $n · El alumno inicia la evaluación" \
"La API valida inscripción, prerrequisitos, intentos disponibles y que no lo haya aprobado."
  else
    paso "Intento $n · El alumno $txt" "Mismo flujo completo: iniciar, responder, entregar y corregir."
  fi
  E=$(post /evaluaciones "{\"alumnoId\":\"$A\",\"cursoId\":\"$C_ID\",\"temaId\":\"$T\"}" | jq -r .id)
  echo "  ${G}✔${N} Evaluación creada y ${B}EN_PROCESO${N}  ${D}(intento $n de $MAX_INTENTOS)${N}"

  if [ "$det" = 1 ]; then
    paso "El alumno ve las preguntas" "El alumno ve enunciados y opciones, nunca la respuesta correcta."
    get "/evaluaciones/$E/preguntas" | jq -r 'def n: if . == floor then floor else . end;
      to_entries[] | "  \(.key+1). \(.value.enunciado)  (\(if .value.tipo=="ABIERTA" then "abierta" else "opción múltiple" end), \(.value.puntaje|n) pts)"' \
      | while IFS= read -r l; do echo "$l"; lento; done
    paso "El alumno responde ($txt)" "Las respuestas se guardan; todavía nada se corrige."
  fi

  i=0
  while read -r p; do
    i=$((i+1))
    pid=$(echo "$p" | jq -r .id); enun=$(echo "$p" | jq -r .enunciado)
    respuesta_para "$i" "$p" "$modo"
    post "/evaluaciones/$E/respuestas" "{\"preguntaId\":\"$pid\",\"contenido\":\"$RESP\",\"tiempoSegundos\":15}" >/dev/null
    if [ "$det" = 1 ]; then
      if [ "$CORRECTA" = 1 ]; then etq="${G}responde bien${N}"; else etq="${Y}responde mal${N}"; fi
      printf '  %d. %-46s %s\n' "$i" "${enun:0:46}" "$etq"; lento
    fi
  done < <(echo "$PREG" | jq -c '.[]')
  if [ "$det" != 1 ]; then echo "  ${G}✔${N} $N_PREG respuestas enviadas ($txt)"; fi

  if [ "$det" = 1 ]; then
    paso "Entrega: la API publica el evento en Kafka" \
"La API responde al instante; la corrección ocurre en otro servicio."
  fi
  T0=$(date +%s%N)
  ESTADO=$(post "/evaluaciones/$E/entrega" | jq -r .estado)
  echo "  ${G}✔${N} Respuesta inmediata de la API: estado ${B}$ESTADO${N}"
  echo "  ${Y}▶${N} Evento ${B}evaluacion.entregada${N} publicado en Kafka"

  if [ "$det" = 1 ]; then
    paso "El microservicio corrige y la API aplica el resultado" \
"Calificación consume el evento, corrige y publica evaluacion.corregida; la API lo aplica."
  fi
  SP=('⠋' '⠙' '⠹' '⠸' '⠼' '⠴' '⠦' '⠧' '⠇' '⠏'); k=0; ESTADO="ENTREGADO"
  for _ in $(seq 1 150); do
    ESTADO=$(get "/evaluaciones/$E" | jq -r .estado)
    if [ "$ESTADO" = "FINALIZADA" ]; then break; fi
    printf '\r  %s esperando corrección asíncrona...  estado: %s ' "${SP[$((k%10))]}" "$ESTADO"; k=$((k+1)); sleep 0.2
  done
  MS=$(( ($(date +%s%N) - T0) / 1000000 ))
  printf '\r%-70s\r' ""
  if [ "$ESTADO" != "FINALIZADA" ]; then
    echo "  ${R}✘ Sigue en $ESTADO tras 30 s. Revisá: docker compose logs calificacion api${N}"; exit 1
  fi
  echo "  ${G}✔${N} ${B}FINALIZADA${N}  ${D}($(awk -v m=$MS 'BEGIN{printf "%.1f", m/1000}') s desde la entrega)${N}"

  EVAL=$(get "/evaluaciones/$E")
  if [ "$det" = 1 ]; then
    echo
    echo "$EVAL" | jq -r --argjson p "$PREG" '
      def n: if . == floor then floor else . end;
      .respuestas[] as $r | ($p[] | select(.id==$r.preguntaId)) as $q |
      "  \(if $r.correcta then "\u001b[32m✔\u001b[0m" else "\u001b[31m✘\u001b[0m" end)  \(($q.enunciado[0:46] + "                                              ")[0:46])  \($r.puntajeObtenido|n)/\($q.puntaje|n) pts"' \
      | while IFS= read -r l; do echo "$l"; lento; done
    paso "Resultado y progreso del alumno" \
"La API actualiza el nivel de dominio del alumno y lo clasifica en BAJO, MEDIO o ALTO."
  fi

  local prog pt ok nivel dif
  prog=$(get "/alumnos/$A/progresos/$T")
  pt=$(echo "$EVAL" | jq -r '.puntajeTotal'); ok=$(echo "$EVAL" | jq -r '.aprobada')
  nivel=$(echo "$prog" | jq -r '(.nivelDominio // 0) | . * 100 | round / 100')
  dif=$(echo "$prog" | jq -r '.categoriaNivel // "-"')
  RES_PT[$n]="$pt"; RES_OK[$n]="$ok"; RES_NIVEL[$n]="$nivel"; RES_DIF[$n]="$dif"; RES_MS[$n]="$MS"

  local v pct tiempo
  if [ "$ok" = "true" ]; then v="${G}${B}APROBADO${N}"; else v="${R}${B}DESAPROBADO${N}"; fi
  pct=$(awk -v a="$pt" -v m="$PMAX" 'BEGIN{printf "%d", a*100/m}')
  tiempo=$(awk -v m=$MS 'BEGIN{printf "%.1f", m/1000}')
  echo
  echo "  ${B}Resultado del intento $n de $MAX_INTENTOS${N}"
  fila "Puntaje"          "$(num "$pt") / $PMAX_T  (${pct}%)"
  fila "Resultado"        "$v"
  fila "Nivel de dominio" "$(awk -v x="$nivel" 'BEGIN{printf "%.2f", x}')  →  ${B}${M}$dif${N}"
  fila "Tiempo Kafka"     "${tiempo} s"
  if [ "$ok" = "true" ]; then
    echo "  ${D}Aprobó: no puede repetir este tema.${N}"
  elif [ "$n" -lt "$MAX_INTENTOS" ]; then
    echo "  ${D}Desaprobó: le quedan $((MAX_INTENTOS-n)) intento(s).${N}"
  else
    echo "  ${D}Desaprobó y usó los $MAX_INTENTOS intentos.${N}"
  fi
}

# ¿Qué pasa si intenta de nuevo? Muestra la respuesta real de la API.
otro_intento() { # otro_intento <n>
  paso "Intento $1 · ¿Puede volver a intentarlo?" "Se pide a la API iniciar otra evaluación del mismo tema: debe rechazarla."
  call POST /evaluaciones "{\"alumnoId\":\"$A\",\"cursoId\":\"$C_ID\",\"temaId\":\"$T\"}"
  if [ "$CODE" = "409" ]; then
    echo "  ${R}✘ Rechazado por la API${N} ${D}(HTTP 409)${N}"
    echo "  Motivo: ${B}$(echo "$BODY" | jq -r .detail)${N}"
  elif [[ "$CODE" =~ ^2 ]]; then
    echo "  ${Y}⚠ La API lo permitió (HTTP $CODE). Se esperaba un rechazo: revisá las reglas de intentos.${N}"
  else
    echo "  ${R}Respuesta inesperada: HTTP $CODE${N} $BODY"
  fi
}

comparar() { # tabla intento 1 vs 2
  local c1 c2
  if [ "${RES_OK[1]}" = "true" ]; then c1="APROBADO"; else c1="DESAPROBADO"; fi
  if [ "${RES_OK[2]}" = "true" ]; then c2="APROBADO"; else c2="DESAPROBADO"; fi
  paso "Comparación de los dos intentos" "Mismo alumno y tema: el nivel de dominio se actualiza con el nuevo desempeño."
  printf '  %-18s %s%-14s%s %s%-14s%s\n' "" "$B" "Intento 1" "$N" "$B" "Intento 2" "$N"
  printf '  %-18s %-14s %-14s\n' "Puntaje" "$(num "${RES_PT[1]}") / $PMAX_T" "$(num "${RES_PT[2]}") / $PMAX_T"
  printf '  %-18s %s%-14s%s %s%-14s%s\n' "Resultado" "$R" "$c1" "$N" "$G" "$c2" "$N"
  printf '  %-18s %-14s %-14s\n' "Nivel de dominio" "$(awk -v x="${RES_NIVEL[1]}" 'BEGIN{printf "%.2f", x}')" "$(awk -v x="${RES_NIVEL[2]}" 'BEGIN{printf "%.2f", x}')"
  printf '  %-18s %s%-14s%s %s%-14s%s\n' "Categoría" "$M" "${RES_DIF[1]}" "$N" "$M" "${RES_DIF[2]}" "$N"
  echo
  echo "  ${D}Nivel de dominio = 40% intento nuevo + 60% historial.${N}"
}

# ---------------- Escenarios ----------------
case "$ESC" in
  aprobado)
    intento 1 bien 1
    otro_intento 2
    ;;
  reintento)
    intento 1 ultima 1
    intento 2 bien 0
    comparar
    otro_intento 3
    ;;
  agotado)
    intento 1 mal 1
    intento 2 mal 0
    intento 3 mal 0
    otro_intento 4
    ;;
esac

echo; echo "${G}${B}Demo completa: $ETIQUETA${N}"
