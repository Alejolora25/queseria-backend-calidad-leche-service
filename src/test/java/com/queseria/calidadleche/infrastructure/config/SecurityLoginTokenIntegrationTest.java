package com.queseria.calidadleche.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.queseria.calidadleche.application.port.AccessTokenProvider;
import com.queseria.calidadleche.application.port.PasswordVerifier;
import com.queseria.calidadleche.application.usecase.LoginUseCase;
import com.queseria.calidadleche.domain.model.NombreRol;
import com.queseria.calidadleche.domain.model.Usuario;
import com.queseria.calidadleche.domain.repo.UsuarioRepository;
import com.queseria.calidadleche.infrastructure.security.BCryptPasswordVerifier;
import com.queseria.calidadleche.infrastructure.security.JwtService;
import com.queseria.calidadleche.interfaces.web.AuthController;

import reactor.core.publisher.Mono;
import testsupport.security.SecurityRoleEndpointsTestController;

@WebFluxTest(
    controllers = { AuthController.class, SecurityRoleEndpointsTestController.class },
    properties = {
        "app.security.jwt.secret=test-jwt-secret-with-at-least-32-characters",
        "app.security.jwt.issuer=queseria-test",
        "app.security.jwt.expiration-minutes=60"
    }
)
@Import({
    SecurityConfig.class,
    WebFluxCorsConfig.class,
    JwtService.class,
    BCryptPasswordVerifier.class,
    SecurityRoleEndpointsTestController.class,
    SecurityLoginTokenIntegrationTest.LoginTestConfig.class
})
class SecurityLoginTokenIntegrationTest {
  @Autowired
  private WebTestClient webTestClient;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @MockitoBean
  private UsuarioRepository usuarioRepository;

  @Test
  void tokenAdminGeneradoPorLoginDebePermitirEndpointProtegido() {
    when(usuarioRepository.findByEmail("admin@queseria.local"))
        .thenReturn(Mono.just(usuario(
            1L,
            "Administrador",
            "admin@queseria.local",
            "Admin123*",
            NombreRol.ADMIN)));

    String token = login("admin@queseria.local", "Admin123*");

    webTestClient.get()
        .uri("/api/v1/proveedores/prueba")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.status").isEqualTo("ok");
  }

  @Test
  void tokenLectorGeneradoPorLoginDebePermitirLecturaYDenegarEscritura() {
    when(usuarioRepository.findByEmail("lector@queseria.local"))
        .thenReturn(Mono.just(usuario(
            2L,
            "Lector",
            "lector@queseria.local",
            "Lector123*",
            NombreRol.LECTOR)));

    String token = login("lector@queseria.local", "Lector123*");

    webTestClient.get()
        .uri("/api/v1/proveedores/prueba")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .exchange()
        .expectStatus().isOk();

    webTestClient.post()
        .uri("/api/v1/proveedores")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .exchange()
        .expectStatus().isForbidden()
        .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.error").isEqualTo("forbidden")
        .jsonPath("$.message").isEqualTo("No tienes permisos para realizar esta acción");
  }

  private String login(String email, String password) {
    AuthController.LoginResponse response = webTestClient.post()
        .uri("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(new AuthController.LoginRequest(email, password))
        .exchange()
        .expectStatus().isOk()
        .expectBody(AuthController.LoginResponse.class)
        .returnResult()
        .getResponseBody();

    assertThat(response).isNotNull();
    assertThat(response.tokenType()).isEqualTo("Bearer");
    assertThat(response.accessToken()).isNotBlank();
    return response.accessToken();
  }

  private Usuario usuario(
      Long id,
      String nombre,
      String email,
      String rawPassword,
      NombreRol rol
  ) {
    OffsetDateTime now = OffsetDateTime.now();
    return Usuario.reconstruir(
        id,
        nombre,
        email,
        passwordEncoder.encode(rawPassword),
        true,
        null,
        Set.of(rol),
        now,
        now);
  }

  @TestConfiguration
  static class LoginTestConfig {
    @Bean
    LoginUseCase loginUseCase(
        UsuarioRepository usuarioRepository,
        PasswordVerifier passwordVerifier,
        AccessTokenProvider accessTokenProvider
    ) {
      return new LoginUseCase(usuarioRepository, passwordVerifier, accessTokenProvider);
    }
  }
}
