package com.mindpath.api.curso;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mindpath.api.curso.TemaService;
import com.mindpath.api.curso.dto.RecursoRequest;
import com.mindpath.api.curso.dto.TemaRequest;
import com.mindpath.api.curso.dto.TemaResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/cursos/{cursoId}/temas")
@RequiredArgsConstructor
public class TemaController {
    private final TemaService temaService;
    
    @GetMapping("/{temaId}")
    public TemaResponse obtener(@PathVariable String cursoId, @PathVariable String temaId) {
        return TemaResponse.de(temaService.obtener(cursoId, temaId));
    }

    @GetMapping
    public List<TemaResponse> listar(@PathVariable String cursoId) {
        return temaService.listar(cursoId).stream().map(TemaResponse::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TemaResponse agregar(@PathVariable String cursoId, @Valid @RequestBody TemaRequest req) {
        return TemaResponse.de(temaService.addTema(cursoId, req));
    }

    @PostMapping("/{temaId}/recursos")
    @ResponseStatus(HttpStatus.CREATED)
    public Recurso agregarRecurso(@PathVariable String cursoId, @PathVariable String temaId,
            @Valid @RequestBody RecursoRequest req) {
        return temaService.addRecurso(cursoId, temaId, req);
    }

    @PutMapping("/{temaId}/prerrequisitos/{prerrequisitoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void agregarPrerrequisito(@PathVariable String cursoId, @PathVariable String temaId,
            @PathVariable String prerrequisitoId) {
        temaService.addPrerrquisito(cursoId, temaId, prerrequisitoId);
    }
}
