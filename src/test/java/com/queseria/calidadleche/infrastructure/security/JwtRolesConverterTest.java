package com.queseria.calidadleche.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import reactor.test.StepVerifier;

class JwtRolesConverterTest {
  private final JwtRolesConverter converter = new JwtRolesConverter();

  @Test
  void debeConvertirRolesConPrefijoEIgnorarValoresInvalidos() {
    Jwt jwt = jwtWithRoles(Arrays.asList(
        "ADMIN",
        null,
        "",
        "   ",
        "operador",
        "LECTOR",
        "DESCONOCIDO",
        "ADMIN"
    ));

    StepVerifier.create(converter.convert(jwt).map(GrantedAuthority::getAuthority).collectList())
        .assertNext(authorities -> assertThat(authorities)
            .containsExactly("ROLE_ADMIN", "ROLE_OPERADOR", "ROLE_LECTOR"))
        .verifyComplete();
  }

  @Test
  void debeRetornarAutoridadesVaciasCuandoNoHayRoles() {
    Jwt jwt = Jwt.withTokenValue("token")
        .header("alg", "HS256")
        .subject("1")
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(300))
        .build();

    StepVerifier.create(converter.convert(jwt).collectList())
        .assertNext(authorities -> assertThat(authorities).isEmpty())
        .verifyComplete();
  }

  private Jwt jwtWithRoles(List<?> roles) {
    Instant now = Instant.now();
    return Jwt.withTokenValue("token")
        .header("alg", "HS256")
        .subject("1")
        .issuedAt(now)
        .expiresAt(now.plusSeconds(300))
        .claim("roles", roles)
        .build();
  }
}
