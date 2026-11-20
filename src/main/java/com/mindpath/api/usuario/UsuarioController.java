package com.mindpath.api.usuario;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mindpath.api.usuario.dto.AlumnoRequest;
import com.mindpath.api.usuario.dto.DocenteRequest;
import com.mindpath.api.usuario.dto.UsuarioResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class UsuarioController {
    private final UsuarioService usuarioService;

    @PostMapping("/alumnos")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crearAlumno(@Valid @RequestBody AlumnoRequest req) {
        return UsuarioResponse.de(usuarioService.crearAlumno(req));
    }

    @PostMapping("/docentes")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crearDocente(@Valid @RequestBody DocenteRequest req) {
        return UsuarioResponse.de(usuarioService.crearDocente(req));
    }

    @GetMapping("/usuarios/{id}")
    public UsuarioResponse obtener(@PathVariable String id) {
        return UsuarioResponse.de(usuarioService.obtener(id));
    }

    @GetMapping("/usuarios")
    public List<UsuarioResponse> listar() {
        return usuarioService.listar().stream().map(UsuarioResponse::de).toList();
    }
}
