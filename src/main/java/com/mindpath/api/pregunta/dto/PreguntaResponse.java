package com.mindpath.api.pregunta.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mindpath.api.common.enums.DificultadTemaEnum;
import com.mindpath.api.pregunta.Pregunta;
import com.mindpath.api.pregunta.PreguntaAbierta;
import com.mindpath.api.pregunta.PreguntaOpcionMultiple;
import com.mindpath.api.pregunta.enums.TipoPreguntaEnum;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PreguntaResponse(
        String id,
        TipoPreguntaEnum tipo,
        String temaId,
        String enunciado,
        DificultadTemaEnum dificultad,
        double puntaje,
        double tasaAcierto,
        List<String> opciones,
        Integer opcionCorrecta,
        String respuestaEsperada,
        List<String> palabrasClave) {

    public static PreguntaResponse de(Pregunta pregunta) {
        return switch (pregunta) {
            case PreguntaOpcionMultiple m -> new PreguntaResponse(m.getId(), TipoPreguntaEnum.OPCION_MULTIPLE,
                    m.getTemaId(), m.getEnunciado(), m.getDificultad(), m.getPuntaje(), m.getTasaAcierto(),
                    m.getOpciones(), m.getOpcionCorrecta(), null, null);
            case PreguntaAbierta a -> new PreguntaResponse(a.getId(), TipoPreguntaEnum.ABIERTA,
                    a.getTemaId(), a.getEnunciado(), a.getDificultad(), a.getPuntaje(), a.getTasaAcierto(),
                    null, null, a.getRespuestaEsperada(), List.copyOf(a.getPalabrasClave()));
        };
    }
}
