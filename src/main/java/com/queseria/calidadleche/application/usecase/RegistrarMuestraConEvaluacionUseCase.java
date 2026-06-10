package com.queseria.calidadleche.application.usecase;

import com.queseria.calidadleche.application.port.AnaliticaRepository;
import com.queseria.calidadleche.domain.model.MuestraLeche;
import com.queseria.calidadleche.domain.repo.MuestraRepository;
import com.queseria.calidadleche.domain.service.EvaluacionCalidadService;
import reactor.core.publisher.Mono;

public class RegistrarMuestraConEvaluacionUseCase {
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
          var evaluacion = evaluacionService.evaluar(
              saved.composicion().grasa(),
              saved.composicion().proteina(),
              saved.fisicoQuimico().densidad(),
              saved.fisicoQuimico().temperaturaC(),
              saved.composicion().solidosTotales(),
              saved.sng(),
              saved.fisicoQuimico().acidezDornic(),
              saved.aguaPct()
          );

          return analiticaRepository.saveAnalisis(saved, evaluacion)
              .thenReturn(new Resultado(saved, evaluacion));
        });
  }

  public record Resultado(
      MuestraLeche muestra,
      EvaluacionCalidadService.EvaluacionMuestra evaluacion
  ) {}
}
