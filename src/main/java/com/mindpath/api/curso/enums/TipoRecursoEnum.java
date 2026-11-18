package com.mindpath.api.curso.enums;

public enum TipoRecursoEnum {
    VIDEO("Video"),
    AUDIO("Audio"),
    PDF("PDF"),
    INFOGRAFIA("Infografía"),
    LINK("Link"),
    EJERCICIO("Ejercicio"),
    PRESENTACION("Presentación");

    private final String etiqueta;

    TipoRecursoEnum(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
