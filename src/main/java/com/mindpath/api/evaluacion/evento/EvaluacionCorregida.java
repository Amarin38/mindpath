package com.mindpath.api.evaluacion.evento;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EvaluacionCorregida(
        String evaluacionId,
        double puntajeMaximo,
        List<RespuestaCorregidaEvento> respuestas) {}
