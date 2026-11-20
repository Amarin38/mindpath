package com.mindpath.api.progreso;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.mindpath.api.common.exception.ResourceNotFoundException;
import com.mindpath.api.usuario.UsuarioService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProgresoService {
    private final ProgresoTemaRepository progresoRepository;
    private final UsuarioService usuarioService;

    public ProgresoTema obtener(String alumnoId, String temaId) {
        usuarioService.obtenerAlumno(alumnoId);
        return progresoRepository.findByAlumnoIdAndTemaId(alumnoId, temaId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Progreso del alumno " + alumnoId + " en el tema " + temaId));
    }

    public List<ProgresoTema> listar(String alumnoId) {
        usuarioService.obtenerAlumno(alumnoId);
        return progresoRepository.findByAlumnoId(alumnoId);
    }

    public ProgresoTema obtenerOCrear(String alumnoId, String temaId) {
        return progresoRepository.findByAlumnoIdAndTemaId(alumnoId, temaId)
                .orElseGet(() -> new ProgresoTema(alumnoId, temaId));
    }

    public ProgresoTema guardar(ProgresoTema progreso) {
        return progresoRepository.save(progreso);
    }

    public Set<String> temasAprobados(String alumnoId, Collection<String> temaIds) {
        if (temaIds.isEmpty())
            return Set.of();

        return progresoRepository.findByAlumnoIdAndTemaIdIn(alumnoId, temaIds).stream()
                .filter(ProgresoTema::estaAprobado)
                .map(ProgresoTema::getTemaId)
                .collect(Collectors.toSet());
    }
}
