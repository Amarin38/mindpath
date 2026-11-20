package com.mindpath.api.pregunta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mindpath.api.common.enums.DificultadTemaEnum;
import com.mindpath.api.evaluacion.Respuesta;

class PreguntaTest {

    private PreguntaOpcionMultiple multiple() {
        return new PreguntaOpcionMultiple("tema1", "¿Cuál es int?", DificultadTemaEnum.BAJA, 1,
                List.of("long", "int", "double"), 1);
    }

    private PreguntaAbierta abierta() {
        return new PreguntaAbierta("tema1", "¿Qué es una variable?", DificultadTemaEnum.BAJA, 4,
                "Un espacio de memoria con un valor", List.of("memoria", "valor"));
    }

    private Respuesta respuesta(String contenido) {
        return new Respuesta("p1", contenido, 5);
    }

    @Test
    @DisplayName("Opción múltiple: acierta con el índice correcto, incluso con espacios")
    void multipleCorrecta() {
        assertThat(multiple().corregir(respuesta("1"))).isTrue();
        assertThat(multiple().corregir(respuesta(" 1 "))).isTrue();
    }

    @Test
    @DisplayName("Opción múltiple: falla con otro índice, texto o vacío")
    void multipleIncorrecta() {
        assertThat(multiple().corregir(respuesta("0"))).isFalse();
        assertThat(multiple().corregir(respuesta("int"))).isFalse();
        assertThat(multiple().corregir(respuesta(null))).isFalse();
        assertThat(multiple().corregir(null)).isFalse();
    }

    @Test
    @DisplayName("Opción múltiple: valida cantidad de opciones e índice")
    void multipleValidaciones() {
        assertThatThrownBy(() -> new PreguntaOpcionMultiple("t", "e", DificultadTemaEnum.BAJA, 1, List.of("a"), 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PreguntaOpcionMultiple("t", "e", DificultadTemaEnum.BAJA, 1,
                List.of("a", "b"), 2)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PreguntaOpcionMultiple("t", "e", DificultadTemaEnum.BAJA, 1,
                List.of("a", "b"), -1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Abierta: acierta si aparecen todas las palabras clave, sin importar mayúsculas ni tildes")
    void abiertaCorrecta() {
        assertThat(abierta().corregir(respuesta("Es un espacio de MEMORIA que guarda un Valor"))).isTrue();

        PreguntaAbierta conTilde = new PreguntaAbierta("t", "e", DificultadTemaEnum.BAJA, 1, "x",
                List.of("polimorfismo", "herencia"));
        assertThat(conTilde.corregir(respuesta("Polimorfísmo y HERÉNCIA"))).isTrue();
    }

    @Test
    @DisplayName("Abierta: falla si falta una palabra clave, está vacía o no hay claves")
    void abiertaIncorrecta() {
        assertThat(abierta().corregir(respuesta("solo menciona memoria"))).isFalse();
        assertThat(abierta().corregir(respuesta(null))).isFalse();

        PreguntaAbierta sinClaves = new PreguntaAbierta("t", "e", DificultadTemaEnum.BAJA, 1, "x", List.of());
        assertThat(sinClaves.corregir(respuesta("lo que sea"))).isFalse();
    }

    @Test
    @DisplayName("La tasa de acierto se actualiza como promedio de respuestas")
    void tasaDeAcierto() {
        PreguntaOpcionMultiple p = multiple();
        p.registrarRespuesta(true);
        p.registrarRespuesta(false);
        p.registrarRespuesta(true);
        p.registrarRespuesta(true);

        assertThat(p.getCantidadRespuestas()).isEqualTo(4);
        assertThat(p.getTasaAcierto()).isCloseTo(0.75, within(1e-9));
    }
}
