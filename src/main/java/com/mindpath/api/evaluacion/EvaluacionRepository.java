package com.mindpath.api.evaluacion;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.mindpath.api.evaluacion.enums.EstadoEvaluacionEnum;

public interface EvaluacionRepository extends MongoRepository<Evaluacion, String> {
    List<Evaluacion> findByTemaId(String temaId);

    List<Evaluacion> findByAlumnoId(String alumnoId);

    long countByAlumnoIdAndTemaId(String alumnoId, String temaId);

    boolean existsByAlumnoIdAndTemaIdAndAprobadaTrue(String alumnoId, String temaId);

    boolean existsByAlumnoIdAndTemaIdAndEstado(String alumnoId, String temaId, EstadoEvaluacionEnum estado);
}
