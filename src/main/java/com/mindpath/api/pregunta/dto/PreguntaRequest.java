package com.mindpath.api.pregunta.dto;

import java.util.List;

import com.mindpath.api.common.enums.DificultadTemaEnum;
import com.mindpath.api.pregunta.enums.TipoPreguntaEnum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PreguntaRequest(
        @NotNull TipoPreguntaEnum tipo,
        @NotBlank String enunciado,
        @NotNull DificultadTemaEnum dificultad,
        @Positive double puntaje,
        // OPCION_MULTIPLE
        List<String> opciones,
        Integer opcionCorrecta,
        // ABIERTA
        String respuestaEsperada,
        List<String> palabrasClave) {}
