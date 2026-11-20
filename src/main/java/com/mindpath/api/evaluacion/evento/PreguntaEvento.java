package com.mindpath.api.evaluacion.evento;

import java.util.List;

import com.mindpath.api.pregunta.Pregunta;
import com.mindpath.api.pregunta.PreguntaAbierta;
import com.mindpath.api.pregunta.PreguntaOpcionMultiple;
import com.mindpath.api.pregunta.enums.TipoPreguntaEnum;

public record PreguntaEvento(
        String id,
        TipoPreguntaEnum tipo,
        double puntaje,
        Integer opcionCorrecta,
        List<String> palabrasClave) {

    public static PreguntaEvento de(Pregunta pregunta) {
        return switch (pregunta) {
            case PreguntaOpcionMultiple m -> new PreguntaEvento(m.getId(), TipoPreguntaEnum.OPCION_MULTIPLE,
                    m.getPuntaje(), m.getOpcionCorrecta(), null);
            case PreguntaAbierta a -> new PreguntaEvento(a.getId(), TipoPreguntaEnum.ABIERTA,
                    a.getPuntaje(), null, List.copyOf(a.getPalabrasClave()));
        };
    }
}
