export MP_DIR="${MP_DIR:-$HOME/repos/mindpath}"

# Logs de api + calificacion: requests HTTP ([REQ]), eventos Kafka ([EVT]), WARN y ERROR, con color y hora.
#   mp-logs
mp-logs() {
  local line pre rest msg col name style
  printf '\e[1mmp-logs\e[0m · requests y eventos de api y calificacion (esperando...)\n'
  docker compose -f "$MP_DIR/docker-compose.yml" logs -f --tail 0 --no-color api calificacion 2>&1 |
  while IFS= read -r line; do
    pre="${line%%|*}"; rest="${line#*| }"
    case "$pre" in
      *calificacion*) col=$'\e[35m'; name="calificacion" ;;
      *)              col=$'\e[36m'; name="api" ;;
    esac
    case "$rest" in
      *"[EVT]"*)  msg="${rest#* : }"; msg="${msg/\[EVT\] /}"; style=$'\e[1m' ;;
      *"[REQ]"*)  msg="${rest#* : }"; msg="${msg/\[REQ\] /}"
                  case "$msg" in *"-> 2"*) style=$'\e[32m' ;; *"-> 4"*) style=$'\e[33m' ;; *) style=$'\e[31m' ;; esac ;;
      *" ERROR "*) msg="${rest#* : }"; style=$'\e[31m' ;;
      *" WARN "*)  case "$rest" in *poringdoc*|*SpringDoc*) continue ;; esac
                   msg="${rest#* : }"; style=$'\e[33m' ;;
      *) continue ;;
    esac
    printf '\e[2m%(%H:%M:%S)T\e[0m %b%-12s\e[0m │ %b%s\e[0m\n' -1 "$col" "$name" "$style" "$msg"
  done
}

# Mensajes que viajan por Kafka, resumidos y con color.
#   mp-kafka                  (solo mensajes nuevos)
#   mp-kafka --from-beginning (todo el historial)
mp-kafka() {
  printf '\e[1mmp-kafka\e[0m · topics evaluacion.* (esperando mensajes...)\n'
  docker exec mindpath-kafka /opt/kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server kafka:9092 --include 'evaluacion\..*' \
    --property print.key=true --property key.separator=$'\t' "$@" 2>/dev/null |
  while IFS=$'\t' read -r key val; do
    printf '%s' "$val" | jq -r --arg ts "$(date +%H:%M:%S)" '
      if has("preguntas") then
        "\u001b[33m\($ts)  evaluacion.entregada   API ──▶ calificación\u001b[0m\n" +
        "   eval \(.evaluacionId) · \(.preguntas|length) preguntas · \(.respuestas|length) respuestas · nivel previo: \(.nivelDominioActual // "primer intento")"
      else
        "\u001b[35m\($ts)  evaluacion.corregida   calificación ──▶ API\u001b[0m\n" +
        "   eval \(.evaluacionId) · \([.respuestas[]|select(.correcta)]|length)/\(.respuestas|length) correctas · puntaje máx \(.puntajeMaximo) · nivel: \(.nivelDominio*100|round/100) (\(.categoriaNivel))"
      end' 2>/dev/null
  done
}

