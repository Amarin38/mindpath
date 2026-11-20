package com.mindpath.api.evaluacion;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.mindpath.api.evaluacion.enums.EstadoEvaluacionEnum;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@CompoundIndex(name = "alumno_tema", def = "{'alumnoId': 1, 'temaId': 1}")
@Document(collection = "evaluaciones")
public class Evaluacion {
    @Id
    private String id;

    private String temaId;
    private String alumnoId;

    private Instant fechaInicio;
    private Instant fechaEntrega;
    private EstadoEvaluacionEnum estado = EstadoEvaluacionEnum.PENDIENTE;
    private double puntajeTotal;
    private Boolean aprobada;

    private List<Respuesta> respuestas = new ArrayList<>();

    private static final double UMBRAL_APROBACION = 0.7;

    public Evaluacion(String alumnoId, String temaId) {
        this.alumnoId = alumnoId;
        this.temaId = temaId;
    }

    public void iniciarEvaluacion() {
        if (estado != EstadoEvaluacionEnum.PENDIENTE) {
            throw new IllegalStateException("La evaluación ya fue iniciada.");
        }

        this.fechaInicio = Instant.now();
        this.estado = EstadoEvaluacionEnum.EN_PROCESO;
    }

    public void agregarRespuesta(Respuesta respuesta) {
        if (estado != EstadoEvaluacionEnum.EN_PROCESO) {
            throw new IllegalStateException("Solo se puede responder una evaluación en proceso.");
        }

        if (respuestas.stream().anyMatch(r -> r.getPreguntaId().equals(respuesta.getPreguntaId()))) {
            throw new IllegalStateException("La pregunta ya fue respondida.");
        }

        this.respuestas.add(respuesta);
    }

    public void terminarEvaluacion(Instant momento) {
        if (estado != EstadoEvaluacionEnum.EN_PROCESO) {
            throw new IllegalStateException("Solo se puede terminar una evaluación en proceso.");
        }

        this.fechaEntrega = momento;
        this.estado = EstadoEvaluacionEnum.ENTREGADO;
    }

    public void finalizarEvaluacion(double puntajeMaximo) {
        if (estado != EstadoEvaluacionEnum.ENTREGADO) {
            throw new IllegalStateException("Solo se puede finalizar una evaluación entregada.");
        }

        if (puntajeMaximo <= 0) {
            throw new IllegalArgumentException("El puntaje máximo debe ser mayor a 0.");
        }

        if (!respuestas.stream().allMatch(Respuesta::estaCorregida)) {
            throw new IllegalStateException("Hay respuestas sin corregir.");
        }

        this.puntajeTotal = respuestas.stream()
                .mapToDouble(Respuesta::getPuntajeObtenido)
                .sum();
        this.aprobada = puntajeTotal / puntajeMaximo >= UMBRAL_APROBACION;
        this.estado = EstadoEvaluacionEnum.FINALIZADA;
    }
}
