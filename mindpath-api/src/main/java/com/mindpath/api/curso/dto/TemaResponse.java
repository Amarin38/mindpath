package com.mindpath.api.curso.dto;

import java.util.List;

import com.mindpath.api.common.enums.DificultadTemaEnum;
import com.mindpath.api.curso.Recurso;
import com.mindpath.api.curso.Tema;

public record TemaResponse(
        String id,
        String nombre,
        String descripcion,
        DificultadTemaEnum dificultad,
        int orden,
        List<String> prerrequisitos,
        List<Recurso> recursos) {

    public static TemaResponse de(Tema tema) {
        return new TemaResponse(
                tema.getId(),
                tema.getNombre(),
                tema.getDescripcion(),
                tema.getDificultad(),
                tema.getOrden(),
                tema.getPrerrequisitos(),
                tema.getRecursos());
    }
}
