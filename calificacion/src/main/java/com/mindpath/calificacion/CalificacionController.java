package com.mindpath.calificacion;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mindpath.calificacion.dto.CalificacionRequest;
import com.mindpath.calificacion.dto.CalificacionResponse;

import jakarta.validation.Valid;

// Sirve para probar desde Swagger.
@RestController
@RequestMapping("/calificaciones")
public class CalificacionController {
    private final Calificador calificador;

    public CalificacionController(Calificador calificador) {
        this.calificador = calificador;
    }

    @PostMapping
    public CalificacionResponse calificar(@Valid @RequestBody CalificacionRequest req) {
        return calificador.calificar(req);
    }
}
