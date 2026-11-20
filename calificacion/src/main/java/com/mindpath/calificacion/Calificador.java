package com.mindpath.calificacion;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.mindpath.calificacion.dto.CalificacionRequest;
import com.mindpath.calificacion.dto.CalificacionResponse;
import com.mindpath.calificacion.dto.PreguntaDto;
import com.mindpath.calificacion.dto.RespuestaCorregida;

import com.mindpath.calificacion.enums.CategoriaNivelEnum;

@Service
public class Calificador {
    private static final double FACTOR_APRENDIZAJE = 0.4;

    public CalificacionResponse calificar(CalificacionRequest req) {
        Map<String, PreguntaDto> porId = req.preguntas().stream()
                .collect(Collectors.toMap(PreguntaDto::id, Function.identity()));

        List<RespuestaCorregida> corregidas = req.respuestas().stream()
                .map(r -> {
                    PreguntaDto pregunta = porId.get(r.preguntaId());
                    if (pregunta == null)
                        throw new IllegalArgumentException(
                                "La pregunta " + r.preguntaId() + " no está en el request.");

                    boolean correcta = corregir(pregunta, r.contenido());
                    return new RespuestaCorregida(r.preguntaId(), correcta, correcta ? pregunta.puntaje() : 0);
                })
                .toList();

        double puntajeMaximo = req.preguntas().stream().mapToDouble(PreguntaDto::puntaje).sum();
        double obtenido = corregidas.stream().mapToDouble(RespuestaCorregida::puntajeObtenido).sum();

        double nivel = estimarNivel(obtenido / puntajeMaximo, req.nivelDominioActual());

        return new CalificacionResponse(req.evaluacionId(), puntajeMaximo, corregidas, nivel, categorizar(nivel));
    }

    private boolean corregir(PreguntaDto pregunta, String contenido) {
        if (contenido == null)
            return false;

        return switch (pregunta.tipo()) {
            case OPCION_MULTIPLE -> {
                try {
                    yield pregunta.opcionCorrecta() != null
                            && Integer.parseInt(contenido.trim()) == pregunta.opcionCorrecta();
                } catch (NumberFormatException e) {
                    yield false;
                }
            }
            case ABIERTA -> {
                if (pregunta.palabrasClave() == null || pregunta.palabrasClave().isEmpty())
                    yield false;

                String texto = normalizar(contenido);
                yield pregunta.palabrasClave().stream().allMatch(k -> texto.contains(normalizar(k)));
            }
        };
    }

    private double estimarNivel(double porcentaje, Double nivelActual) {
        return nivelActual == null
                ? porcentaje
                : FACTOR_APRENDIZAJE * porcentaje + (1 - FACTOR_APRENDIZAJE) * nivelActual;
    }

    // BAJO < 0.4 <= MEDIO < 0.7 <= ALTO
    private CategoriaNivelEnum categorizar(double nivel) {
        if (nivel < 0.4)
            return CategoriaNivelEnum.BAJO;
        if (nivel < 0.7)
            return CategoriaNivelEnum.MEDIO;
        return CategoriaNivelEnum.ALTO;
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}
