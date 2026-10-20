package com.mindpath.api.curso;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mindpath.api.curso.dto.CursoRequest;
import com.mindpath.api.curso.dto.CursoResponse;

import org.springframework.web.bind.annotation.RequestBody;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/cursos")
@RequiredArgsConstructor
public class CursoController {
    private final CursoService cursoService;

    @GetMapping("/{id}")
    public CursoResponse obtener(@PathVariable String id) {
        return CursoResponse.de(cursoService.obtener(id),
                cursoService.cantidadInscriptos(id));
    }

    @GetMapping
    public List<CursoResponse> listar(@RequestParam(required = false) String docenteId) {
        List<Curso> curso = docenteId != null
                ? cursoService.listarPorDocente(docenteId)
                : cursoService.listarTodos();
        return curso.stream().map(CursoResponse::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CursoResponse crear(@RequestParam String docenteId, @Valid @RequestBody CursoRequest req) {
        return CursoResponse.de(cursoService.crear(docenteId, req));
    }

    @PutMapping("/{id}")
    public CursoResponse actualizar(@PathVariable String id, @Valid @RequestBody CursoRequest req) {
        return CursoResponse.de(cursoService.actualizar(id, req));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable String id) {
        cursoService.eliminar(id);
    }
}
