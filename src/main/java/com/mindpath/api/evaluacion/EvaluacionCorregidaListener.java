package com.mindpath.api.evaluacion;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.mindpath.api.config.KafkaConfig;
import com.mindpath.api.evaluacion.evento.EvaluacionCorregida;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EvaluacionCorregidaListener {
    private final EvaluacionService evaluacionService;

    @KafkaListener(topics = KafkaConfig.EVALUACION_CORREGIDA, groupId = "mindpath-api")
    public void alCorregir(EvaluacionCorregida resultado) {
        evaluacionService.aplicarCorreccion(resultado);
    }
}
