package com.mindpath.api.evaluacion;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.mindpath.api.common.exception.ResourceNotFoundException;
import com.mindpath.api.config.KafkaConfig;
import com.mindpath.api.curso.CursoService;
import com.mindpath.api.curso.Tema;
import com.mindpath.api.evaluacion.dto.EvaluacionRequest;
import com.mindpath.api.evaluacion.dto.RespuestaRequest;
import com.mindpath.api.evaluacion.enums.EstadoEvaluacionEnum;
import com.mindpath.api.evaluacion.evento.EvaluacionCorregida;
import com.mindpath.api.evaluacion.evento.EvaluacionEntregada;
import com.mindpath.api.evaluacion.evento.PreguntaEvento;
import com.mindpath.api.evaluacion.evento.RespuestaCorregidaEvento;
import com.mindpath.api.evaluacion.evento.RespuestaEvento;
import com.mindpath.api.inscripcion.InscripcionRepository;
import com.mindpath.api.inscripcion.enums.EstadoInscripcionEnum;
import com.mindpath.api.pregunta.Pregunta;
import com.mindpath.api.pregunta.PreguntaRepository;
import com.mindpath.api.progreso.ProgresoService;
import com.mindpath.api.progreso.ProgresoTema;
import com.mindpath.api.usuario.UsuarioService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluacionService {
    public static final int MAX_INTENTOS = 3;
    public static final double PUNTAJE_MINIMO_TEMA = 10;

    private final EvaluacionRepository evaluacionRepository;
    private final PreguntaRepository preguntaRepository;
    private final InscripcionRepository inscripcionRepository;
    private final CursoService cursoService;
    private final UsuarioService usuarioService;
    private final ProgresoService progresoService;
    private final KafkaTemplate<String, EvaluacionEntregada> kafkaTemplate;

    public Evaluacion iniciar(EvaluacionRequest req) {
        usuarioService.obtenerAlumno(req.alumnoId());
        Tema tema = cursoService.obtener(req.cursoId()).getTema(req.temaId());

        if (!inscripcionRepository.existsByAlumnoIdAndCursoIdAndEstado(
                req.alumnoId(), req.cursoId(), EstadoInscripcionEnum.ACTIVA))
            throw new IllegalStateException("El alumno no tiene una inscripción activa en el curso.");

        Set<String> aprobados = progresoService.temasAprobados(req.alumnoId(), tema.getPrerrequisitos());
        if (!tema.estaDesbloqueadoPara(aprobados))
            throw new IllegalStateException("El tema todavía no está desbloqueado para el alumno.");

        List<Pregunta> delTema = preguntaRepository.findByTemaId(tema.getId());
        if (delTema.isEmpty())
            throw new IllegalStateException("El tema no tiene preguntas.");
        double puntosTema = delTema.stream().mapToDouble(Pregunta::getPuntaje).sum();
        if (puntosTema < PUNTAJE_MINIMO_TEMA)
            throw new IllegalStateException("El tema suma " + puntosTema + " puntos en preguntas y necesita al menos "
                    + (int) PUNTAJE_MINIMO_TEMA + ".");

        if (evaluacionRepository.existsByAlumnoIdAndTemaIdAndEstado(
                req.alumnoId(), tema.getId(), EstadoEvaluacionEnum.EN_PROCESO)
                || evaluacionRepository.existsByAlumnoIdAndTemaIdAndEstado(
                        req.alumnoId(), tema.getId(), EstadoEvaluacionEnum.ENTREGADO))
            throw new IllegalStateException("El alumno ya tiene una evaluación en curso o en corrección para este tema.");

        if (evaluacionRepository.existsByAlumnoIdAndTemaIdAndAprobadaTrue(req.alumnoId(), tema.getId()))
            throw new IllegalStateException("El alumno ya aprobó este tema.");

        if (evaluacionRepository.countByAlumnoIdAndTemaId(req.alumnoId(), tema.getId()) >= MAX_INTENTOS)
            throw new IllegalStateException("El alumno alcanzó el máximo de " + MAX_INTENTOS + " intentos para este tema.");

        ProgresoTema progreso = progresoService.obtenerOCrear(req.alumnoId(), tema.getId());
        progreso.desbloquear();
        progresoService.guardar(progreso);

        Evaluacion evaluacion = new Evaluacion(req.alumnoId(), tema.getId());
        evaluacion.iniciarEvaluacion();
        return evaluacionRepository.save(evaluacion);
    }

    public Evaluacion obtener(String id) {
        return evaluacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evaluación " + id));
    }

    public List<Evaluacion> listar(String alumnoId, String temaId) {
        List<Evaluacion> base = alumnoId != null
                ? evaluacionRepository.findByAlumnoId(alumnoId)
                : temaId != null
                        ? evaluacionRepository.findByTemaId(temaId)
                        : evaluacionRepository.findAll();

        return base.stream()
                .filter(e -> alumnoId == null || e.getAlumnoId().equals(alumnoId))
                .filter(e -> temaId == null || e.getTemaId().equals(temaId))
                .toList();
    }

    public List<Pregunta> preguntas(String evaluacionId) {
        return preguntaRepository.findByTemaId(obtener(evaluacionId).getTemaId());
    }

    public Respuesta responder(String evaluacionId, RespuestaRequest req) {
        Evaluacion evaluacion = obtener(evaluacionId);
        Pregunta pregunta = preguntaRepository.findById(req.preguntaId())
                .orElseThrow(() -> new ResourceNotFoundException("Pregunta " + req.preguntaId()));

        if (!pregunta.getTemaId().equals(evaluacion.getTemaId()))
            throw new IllegalArgumentException("La pregunta no pertenece al tema de la evaluación.");

        Respuesta respuesta = new Respuesta(req.preguntaId(), req.contenido(), req.tiempoSegundos());
        evaluacion.agregarRespuesta(respuesta);
        evaluacionRepository.save(evaluacion);
        return respuesta;
    }

    public Evaluacion entregar(String evaluacionId) {
        Evaluacion evaluacion = obtener(evaluacionId);
        evaluacion.terminarEvaluacion(Instant.now());
        evaluacionRepository.save(evaluacion);

        Double nivelActual = progresoService
                .obtenerOCrear(evaluacion.getAlumnoId(), evaluacion.getTemaId())
                .getNivelDominio();

        List<PreguntaEvento> preguntas = preguntaRepository.findByTemaId(evaluacion.getTemaId()).stream()
                .map(PreguntaEvento::de)
                .toList();

        List<RespuestaEvento> respuestas = evaluacion.getRespuestas().stream()
                .map(r -> new RespuestaEvento(r.getPreguntaId(), r.getContenido()))
                .toList();

        kafkaTemplate.send(KafkaConfig.EVALUACION_ENTREGADA, evaluacion.getId(),
                new EvaluacionEntregada(evaluacion.getId(), nivelActual, preguntas, respuestas));

        log.info("[EVT] >> publicado evaluacion.entregada  eval={}  ({} preguntas, {} respuestas)",
                evaluacion.getId(), preguntas.size(), respuestas.size());

        return evaluacion;
    }

    public void aplicarCorreccion(EvaluacionCorregida resultado) {
        log.info("[EVT] << recibido evaluacion.corregida  eval={}", resultado.evaluacionId());
        Evaluacion evaluacion = obtener(resultado.evaluacionId());
        if (evaluacion.getEstado() != EstadoEvaluacionEnum.ENTREGADO) {
            log.warn("[EVT] ignorado: la evaluación {} ya estaba {}", evaluacion.getId(), evaluacion.getEstado());
            return;
        }

        Map<String, Pregunta> porId = preguntaRepository.findByTemaId(evaluacion.getTemaId()).stream()
                .collect(Collectors.toMap(Pregunta::getId, Function.identity()));

        List<Pregunta> corregidas = new ArrayList<>();
        for (RespuestaCorregidaEvento corregida : resultado.respuestas()) {
            Respuesta respuesta = evaluacion.getRespuestas().stream()
                    .filter(r -> r.getPreguntaId().equals(corregida.preguntaId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "No hay respuesta para la pregunta " + corregida.preguntaId()));

            respuesta.corregir(corregida.correcta(), corregida.puntajeObtenido(), true);

            Pregunta pregunta = porId.get(corregida.preguntaId());
            if (pregunta != null) {
                pregunta.registrarRespuesta(corregida.correcta());
                corregidas.add(pregunta);
            }
        }

        evaluacion.finalizarEvaluacion(resultado.puntajeMaximo());

        ProgresoTema progreso = progresoService.obtenerOCrear(evaluacion.getAlumnoId(), evaluacion.getTemaId());
        progreso.registrarEvaluacion(evaluacion.getPuntajeTotal(), resultado.puntajeMaximo(),
                evaluacion.getFechaEntrega());

        evaluacionRepository.save(evaluacion);
        progresoService.guardar(progreso);
        preguntaRepository.saveAll(corregidas);

        log.info("[EVT] evaluación {} FINALIZADA: {}/{} puntos, aprobada={}, nivel de dominio={} ({})",
                evaluacion.getId(), evaluacion.getPuntajeTotal(), resultado.puntajeMaximo(),
                evaluacion.getAprobada(), progreso.getNivelDominio(), progreso.categoriaNivel());
    }
}
