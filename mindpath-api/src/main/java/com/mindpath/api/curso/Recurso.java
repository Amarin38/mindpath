package com.mindpath.api.curso;

import org.bson.types.ObjectId;

import com.mindpath.api.curso.enums.TipoRecursoEnum;

public record Recurso(
        String id,
        TipoRecursoEnum tipo,
        String titulo,
        String url,
        int orden) {
    public Recurso(TipoRecursoEnum tipo, String titulo, String url, int orden) {
        this(new ObjectId().toHexString(), tipo, titulo, url, orden);
    }
}
