package com.mindpath.api.inscripcion;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mindpath.api.inscripcion.dto.InscripcionRequest;
import com.mindpath.api.inscripcion.dto.InscripcionResponse;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/inscripciones")
@RequiredArgsConstructor
public class InscripcionController {
    private final InscripcionService inscripcionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InscripcionResponse inscribir(@Valid @RequestBody InscripcionRequest req) {
        return InscripcionResponse.de(inscripcionService.inscribir(req));
    }

    @GetMapping("/{id}")
    public InscripcionResponse obtener(@PathVariable String id) {
        return InscripcionResponse.de(inscripcionService.obtener(id));
    }

    @GetMapping
    public List<InscripcionResponse> listar(@RequestParam(required = false) String alumnoId,
            @RequestParam(required = false) String cursoId) {
        return inscripcionService.listar(alumnoId, cursoId).stream()
                .map(InscripcionResponse::de)
                .toList();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Baja lógica: la inscripción pasa a estado BAJA y no se borra")
    public void darDeBaja(@PathVariable String id) {
        inscripcionService.darDeBaja(id);
    }
}
