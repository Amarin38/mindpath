package com.mindpath.api.usuario;

import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document(collection = "usuarios")
@TypeAlias("docentes")
public final class Docente extends Usuario {
    private String especialidad;

    public Docente(String email, String passwordHash, String nombre, String apellido, String especialidad) {
        super(email, passwordHash, nombre, apellido);
        this.especialidad = especialidad;
    }
}
