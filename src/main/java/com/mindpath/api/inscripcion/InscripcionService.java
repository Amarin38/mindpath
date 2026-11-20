package com.mindpath.api.inscripcion;

import java.util.List;

import org.springframework.stereotype.Service;

import com.mindpath.api.common.exception.ResourceNotFoundException;
import com.mindpath.api.curso.CursoService;
import com.mindpath.api.inscripcion.dto.InscripcionRequest;
import com.mindpath.api.inscripcion.enums.EstadoInscripcionEnum;
import com.mindpath.api.usuario.UsuarioService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InscripcionService {
    private final InscripcionRepository inscripcionRepository;
    private final UsuarioService usuarioService;
    private final CursoService cursoService;

    public Inscripcion inscribir(InscripcionRequest req) {
        usuarioService.obtenerAlumno(req.alumnoId());
        cursoService.obtener(req.cursoId());

        if (inscripcionRepository.existsByAlumnoIdAndCursoIdAndEstado(
                req.alumnoId(), req.cursoId(), EstadoInscripcionEnum.ACTIVA))
            throw new IllegalStateException("El alumno ya está inscripto en el curso.");

        return inscripcionRepository.save(new Inscripcion(req.alumnoId(), req.cursoId()));
    }

    public Inscripcion obtener(String id) {
        return inscripcionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Inscripción " + id));
    }

    public List<Inscripcion> listar(String alumnoId, String cursoId) {
        List<Inscripcion> base = alumnoId != null
                ? inscripcionRepository.findByAlumnoId(alumnoId)
                : cursoId != null
                        ? inscripcionRepository.findByCursoId(cursoId)
                        : inscripcionRepository.findAll();

        return base.stream()
                .filter(i -> alumnoId == null || i.getAlumnoId().equals(alumnoId))
                .filter(i -> cursoId == null || i.getCursoId().equals(cursoId))
                .toList();
    }

    public void darDeBaja(String id) {
        Inscripcion inscripcion = obtener(id);

        inscripcion.darDeBaja();
        inscripcionRepository.save(inscripcion);
    }
}
