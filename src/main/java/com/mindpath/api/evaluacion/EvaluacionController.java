package com.mindpath.api.evaluacion;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mindpath.api.evaluacion.dto.EvaluacionRequest;
import com.mindpath.api.evaluacion.dto.EvaluacionResponse;
import com.mindpath.api.evaluacion.dto.RespuestaRequest;
import com.mindpath.api.evaluacion.dto.RespuestaResponse;
import com.mindpath.api.pregunta.dto.PreguntaAlumnoResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/evaluaciones")
@RequiredArgsConstructor
public class EvaluacionController {
    private final EvaluacionService evaluacionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EvaluacionResponse iniciar(@Valid @RequestBody EvaluacionRequest req) {
        return EvaluacionResponse.de(evaluacionService.iniciar(req));
    }

    @GetMapping("/{id}")
    public EvaluacionResponse obtener(@PathVariable String id) {
        return EvaluacionResponse.de(evaluacionService.obtener(id));
    }

    @GetMapping
    public List<EvaluacionResponse> listar(@RequestParam(required = false) String alumnoId,
            @RequestParam(required = false) String temaId) {
        return evaluacionService.listar(alumnoId, temaId).stream()
                .map(EvaluacionResponse::de)
                .toList();
    }

    @GetMapping("/{id}/preguntas")
    public List<PreguntaAlumnoResponse> preguntas(@PathVariable String id) {
        return evaluacionService.preguntas(id).stream()
                .map(PreguntaAlumnoResponse::de)
                .toList();
    }

    @PostMapping("/{id}/respuestas")
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaResponse responder(@PathVariable String id, @Valid @RequestBody RespuestaRequest req) {
        return RespuestaResponse.de(evaluacionService.responder(id, req));
    }

    @PostMapping("/{id}/entrega")
    public EvaluacionResponse entregar(@PathVariable String id) {
        return EvaluacionResponse.de(evaluacionService.entregar(id));
    }
}
