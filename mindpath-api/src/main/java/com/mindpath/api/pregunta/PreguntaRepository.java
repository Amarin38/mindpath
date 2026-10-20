package com.mindpath.api.pregunta;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface PreguntaRepository extends MongoRepository<Pregunta, String> {
    List<Pregunta> findByTemaId(String temaId);
}
