package com.mindpath.api.curso.dto;

import jakarta.validation.constraints.NotBlank;

public record CursoRequest(
        @NotBlank String titulo, 
        @NotBlank String descripcion) {}
