package com.mindpath.api.progreso;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mindpath.api.progreso.dto.ProgresoTemaResponse;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/alumnos/{alumnoId}/progresos")
@RequiredArgsConstructor
public class ProgresoController {
    private final ProgresoService progresoService;

    @GetMapping
    public List<ProgresoTemaResponse> listar(@PathVariable String alumnoId) {
        return progresoService.listar(alumnoId).stream().map(ProgresoTemaResponse::de).toList();
    }

    @GetMapping("/{temaId}")
    public ProgresoTemaResponse obtener(@PathVariable String alumnoId, @PathVariable String temaId) {
        return ProgresoTemaResponse.de(progresoService.obtener(alumnoId, temaId));
    }
}
