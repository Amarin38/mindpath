package com.mindpath.api.curso;

import java.util.List;

import org.springframework.stereotype.Service;

import com.mindpath.api.common.exception.ResourceNotFoundException;
import com.mindpath.api.curso.dto.CursoRequest;
import com.mindpath.api.inscripcion.enums.EstadoInscripcionEnum;
import com.mindpath.api.inscripcion.InscripcionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CursoService {
    private final CursoRepository cursoRepository;
    private final InscripcionRepository inscripcionRepository;

    public Curso crear(String docenteId, CursoRequest req) {
        return cursoRepository.save(new Curso(req.titulo(), req.descripcion(), docenteId));
    }

    public Curso obtener(String id) {
        return cursoRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Curso " + id));
    }

    public List<Curso> listarTodos() {
        return cursoRepository.findAll();
    }

    public List<Curso> listarPorDocente(String docenteId) {
        return cursoRepository.findByDocenteId(docenteId);
    }

    public Curso actualizar(String id, CursoRequest req) {
        Curso curso = obtener(id);

        curso.actualizar(req.titulo(), req.descripcion());
        return cursoRepository.save(curso);
    }

    public void eliminar(String id) {
        Curso curso = obtener(id);

        if (cantidadInscriptos(id) > 0)
            throw new IllegalStateException("El curso tiene inscripciones activas");

        cursoRepository.delete(curso);
    }

    public long cantidadInscriptos(String cursoId) {
        return inscripcionRepository.countByCursoIdAndEstado(cursoId, EstadoInscripcionEnum.ACTIVA);
    }
}
