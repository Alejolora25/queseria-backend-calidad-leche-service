package com.queseria.calidadleche.infrastructure.persistence.mongo.impl;

import com.queseria.calidadleche.application.port.AnaliticaRepository;
import com.queseria.calidadleche.domain.model.MuestraLeche;
import com.queseria.calidadleche.domain.service.EvaluacionCalidadService;
import com.queseria.calidadleche.infrastructure.persistence.mongo.doc.AnaliticaMuestraDoc;
import com.queseria.calidadleche.infrastructure.persistence.mongo.repo.AnaliticaMuestraMongoRepository;
import com.queseria.calidadleche.infrastructure.util.HashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class AnaliticaRepositoryImpl implements AnaliticaRepository {

  private final AnaliticaMuestraMongoRepository mongoRepo;

  @Override
  public Mono<Void> saveAnalisis(
      MuestraLeche muestra,
      EvaluacionCalidadService.EvaluacionMuestra evaluacion
  ) {
    AnaliticaMuestraDoc.BaseValores base = AnaliticaMuestraDoc.BaseValores.builder()
        .grasa(muestra.composicion().grasa())
        .proteina(muestra.composicion().proteina())
        .lactosa(muestra.composicion().lactosa())
        .solidosTotales(muestra.composicion().solidosTotales())
        .densidad(muestra.fisicoQuimico().densidad())
        .acidezDornic(muestra.fisicoQuimico().acidezDornic())
        .temperaturaC(muestra.fisicoQuimico().temperaturaC())
        .sng(muestra.sng())
        .aguaPct(muestra.aguaPct())
        .build();

    Map<String, AnaliticaMuestraDoc.ResultadoParametroDoc> porParametro =
        evaluacion.porParametro().entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> AnaliticaMuestraDoc.ResultadoParametroDoc.builder()
                    .estado(e.getValue().estado())
                    .mensajes(e.getValue().mensajes())
                    .build(),
                (a, b) -> a,
                LinkedHashMap::new
            ));

    AnaliticaMuestraDoc.EvaluacionDoc evalDoc = AnaliticaMuestraDoc.EvaluacionDoc.builder()
        .porParametro(porParametro)
        .build();

    String hash = HashUtil.sha256CanonicalJson(base);

    Set<String> flags = porParametro.values().stream()
        .map(AnaliticaMuestraDoc.ResultadoParametroDoc::getEstado)
        .filter(Objects::nonNull)
        .filter(s -> !s.equalsIgnoreCase("INFO") && !s.equalsIgnoreCase("SIN_DATO"))
        .collect(Collectors.toCollection(LinkedHashSet::new));

    BigDecimal kpi = calcularKpiSimple(porParametro);

    AnaliticaMuestraDoc doc = AnaliticaMuestraDoc.builder()
        .sampleId(muestra.id())
        .proveedorId(muestra.proveedorId())
        .timestamp(Instant.now())
        .base(base)
        .evaluacion(evalDoc)
        .hashBase(hash)
        .flags(new ArrayList<>(flags))
        .kpiCalidad(kpi)
        .build();

    return mongoRepo.save(doc).then();
  }

  private BigDecimal calcularKpiSimple(Map<String, AnaliticaMuestraDoc.ResultadoParametroDoc> porParametro) {
    if (porParametro == null || porParametro.isEmpty()) return BigDecimal.ONE;

    int total = 0;
    int puntos = 0;
    for (var v : porParametro.values()) {
      if (v.getEstado() == null) continue;
      String e = v.getEstado();
      if (e.equals("INFO") || e.equals("SIN_DATO")) continue;
      total++;
      if (e.equals("ACEPTABLE")) puntos += 2;
      else if (e.equals("ALERTA")) puntos += 1;
      else if (e.equals("RECHAZAR")) puntos += 0;
    }
    if (total == 0) return BigDecimal.ONE;
    return BigDecimal.valueOf(puntos).divide(BigDecimal.valueOf(total * 2L), 2, java.math.RoundingMode.HALF_UP);
  }
}
