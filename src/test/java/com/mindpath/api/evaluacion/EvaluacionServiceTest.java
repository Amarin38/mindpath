package com.mindpath.api.evaluacion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import com.mindpath.api.common.enums.DificultadTemaEnum;
import com.mindpath.api.common.enums.NivelDominioEnum;
import com.mindpath.api.config.KafkaConfig;
import com.mindpath.api.curso.Curso;
import com.mindpath.api.curso.CursoService;
import com.mindpath.api.curso.Tema;
import com.mindpath.api.evaluacion.dto.EvaluacionRequest;
import com.mindpath.api.evaluacion.enums.EstadoEvaluacionEnum;
import com.mindpath.api.evaluacion.evento.EvaluacionCorregida;
import com.mindpath.api.evaluacion.evento.EvaluacionEntregada;
import com.mindpath.api.evaluacion.evento.RespuestaCorregidaEvento;
import com.mindpath.api.inscripcion.InscripcionRepository;
import com.mindpath.api.inscripcion.enums.EstadoInscripcionEnum;
import com.mindpath.api.pregunta.Pregunta;
import com.mindpath.api.pregunta.PreguntaOpcionMultiple;
import com.mindpath.api.pregunta.PreguntaRepository;
import com.mindpath.api.progreso.ProgresoService;
import com.mindpath.api.progreso.ProgresoTema;
import com.mindpath.api.usuario.UsuarioService;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EvaluacionServiceTest {
    @Mock EvaluacionRepository evaluacionRepository;
    @Mock PreguntaRepository preguntaRepository;
    @Mock InscripcionRepository inscripcionRepository;
    @Mock CursoService cursoService;
    @Mock UsuarioService usuarioService;
    @Mock ProgresoService progresoService;
    @Mock KafkaTemplate<String, EvaluacionEntregada> kafkaTemplate;

    @InjectMocks EvaluacionService service;

    private Curso curso;
    private Tema tema;
    private EvaluacionRequest req;

    @BeforeEach
    void setUp() {
        curso = new Curso("Java", "desc", "docente1");
        tema = curso.addTema("Variables", "desc", DificultadTemaEnum.BAJA);
        req = new EvaluacionRequest("alumno1", "curso1", tema.getId());

        when(cursoService.obtener("curso1")).thenReturn(curso);
        when(inscripcionRepository.existsByAlumnoIdAndCursoIdAndEstado("alumno1", "curso1",
                EstadoInscripcionEnum.ACTIVA)).thenReturn(true);
        when(progresoService.temasAprobados(eq("alumno1"), any())).thenReturn(Set.of());
        when(preguntaRepository.findByTemaId(tema.getId())).thenReturn(List.of(pregunta("p1", 5), pregunta("p2", 5)));
        when(progresoService.obtenerOCrear("alumno1", tema.getId()))
                .thenReturn(new ProgresoTema("alumno1", tema.getId()));
        when(evaluacionRepository.save(any(Evaluacion.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---------- iniciar ----------

    @Test
    @DisplayName("Inicia la evaluación cuando se cumplen todas las reglas")
    void iniciarOk() {
        Evaluacion e = service.iniciar(req);

        assertThat(e.getEstado()).isEqualTo(EstadoEvaluacionEnum.EN_PROCESO);
        assertThat(e.getAlumnoId()).isEqualTo("alumno1");
    }

    @Test
    @DisplayName("Rechaza si el alumno no tiene inscripción activa")
    void sinInscripcion() {
        when(inscripcionRepository.existsByAlumnoIdAndCursoIdAndEstado(anyString(), anyString(), any()))
                .thenReturn(false);

        assertThatThrownBy(() -> service.iniciar(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("inscripción");
    }

    @Test
    @DisplayName("Rechaza si el tema tiene un prerrequisito sin aprobar")
    void temaBloqueado() {
        Tema previo = curso.addTema("Previo", "desc", DificultadTemaEnum.BAJA);
        curso.addPrerrequisito(tema.getId(), previo.getId());

        assertThatThrownBy(() -> service.iniciar(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("desbloqueado");
    }

    @Test
    @DisplayName("Rechaza si el tema no tiene preguntas")
    void sinPreguntas() {
        when(preguntaRepository.findByTemaId(tema.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> service.iniciar(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("preguntas");
    }

    @Test
    @DisplayName("Rechaza si las preguntas del tema suman menos de 10 puntos")
    void menosDeDiezPuntos() {
        when(preguntaRepository.findByTemaId(tema.getId())).thenReturn(List.of(pregunta("p1", 4), pregunta("p2", 5)));

        assertThatThrownBy(() -> service.iniciar(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("al menos 10");
    }

    @Test
    @DisplayName("Rechaza si ya hay una evaluación EN_PROCESO")
    void yaEnProceso() {
        when(evaluacionRepository.existsByAlumnoIdAndTemaIdAndEstado("alumno1", tema.getId(),
                EstadoEvaluacionEnum.EN_PROCESO)).thenReturn(true);

        assertThatThrownBy(() -> service.iniciar(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("en curso");
    }

    @Test
    @DisplayName("Rechaza si hay una evaluación ENTREGADA esperando corrección")
    void yaEntregada() {
        when(evaluacionRepository.existsByAlumnoIdAndTemaIdAndEstado("alumno1", tema.getId(),
                EstadoEvaluacionEnum.ENTREGADO)).thenReturn(true);

        assertThatThrownBy(() -> service.iniciar(req)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Rechaza repetir un tema que el alumno ya aprobó")
    void yaAprobado() {
        when(evaluacionRepository.existsByAlumnoIdAndTemaIdAndAprobadaTrue("alumno1", tema.getId()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.iniciar(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("aprobó");
    }

    @Test
    @DisplayName("Permite el tercer intento pero rechaza el cuarto")
    void maximoDeIntentos() {
        when(evaluacionRepository.countByAlumnoIdAndTemaId("alumno1", tema.getId())).thenReturn(2L);
        assertThat(service.iniciar(req).getEstado()).isEqualTo(EstadoEvaluacionEnum.EN_PROCESO);

        when(evaluacionRepository.countByAlumnoIdAndTemaId("alumno1", tema.getId())).thenReturn(3L);
        assertThatThrownBy(() -> service.iniciar(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("máximo");
    }

    // ---------- entregar ----------

    private Evaluacion evaluacionEnProceso(String... preguntaIds) {
        Evaluacion e = new Evaluacion("alumno1", tema.getId());
        ReflectionTestUtils.setField(e, "id", "e1");
        e.iniciarEvaluacion();
        for (String p : preguntaIds)
            e.agregarRespuesta(new Respuesta(p, "1", 10));
        return e;
    }

    private Pregunta pregunta(String id, double puntaje) {
        PreguntaOpcionMultiple p = new PreguntaOpcionMultiple(tema.getId(), "enunciado " + id,
                DificultadTemaEnum.BAJA, puntaje, List.of("a", "b"), 1);
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    @Test
    @DisplayName("Entregar deja la evaluación ENTREGADA y publica el evento en Kafka")
    void entregarPublicaEvento() {
        Evaluacion e = evaluacionEnProceso("p1");
        when(evaluacionRepository.findById("e1")).thenReturn(Optional.of(e));
        when(preguntaRepository.findByTemaId(tema.getId())).thenReturn(List.of(pregunta("p1", 10), pregunta("p2", 10)));

        service.entregar("e1");

        assertThat(e.getEstado()).isEqualTo(EstadoEvaluacionEnum.ENTREGADO);

        ArgumentCaptor<EvaluacionEntregada> evento = ArgumentCaptor.forClass(EvaluacionEntregada.class);
        verify(kafkaTemplate).send(eq(KafkaConfig.EVALUACION_ENTREGADA), eq("e1"), evento.capture());
        assertThat(evento.getValue().evaluacionId()).isEqualTo("e1");
        assertThat(evento.getValue().preguntas()).hasSize(2);
        assertThat(evento.getValue().respuestas()).hasSize(1);
        assertThat(evento.getValue().nivelDominioActual()).isNull();
    }


    private Evaluacion entregadaConDosRespuestas() {
        Evaluacion e = evaluacionEnProceso("p1", "p2");
        e.terminarEvaluacion(Instant.now());
        when(evaluacionRepository.findById("e1")).thenReturn(Optional.of(e));
        when(preguntaRepository.findByTemaId(tema.getId())).thenReturn(List.of(pregunta("p1", 10), pregunta("p2", 10)));
        return e;
    }

    @Test
    @DisplayName("Aplicar la corrección finaliza la evaluación, actualiza el progreso y su categoría de nivel")
    void aplicarCorreccion() {
        Evaluacion e = entregadaConDosRespuestas();
        ProgresoTema progreso = new ProgresoTema("alumno1", tema.getId());
        when(progresoService.obtenerOCrear("alumno1", tema.getId())).thenReturn(progreso);

        service.aplicarCorreccion(new EvaluacionCorregida("e1", 20,
                List.of(new RespuestaCorregidaEvento("p1", true, 10), new RespuestaCorregidaEvento("p2", true, 10))));

        assertThat(e.getEstado()).isEqualTo(EstadoEvaluacionEnum.FINALIZADA);
        assertThat(e.getPuntajeTotal()).isEqualTo(20);
        assertThat(e.getAprobada()).isTrue();
        assertThat(progreso.getIntentos()).isEqualTo(1);
        assertThat(progreso.getNivelDominio()).isEqualTo(1.0);
        assertThat(progreso.categoriaNivel()).isEqualTo(NivelDominioEnum.ALTO);
        verify(progresoService).guardar(progreso);
    }

    @Test
    @DisplayName("Un resultado con menos del 70% deja la evaluación desaprobada")
    void aplicarCorreccionDesaprobada() {
        Evaluacion e = entregadaConDosRespuestas();

        service.aplicarCorreccion(new EvaluacionCorregida("e1", 20,
                List.of(new RespuestaCorregidaEvento("p1", true, 10), new RespuestaCorregidaEvento("p2", false, 0))));

        assertThat(e.getAprobada()).isFalse();
        assertThat(e.getPuntajeTotal()).isEqualTo(10);
    }

    @Test
    @DisplayName("Es idempotente: un mensaje duplicado no se vuelve a aplicar")
    void aplicarCorreccionIdempotente() {
        Evaluacion e = entregadaConDosRespuestas();
        EvaluacionCorregida resultado = new EvaluacionCorregida("e1", 20,
                List.of(new RespuestaCorregidaEvento("p1", true, 10), new RespuestaCorregidaEvento("p2", true, 10)));

        service.aplicarCorreccion(resultado);
        service.aplicarCorreccion(resultado);

        assertThat(e.getEstado()).isEqualTo(EstadoEvaluacionEnum.FINALIZADA);
        verify(evaluacionRepository, times(1)).save(e);
        verify(progresoService, times(1)).guardar(any(ProgresoTema.class));
    }

    @Test
    @DisplayName("Ignora el resultado si la evaluación todavía no fue entregada")
    void aplicarCorreccionSinEntregar() {
        Evaluacion e = evaluacionEnProceso("p1");
        when(evaluacionRepository.findById("e1")).thenReturn(Optional.of(e));

        service.aplicarCorreccion(new EvaluacionCorregida("e1", 5,
                List.of(new RespuestaCorregidaEvento("p1", true, 5))));

        assertThat(e.getEstado()).isEqualTo(EstadoEvaluacionEnum.EN_PROCESO);
        verify(evaluacionRepository, never()).save(any());
    }
}
