package com.mindpath.api.evaluacion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record RespuestaRequest(
        @NotBlank String preguntaId,
        @NotBlank String contenido,
        @PositiveOrZero int tiempoSegundos) {}
