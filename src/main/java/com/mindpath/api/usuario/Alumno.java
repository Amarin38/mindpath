package com.mindpath.api.usuario;

import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document(collection = "usuarios")
@TypeAlias("alumnos")
public final class Alumno extends Usuario {
    @Indexed(unique = true, sparse = true)
    private String legajo;

    public Alumno(String email, String passwordHash, String nombre, String apellido, String legajo) {
        super(email, passwordHash, nombre, apellido);
        this.legajo = legajo;
    }
}
