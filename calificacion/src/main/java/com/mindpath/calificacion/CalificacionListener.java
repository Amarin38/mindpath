package com.mindpath.calificacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.mindpath.calificacion.dto.CalificacionRequest;
import com.mindpath.calificacion.dto.CalificacionResponse;

@Component
public class CalificacionListener {
    private static final Logger log = LoggerFactory.getLogger(CalificacionListener.class);

    private final Calificador calificador;
    private final KafkaTemplate<String, CalificacionResponse> kafkaTemplate;

    public CalificacionListener(Calificador calificador, KafkaTemplate<String, CalificacionResponse> kafkaTemplate) {
        this.calificador = calificador;
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = "evaluacion.entregada")
    public void alEntregar(CalificacionRequest req) {
        log.info("[EVT] << recibido evaluacion.entregada  eval={}  ({} preguntas, {} respuestas)",
                req.evaluacionId(), req.preguntas().size(), req.respuestas().size());

        CalificacionResponse resultado = calificador.calificar(req);
        kafkaTemplate.send("evaluacion.corregida", req.evaluacionId(), resultado);

        long correctas = resultado.respuestas().stream().filter(r -> r.correcta()).count();
        log.info("[EVT] >> publicado evaluacion.corregida  eval={}  ({}/{} correctas, nivel={} {})",
                req.evaluacionId(), correctas, resultado.respuestas().size(),
                Math.round(resultado.nivelDominio() * 100) / 100.0, resultado.categoriaNivel());
    }
}
