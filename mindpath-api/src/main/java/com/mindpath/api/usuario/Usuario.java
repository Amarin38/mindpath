package com.mindpath.api.usuario;
import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document(collection = "usuarios")
public abstract sealed class Usuario permits Docente, Alumno{
    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String passwordHash;
    private String nombre;
    private String apellido;
    
    @CreatedDate
    private Instant fechaAlta;

    public Usuario(String email, String passwordHash, String nombre, String apellido){
        this.email = email;
        this.passwordHash = passwordHash;
        this.nombre = nombre;
        this.apellido = apellido;
    }
}
