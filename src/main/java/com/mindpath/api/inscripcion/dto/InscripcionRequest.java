package com.mindpath.api.inscripcion.dto;

import jakarta.validation.constraints.NotBlank;

public record InscripcionRequest(
        @NotBlank String alumnoId,
        @NotBlank String cursoId) {}
