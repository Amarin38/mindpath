package com.mindpath.api.inscripcion;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.mindpath.api.inscripcion.enums.EstadoInscripcionEnum;

public interface InscripcionRepository extends MongoRepository<Inscripcion, String> {
    List<Inscripcion> findByAlumnoId(String alumnoId);

    List<Inscripcion> findByCursoId(String cursoId);

    long countByCursoIdAndEstado(String cursoId, EstadoInscripcionEnum estado);
}
