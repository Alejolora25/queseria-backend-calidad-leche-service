package com.queseria.calidadleche.infrastructure.persistence.mongo.impl;

import com.queseria.calidadleche.application.port.AnaliticaRepository;
import com.queseria.calidadleche.application.service.AnaliticaCalculos;
import com.queseria.calidadleche.domain.model.MuestraLeche;
import com.queseria.calidadleche.domain.service.EvaluacionCalidadService;
import com.queseria.calidadleche.infrastructure.persistence.mongo.doc.AnaliticaMuestraDoc;
import com.queseria.calidadleche.infrastructure.util.HashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class AnaliticaRepositoryImpl implements AnaliticaRepository {

  private final ReactiveMongoTemplate mongo;

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

    Update update = new Update()
        .set("sampleId", muestra.id())
        .set("proveedorId", muestra.proveedorId())
        .set("timestamp", muestra.fechaMuestra().toInstant())
        .set("calculadaEn", Instant.now())
        .set("estadoGeneral", AnaliticaCalculos.estadoGeneral(evaluacion))
        .set("base", base)
        .set("evaluacion", evalDoc)
        .set("hashBase", hash)
        .set("flags", AnaliticaCalculos.flags(evaluacion))
        .set("kpiCalidad", AnaliticaCalculos.kpi(evaluacion));

    Query query = Query.query(Criteria.where("sampleId").is(muestra.id()));
    FindAndModifyOptions options = FindAndModifyOptions.options().upsert(true).returnNew(true);

    return mongo.findAndModify(query, update, options, AnaliticaMuestraDoc.class)
        .retryWhen(Retry.max(1).filter(DuplicateKeyException.class::isInstance))
        .then();
  }
}
