package com.queseria.calidadleche.application.usecase;

import com.queseria.calidadleche.application.model.AnaliticaMuestraConsulta;
import com.queseria.calidadleche.application.port.AnaliticaRepository;
import com.queseria.calidadleche.application.port.AnaliticaConsultaRepository;
import com.queseria.calidadleche.application.service.AnaliticaCalculos;
import com.queseria.calidadleche.domain.repo.MuestraRepository;
import com.queseria.calidadleche.domain.service.EvaluacionCalidadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.time.Duration;

public class BuscarAnaliticaPorMuestraUseCase {
  private static final Logger log = LoggerFactory.getLogger(BuscarAnaliticaPorMuestraUseCase.class);
  private static final Duration MONGO_TIMEOUT = Duration.ofSeconds(5);

  private final AnaliticaConsultaRepository analiticaConsultaRepository;
  private final AnaliticaRepository analiticaRepository;
  private final MuestraRepository muestraRepository;
  private final EvaluacionCalidadService evaluacionService;

  public BuscarAnaliticaPorMuestraUseCase(
      AnaliticaConsultaRepository analiticaConsultaRepository,
      AnaliticaRepository analiticaRepository,
      MuestraRepository muestraRepository,
      EvaluacionCalidadService evaluacionService
  ) {
    this.analiticaConsultaRepository = analiticaConsultaRepository;
    this.analiticaRepository = analiticaRepository;
    this.muestraRepository = muestraRepository;
    this.evaluacionService = evaluacionService;
  }

  public Mono<AnaliticaMuestraConsulta> execute(Long sampleId) {
    return analiticaConsultaRepository.buscarUltimaPorMuestra(sampleId)
        .timeout(MONGO_TIMEOUT)
        .switchIfEmpty(Mono.defer(() -> recalcularDesdePostgres(sampleId, true)))
        .onErrorResume(error -> {
          log.warn(
              "No fue posible consultar Mongo para la muestra {}; se recalculará desde PostgreSQL: {}",
              sampleId,
              error.toString()
          );
          log.debug("Detalle del fallo al consultar Mongo para la muestra {}", sampleId, error);
          return recalcularDesdePostgres(sampleId, false);
        });
  }

  private Mono<AnaliticaMuestraConsulta> recalcularDesdePostgres(Long sampleId, boolean persistirEnMongo) {
    return muestraRepository.findById(sampleId)
        .flatMap(muestra -> {
          var evaluacion = AnaliticaCalculos.evaluar(muestra, evaluacionService);
          var temporal = AnaliticaCalculos.consultaTemporal(muestra, evaluacion);

          if (!persistirEnMongo) return Mono.just(temporal);

          return analiticaRepository.saveAnalisis(muestra, evaluacion)
              .timeout(MONGO_TIMEOUT)
              .then(analiticaConsultaRepository.buscarUltimaPorMuestra(sampleId).timeout(MONGO_TIMEOUT))
              .switchIfEmpty(Mono.just(temporal))
              .onErrorResume(error -> {
                log.warn(
                    "La analítica de la muestra {} fue recalculada, pero no pudo almacenarse en Mongo: {}",
                    sampleId,
                    error.toString()
                );
                log.debug("Detalle del fallo al reparar la analítica de la muestra {}", sampleId, error);
                return Mono.just(temporal);
              });
        });
  }
}
