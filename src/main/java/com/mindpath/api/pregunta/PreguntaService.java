package com.mindpath.api.pregunta;

import java.util.List;

import org.springframework.stereotype.Service;

import com.mindpath.api.common.exception.ResourceNotFoundException;
import com.mindpath.api.curso.TemaService;
import com.mindpath.api.pregunta.dto.PreguntaRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PreguntaService {
    private final PreguntaRepository preguntaRepository;
    private final TemaService temaService;

    public Pregunta crear(String cursoId, String temaId, PreguntaRequest req) {
        temaService.obtener(cursoId, temaId);

        Pregunta pregunta = switch (req.tipo()) {
            case OPCION_MULTIPLE -> {
                if (req.opciones() == null || req.opcionCorrecta() == null)
                    throw new IllegalArgumentException(
                            "Una pregunta de opción múltiple necesita opciones y opcionCorrecta.");

                yield new PreguntaOpcionMultiple(temaId, req.enunciado(), req.dificultad(),
                        req.puntaje(), req.opciones(), req.opcionCorrecta());
            }
            case ABIERTA -> {
                if (req.respuestaEsperada() == null || req.palabrasClave() == null
                        || req.palabrasClave().isEmpty())
                    throw new IllegalArgumentException(
                            "Una pregunta abierta necesita respuestaEsperada y palabrasClave.");

                yield new PreguntaAbierta(temaId, req.enunciado(), req.dificultad(),
                        req.puntaje(), req.respuestaEsperada(), req.palabrasClave());
            }
        };

        return preguntaRepository.save(pregunta);
    }

    public Pregunta obtener(String id) {
        return preguntaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pregunta " + id));
    }

    public List<Pregunta> listarPorTema(String cursoId, String temaId) {
        temaService.obtener(cursoId, temaId);
        return preguntaRepository.findByTemaId(temaId);
    }
}
