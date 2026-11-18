package com.mindpath.api.progreso;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@CompoundIndex(name = "alumno_tema_unico", def = "{'alumnoId': 1, 'temaId': 1}", unique = true)
@Document(collection = "progresos_temas")
public class ProgresoTema {
    @Id
    private String id;

    private String alumnoId;

    @Indexed
    private String temaId;

    private Double nivelDominio = null;

    private int intentos;

    private Instant fechaUltimaEvaluacion;

    private double mejorPuntaje = 0;

    private boolean desbloqueado = false;

    private static final double FACTOR_APRENDIZAJE = 0.4;
    private static final double UMBRAL_APROBACION = 0.7;

    public ProgresoTema(String alumnoId, String temaId) {
        this.alumnoId = alumnoId;
        this.temaId = temaId;
    }

    public void registrarEvaluacion(double puntaje, double puntajeMaximo, Instant momento) {
        this.intentos++;
        this.fechaUltimaEvaluacion = momento;

        if (puntaje > mejorPuntaje) {
            this.mejorPuntaje = puntaje;
        }

        double normalizado = puntajeMaximo > 0 ? puntaje / puntajeMaximo : 0;

        if (nivelDominio == null) {
            this.nivelDominio = normalizado;
        } else {
            this.nivelDominio = FACTOR_APRENDIZAJE * normalizado + (1 - FACTOR_APRENDIZAJE) * nivelDominio;
        }
    }

    public boolean estaAprobado() {
        return nivelDominio != null && nivelDominio >= UMBRAL_APROBACION;
    }

    public void desbloquear() {
        if (!desbloqueado)
            this.desbloqueado = true;
    }
}
