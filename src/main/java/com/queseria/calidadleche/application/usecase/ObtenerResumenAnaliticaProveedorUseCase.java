package com.queseria.calidadleche.application.usecase;

import com.queseria.calidadleche.application.port.AnaliticaConsultaRepository;
import com.queseria.calidadleche.application.port.AnaliticaRepository;
import com.queseria.calidadleche.application.service.AnaliticaCalculos;
import com.queseria.calidadleche.domain.repo.MuestraRepository;
import com.queseria.calidadleche.domain.service.EvaluacionCalidadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

public class ObtenerResumenAnaliticaProveedorUseCase {
  private static final Logger log = LoggerFactory.getLogger(ObtenerResumenAnaliticaProveedorUseCase.class);
  private static final Duration MONGO_TIMEOUT = Duration.ofSeconds(5);
  private static final int TAMANO_LOTE = 500;
  private static final int CONCURRENCIA_REPARACION = 4;

  private final AnaliticaConsultaRepository analiticaConsultaRepository;
  private final AnaliticaRepository analiticaRepository;
  private final MuestraRepository muestraRepository;
  private final EvaluacionCalidadService evaluacionService;

  public ObtenerResumenAnaliticaProveedorUseCase(
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

  public Mono<Map<String, Object>> execute(Long proveedorId, Instant desde, Instant hasta) {
    OffsetDateTime desdeOffset = OffsetDateTime.ofInstant(desde, ZoneOffset.UTC);
    OffsetDateTime hastaOffset = OffsetDateTime.ofInstant(hasta, ZoneOffset.UTC);

    return muestraRepository.findReferenciasByProveedorAndRango(proveedorId, desdeOffset, hastaOffset)
        .collectList()
        .flatMap(referencias -> {
          if (referencias.isEmpty()) {
            return Mono.just(AnaliticaCalculos.resumenDesdeMuestras(List.of(), evaluacionService));
          }
          return sincronizarYResumir(proveedorId, desde, hasta, referencias)
              .onErrorResume(error -> resumenDesdePostgres(proveedorId, desdeOffset, hastaOffset, error));
        });
  }

  private Mono<Map<String, Object>> sincronizarYResumir(
      Long proveedorId,
      Instant desde,
      Instant hasta,
      List<MuestraRepository.ReferenciaMuestra> referencias
  ) {
    return Flux.fromIterable(referencias)
        .map(MuestraRepository.ReferenciaMuestra::id)
        .buffer(TAMANO_LOTE)
        .concatMap(lote -> analiticaConsultaRepository.buscarMetadatosPorMuestras(lote)
            .timeout(MONGO_TIMEOUT))
        .collectMap(AnaliticaConsultaRepository.MetadataAnalitica::sampleId)
        .flatMap(metadatos -> {
          List<MuestraRepository.ReferenciaMuestra> pendientes = referencias.stream()
              .filter(referencia -> requiereReparacion(referencia, metadatos.get(referencia.id())))
              .toList();

          if (!pendientes.isEmpty()) {
            log.info(
                "Se repararán {} analíticas faltantes o antiguas para el proveedor {}",
                pendientes.size(),
                proveedorId
            );
          }

          return Flux.fromIterable(pendientes)
              .flatMap(this::repararAnalitica, CONCURRENCIA_REPARACION)
              .then(analiticaConsultaRepository.obtenerResumenProveedor(proveedorId, desde, hasta)
                  .timeout(MONGO_TIMEOUT))
              .switchIfEmpty(Mono.just(AnaliticaCalculos.resumenDesdeMuestras(List.of(), evaluacionService)));
        });
  }

  private boolean requiereReparacion(
      MuestraRepository.ReferenciaMuestra referencia,
      AnaliticaConsultaRepository.MetadataAnalitica metadata
  ) {
    if (metadata == null || metadata.fechaMuestra() == null || metadata.estadoGeneral() == null) return true;
    Instant fechaMongo = metadata.fechaMuestra().truncatedTo(ChronoUnit.MILLIS);
    Instant fechaPostgres = referencia.fechaMuestra().toInstant().truncatedTo(ChronoUnit.MILLIS);
    return !fechaMongo.equals(fechaPostgres);
  }

  private Mono<Void> repararAnalitica(MuestraRepository.ReferenciaMuestra referencia) {
    return muestraRepository.findById(referencia.id())
        .flatMap(muestra -> {
          var evaluacion = AnaliticaCalculos.evaluar(muestra, evaluacionService);
          return analiticaRepository.saveAnalisis(muestra, evaluacion).timeout(MONGO_TIMEOUT);
        });
  }

  private Mono<Map<String, Object>> resumenDesdePostgres(
      Long proveedorId,
      OffsetDateTime desde,
      OffsetDateTime hasta,
      Throwable causa
  ) {
    log.warn(
        "Mongo no pudo completar el resumen del proveedor {}; se calculará desde PostgreSQL: {}",
        proveedorId,
        causa.toString()
    );
    log.debug("Detalle del fallo de Mongo para el resumen del proveedor {}", proveedorId, causa);
    return muestraRepository.findByProveedorAndRango(proveedorId, desde, hasta, Integer.MAX_VALUE, 0)
        .collectList()
        .map(muestras -> AnaliticaCalculos.resumenDesdeMuestras(muestras, evaluacionService));
  }
}
