package com.mindpath.calificacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mindpath.calificacion.dto.CalificacionRequest;
import com.mindpath.calificacion.dto.CalificacionResponse;
import com.mindpath.calificacion.dto.PreguntaDto;
import com.mindpath.calificacion.dto.RespuestaDto;
import com.mindpath.calificacion.enums.TipoPreguntaEnum;

import com.mindpath.calificacion.enums.CategoriaNivelEnum;

class CalificadorTest {
    private final Calificador calificador = new Calificador();

    private PreguntaDto multiple(String id, double puntaje, int correcta) {
        return new PreguntaDto(id, TipoPreguntaEnum.OPCION_MULTIPLE, puntaje, correcta, null);
    }

    private PreguntaDto abierta(String id, double puntaje, String... claves) {
        return new PreguntaDto(id, TipoPreguntaEnum.ABIERTA, puntaje, null, List.of(claves));
    }

    private CalificacionResponse calificar(Double nivelActual, List<PreguntaDto> preguntas,
            List<RespuestaDto> respuestas) {
        return calificador.calificar(new CalificacionRequest("e1", nivelActual, preguntas, respuestas));
    }

    @Test
    @DisplayName("Corrige opción múltiple y suma los puntajes")
    void opcionMultiple() {
        CalificacionResponse r = calificar(null,
                List.of(multiple("p1", 3, 1), multiple("p2", 3, 0)),
                List.of(new RespuestaDto("p1", "1"), new RespuestaDto("p2", "2")));

        assertThat(r.puntajeMaximo()).isEqualTo(6);
        assertThat(r.respuestas()).extracting("correcta").containsExactly(true, false);
        assertThat(r.respuestas()).extracting("puntajeObtenido").containsExactly(3.0, 0.0);
    }

    @Test
    @DisplayName("Corrige abiertas por palabras clave, ignorando mayúsculas y tildes")
    void abiertas() {
        CalificacionResponse r = calificar(null,
                List.of(abierta("p1", 4, "memoria", "valor"), abierta("p2", 4, "herencia")),
                List.of(new RespuestaDto("p1", "Espacio de MEMORIA con un Valor"),
                        new RespuestaDto("p2", "no sé")));

        assertThat(r.respuestas()).extracting("correcta").containsExactly(true, false);
    }

    @Test
    @DisplayName("Una respuesta vacía o no numérica es incorrecta")
    void respuestasInvalidas() {
        CalificacionResponse r = calificar(null,
                List.of(multiple("p1", 2, 0), multiple("p2", 2, 0)),
                List.of(new RespuestaDto("p1", null), new RespuestaDto("p2", "abc")));

        assertThat(r.respuestas()).extracting("correcta").containsExactly(false, false);
    }

    @Test
    @DisplayName("El puntaje máximo incluye preguntas sin responder")
    void puntajeMaximoCuentaTodas() {
        CalificacionResponse r = calificar(null,
                List.of(multiple("p1", 5, 0), multiple("p2", 5, 0)),
                List.of(new RespuestaDto("p1", "0")));

        assertThat(r.puntajeMaximo()).isEqualTo(10);
    }

    @Test
    @DisplayName("Responder una pregunta que no está en el request es un error")
    void preguntaDesconocida() {
        assertThatThrownBy(() -> calificar(null, List.of(multiple("p1", 5, 0)),
                List.of(new RespuestaDto("otra", "0"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Primer intento: el nivel es el porcentaje (<40% BAJO, <70% MEDIO, resto ALTO)")
    void nivelPrimerIntento() {
        List<PreguntaDto> preguntas = List.of(multiple("p1", 10, 0));

        assertThat(calificar(null, preguntas, List.of(new RespuestaDto("p1", "0"))).categoriaNivel())
                .isEqualTo(CategoriaNivelEnum.ALTO);
        assertThat(calificar(null, preguntas, List.of(new RespuestaDto("p1", "9"))).categoriaNivel())
                .isEqualTo(CategoriaNivelEnum.BAJO);

        List<PreguntaDto> dos = List.of(multiple("p1", 5, 0), multiple("p2", 5, 0));
        assertThat(calificar(null, dos, List.of(new RespuestaDto("p1", "0"), new RespuestaDto("p2", "9")))
                .categoriaNivel()).isEqualTo(CategoriaNivelEnum.MEDIO);
    }

    @Test
    @DisplayName("Con historial pondera: 0.4*porcentaje + 0.6*nivel previo")
    void nivelConHistorial() {
        List<PreguntaDto> preguntas = List.of(multiple("p1", 10, 0));
        List<RespuestaDto> mal = List.of(new RespuestaDto("p1", "9"));
        List<RespuestaDto> bien = List.of(new RespuestaDto("p1", "0"));

        assertThat(calificar(0.9, preguntas, mal).nivelDominio()).isCloseTo(0.54, org.assertj.core.api.Assertions.within(1e-9));
        // 0.4*0.0 + 0.6*0.9 = 0.54 -> MEDIA (un mal intento no hunde a un alumno con buen nivel)
        assertThat(calificar(0.9, preguntas, mal).categoriaNivel()).isEqualTo(CategoriaNivelEnum.MEDIO);
        // 0.4*1.0 + 0.6*0.2 = 0.52 -> MEDIA (un buen intento no sube del todo a quien venía mal)
        assertThat(calificar(0.2, preguntas, bien).categoriaNivel()).isEqualTo(CategoriaNivelEnum.MEDIO);
        // 0.4*0.0 + 0.6*0.3 = 0.18 -> BAJA
        assertThat(calificar(0.3, preguntas, mal).categoriaNivel()).isEqualTo(CategoriaNivelEnum.BAJO);
        // 0.4*1.0 + 0.6*0.9 = 0.94 -> ALTA
        assertThat(calificar(0.9, preguntas, bien).categoriaNivel()).isEqualTo(CategoriaNivelEnum.ALTO);
    }
}
