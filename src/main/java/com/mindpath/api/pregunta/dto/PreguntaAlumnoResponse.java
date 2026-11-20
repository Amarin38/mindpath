package com.mindpath.api.pregunta.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mindpath.api.common.enums.DificultadTemaEnum;
import com.mindpath.api.pregunta.Pregunta;
import com.mindpath.api.pregunta.PreguntaAbierta;
import com.mindpath.api.pregunta.PreguntaOpcionMultiple;
import com.mindpath.api.pregunta.enums.TipoPreguntaEnum;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PreguntaAlumnoResponse(
        String id,
        TipoPreguntaEnum tipo,
        String enunciado,
        DificultadTemaEnum dificultad,
        double puntaje,
        List<String> opciones) {

    public static PreguntaAlumnoResponse de(Pregunta pregunta) {
        return switch (pregunta) {
            case PreguntaOpcionMultiple m -> new PreguntaAlumnoResponse(m.getId(),
                    TipoPreguntaEnum.OPCION_MULTIPLE, m.getEnunciado(), m.getDificultad(), m.getPuntaje(),
                    m.getOpciones());
            case PreguntaAbierta a -> new PreguntaAlumnoResponse(a.getId(), TipoPreguntaEnum.ABIERTA,
                    a.getEnunciado(), a.getDificultad(), a.getPuntaje(), null);
        };
    }
}
