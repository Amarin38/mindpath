package com.mindpath.api.evaluacion;

import org.bson.types.ObjectId;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Respuesta {
    private String id;
    private String preguntaId;
    private String contenido;
    private Boolean correcta;
    private Double puntajeObtenido;
    private int tiempoSegundos;
    private boolean corregidaAutomaticamente;

    public Respuesta(String preguntaId, String contenido, int tiempoSegundos) {
        this.id = new ObjectId().toHexString();
        this.preguntaId = preguntaId;
        this.contenido = contenido;
        this.tiempoSegundos = tiempoSegundos;
    }

    public void corregir(boolean correcta, double puntajeObtenido, boolean automatica) {
        this.correcta = correcta;
        this.puntajeObtenido = puntajeObtenido;
        this.corregidaAutomaticamente = automatica;
    }

    public boolean estaCorregida() {
        return correcta != null && puntajeObtenido != null;
    }
}
