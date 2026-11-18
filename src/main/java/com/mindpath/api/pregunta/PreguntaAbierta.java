package com.mindpath.api.pregunta;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
@TypeAlias("abierta")
public final class PreguntaAbierta extends Pregunta {
    private String respuestaEsperada;
    private List<String> palabrasClave = new ArrayList<>();

    public PreguntaAbierta(String temaId, String enunciado, DificultadTemaEnum dificultad, double puntaje,
            String respuestaEsperada, List<String> palabrasClave) {
        super(temaId, enunciado, dificultad, puntaje);
        this.respuestaEsperada = respuestaEsperada;
        this.palabrasClave = new ArrayList<>(palabrasClave);
    }

    @Override
    public boolean corregir(Respuesta respuesta) {
        if (respuesta == null || respuesta.getContenido() == null || palabrasClave.isEmpty())
            return false;

        String texto = normalizar(respuesta.getContenido());

        return palabrasClave.stream()
                .allMatch(palabra -> texto.contains(normalizar(palabra)));
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }
}
