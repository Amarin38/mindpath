package com.mindpath.api.progreso;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mindpath.api.common.enums.NivelDominioEnum;

class ProgresoTemaTest {

    @Test
    @DisplayName("Sin evaluaciones: no tiene nivel y no está aprobado")
    void inicial() {
        ProgresoTema p = new ProgresoTema("alumno1", "tema1");

        assertThat(p.getNivelDominio()).isNull();
        assertThat(p.getIntentos()).isZero();
        assertThat(p.estaAprobado()).isFalse();
        assertThat(p.isDesbloqueado()).isFalse();
    }

    @Test
    @DisplayName("La primera evaluación fija el nivel en el porcentaje obtenido")
    void primeraEvaluacion() {
        ProgresoTema p = new ProgresoTema("alumno1", "tema1");

        p.registrarEvaluacion(8, 10, Instant.now());

        assertThat(p.getNivelDominio()).isCloseTo(0.8, within(1e-9));
        assertThat(p.getIntentos()).isEqualTo(1);
        assertThat(p.getMejorPuntaje()).isEqualTo(8);
        assertThat(p.estaAprobado()).isTrue();
    }

    @Test
    @DisplayName("Las siguientes evaluaciones promedian: 0.4*nuevo + 0.6*previo")
    void promedioPonderado() {
        ProgresoTema p = new ProgresoTema("alumno1", "tema1");
        p.registrarEvaluacion(5, 10, Instant.now());   // 0.5
        p.registrarEvaluacion(10, 10, Instant.now());  // 0.4*1.0 + 0.6*0.5 = 0.7

        assertThat(p.getNivelDominio()).isCloseTo(0.7, within(1e-9));
        assertThat(p.getIntentos()).isEqualTo(2);
        assertThat(p.estaAprobado()).isTrue();
    }

    @Test
    @DisplayName("El mejor puntaje no baja si el siguiente intento es peor")
    void mejorPuntaje() {
        ProgresoTema p = new ProgresoTema("alumno1", "tema1");
        p.registrarEvaluacion(9, 10, Instant.now());
        p.registrarEvaluacion(2, 10, Instant.now());

        assertThat(p.getMejorPuntaje()).isEqualTo(9);
    }

    @Test
    @DisplayName("Con nivel 0.69 no está aprobado")
    void bajoElUmbral() {
        ProgresoTema p = new ProgresoTema("alumno1", "tema1");
        p.registrarEvaluacion(6.9, 10, Instant.now());

        assertThat(p.estaAprobado()).isFalse();
    }

    @Test
    @DisplayName("Desbloquear es idempotente")
    void desbloquear() {
        ProgresoTema p = new ProgresoTema("alumno1", "tema1");
        p.desbloquear();
        p.desbloquear();

        assertThat(p.isDesbloqueado()).isTrue();
    }

    @Test
    @DisplayName("Sin evaluaciones no hay categoría de nivel")
    void categoriaSinEvaluaciones() {
        assertThat(new ProgresoTema("alumno1", "tema1").categoriaNivel()).isNull();
    }

    @Test
    @DisplayName("Categoría de nivel: BAJO < 0.4, MEDIO < 0.7, ALTO desde 0.7")
    void categoriaDeNivel() {
        assertThat(conPrimerPuntaje(3.9).categoriaNivel()).isEqualTo(NivelDominioEnum.BAJO);
        assertThat(conPrimerPuntaje(4.0).categoriaNivel()).isEqualTo(NivelDominioEnum.MEDIO);
        assertThat(conPrimerPuntaje(6.9).categoriaNivel()).isEqualTo(NivelDominioEnum.MEDIO);
        assertThat(conPrimerPuntaje(7.0).categoriaNivel()).isEqualTo(NivelDominioEnum.ALTO);
    }

    @Test
    @DisplayName("Reintentar y mejorar sube la categoría (6/10 y luego 10/10: MEDIO -> ALTO)")
    void categoriaSubeAlMejorar() {
        ProgresoTema p = new ProgresoTema("alumno1", "tema1");
        p.registrarEvaluacion(6, 10, Instant.now());
        assertThat(p.categoriaNivel()).isEqualTo(NivelDominioEnum.MEDIO);

        p.registrarEvaluacion(10, 10, Instant.now());   // 0.4*1.0 + 0.6*0.6 = 0.76
        assertThat(p.categoriaNivel()).isEqualTo(NivelDominioEnum.ALTO);
    }

    private ProgresoTema conPrimerPuntaje(double puntaje) {
        ProgresoTema p = new ProgresoTema("alumno1", "tema1");
        p.registrarEvaluacion(puntaje, 10, Instant.now());
        return p;
    }
}
