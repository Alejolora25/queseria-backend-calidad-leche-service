package com.queseria.calidadleche.application.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import com.queseria.calidadleche.domain.model.NombreRol;

public record LoginResult(
    String accessToken,
    long expiresInSeconds,
    Long usuarioId,
    String nombre,
    String email,
    Set<NombreRol> roles,
    Long queseriaId
) {
  public LoginResult {
    roles = Collections.unmodifiableSet(new LinkedHashSet<>(roles));
  }
}
