package com.queseria.calidadleche.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
    String secret,
    String issuer,
    long expirationMinutes
) {
  private static final int MINIMUM_SECRET_LENGTH = 32;

  public void validate(boolean production) {
    if (isBlank(secret)) {
      String message = production
          ? "JWT_SECRET es obligatorio en produccion"
          : "JWT_SECRET no puede estar vacio";
      throw new IllegalStateException(message);
    }
    if (secret.length() < MINIMUM_SECRET_LENGTH) {
      throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres");
    }
    if (isBlank(issuer)) {
      throw new IllegalStateException("JWT_ISSUER no puede estar vacio");
    }
    if (expirationMinutes <= 0) {
      throw new IllegalStateException("JWT_EXPIRATION_MINUTES debe ser mayor que cero");
    }
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
