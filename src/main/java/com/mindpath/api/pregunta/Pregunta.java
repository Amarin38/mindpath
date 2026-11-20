package com.mindpath.api.pregunta;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import com.mindpath.api.common.enums.DificultadTemaEnum;
import com.mindpath.api.evaluacion.Respuesta;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@CompoundIndex(name = "tema_dificultad", def = "{'temaId': 1, 'dificultad': 1}")
@Document(collection = "preguntas")
public abstract sealed class Pregunta permits PreguntaOpcionMultiple, PreguntaAbierta {
    @Id
    private String id;

    private String temaId;

    private String enunciado;
    private DificultadTemaEnum dificultad;
    private double puntaje;
    private double tasaAcierto;
    private int cantidadRespuestas;

    protected Pregunta(String temaId, String enunciado, DificultadTemaEnum dificultad, double puntaje) {
        this.temaId = temaId;
        this.enunciado = enunciado;
        this.dificultad = dificultad;
        this.puntaje = puntaje;
    }

    public abstract boolean corregir(Respuesta respuesta);

    public void registrarRespuesta(boolean correcta) {
        this.tasaAcierto = (tasaAcierto * cantidadRespuestas + (correcta ? 1 : 0)) / (cantidadRespuestas + 1);
        this.cantidadRespuestas++;
    }
}
