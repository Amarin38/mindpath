package com.mindpath.api.pregunta;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import com.mindpath.api.common.enums.DificultadTemaEnum;
import com.mindpath.api.evaluacion.Respuesta;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Document(collection = "preguntas")
@TypeAlias("opcion_multiple")
public final class PreguntaOpcionMultiple extends Pregunta {

    private List<String> opciones = new ArrayList<>();
    private int opcionCorrecta;

    public PreguntaOpcionMultiple(String temaId, String enunciado, DificultadTemaEnum dificultad,
            double puntaje, List<String> opciones, int opcionCorrecta) {
        super(temaId, enunciado, dificultad, puntaje);
        if (opciones == null || opciones.size() < 2) {
            throw new IllegalArgumentException("Una pregunta de opción múltiple necesita al menos 2 opciones");
        }
        if (opcionCorrecta < 0 || opcionCorrecta >= opciones.size()) {
            throw new IllegalArgumentException("El índice de la opción correcta está fuera de rango");
        }
        this.opciones = new ArrayList<>(opciones);
        this.opcionCorrecta = opcionCorrecta;
    }

    @Override
    public boolean corregir(Respuesta respuesta) {
        if (respuesta == null || respuesta.getContenido() == null) {
            return false;
        }
        try {
            return Integer.parseInt(respuesta.getContenido().trim()) == opcionCorrecta;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public List<String> getOpciones() {
        return List.copyOf(opciones);
    }
}
