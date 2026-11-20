package com.mindpath.api.pregunta;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mindpath.api.pregunta.dto.PreguntaRequest;
import com.mindpath.api.pregunta.dto.PreguntaResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/cursos/{cursoId}/temas/{temaId}/preguntas")
@RequiredArgsConstructor
public class PreguntaController {
    private final PreguntaService preguntaService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PreguntaResponse crear(@PathVariable String cursoId, @PathVariable String temaId,
            @Valid @RequestBody PreguntaRequest req) {
        return PreguntaResponse.de(preguntaService.crear(cursoId, temaId, req));
    }

    @GetMapping
    public List<PreguntaResponse> listar(@PathVariable String cursoId, @PathVariable String temaId) {
        return preguntaService.listarPorTema(cursoId, temaId).stream()
                .map(PreguntaResponse::de)
                .toList();
    }
}
