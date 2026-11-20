package com.mindpath.calificacion.dto;

public record RespuestaCorregida(
        String preguntaId,
        boolean correcta,
        double puntajeObtenido) {}
