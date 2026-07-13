package com.queseria.calidadleche.interfaces.web;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.queseria.calidadleche.application.exception.CredencialesInvalidasException;
import com.queseria.calidadleche.application.model.LoginResult;
import com.queseria.calidadleche.application.usecase.LoginUseCase;
import com.queseria.calidadleche.domain.model.NombreRol;
import com.queseria.calidadleche.infrastructure.config.SecurityConfig;

import reactor.core.publisher.Mono;

@WebFluxTest(controllers = AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

  @Autowired
  private WebTestClient webTestClient;

  @MockitoBean
  private LoginUseCase loginUseCase;

  @Test
  void loginCorrectoDebeResponderTokenYUsuarioSinPasswordHash() {
    when(loginUseCase.execute("admin@queseria.local", "Admin123*")).thenReturn(Mono.just(
        new LoginResult(
            "jwt-generado",
            3600,
            1L,
            "Administrador",
            "admin@queseria.local",
            Set.of(NombreRol.OPERADOR, NombreRol.ADMIN),
            9L
        )
    ));

    webTestClient.post()
        .uri("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {
              "email": "admin@queseria.local",
              "password": "Admin123*"
            }
            """)
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.accessToken").isEqualTo("jwt-generado")
        .jsonPath("$.tokenType").isEqualTo("Bearer")
        .jsonPath("$.expiresIn").isEqualTo(3600)
        .jsonPath("$.usuario.id").isEqualTo(1)
        .jsonPath("$.usuario.nombre").isEqualTo("Administrador")
        .jsonPath("$.usuario.email").isEqualTo("admin@queseria.local")
        .jsonPath("$.usuario.roles[0]").isEqualTo("ADMIN")
        .jsonPath("$.usuario.roles[1]").isEqualTo("OPERADOR")
        .jsonPath("$.usuario.queseriaId").isEqualTo(9)
        .jsonPath("$.usuario.passwordHash").doesNotExist()
        .jsonPath("$.passwordHash").doesNotExist();
  }

  @Test
  void credencialesInvalidasDebenResponderUnauthorizedConMensajeGeneral() {
    when(loginUseCase.execute("admin@queseria.local", "incorrecta"))
        .thenReturn(Mono.error(new CredencialesInvalidasException()));

    webTestClient.post()
        .uri("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {
              "email": "admin@queseria.local",
              "password": "incorrecta"
            }
            """)
        .exchange()
        .expectStatus().isUnauthorized()
        .expectBody()
        .jsonPath("$.error").isEqualTo("unauthorized")
        .jsonPath("$.message").isEqualTo("Credenciales inválidas");
  }

  @Test
  void requestInvalidoDebeResponderBadRequest() {
    webTestClient.post()
        .uri("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {
              "email": "email-invalido",
              "password": ""
            }
            """)
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.error").isEqualTo("bad_request")
        .jsonPath("$.fields.email").exists()
        .jsonPath("$.fields.password").exists();

    verifyNoInteractions(loginUseCase);
  }
}
