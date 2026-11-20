package com.mindpath.calificacion.dto;

import com.mindpath.calificacion.enums.CategoriaNivelEnum;
import java.util.List;

public record CalificacionResponse(
        String evaluacionId,
        double puntajeMaximo,
        List<RespuestaCorregida> respuestas,
        double nivelDominio,
        CategoriaNivelEnum categoriaNivel) {
}
