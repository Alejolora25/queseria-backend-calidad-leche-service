package com.queseria.calidadleche.domain.service;

import com.queseria.calidadleche.domain.model.MuestraLeche;
import com.queseria.calidadleche.infrastructure.persistence.mongo.doc.AnaliticaMuestraDoc;
import com.queseria.calidadleche.infrastructure.persistence.mongo.repo.AnaliticaMuestraMongoRepository;
import com.queseria.calidadleche.infrastructure.util.HashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PersistirAnaliticaService {

  private final AnaliticaMuestraMongoRepository mongoRepo;

  public Mono<Void> persistirAnalisis(
      MuestraLeche muestra,
      EvaluacionCalidadService.EvaluacionMuestra evaluacion
  ) {
    // 1) Construir bloque base con los valores tal como llegaron/derivados
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

    // 2) Mapear evaluación a documento
    Map<String, AnaliticaMuestraDoc.ResultadoParametroDoc> porParametro =
        evaluacion.porParametro().entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> AnaliticaMuestraDoc.ResultadoParametroDoc.builder()
                        .estado(e.getValue().estado())
                        .mensajes(e.getValue().mensajes())
                        .build(),
                (a,b)->a, LinkedHashMap::new
            ));

    AnaliticaMuestraDoc.EvaluacionDoc evalDoc = AnaliticaMuestraDoc.EvaluacionDoc.builder()
        .porParametro(porParametro)
        .build();

    // 3) Hash canónico de la base
    String hash = HashUtil.sha256CanonicalJson(base);

    // 4) Flags y KPI (sencillo por ahora)
    Set<String> flags = porParametro.values().stream()
        .map(AnaliticaMuestraDoc.ResultadoParametroDoc::getEstado)
        .filter(Objects::nonNull)
        .filter(s -> !s.equalsIgnoreCase("INFO") && !s.equalsIgnoreCase("SIN_DATO"))
        .collect(Collectors.toCollection(LinkedHashSet::new));

    BigDecimal kpi = calcularKpiSimple(porParametro);

    // 5) Armar doc y guardar
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

  // KPI simple: 1.0 = todo ACEPTABLE; penaliza ALERTA y RECHAZAR.
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
