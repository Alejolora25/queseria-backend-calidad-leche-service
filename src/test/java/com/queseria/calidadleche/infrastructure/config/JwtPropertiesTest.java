package com.queseria.calidadleche.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class JwtPropertiesTest {

  @Test
  void debeRechazarSecretoVacioEnProduccion() {
    JwtProperties properties = new JwtProperties("", "calidad-leche-service", 60);

    assertThatThrownBy(() -> properties.validate(true))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("JWT_SECRET es obligatorio en produccion");
  }

  @Test
  void debeRechazarSecretoConMenosDe32Caracteres() {
    JwtProperties properties = new JwtProperties("secreto-demasiado-corto", "calidad-leche-service", 60);

    assertThatThrownBy(() -> properties.validate(false))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("JWT_SECRET debe tener al menos 32 caracteres");
  }

  @Test
  void debeAceptarConfiguracionValida() {
    JwtProperties properties = new JwtProperties(
        "test-jwt-secret-with-at-least-32-characters",
        "calidad-leche-service",
        60
    );

    assertThatCode(() -> properties.validate(true)).doesNotThrowAnyException();
  }
}
