package com.mindpath.api.progreso.dto;

import java.time.Instant;

import com.mindpath.api.common.enums.NivelDominioEnum;
import com.mindpath.api.progreso.ProgresoTema;

public record ProgresoTemaResponse(
        String id,
        String alumnoId,
        String temaId,
        Double nivelDominio,
        int intentos,
        Instant fechaUltimaEvaluacion,
        double mejorPuntaje,
        boolean desbloqueado,
        boolean aprobado,
        NivelDominioEnum categoriaNivel) {

    public static ProgresoTemaResponse de(ProgresoTema progreso) {
        return new ProgresoTemaResponse(
                progreso.getId(),
                progreso.getAlumnoId(),
                progreso.getTemaId(),
                progreso.getNivelDominio(),
                progreso.getIntentos(),
                progreso.getFechaUltimaEvaluacion(),
                progreso.getMejorPuntaje(),
                progreso.isDesbloqueado(),
                progreso.estaAprobado(),
                progreso.categoriaNivel());
    }
}
