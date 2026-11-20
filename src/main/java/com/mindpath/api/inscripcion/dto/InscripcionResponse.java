package com.mindpath.api.inscripcion.dto;

import java.time.Instant;

import com.mindpath.api.inscripcion.Inscripcion;
import com.mindpath.api.inscripcion.enums.EstadoInscripcionEnum;

public record InscripcionResponse(
        String id,
        String alumnoId,
        String cursoId,
        Instant fechaInscripcion,
        Instant fechaBaja,
        EstadoInscripcionEnum estado) {

    public static InscripcionResponse de(Inscripcion inscripcion) {
        return new InscripcionResponse(
                inscripcion.getId(),
                inscripcion.getAlumnoId(),
                inscripcion.getCursoId(),
                inscripcion.getFechaInscripcion(),
                inscripcion.getFechaBaja(),
                inscripcion.getEstado());
    }
}
