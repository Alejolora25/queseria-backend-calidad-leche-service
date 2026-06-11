package com.queseria.calidadleche.infrastructure.persistence.mongo.impl;

import com.queseria.calidadleche.application.model.AnaliticaMuestraConsulta;
import com.queseria.calidadleche.application.port.AnaliticaConsultaRepository;
import com.queseria.calidadleche.infrastructure.persistence.mongo.doc.AnaliticaMuestraDoc;
import com.queseria.calidadleche.infrastructure.persistence.mongo.repo.AnaliticaMuestraMongoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.ConditionalOperators;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ObjectOperators;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class AnaliticaConsultaRepositoryImpl implements AnaliticaConsultaRepository {

  private final AnaliticaMuestraMongoRepository repo;
  private final ReactiveMongoTemplate mongo;

  public record ResumenAgg(
      Double avgGrasa,
      Double avgProteina,
      Double avgST,
      Double avgSNG,
      Double avgKPI,
      Long cntAceptable,
      Long cntAlerta,
      Long cntRechazar,
      Long totalFilas
  ) {}

  @Override
  public Mono<AnaliticaMuestraConsulta> buscarUltimaPorMuestra(Long sampleId) {
    return repo.findFirstBySampleIdOrderByTimestampDesc(sampleId)
        .map(this::toConsulta);
  }

  @Override
  public Mono<Map<String, Object>> obtenerResumenProveedor(Long proveedorId, Instant desde, Instant hasta) {
    MatchOperation match = Aggregation.match(
        Criteria.where("proveedorId").is(proveedorId)
            .and("timestamp").gte(desde).lte(hasta)
    );

    ProjectionOperation project = Aggregation.project()
        .and("base.grasa").as("grasa")
        .and("base.proteina").as("proteina")
        .and("base.solidosTotales").as("solidosTotales")
        .and("base.sng").as("sng")
        .and("kpiCalidad").as("kpi")
        .and(ObjectOperators.valueOf("evaluacion.porParametro").toArray()).as("params");

    UnwindOperation unwind = Aggregation.unwind("params", true);

    ProjectionOperation states = Aggregation.project("grasa", "proteina", "solidosTotales", "sng", "kpi")
        .and("params.v.estado").as("estado");

    GroupOperation group = Aggregation.group()
        .avg("grasa").as("avgGrasa")
        .avg("proteina").as("avgProteina")
        .avg("solidosTotales").as("avgST")
        .avg("sng").as("avgSNG")
        .avg("kpi").as("avgKPI")
        .sum(ConditionalOperators.when(
            Criteria.where("estado").is("ACEPTABLE")
        ).then(1).otherwise(0)).as("cntAceptable")
        .sum(ConditionalOperators.when(
            Criteria.where("estado").is("ALERTA")
        ).then(1).otherwise(0)).as("cntAlerta")
        .sum(ConditionalOperators.when(
            Criteria.where("estado").is("RECHAZAR")
        ).then(1).otherwise(0)).as("cntRechazar")
        .count().as("totalFilas");

    Aggregation agg = Aggregation.newAggregation(match, project, unwind, states, group);

    return mongo.aggregate(agg, "analiticas_muestra", ResumenAgg.class)
        .singleOrEmpty()
        .map(this::toResumen);
  }

  private Map<String, Object> toResumen(ResumenAgg r) {
    long total = r.totalFilas() == null ? 0L : r.totalFilas();
    long aceptable = r.cntAceptable() == null ? 0L : r.cntAceptable();
    long alerta = r.cntAlerta() == null ? 0L : r.cntAlerta();
    long rechazar = r.cntRechazar() == null ? 0L : r.cntRechazar();

    return Map.of(
        "promedios", Map.of(
            "grasa", r.avgGrasa(),
            "proteina", r.avgProteina(),
            "solidosTotales", r.avgST(),
            "sng", r.avgSNG(),
            "kpi", r.avgKPI()
        ),
        "distribucionEstados", Map.of(
            "ACEPTABLE", aceptable,
            "ALERTA", alerta,
            "RECHAZAR", rechazar,
            "totalEstados", total
        )
    );
  }

  private AnaliticaMuestraConsulta toConsulta(AnaliticaMuestraDoc doc) {
    return new AnaliticaMuestraConsulta(
        doc.getId(),
        doc.getSampleId(),
        doc.getProveedorId(),
        doc.getTimestamp(),
        toBase(doc.getBase()),
        toEvaluacion(doc.getEvaluacion()),
        doc.getHashBase(),
        doc.getFlags(),
        doc.getKpiCalidad()
    );
  }

  private AnaliticaMuestraConsulta.BaseValores toBase(AnaliticaMuestraDoc.BaseValores base) {
    if (base == null) return null;
    return new AnaliticaMuestraConsulta.BaseValores(
        base.getGrasa(),
        base.getProteina(),
        base.getLactosa(),
        base.getSolidosTotales(),
        base.getDensidad(),
        base.getAcidezDornic(),
        base.getTemperaturaC(),
        base.getSng(),
        base.getAguaPct()
    );
  }

  private AnaliticaMuestraConsulta.Evaluacion toEvaluacion(AnaliticaMuestraDoc.EvaluacionDoc evaluacion) {
    if (evaluacion == null) return null;
    Map<String, AnaliticaMuestraConsulta.ResultadoParametro> porParametro = evaluacion.getPorParametro()
        .entrySet()
        .stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            e -> toResultadoParametro(e.getValue()),
            (a, b) -> a,
            LinkedHashMap::new
        ));
    return new AnaliticaMuestraConsulta.Evaluacion(porParametro);
  }

  private AnaliticaMuestraConsulta.ResultadoParametro toResultadoParametro(
      AnaliticaMuestraDoc.ResultadoParametroDoc resultado
  ) {
    if (resultado == null) return null;
    return new AnaliticaMuestraConsulta.ResultadoParametro(
        resultado.getEstado(),
        resultado.getMensajes()
    );
  }
}
