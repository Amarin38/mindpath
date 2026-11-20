package com.mindpath.api.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {
    public static final String EVALUACION_ENTREGADA = "evaluacion.entregada";
    public static final String EVALUACION_CORREGIDA = "evaluacion.corregida";

    @Bean
    public NewTopic topicEntregada() {
        return TopicBuilder.name(EVALUACION_ENTREGADA).build();
    }

    @Bean
    public NewTopic topicCorregida() {
        return TopicBuilder.name(EVALUACION_CORREGIDA).build();
    }
}
