package com.mindpath.api.inscripcion;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.mindpath.api.inscripcion.enums.EstadoInscripcionEnum;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document(collection = "inscripciones")
public class Inscripcion {
    @Id
    private String id;

    @Indexed
    private String alumnoId;

    @Indexed
    private String cursoId;

    @CreatedDate
    private Instant fechaInscripcion;

    private Instant fechaBaja;
    private EstadoInscripcionEnum estado = EstadoInscripcionEnum.ACTIVA;

    public void darDeBaja() {
        if (estado != EstadoInscripcionEnum.ACTIVA) {
            throw new IllegalStateException(
                    "Solo se puede dar de baja una inscripción si previamente estaba activa o abandonada");
        }

        this.estado = EstadoInscripcionEnum.BAJA;
        this.fechaBaja = Instant.now();
    }

    public boolean estaActiva() {
        return estado == EstadoInscripcionEnum.ACTIVA;
    }
}
