package com.mindpath.api.evaluacion;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.mindpath.api.evaluacion.enums.EstadoEvaluacionEnum;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Document(collection = "evaluaciones")
public class Evaluacion {
    @Id
    private String id;

    @Indexed
    private String temaId;

    @Indexed
    private String alumnoId;

    private Instant fechaInicio;
    private Instant fechaEntrega;
    private EstadoEvaluacionEnum estado = EstadoEvaluacionEnum.PENDIENTE;
    private double puntajeTotal;

    private List<Respuesta> respuestas = new ArrayList<>();

    public Evaluacion(String alumnoId, String temaId) {
        this.alumnoId = alumnoId;
        this.temaId = temaId;
    }

    public void agregarRespuesta(Respuesta respuesta) {
        if (estado != EstadoEvaluacionEnum.EN_PROCESO) {
            throw new IllegalMonitorStateException("Solo se puede responder la evaluación en proceso.");
        }
        this.respuestas.add(respuesta);
    }

    public void iniciarEvaluacion() {
        if (estado != EstadoEvaluacionEnum.PENDIENTE) {
            throw new IllegalStateException("La evaluación ya fue iniciada.");
        }

        this.fechaInicio = Instant.now();
        this.estado = EstadoEvaluacionEnum.EN_PROCESO;
    }

    public void terminarEvaluacion(Instant momento) {
        if (estado != EstadoEvaluacionEnum.EN_PROCESO) {
            throw new IllegalStateException("Solo se puede terminar una evaluación en proceso.");
        }

        this.fechaEntrega = momento;
        this.estado = EstadoEvaluacionEnum.ENTREGADO;
        this.puntajeTotal = respuestas.stream()
                .mapToDouble(Respuesta::getPuntajeObtenido)
                .sum();
    }
}
