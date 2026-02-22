package com.queseria.calidadleche.domain.model;

import java.util.Objects;

public record Higiene(Integer ufcBacterias, Integer ccSomaticas) {
  public Higiene {
    Objects.requireNonNull(ufcBacterias); Objects.requireNonNull(ccSomaticas);
    if (ufcBacterias < 0 || ccSomaticas < 0)
      throw new IllegalArgumentException("UFC/CC no pueden ser negativos");
  }
}