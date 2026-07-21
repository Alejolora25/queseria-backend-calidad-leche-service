package com.queseria.calidadleche.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
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
@Import({ SecurityConfig.class, WebFluxCorsConfig.class })
class SecurityConfigTest {
  private static final String ISSUER = "queseria-test";
  private static final String OTHER_SECRET = "other-test-jwt-secret-with-at-least-32-characters";

  @Autowired
  private WebTestClient webTestClient;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private JwtEncoder jwtEncoder;

  @MockitoBean
  private BuscarAnaliticaPorMuestraUseCase buscarAnaliticaPorMuestraUseCase;

  @MockitoBean
  private ObtenerResumenAnaliticaProveedorUseCase obtenerResumenAnaliticaProveedorUseCase;

  @ParameterizedTest
  @ValueSource(strings = {
      "/api/v1/proveedores",
      "/api/v1/muestras",
      "/api/v1/analiticas/muestra/1"
  })
  void endpointsDeNegocioSinTokenDebenResponderUnauthorized(String uri) {
    webTestClient.get()
        .uri(uri)
        .exchange()
        .expectStatus().isUnauthorized()
        .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
        .expectHeader().valueEquals(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
        .expectBody()
        .jsonPath("$.error").isEqualTo("unauthorized")
        .jsonPath("$.message").isEqualTo("Token inválido o expirado");
  }

  @Test
  void debeExponerPasswordEncoderBCrypt() {
    String hash = passwordEncoder.encode("Admin123*");

    assertThat(passwordEncoder.matches("Admin123*", hash)).isTrue();
    assertThat(passwordEncoder.matches("incorrecta", hash)).isFalse();
  }

  @Test
  void tokenValidoDebePasarSeguridadYLlegarAlControlador() {
    when(buscarAnaliticaPorMuestraUseCase.execute(1L)).thenReturn(Mono.empty());

    webTestClient.get()
        .uri("/api/v1/analiticas/muestra/1")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken(jwtEncoder, ISSUER))
        .exchange()
        .expectStatus().isNotFound();
  }

  @Test
  void tokenValidoSinRolesDebeResponderForbidden() {
    Instant now = Instant.now();
    String token = token(jwtEncoder, ISSUER, now, now.plusSeconds(300), null, List.of());

    webTestClient.get()
        .uri("/api/v1/analiticas/muestra/1")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .exchange()
        .expectStatus().isForbidden()
        .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
        .expectBody()
        .jsonPath("$.error").isEqualTo("forbidden")
        .jsonPath("$.message").isEqualTo("No tienes permisos para realizar esta acción");
  }

  @Test
  void tokenExpiradoDebeResponderUnauthorized() {
    Instant now = Instant.now();
    String token = token(
        jwtEncoder,
        ISSUER,
        now.minusSeconds(600),
        now.minusSeconds(300),
        null,
        List.of("ADMIN", "LECTOR")
    );

    expectUnauthorized(token);
  }

  @Test
  void tokenConFirmaIncorrectaDebeResponderUnauthorized() {
    SecretKey otherKey = new SecretKeySpec(
        OTHER_SECRET.getBytes(StandardCharsets.UTF_8),
        "HmacSHA256"
    );
    JwtEncoder otherEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(otherKey));

    expectUnauthorized(validToken(otherEncoder, ISSUER));
  }

  @Test
  void tokenConIssuerIncorrectoDebeResponderUnauthorized() {
    expectUnauthorized(validToken(jwtEncoder, "otro-issuer"));
  }

  @Test
  void tokenConNotBeforeFuturoDebeResponderUnauthorized() {
    Instant now = Instant.now();
    String token = token(
        jwtEncoder,
        ISSUER,
        now,
        now.plusSeconds(600),
        now.plusSeconds(300),
        List.of("ADMIN", "LECTOR")
    );

    expectUnauthorized(token);
  }

  @Test
  void tokenMalformadoDebeResponderUnauthorizedConJsonYHeaderBearer() {
    expectUnauthorized("token-malformado");
  }

  private String validToken(JwtEncoder encoder, String issuer) {
    Instant now = Instant.now();
    return token(encoder, issuer, now, now.plusSeconds(300), null, List.of("ADMIN", "LECTOR"));
  }

  private String token(
      JwtEncoder encoder,
      String issuer,
      Instant issuedAt,
      Instant expiresAt,
      Instant notBefore,
      List<String> roles
  ) {
    JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
        .issuer(issuer)
        .subject("1")
        .issuedAt(issuedAt)
        .expiresAt(expiresAt)
        .claim("roles", roles);
    if (notBefore != null) {
      claims.notBefore(notBefore);
    }
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
  }

  private void expectUnauthorized(String token) {
    webTestClient.get()
        .uri("/api/v1/analiticas/muestra/1")
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .exchange()
        .expectStatus().isUnauthorized()
        .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
        .expectHeader().valueEquals(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
        .expectBody()
        .jsonPath("$.error").isEqualTo("unauthorized")
        .jsonPath("$.message").isEqualTo("Token inválido o expirado");
  }
}
