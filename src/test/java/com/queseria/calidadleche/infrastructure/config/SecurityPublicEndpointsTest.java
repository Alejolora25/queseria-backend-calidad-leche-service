package com.queseria.calidadleche.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.beans.factory.annotation.Autowired;

import testsupport.security.SecurityPublicEndpointsTestController;

@WebFluxTest(
    controllers = SecurityPublicEndpointsTestController.class,
    properties = {
        "app.security.jwt.secret=test-jwt-secret-with-at-least-32-characters",
        "app.security.jwt.issuer=queseria-test",
        "app.security.jwt.expiration-minutes=60"
    }
)
@Import({
    SecurityConfig.class,
    WebFluxCorsConfig.class,
    SecurityPublicEndpointsTestController.class
})
class SecurityPublicEndpointsTest {

  @Autowired
  private WebTestClient webTestClient;

  @Test
  void loginDebePermanecerPublico() {
    webTestClient.post()
        .uri("/api/v1/auth/login")
        .exchange()
        .expectStatus().isOk();
  }

  @Test
  void healthDebePermanecerPublico() {
    webTestClient.get()
        .uri("/actuator/health")
        .exchange()
        .expectStatus().isOk();
  }

  @Test
  void preflightCorsDebePermanecerPublico() {
    webTestClient.options()
        .uri("http://localhost:8080/api/v1/proveedores/prueba")
        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name())
        .exchange()
        .expectStatus().isOk()
        .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200");
  }
}