# Tests. Unitarios (API y microservicio, con Maven en Docker) y de sistema (contra el stack levantado).
#   mp-test             unitarios + sistema
#   mp-test unit        solo unitarios
#   mp-test sistema     solo sistema (necesita docker compose up y jq)
mp-test-unit() {
  local mod dir out l n f cls rc=0 args
  for mod in api calificacion; do
    if [ "$mod" = api ]; then dir="$MP_DIR"; else dir="$MP_DIR/calificacion"; fi
    args=(-B -ntp test)
    [ "$mod" = api ] && args+=("-Dtest=!MindpathApiApplicationTests" "-Dsurefire.failIfNoSpecifiedTests=false")
    printf '\n\e[1m\e[36mTests unitarios: %s\e[0m \e[2m(la primera vez descarga dependencias)\e[0m\n' "$mod"
    out=$(docker run --rm -v "$HOME/.m2:/root/.m2" -v "$dir":/app -w /app \
          maven:3-eclipse-temurin-25 mvn "${args[@]}" 2>&1) || rc=1
    while IFS= read -r l; do
      if [[ $l =~ Tests\ run:\ ([0-9]+),\ Failures:\ ([0-9]+),\ Errors:\ ([0-9]+).*\ in\ ([A-Za-z0-9_.]+) ]]; then
        n=${BASH_REMATCH[1]}; f=$((BASH_REMATCH[2] + BASH_REMATCH[3])); cls=${BASH_REMATCH[4]##*.}
        if [ "$f" -eq 0 ]; then printf '  \e[32m✔\e[0m %-28s %s tests\n' "$cls" "$n"
        else printf '  \e[31m✘\e[0m %-28s %s de %s fallaron\n' "$cls" "$f" "$n"; fi
      fi
    done <<<"$out"
    if [ $rc -ne 0 ]; then
      printf '\e[31m'; grep -E '^\[ERROR\]' <<<"$out" | head -40; printf '\e[0m'
    fi
    grep -E '^\[INFO\] Tests run:' <<<"$out" | tail -1
  done
  return $rc
}

mp-test() {
  local modo="${1:-todo}" rc=0
  case "$modo" in unit|sistema|todo) ;; *) echo "Uso: mp-test [unit|sistema|todo]"; return 1 ;; esac
  [ "$modo" != sistema ] && { mp-test-unit || rc=1; }
  [ "$modo" != unit ] && { "$MP_DIR/tests/sistema.sh" || rc=1; }
  return $rc
}

# Pantalla de demo con tmux: izquierda demo.sh, derecha arriba Kafka, derecha abajo logs.
#   mp-demo aprobado    Matías aprueba directo (MongoDB)
#   mp-demo reintento   Luis desaprueba y luego aprueba (Java desde cero)
#   mp-demo agotado     Sofía desaprueba 3 veces y no puede hacer un 4º intento (Spring Boot)
mp-demo() {
  local modo="${1:-}"
  case "$modo" in aprobado|reintento|agotado) ;; *) echo "Uso: mp-demo aprobado|reintento|agotado"; return 1 ;; esac
  command -v tmux >/dev/null || { echo "Instalá tmux: sudo apt install tmux"; return 1; }
  local f=/tmp/mindpath-funcs.sh
  declare -f mp-kafka mp-logs > "$f"        # las panes cargan las funciones desde acá, sin pasar por .bashrc
  local pre="export MP_DIR='$MP_DIR'; source $f;"
  tmux kill-session -t mindpath 2>/dev/null
  # Layout exacto: se aplica al attachar (y al redimensionar), con el tamaño real de la ventana.
  #   demo: MP_DEMO_COLS columnas (default 95), alto completo
  #   mp-kafka: arriba a la derecha, MP_KAFKA_ROWS filas (default 14); mp-logs: el resto de alto
  cat > /tmp/mindpath-layout.sh <<'LAYOUT'
#!/bin/bash
tmux resize-pane -t mindpath:0.0 -x "${1:-95}"
tmux resize-pane -t mindpath:0.1 -y "${2:-14}"
LAYOUT
  chmod +x /tmp/mindpath-layout.sh
  tmux new-session -d -s mindpath -x "$(tput cols)" -y "$(tput lines)" -c "$MP_DIR" "bash -i"
  tmux set -t mindpath mouse on
  tmux split-window -h -t mindpath:0.0 -c "$MP_DIR" "bash --norc -c '$pre mp-kafka'"
  tmux split-window -v -t mindpath:0.1 -c "$MP_DIR" "bash --norc -c '$pre mp-logs'"
  tmux set-hook -t mindpath client-attached "run-shell '/tmp/mindpath-layout.sh ${MP_DEMO_COLS:-95} ${MP_KAFKA_ROWS:-14}'"
  tmux set-hook -t mindpath client-resized  "run-shell '/tmp/mindpath-layout.sh ${MP_DEMO_COLS:-95} ${MP_KAFKA_ROWS:-14}'"
  tmux select-pane -t mindpath:0.0
  tmux send-keys -t mindpath:0.0 "clear; ./demo.sh $modo" Enter   # arranca solo: carga los datos y espera tu Enter
  tmux attach -t mindpath
}
