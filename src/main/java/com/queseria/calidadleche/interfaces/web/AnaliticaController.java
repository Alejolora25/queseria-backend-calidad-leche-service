package com.queseria.calidadleche.interfaces.web;

import com.queseria.calidadleche.infrastructure.persistence.mongo.doc.AnaliticaMuestraDoc;
import com.queseria.calidadleche.infrastructure.persistence.mongo.repo.AnaliticaMuestraMongoRepository;

import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analiticas")
public class AnaliticaController {

  private final AnaliticaMuestraMongoRepository repo;
  private final ReactiveMongoTemplate mongo;

  public AnaliticaController(AnaliticaMuestraMongoRepository repo, ReactiveMongoTemplate mongo) {
    this.repo = repo;
    this.mongo = mongo;
  }

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

  // 2a) Última analítica por muestra
  @GetMapping("/muestra/{sampleId}")
  public Mono<AnaliticaMuestraDoc> porMuestra(@PathVariable Long sampleId) {
    return repo.findFirstBySampleIdOrderByTimestampDesc(sampleId);
  }

  // 2b) Resumen/KPIs por proveedor en rango
  @GetMapping("/proveedor/{proveedorId}/resumen")
  public Mono<Map<String,Object>> resumenProveedor(
      @PathVariable Long proveedorId,
      @RequestParam Instant desde,
      @RequestParam Instant hasta
  ) {
    MatchOperation match = Aggregation.match(
        org.springframework.data.mongodb.core.query.Criteria.where("proveedorId").is(proveedorId)
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

    ProjectionOperation states = Aggregation.project("grasa","proteina","solidosTotales","sng","kpi")
        .and("params.v.estado").as("estado");

    GroupOperation group = Aggregation.group()
        .avg("grasa").as("avgGrasa")
        .avg("proteina").as("avgProteina")
        .avg("solidosTotales").as("avgST")
        .avg("sng").as("avgSNG")
        .avg("kpi").as("avgKPI")
        .sum(ConditionalOperators.when(
            org.springframework.data.mongodb.core.query.Criteria.where("estado").is("ACEPTABLE")
        ).then(1).otherwise(0)).as("cntAceptable")
        .sum(ConditionalOperators.when(
            org.springframework.data.mongodb.core.query.Criteria.where("estado").is("ALERTA")
        ).then(1).otherwise(0)).as("cntAlerta")
        .sum(ConditionalOperators.when(
            org.springframework.data.mongodb.core.query.Criteria.where("estado").is("RECHAZAR")
        ).then(1).otherwise(0)).as("cntRechazar")
        .count().as("totalFilas");

    Aggregation agg = Aggregation.newAggregation(match, project, unwind, states, group);

    return mongo.aggregate(agg, "analiticas_muestra", ResumenAgg.class)
    .singleOrEmpty()
    .map(r -> {
      long total = r.totalFilas() == null ? 0L : r.totalFilas();
      long a     = r.cntAceptable() == null ? 0L : r.cntAceptable();
      long w     = r.cntAlerta() == null ? 0L : r.cntAlerta();
      long rj    = r.cntRechazar() == null ? 0L : r.cntRechazar();

      return Map.of(
          "promedios", Map.of(
              "grasa", r.avgGrasa(),
              "proteina", r.avgProteina(),
              "solidosTotales", r.avgST(),
              "sng", r.avgSNG(),
              "kpi", r.avgKPI()
          ),
          "distribucionEstados", Map.of(
              "ACEPTABLE", a, "ALERTA", w, "RECHAZAR", rj, "totalEstados", total
          )
      );
    });
  }
}