package com.mindpath.api.curso.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mindpath.api.curso.Curso;

@JsonInclude
public record CursoResponse(
        String id,
        String titulo,
        String descripcion,
        String docenteId,
        Instant fechaCreacion,
        Long cantidadInscriptos) {

    public static CursoResponse de(Curso curso) {
        return new CursoResponse(
                curso.getId(),
                curso.getTitulo(),
                curso.getDescripcion(),
                curso.getDocenteId(),
                curso.getFechaCreacion(),
                null);
    }

    public static CursoResponse de(Curso curso, long cantidadInscriptos) {
        return new CursoResponse(
                curso.getId(),
                curso.getTitulo(),
                curso.getDescripcion(),
                curso.getDocenteId(),
                curso.getFechaCreacion(),
                cantidadInscriptos);
    }
}
