package com.mindpath.api.curso.dto;

import com.mindpath.api.curso.enums.TipoRecursoEnum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RecursoRequest(
        @NotNull TipoRecursoEnum tipo,
        @NotBlank String titulo,
        @NotBlank String url) {}
