package com.mindpath.api.evaluacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mindpath.api.evaluacion.enums.EstadoEvaluacionEnum;

class EvaluacionTest {

    private Evaluacion enProceso() {
        Evaluacion e = new Evaluacion("alumno1", "tema1");
        e.iniciarEvaluacion();
        return e;
    }

    private Respuesta respuestaCorregida(String preguntaId, boolean correcta, double puntaje) {
        Respuesta r = new Respuesta(preguntaId, "x", 10);
        r.corregir(correcta, puntaje, true);
        return r;
    }

    @Test
    @DisplayName("Nace PENDIENTE y al iniciar pasa a EN_PROCESO")
    void iniciar() {
        Evaluacion e = new Evaluacion("alumno1", "tema1");
        assertThat(e.getEstado()).isEqualTo(EstadoEvaluacionEnum.PENDIENTE);

        e.iniciarEvaluacion();

        assertThat(e.getEstado()).isEqualTo(EstadoEvaluacionEnum.EN_PROCESO);
        assertThat(e.getFechaInicio()).isNotNull();
    }

    @Test
    @DisplayName("No se puede iniciar dos veces")
    void iniciarDosVeces() {
        Evaluacion e = enProceso();
        assertThatThrownBy(e::iniciarEvaluacion).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("No se puede responder si no está EN_PROCESO")
    void responderSinIniciar() {
        Evaluacion e = new Evaluacion("alumno1", "tema1");
        assertThatThrownBy(() -> e.agregarRespuesta(new Respuesta("p1", "1", 5)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("No se puede responder dos veces la misma pregunta")
    void respuestaDuplicada() {
        Evaluacion e = enProceso();
        e.agregarRespuesta(new Respuesta("p1", "1", 5));

        assertThatThrownBy(() -> e.agregarRespuesta(new Respuesta("p1", "2", 5)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ya fue respondida");
    }

    @Test
    @DisplayName("Terminar la deja ENTREGADO y ya no admite respuestas")
    void entregar() {
        Evaluacion e = enProceso();
        Instant ahora = Instant.now();

        e.terminarEvaluacion(ahora);

        assertThat(e.getEstado()).isEqualTo(EstadoEvaluacionEnum.ENTREGADO);
        assertThat(e.getFechaEntrega()).isEqualTo(ahora);
        assertThatThrownBy(() -> e.agregarRespuesta(new Respuesta("p1", "1", 5)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Solo se finaliza una evaluación ENTREGADA")
    void finalizarSinEntregar() {
        Evaluacion e = enProceso();
        assertThatThrownBy(() -> e.finalizarEvaluacion(10)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("No se finaliza con respuestas sin corregir")
    void finalizarSinCorregir() {
        Evaluacion e = enProceso();
        e.agregarRespuesta(new Respuesta("p1", "1", 5));
        e.terminarEvaluacion(Instant.now());

        assertThatThrownBy(() -> e.finalizarEvaluacion(10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sin corregir");
    }

    @Test
    @DisplayName("El puntaje máximo debe ser positivo")
    void puntajeMaximoInvalido() {
        Evaluacion e = enProceso();
        e.terminarEvaluacion(Instant.now());

        assertThatThrownBy(() -> e.finalizarEvaluacion(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("70% exacto aprueba")
    void apruebaConSetentaPorCiento() {
        Evaluacion e = enProceso();
        e.agregarRespuesta(respuestaCorregida("p1", true, 7));
        e.agregarRespuesta(respuestaCorregida("p2", false, 0));
        e.terminarEvaluacion(Instant.now());

        e.finalizarEvaluacion(10);

        assertThat(e.getEstado()).isEqualTo(EstadoEvaluacionEnum.FINALIZADA);
        assertThat(e.getPuntajeTotal()).isEqualTo(7);
        assertThat(e.getAprobada()).isTrue();
    }

    @Test
    @DisplayName("Por debajo del 70% desaprueba")
    void desapruebaPorDebajo() {
        Evaluacion e = enProceso();
        e.agregarRespuesta(respuestaCorregida("p1", true, 6));
        e.agregarRespuesta(respuestaCorregida("p2", false, 0));
        e.terminarEvaluacion(Instant.now());

        e.finalizarEvaluacion(10);

        assertThat(e.getPuntajeTotal()).isEqualTo(6);
        assertThat(e.getAprobada()).isFalse();
    }

    @Test
    @DisplayName("No se puede finalizar dos veces")
    void finalizarDosVeces() {
        Evaluacion e = enProceso();
        e.agregarRespuesta(respuestaCorregida("p1", true, 10));
        e.terminarEvaluacion(Instant.now());
        e.finalizarEvaluacion(10);

        assertThatThrownBy(() -> e.finalizarEvaluacion(10)).isInstanceOf(IllegalStateException.class);
    }
}
