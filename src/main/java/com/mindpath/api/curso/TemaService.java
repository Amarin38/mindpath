package com.mindpath.api.curso;

import java.util.List;

import org.springframework.stereotype.Service;

import com.mindpath.api.curso.dto.RecursoRequest;
import com.mindpath.api.curso.dto.TemaRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TemaService {
    private final CursoService cursoService;
    private final CursoRepository cursoRepository;

    public Tema addTema(String cursoId, TemaRequest req) {
        Curso curso = cursoService.obtener(cursoId);
        Tema tema = curso.addTema(req.nombre(), req.descripcion(), req.dificultad());
        cursoRepository.save(curso);
        return tema;
    }

    public List<Tema> listar(String cursoId) {
        return cursoService.obtener(cursoId).getTemas();
    }

    public Tema obtener(String cursoId, String temaId) {
        return cursoService.obtener(cursoId).getTema(temaId);
    }

    public Recurso addRecurso(String cursoId, String temaId, RecursoRequest req) {
        Curso curso = cursoService.obtener(cursoId);
        Recurso recurso = curso.getTema(temaId)
                .addRecurso(req.tipo(), req.titulo(), req.url());
        cursoRepository.save(curso);
        return recurso;
    }

    public void addPrerrquisito(String cursoId, String temaId, String prerrequisitoId) {
        Curso curso = cursoService.obtener(cursoId);
        curso.addPrerrequisito(temaId, prerrequisitoId);
        cursoRepository.save(curso);
    }
}
