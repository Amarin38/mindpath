package com.mindpath.api.usuario.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mindpath.api.usuario.Alumno;
import com.mindpath.api.usuario.Docente;
import com.mindpath.api.usuario.Usuario;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UsuarioResponse(
        String id,
        String tipo,
        String email,
        String nombre,
        String apellido,
        Instant fechaAlta,
        String legajo,
        String especialidad) {

    public static UsuarioResponse de(Usuario usuario) {
        return switch (usuario) {
            case Alumno a -> new UsuarioResponse(a.getId(), "ALUMNO", a.getEmail(), a.getNombre(),
                    a.getApellido(), a.getFechaAlta(), a.getLegajo(), null);
            case Docente d -> new UsuarioResponse(d.getId(), "DOCENTE", d.getEmail(), d.getNombre(),
                    d.getApellido(), d.getFechaAlta(), null, d.getEspecialidad());
        };
    }
}
