package com.queseria.calidadleche.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.queseria.calidadleche.domain.model.Composicion;
import com.queseria.calidadleche.domain.model.FisicoQuimico;
import com.queseria.calidadleche.domain.model.Higiene;
import com.queseria.calidadleche.domain.model.MuestraLeche;
import com.queseria.calidadleche.domain.service.EvaluacionCalidadService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

class AnaliticaCalculosTest {

  private final EvaluacionCalidadService evaluacionService = new EvaluacionCalidadService();

  @Test
  void resumenDebeContarMuestrasYNoParametrosEvaluados() {
    MuestraLeche aceptable = muestra(1L, "4.0", "3.2", "12.8", "8.8", "1.032", "15", "0");
    MuestraLeche rechazada = muestra(2L, "3.0", "2.5", "11.0", "7.5", "1.025", "19", "2");

    Map<String, Object> resumen = AnaliticaCalculos.resumenDesdeMuestras(
        List.of(aceptable, rechazada),
        evaluacionService
    );

    Map<?, ?> distribucion = (Map<?, ?>) resumen.get("distribucionEstados");
    assertThat(distribucion.get("ACEPTABLE")).isEqualTo(1L);
    assertThat(distribucion.get("RECHAZAR")).isEqualTo(1L);
    assertThat(distribucion.get("totalEstados")).isEqualTo(2L);
  }

  @Test
  void estadoGeneralDebeAplicarLaMayorSeveridad() {
    MuestraLeche rechazada = muestra(2L, "3.0", "3.2", "12.8", "8.8", "1.032", "15", "0");
    var evaluacion = AnaliticaCalculos.evaluar(rechazada, evaluacionService);

    assertThat(AnaliticaCalculos.estadoGeneral(evaluacion)).isEqualTo("RECHAZAR");
  }

  private MuestraLeche muestra(
      Long id,
      String grasa,
      String proteina,
      String solidos,
      String sng,
      String densidad,
      String acidez,
      String agua
  ) {
    return MuestraLeche.reconstruir(
        id,
        4L,
        OffsetDateTime.parse("2026-01-10T08:00:00-05:00"),
        new BigDecimal("120.50"),
        new BigDecimal("1800"),
        null,
        new Composicion(new BigDecimal(grasa), new BigDecimal(proteina), BigDecimal.ZERO, new BigDecimal(solidos)),
        new FisicoQuimico(new BigDecimal(densidad), new BigDecimal(acidez), new BigDecimal("18")),
        new Higiene(0, 0),
        new BigDecimal(agua),
        new BigDecimal(sng),
        null,
        null
    );
  }
}
