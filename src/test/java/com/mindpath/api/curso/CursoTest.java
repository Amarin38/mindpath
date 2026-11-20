package com.mindpath.api.curso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mindpath.api.common.enums.DificultadTemaEnum;
import com.mindpath.api.common.exception.ResourceNotFoundException;

class CursoTest {
    private Curso curso;
    private Tema a;
    private Tema b;
    private Tema c;

    @BeforeEach
    void setUp() {
        curso = new Curso("Java", "Curso de Java", "docente1");
        a = curso.addTema("A", "primero", DificultadTemaEnum.BAJA);
        b = curso.addTema("B", "segundo", DificultadTemaEnum.MEDIA);
        c = curso.addTema("C", "tercero", DificultadTemaEnum.ALTA);
    }

    @Test
    @DisplayName("Los temas se numeran en orden de alta")
    void ordenDeTemas() {
        assertThat(a.getOrden()).isEqualTo(1);
        assertThat(b.getOrden()).isEqualTo(2);
        assertThat(c.getOrden()).isEqualTo(3);
        assertThat(curso.getTemas()).hasSize(3);
    }

    @Test
    @DisplayName("Agrega un prerrequisito válido")
    void prerrequisitoValido() {
        curso.addPrerrequisito(b.getId(), a.getId());

        assertThat(curso.getTema(b.getId()).getPrerrequisitos()).containsExactly(a.getId());
    }

    @Test
    @DisplayName("Rechaza dependencia circular directa (A requiere B y B requiere A)")
    void cicloDirecto() {
        curso.addPrerrequisito(b.getId(), a.getId());

        assertThatThrownBy(() -> curso.addPrerrequisito(a.getId(), b.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("circular");
    }

    @Test
    @DisplayName("Rechaza dependencia circular transitiva (A -> B -> C -> A)")
    void cicloTransitivo() {
        curso.addPrerrequisito(b.getId(), a.getId());
        curso.addPrerrequisito(c.getId(), b.getId());

        assertThatThrownBy(() -> curso.addPrerrequisito(a.getId(), c.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Un tema no puede ser prerrequisito de sí mismo")
    void autoPrerrequisito() {
        assertThatThrownBy(() -> curso.addPrerrequisito(a.getId(), a.getId()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Tema inexistente lanza ResourceNotFoundException")
    void temaInexistente() {
        assertThatThrownBy(() -> curso.getTema("no-existe")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> curso.addPrerrequisito(a.getId(), "no-existe"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Un tema sin prerrequisitos está siempre desbloqueado")
    void desbloqueadoSinPrerrequisitos() {
        assertThat(a.estaDesbloqueadoPara(Set.of())).isTrue();
    }

    @Test
    @DisplayName("Un tema se desbloquea solo si se aprobaron todos sus prerrequisitos")
    void desbloqueoConPrerrequisitos() {
        curso.addPrerrequisito(c.getId(), a.getId());
        curso.addPrerrequisito(c.getId(), b.getId());
        Tema tema = curso.getTema(c.getId());

        assertThat(tema.estaDesbloqueadoPara(Set.of())).isFalse();
        assertThat(tema.estaDesbloqueadoPara(Set.of(a.getId()))).isFalse();
        assertThat(tema.estaDesbloqueadoPara(Set.of(a.getId(), b.getId()))).isTrue();
    }

    @Test
    @DisplayName("Un prerrequisito repetido no se duplica")
    void prerrequisitoRepetido() {
        curso.addPrerrequisito(b.getId(), a.getId());
        curso.addPrerrequisito(b.getId(), a.getId());

        assertThat(curso.getTema(b.getId()).getPrerrequisitos()).hasSize(1);
    }
}
