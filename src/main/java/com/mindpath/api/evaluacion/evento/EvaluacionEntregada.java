package com.mindpath.api.evaluacion.evento;

import java.util.List;

public record EvaluacionEntregada(
        String evaluacionId,
        Double nivelDominioActual,
        List<PreguntaEvento> preguntas,
        List<RespuestaEvento> respuestas) {}
