package com.queseria.calidadleche.application.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record AnaliticaMuestraConsulta(
    String id,
    Long sampleId,
    Long proveedorId,
    Instant timestamp,
    BaseValores base,
    Evaluacion evaluacion,
    String hashBase,
    List<String> flags,
    BigDecimal kpiCalidad
) {

  public record BaseValores(
      BigDecimal grasa,
      BigDecimal proteina,
      BigDecimal lactosa,
      BigDecimal solidosTotales,
      BigDecimal densidad,
      BigDecimal acidezDornic,
      BigDecimal temperaturaC,
      BigDecimal sng,
      BigDecimal aguaPct
  ) {}

  public record Evaluacion(Map<String, ResultadoParametro> porParametro) {}

  public record ResultadoParametro(String estado, List<String> mensajes) {}
}
