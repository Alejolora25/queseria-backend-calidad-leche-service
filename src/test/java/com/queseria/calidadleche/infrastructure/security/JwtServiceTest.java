package com.queseria.calidadleche.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Set;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.queseria.calidadleche.application.port.AccessTokenProvider;
import com.queseria.calidadleche.domain.model.NombreRol;
import com.queseria.calidadleche.domain.model.Usuario;
import com.queseria.calidadleche.infrastructure.config.JwtProperties;

class JwtServiceTest {
  private static final String SECRET = "test-jwt-secret-with-at-least-32-characters";
  private static final String ISSUER = "queseria-test";

  private JwtService jwtService;
  private NimbusJwtDecoder jwtDecoder;

  @BeforeEach
  void setUp() {
    SecretKey secretKey = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    NimbusJwtEncoder jwtEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey)
        .macAlgorithm(MacAlgorithm.HS256)
        .build();
    jwtService = new JwtService(jwtEncoder, new JwtProperties(SECRET, ISSUER, 30));
  }

  @Test
  void debeGenerarTokenConUsuarioRolesYQueseria() {
    Usuario usuario = usuario(42L, 7L, Set.of(NombreRol.OPERADOR, NombreRol.ADMIN));

    Jwt jwt = jwtDecoder.decode(jwtService.generateToken(usuario));

    assertThat(jwt.getClaimAsString("iss")).isEqualTo(ISSUER);
    assertThat(jwt.getSubject()).isEqualTo("42");
    assertThat(jwt.getClaimAsString("email")).isEqualTo("usuario@queseria.local");
    assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ADMIN", "OPERADOR");
    assertThat(((Number) jwt.getClaim("userId")).longValue()).isEqualTo(42L);
    assertThat(((Number) jwt.getClaim("queseriaId")).longValue()).isEqualTo(7L);
    assertThat(jwt.getIssuedAt()).isNotNull();
    assertThat(jwt.getExpiresAt()).isNotNull();
    assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofMinutes(30));
    assertThat(jwt.getIssuedAt()).isBetween(Instant.now().minusSeconds(5), Instant.now().plusSeconds(1));
  }

  @Test
  void debeOmitirQueseriaIdCuandoNoExiste() {
    Usuario usuario = usuario(10L, null, Set.of(NombreRol.LECTOR));

    Jwt jwt = jwtDecoder.decode(jwtService.generateToken(usuario));

    assertThat(jwt.hasClaim("queseriaId")).isFalse();
  }

  @Test
  void debeInformarExpiracionDelTokenEnSegundos() {
    AccessTokenProvider.GeneratedAccessToken token = jwtService.generate(
        usuario(10L, null, Set.of(NombreRol.LECTOR))
    );

    assertThat(token.value()).isNotBlank();
    assertThat(token.expiresInSeconds()).isEqualTo(1800);
  }

  private Usuario usuario(Long id, Long queseriaId, Set<NombreRol> roles) {
    OffsetDateTime now = OffsetDateTime.now();
    return Usuario.reconstruir(
        id,
        "Usuario Prueba",
        "usuario@queseria.local",
        "$2a$10$hash",
        true,
        queseriaId,
        roles,
        now,
        now
    );
  }
}
