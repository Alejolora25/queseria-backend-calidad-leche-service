package com.queseria.calidadleche.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public record Composicion(
    BigDecimal grasa,
    BigDecimal proteina,
    BigDecimal lactosa,
    BigDecimal solidosTotales
) {
  public Composicion {
    Objects.requireNonNull(grasa); Objects.requireNonNull(proteina);
    Objects.requireNonNull(lactosa); Objects.requireNonNull(solidosTotales);
    validatePercent(grasa, "grasa");
    validatePercent(proteina, "proteina");
    validatePercent(lactosa, "lactosa");
    validatePercent(solidosTotales, "solidosTotales");
  }
  private static void validatePercent(BigDecimal v, String f) {
    if (v.compareTo(BigDecimal.ZERO) < 0 || v.compareTo(new BigDecimal("100")) > 0)
      throw new IllegalArgumentException("Valor inválido para " + f + " (0–100)");
  }
}
