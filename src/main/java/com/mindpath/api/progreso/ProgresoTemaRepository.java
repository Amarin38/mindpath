package com.mindpath.api.progreso;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProgresoTemaRepository extends MongoRepository<ProgresoTema, String> {
    Optional<ProgresoTema> findByAlumnoIdAndTemaId(String alumnoId, String temaId);

    List<ProgresoTema> findByAlumnoId(String alumnoId);

    List<ProgresoTema> findByAlumnoIdAndTemaIdIn(String alumnoId, Collection<String> temaIds);

    List<ProgresoTema> findByTemaId(String temaId);
}
