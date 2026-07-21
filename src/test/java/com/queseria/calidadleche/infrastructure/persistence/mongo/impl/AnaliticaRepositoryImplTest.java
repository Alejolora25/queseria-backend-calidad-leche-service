package com.queseria.calidadleche.infrastructure.persistence.mongo.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.queseria.calidadleche.domain.model.Composicion;
import com.queseria.calidadleche.domain.model.FisicoQuimico;
import com.queseria.calidadleche.domain.model.Higiene;
import com.queseria.calidadleche.domain.model.MuestraLeche;
import com.queseria.calidadleche.domain.service.EvaluacionCalidadService;
import com.queseria.calidadleche.infrastructure.persistence.mongo.doc.AnaliticaMuestraDoc;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@ExtendWith(MockitoExtension.class)
class AnaliticaRepositoryImplTest {

  @Mock private ReactiveMongoTemplate mongo;

  @Test
  void saveAnalisisDebeHacerUpsertPorSampleId() {
    MuestraLeche muestra = muestra();
    var evaluacion = new EvaluacionCalidadService().evaluar(
        muestra.composicion().grasa(),
        muestra.composicion().proteina(),
        muestra.fisicoQuimico().densidad(),
        muestra.fisicoQuimico().temperaturaC(),
        muestra.composicion().solidosTotales(),
        muestra.sng(),
        muestra.fisicoQuimico().acidezDornic(),
        muestra.aguaPct()
    );
    when(mongo.findAndModify(
        any(Query.class),
        any(UpdateDefinition.class),
        any(FindAndModifyOptions.class),
        eq(AnaliticaMuestraDoc.class)
    )).thenReturn(Mono.just(new AnaliticaMuestraDoc()));

    var repository = new AnaliticaRepositoryImpl(mongo);

    StepVerifier.create(repository.saveAnalisis(muestra, evaluacion)).verifyComplete();

    ArgumentCaptor<Query> queryCaptor = ArgumentCaptor.forClass(Query.class);
    ArgumentCaptor<UpdateDefinition> updateCaptor = ArgumentCaptor.forClass(UpdateDefinition.class);
    ArgumentCaptor<FindAndModifyOptions> optionsCaptor = ArgumentCaptor.forClass(FindAndModifyOptions.class);
    verify(mongo).findAndModify(
        queryCaptor.capture(),
        updateCaptor.capture(),
        optionsCaptor.capture(),
        eq(AnaliticaMuestraDoc.class)
    );

    assertThat(queryCaptor.getValue().getQueryObject().get("sampleId")).isEqualTo(10L);
    Document values = (Document) updateCaptor.getValue().getUpdateObject().get("$set");
    assertThat(values.get("sampleId")).isEqualTo(10L);
    assertThat(values.get("timestamp")).isEqualTo(muestra.fechaMuestra().toInstant());
    assertThat(values.get("estadoGeneral")).isEqualTo("ACEPTABLE");
    assertThat(optionsCaptor.getValue().isUpsert()).isTrue();
    assertThat(optionsCaptor.getValue().isReturnNew()).isTrue();
  }

  private MuestraLeche muestra() {
    return MuestraLeche.reconstruir(
        10L,
        4L,
        OffsetDateTime.parse("2026-01-10T08:00:00-05:00"),
        new BigDecimal("120.50"),
        new BigDecimal("1800"),
        null,
        new Composicion(new BigDecimal("4.0"), new BigDecimal("3.2"), BigDecimal.ZERO, new BigDecimal("12.8")),
        new FisicoQuimico(new BigDecimal("1.032"), new BigDecimal("15"), new BigDecimal("18")),
        new Higiene(0, 0),
        BigDecimal.ZERO,
        new BigDecimal("8.8"),
        null,
        null
    );
  }
}
