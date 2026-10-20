package com.mindpath.api.curso.dto;

import com.mindpath.api.common.enums.DificultadTemaEnum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TemaRequest(
        @NotBlank String nombre,
        String descripcion,
        @NotNull DificultadTemaEnum dificultad) {
};
