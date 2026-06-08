package com.queseria.calidadleche.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class EvaluacionCalidadServiceTest {

  private final EvaluacionCalidadService service = new EvaluacionCalidadService();

  @Test
  void evaluarDebeMarcarParametrosAceptablesParaMuestraDentroDeRangos() {
    var evaluacion = service.evaluar(
        bd("4.0"), bd("3.2"),
        bd("1.032"), bd("18"),
        bd("13.0"), null,
        bd("15"), bd("0"));

    assertEstado(evaluacion, "grasa", "ACEPTABLE");
    assertEstado(evaluacion, "proteina", "ACEPTABLE");
    assertEstado(evaluacion, "densidad_dq", "ACEPTABLE");
    assertEstado(evaluacion, "solidos_totales", "ACEPTABLE");
    assertEstado(evaluacion, "sng", "ACEPTABLE");
    assertEstado(evaluacion, "acidez_dornic", "ACEPTABLE");
    assertEstado(evaluacion, "agua_pct", "ACEPTABLE");
  }

  @Test
  void evaluarDebeCalcularSngCuandoNoVieneInformado() {
    var evaluacion = service.evaluar(
        bd("4.0"), bd("3.2"),
        bd("1.032"), bd("18"),
        bd("13.0"), null,
        bd("15"), bd("0"));

    assertEstado(evaluacion, "sng", "ACEPTABLE");
  }

  @Test
  void evaluarDebeRechazarAguaMayorACero() {
    var evaluacion = service.evaluar(
        bd("4.0"), bd("3.2"),
        bd("1.032"), bd("18"),
        bd("13.0"), null,
        bd("15"), bd("1.0"));

    assertEstado(evaluacion, "agua_pct", "RECHAZAR");
    assertThat(evaluacion.porParametro().get("agua_pct").mensajes())
        .anySatisfy(mensaje -> assertThat(mensaje).contains("agua > 0%"));
  }

  @Test
  void evaluarDebeRechazarDensidadBaja() {
    var evaluacion = service.evaluar(
        bd("4.0"), bd("3.2"),
        bd("1.028"), bd("18"),
        bd("13.0"), null,
        bd("15"), bd("0"));

    assertEstado(evaluacion, "densidad_dq", "RECHAZAR");
  }

  @Test
  void evaluarDebeRechazarAcidezAlta() {
    var evaluacion = service.evaluar(
        bd("4.0"), bd("3.2"),
        bd("1.032"), bd("18"),
        bd("13.0"), null,
        bd("18"), bd("0"));

    assertEstado(evaluacion, "acidez_dornic", "RECHAZAR");
  }

  @Test
  void evaluarDebeAgregarParametrosInformativos() {
    var evaluacion = service.evaluar(
        bd("4.0"), bd("3.2"),
        bd("1.032"), bd("18"),
        bd("13.0"), null,
        bd("15"), bd("0"));

    assertEstado(evaluacion, "acido_lactico_pct", "INFO");
    assertEstado(evaluacion, "densidad_dq_crudo", "INFO");
    assertEstado(evaluacion, "densidad_dq_corregido", "INFO");
  }

  private static void assertEstado(
      EvaluacionCalidadService.EvaluacionMuestra evaluacion,
      String parametro,
      String esperado
  ) {
    assertThat(evaluacion.porParametro())
        .containsKey(parametro);
    assertThat(evaluacion.porParametro().get(parametro).estado())
        .isEqualTo(esperado);
  }

  private static BigDecimal bd(String value) {
    return new BigDecimal(value);
  }
}
