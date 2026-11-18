package com.mindpath.api.curso;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;

import com.mindpath.api.common.enums.DificultadTemaEnum;
import com.mindpath.api.curso.enums.TipoRecursoEnum;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tema {
    private String id;
    private String nombre;
    private String descripcion;
    private DificultadTemaEnum dificultad;
    private int orden;

    private List<String> prerrequisitos = new ArrayList<>();
    private List<Recurso> recursos = new ArrayList<>();

    public Tema(String nombre, String descripcion, DificultadTemaEnum dificultad, int orden) {
        this.id = new ObjectId().toHexString();
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.dificultad = dificultad;
        this.orden = orden;
    }

    public void addPrerrequisito(String temaId) {
        if (!prerrequisitos.contains(temaId))
            this.prerrequisitos.add(temaId);
    }

    public Recurso addRecurso(TipoRecursoEnum tipo, String titulo, String url) {
        Recurso recurso = new Recurso(tipo, titulo, url, recursos.size() + 1);
        recursos.add(recurso);
        return recurso;
    }

    public List<String> getPrerrequisitos() {
        return List.copyOf(prerrequisitos);
    }

    public List<Recurso> getRecursos() {
        return List.copyOf(recursos);
    }
}
