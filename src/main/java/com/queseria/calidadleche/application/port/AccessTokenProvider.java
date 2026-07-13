package com.queseria.calidadleche.application.port;

import com.queseria.calidadleche.domain.model.Usuario;

public interface AccessTokenProvider {
  GeneratedAccessToken generate(Usuario usuario);

  record GeneratedAccessToken(String value, long expiresInSeconds) {}
}
