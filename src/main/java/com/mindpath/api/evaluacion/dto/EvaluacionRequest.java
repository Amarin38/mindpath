package com.mindpath.api.evaluacion.dto;

import jakarta.validation.constraints.NotBlank;

public record EvaluacionRequest(
        @NotBlank String alumnoId,
        @NotBlank String cursoId,
        @NotBlank String temaId) {}
