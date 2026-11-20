package com.mindpath.calificacion.dto;

import jakarta.validation.constraints.NotBlank;

public record RespuestaDto(
        @NotBlank String preguntaId,
        String contenido) {}
