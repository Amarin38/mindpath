package com.mindpath.api.curso;

import com.mindpath.api.common.exception.ResourceNotFoundException;
import com.mindpath.api.common.enums.DificultadTemaEnum;
import com.mindpath.api.curso.Tema;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Document(collection = "cursos")
public class Curso {
    @Id
    private String id;

    @Indexed
    private String docenteId;

    private String titulo;
    private String descripcion;
    private Instant fechaCreacion;

    private List<Tema> temas = new ArrayList<>();

    public Curso(String titulo, String descripcion, String docenteId) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.docenteId = docenteId;
        this.fechaCreacion = Instant.now();
    }

    public Tema addTema(String nombre, String descripcion, DificultadTemaEnum dificultad) {
        Tema tema = new Tema(nombre, descripcion, dificultad, temas.size() + 1);
        temas.add(tema);
        return tema;
    }

    public void addPrerrequisito(String temaId, String prerrequisitoId) {
        Tema tema = getTema(temaId);
        getTema(prerrequisitoId);

        if (alcanza(prerrequisitoId, temaId, new HashSet<>()))
            throw new IllegalArgumentException("Dependencia circular entre temas");

        tema.addPrerrequisito(prerrequisitoId);
    }

    public Tema getTema(String temaId) {
        return temas.stream()
                .filter(t -> t.getId().equals(temaId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Tema " + temaId));
    }

    public List<Tema> getTemas() {
        return List.copyOf(temas);
    }

    private boolean alcanza(String origen, String destino, Set<String> visitados) {
        if (origen.equals(destino))
            return true;

        if (!visitados.add(origen))
            return false;

        return getTema(origen).getPrerrequisitos().stream()
                .anyMatch(p -> alcanza(p, destino, visitados));
    }

    public void actualizar(String titulo, String descripcion) {
        this.titulo = titulo;
        this.descripcion = descripcion;
    }

}
