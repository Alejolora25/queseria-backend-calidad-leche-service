package com.queseria.calidadleche.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public record FisicoQuimico(
    BigDecimal densidad,
    BigDecimal acidezDornic,
    BigDecimal temperaturaC
) {
  public FisicoQuimico {
    Objects.requireNonNull(densidad); Objects.requireNonNull(acidezDornic);
    Objects.requireNonNull(temperaturaC);
    if (densidad.compareTo(new BigDecimal("0.9")) < 0 ||
        densidad.compareTo(new BigDecimal("1.2")) > 0) {
      throw new IllegalArgumentException("Densidad fuera de rango (0.9–1.2)");
    }
  }
}