package com.queseria.calidadleche.application.port;

import com.queseria.calidadleche.domain.model.MuestraLeche;
import com.queseria.calidadleche.domain.service.EvaluacionCalidadService;
import reactor.core.publisher.Mono;

public interface AnaliticaRepository {
  Mono<Void> saveAnalisis(
      MuestraLeche muestra,
      EvaluacionCalidadService.EvaluacionMuestra evaluacion
  );
}
