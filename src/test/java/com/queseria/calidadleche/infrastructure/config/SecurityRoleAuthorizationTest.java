package com.queseria.calidadleche.infrastructure.config;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.web.reactive.server.WebTestClient;

import testsupport.security.SecurityRoleEndpointsTestController;

@WebFluxTest(
    controllers = SecurityRoleEndpointsTestController.class,
    properties = {
        "app.security.jwt.secret=test-jwt-secret-with-at-least-32-characters",
        "app.security.jwt.issuer=queseria-test",
        "app.security.jwt.expiration-minutes=60"
    }
)
@Import({
    SecurityConfig.class,
    WebFluxCorsConfig.class,
    SecurityRoleEndpointsTestController.class
})
class SecurityRoleAuthorizationTest {
  private static final String ISSUER = "queseria-test";

  @Autowired
  private WebTestClient webTestClient;

  @Autowired
  private JwtEncoder jwtEncoder;

  @ParameterizedTest
  @MethodSource("adminEndpoints")
  void adminDebeAccederATodasLasOperacionesDefinidas(HttpMethod method, String uri) {
    expectAllowed(method, uri, "ADMIN");
  }

  @ParameterizedTest
  @MethodSource("operadorEndpoints")
  void operadorDebeConsultarYModificarDatosOperativos(HttpMethod method, String uri) {
    expectAllowed(method, uri, "OPERADOR");
  }

  @ParameterizedTest
  @MethodSource("readEndpoints")
  void lectorDebeConsultarEndpointsDeNegocio(HttpMethod method, String uri) {
    expectAllowed(method, uri, "LECTOR");
  }

  @ParameterizedTest
  @MethodSource("writeEndpoints")
  void lectorNoDebeModificarDatosOperativos(HttpMethod method, String uri) {
    expectForbidden(method, uri, "LECTOR");
  }

  @ParameterizedTest
  @MethodSource("userEndpoints")
  void operadorNoDebeAdministrarUsuarios(HttpMethod method, String uri) {
    expectForbidden(method, uri, "OPERADOR");
  }

  @Test
  void rolDesconocidoNoDebeConsultarNegocio() {
    expectForbidden(HttpMethod.GET, "/api/v1/proveedores/prueba", "DESCONOCIDO");
  }

  @Test
  void metodoNoContempladoDebePermanecerBloqueadoInclusoParaAdmin() {
    expectForbidden(HttpMethod.DELETE, "/api/v1/proveedores/1", "ADMIN");
  }

  @Test
  void endpointSinTokenDebeConservarRespuestaUnauthorized() {
    webTestClient.get()
        .uri("/api/v1/proveedores/prueba")
        .exchange()
        .expectStatus().isUnauthorized()
        .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
        .expectHeader().valueEquals(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
        .expectBody()
        .jsonPath("$.error").isEqualTo("unauthorized")
        .jsonPath("$.message").isEqualTo("Token inválido o expirado");
  }

  private void expectAllowed(HttpMethod method, String uri, String role) {
    webTestClient.method(method)
        .uri(uri)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(role))
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.status").isEqualTo("ok");
  }

  private void expectForbidden(HttpMethod method, String uri, String role) {
    webTestClient.method(method)
        .uri(uri)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(role))
        .exchange()
        .expectStatus().isForbidden()
        .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
        .expectHeader().doesNotExist(HttpHeaders.WWW_AUTHENTICATE)
        .expectBody()
        .jsonPath("$.error").isEqualTo("forbidden")
        .jsonPath("$.message").isEqualTo("No tienes permisos para realizar esta acción");
  }

  private String token(String role) {
    Instant now = Instant.now();
    JwtClaimsSet claims = JwtClaimsSet.builder()
        .issuer(ISSUER)
        .subject("1")
        .issuedAt(now)
        .expiresAt(now.plusSeconds(300))
        .claim("roles", List.of(role))
        .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }

  private static Stream<Arguments> adminEndpoints() {
    return Stream.concat(operadorEndpoints(), userEndpoints());
  }

  private static Stream<Arguments> operadorEndpoints() {
    return Stream.concat(readEndpoints(), writeEndpoints());
  }

  private static Stream<Arguments> readEndpoints() {
    return Stream.of(
        Arguments.of(HttpMethod.GET, "/api/v1/proveedores/prueba"),
        Arguments.of(HttpMethod.GET, "/api/v1/muestras/prueba"),
        Arguments.of(HttpMethod.GET, "/api/v1/analiticas/prueba"));
  }

  private static Stream<Arguments> writeEndpoints() {
    return Stream.of(
        Arguments.of(HttpMethod.POST, "/api/v1/proveedores"),
        Arguments.of(HttpMethod.PUT, "/api/v1/proveedores/1"),
        Arguments.of(HttpMethod.PATCH, "/api/v1/proveedores/1/activar"),
        Arguments.of(HttpMethod.POST, "/api/v1/muestras"));
  }

  private static Stream<Arguments> userEndpoints() {
    return Stream.of(
        Arguments.of(HttpMethod.GET, "/api/v1/usuarios"),
        Arguments.of(HttpMethod.POST, "/api/v1/usuarios"),
        Arguments.of(HttpMethod.PUT, "/api/v1/usuarios/2/roles"),
        Arguments.of(HttpMethod.PATCH, "/api/v1/usuarios/2/activar"));
  }
}
