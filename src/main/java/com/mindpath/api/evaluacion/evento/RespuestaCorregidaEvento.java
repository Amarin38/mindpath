package com.mindpath.api.evaluacion.evento;

public record RespuestaCorregidaEvento(
        String preguntaId,
        boolean correcta,
        double puntajeObtenido) {}
