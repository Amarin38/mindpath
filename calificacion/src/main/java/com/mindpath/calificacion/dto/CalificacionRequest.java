package com.mindpath.calificacion.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CalificacionRequest(
        @NotBlank String evaluacionId,
        Double nivelDominioActual,
        @NotEmpty List<@Valid PreguntaDto> preguntas,
        @NotNull List<@Valid RespuestaDto> respuestas) {}
