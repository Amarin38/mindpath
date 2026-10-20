package com.mindpath.api.progreso;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProgresoTemaRepository extends MongoRepository<ProgresoTema, String> {
    List<ProgresoTema> findByAlumnoIdAndTemaId(String alumnoId, String temaId);

    List<ProgresoTema> findByTemaId(String temaId);
}
