package com.mindpath.calificacion;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CalificacionControllerTest {
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new CalificacionController(new Calificador())).build();
    }

    @Test
    @DisplayName("POST /calificaciones con un request válido devuelve la corrección")
    void valido() throws Exception {
        String json = """
                {"evaluacionId":"e1",
                 "preguntas":[{"id":"p1","tipo":"OPCION_MULTIPLE","puntaje":5,"opcionCorrecta":1}],
                 "respuestas":[{"preguntaId":"p1","contenido":"1"}]}""";

        mvc.perform(post("/calificaciones").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluacionId").value("e1"))
                .andExpect(jsonPath("$.puntajeMaximo").value(5.0))
                .andExpect(jsonPath("$.respuestas[0].correcta").value(true))
                .andExpect(jsonPath("$.nivelDominio").value(1.0))
                .andExpect(jsonPath("$.categoriaNivel").value("ALTO"));
    }

    @Test
    @DisplayName("POST /calificaciones sin preguntas devuelve 400")
    void sinPreguntas() throws Exception {
        String json = """
                {"evaluacionId":"e1","preguntas":[],"respuestas":[]}""";

        mvc.perform(post("/calificaciones").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /calificaciones con JSON inválido devuelve 400")
    void jsonInvalido() throws Exception {
        mvc.perform(post("/calificaciones").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
    }
}
