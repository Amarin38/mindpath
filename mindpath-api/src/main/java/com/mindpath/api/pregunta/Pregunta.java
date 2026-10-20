package com.mindpath.api.pregunta;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.mindpath.api.common.enums.DificultadTemaEnum;
import com.mindpath.api.evaluacion.Respuesta;

import lombok.Getter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document(collection = "preguntas")
public abstract sealed class Pregunta permits PreguntaOpcionMultiple, PreguntaAbierta {
    @Id
    private String id;
    
    @Indexed
    private String temaId;

    private String enunciado;
    private DificultadTemaEnum dificultad;
    private double puntaje;
    private double tasaAcierto;
    
    protected Pregunta(String temaId, String enunciado, DificultadTemaEnum dificultad, double puntaje) {
        this.temaId = temaId;
        this.enunciado = enunciado;
        this.dificultad = dificultad;
        this.puntaje = puntaje;
    }    

    public abstract boolean corregir(Respuesta respuesta);
}
