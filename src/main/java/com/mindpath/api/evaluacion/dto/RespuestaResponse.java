package com.mindpath.api.evaluacion.dto;

import com.mindpath.api.evaluacion.Respuesta;

public record RespuestaResponse(
        String id,
        String preguntaId,
        String contenido,
        Boolean correcta,
        Double puntajeObtenido,
        int tiempoSegundos,
        boolean corregidaAutomaticamente) {

    public static RespuestaResponse de(Respuesta respuesta) {
        return new RespuestaResponse(
                respuesta.getId(),
                respuesta.getPreguntaId(),
                respuesta.getContenido(),
                respuesta.getCorrecta(),
                respuesta.getPuntajeObtenido(),
                respuesta.getTiempoSegundos(),
                respuesta.isCorregidaAutomaticamente());
    }
}
