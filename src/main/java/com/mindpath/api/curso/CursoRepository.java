package com.mindpath.api.curso;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface CursoRepository extends MongoRepository<Curso, String> {
    List<Curso> findByDocenteId(String docenteId);
}
