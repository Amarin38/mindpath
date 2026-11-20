package com.mindpath.api.evaluacion.dto;

import java.time.Instant;
import java.util.List;

import com.mindpath.api.evaluacion.Evaluacion;
import com.mindpath.api.evaluacion.enums.EstadoEvaluacionEnum;

public record EvaluacionResponse(
        String id,
        String alumnoId,
        String temaId,
        EstadoEvaluacionEnum estado,
        Instant fechaInicio,
        Instant fechaEntrega,
        double puntajeTotal,
        Boolean aprobada,
        List<RespuestaResponse> respuestas) {

    public static EvaluacionResponse de(Evaluacion evaluacion) {
        return new EvaluacionResponse(
                evaluacion.getId(),
                evaluacion.getAlumnoId(),
                evaluacion.getTemaId(),
                evaluacion.getEstado(),
                evaluacion.getFechaInicio(),
                evaluacion.getFechaEntrega(),
                evaluacion.getPuntajeTotal(),
                evaluacion.getAprobada(),
                evaluacion.getRespuestas().stream().map(RespuestaResponse::de).toList());
    }
}
