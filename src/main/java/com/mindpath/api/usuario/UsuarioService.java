package com.mindpath.api.usuario;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.mindpath.api.common.exception.ResourceNotFoundException;
import com.mindpath.api.usuario.dto.AlumnoRequest;
import com.mindpath.api.usuario.dto.DocenteRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public Alumno crearAlumno(AlumnoRequest req) {
        validarEmailLibre(req.email());
        return usuarioRepository.save(new Alumno(
                req.email(),
                passwordEncoder.encode(req.password()),
                req.nombre(),
                req.apellido(),
                req.legajo()));
    }

    public Docente crearDocente(DocenteRequest req) {
        validarEmailLibre(req.email());
        return usuarioRepository.save(new Docente(
                req.email(),
                passwordEncoder.encode(req.password()),
                req.nombre(),
                req.apellido(),
                req.especialidad()));
    }

    public Usuario obtener(String id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario " + id));
    }

    public Alumno obtenerAlumno(String id) {
        if (obtener(id) instanceof Alumno alumno)
            return alumno;

        throw new ResourceNotFoundException("Alumno " + id);
    }

    public Docente obtenerDocente(String id) {
        if (obtener(id) instanceof Docente docente)
            return docente;

        throw new ResourceNotFoundException("Docente " + id);
    }

    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    private void validarEmailLibre(String email) {
        if (usuarioRepository.existsByEmail(email))
            throw new IllegalStateException("Ya existe un usuario con ese email.");
    }
}
