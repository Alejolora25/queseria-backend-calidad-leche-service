package com.queseria.calidadleche.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.queseria.calidadleche.application.usecase.BuscarAnaliticaPorMuestraUseCase;
import com.queseria.calidadleche.application.usecase.ObtenerResumenAnaliticaProveedorUseCase;
import com.queseria.calidadleche.interfaces.web.AnaliticaController;

import reactor.core.publisher.Mono;

@WebFluxTest(
    controllers = AnaliticaController.class,
    properties = {
        "app.security.jwt.secret=test-jwt-secret-with-at-least-32-characters",
        "app.security.jwt.issuer=queseria-test",
        "app.security.jwt.expiration-minutes=60"
    }
)
@Import(SecurityConfig.class)
class SecurityConfigTest {

  @Autowired
  private WebTestClient webTestClient;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @MockitoBean
  private BuscarAnaliticaPorMuestraUseCase buscarAnaliticaPorMuestraUseCase;

  @MockitoBean
  private ObtenerResumenAnaliticaProveedorUseCase obtenerResumenAnaliticaProveedorUseCase;

  @Test
  void debePermitirEndpointsSinAutenticacionTemporalmente() {
    when(buscarAnaliticaPorMuestraUseCase.execute(1L)).thenReturn(Mono.empty());

    webTestClient.get()
        .uri("/api/v1/analiticas/muestra/1")
        .exchange()
        .expectStatus().isOk();
  }

  @Test
  void debeExponerPasswordEncoderBCrypt() {
    String hash = passwordEncoder.encode("Admin123*");

    assertThat(passwordEncoder.matches("Admin123*", hash)).isTrue();
    assertThat(passwordEncoder.matches("incorrecta", hash)).isFalse();
  }
}
