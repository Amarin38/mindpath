package com.mindpath.calificacion.dto;

import java.util.List;

import com.mindpath.calificacion.enums.TipoPreguntaEnum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PreguntaDto(
        @NotBlank String id,
        @NotNull TipoPreguntaEnum tipo,
        @Positive double puntaje,
        Integer opcionCorrecta,          // OPCION_MULTIPLE
        List<String> palabrasClave) {}   // ABIERTA
