package com.queseria.calidadleche.application.usecase;

import com.queseria.calidadleche.application.port.AnaliticaRepository;
import com.queseria.calidadleche.application.service.AnaliticaCalculos;
import com.queseria.calidadleche.domain.model.MuestraLeche;
import com.queseria.calidadleche.domain.repo.MuestraRepository;
import com.queseria.calidadleche.domain.service.EvaluacionCalidadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class RegistrarMuestraConEvaluacionUseCase {
  private static final Logger log = LoggerFactory.getLogger(RegistrarMuestraConEvaluacionUseCase.class);
  private static final Duration MONGO_TIMEOUT = Duration.ofSeconds(5);

  private final MuestraRepository muestraRepo;
  private final EvaluacionCalidadService evaluacionService;
  private final AnaliticaRepository analiticaRepository;

  public RegistrarMuestraConEvaluacionUseCase(
      MuestraRepository muestraRepo,
      EvaluacionCalidadService evaluacionService,
      AnaliticaRepository analiticaRepository
  ) {
    this.muestraRepo = muestraRepo;
    this.evaluacionService = evaluacionService;
    this.analiticaRepository = analiticaRepository;
  }

  public Mono<Resultado> ejecutar(MuestraLeche muestra) {
    return muestraRepo.save(muestra)
        .flatMap(saved -> {
          var evaluacion = AnaliticaCalculos.evaluar(saved, evaluacionService);

          return analiticaRepository.saveAnalisis(saved, evaluacion)
              .timeout(MONGO_TIMEOUT)
              .onErrorResume(error -> {
                log.warn(
                    "La muestra {} quedó guardada en PostgreSQL, pero su analítica no pudo persistirse en Mongo: {}",
                    saved.id(),
                    error.toString()
                );
                log.debug("Detalle del fallo de Mongo para la muestra {}", saved.id(), error);
                return Mono.empty();
              })
              .thenReturn(new Resultado(saved, evaluacion));
        });
  }

  public record Resultado(
      MuestraLeche muestra,
      EvaluacionCalidadService.EvaluacionMuestra evaluacion
  ) {}
}
