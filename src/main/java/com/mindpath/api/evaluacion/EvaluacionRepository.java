package com.mindpath.api.evaluacion;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface EvaluacionRepository extends MongoRepository<Evaluacion, String> {
    List<Evaluacion> findByTemaId(String temaId);

    List<Evaluacion> findByAlumnoId(String alumnoId);
}
