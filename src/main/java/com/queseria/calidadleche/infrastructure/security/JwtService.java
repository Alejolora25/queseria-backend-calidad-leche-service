package com.queseria.calidadleche.infrastructure.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import com.queseria.calidadleche.domain.model.NombreRol;
import com.queseria.calidadleche.domain.model.Usuario;
import com.queseria.calidadleche.infrastructure.config.JwtProperties;

@Service
public class JwtService {
  private final JwtEncoder jwtEncoder;
  private final JwtProperties properties;

  public JwtService(JwtEncoder jwtEncoder, JwtProperties properties) {
    this.jwtEncoder = jwtEncoder;
    this.properties = properties;
  }

  public String generateToken(Usuario usuario) {
    Objects.requireNonNull(usuario, "usuario no puede ser null");
    Objects.requireNonNull(usuario.id(), "El usuario debe estar persistido para generar un JWT");

    Instant issuedAt = Instant.now();
    Instant expiresAt = issuedAt.plus(properties.expirationMinutes(), ChronoUnit.MINUTES);
    List<String> roles = usuario.roles().stream()
        .map(NombreRol::name)
        .sorted()
        .toList();

    JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
        .issuer(properties.issuer())
        .issuedAt(issuedAt)
        .expiresAt(expiresAt)
        .subject(usuario.id().toString())
        .claim("userId", usuario.id())
        .claim("email", usuario.email())
        .claim("roles", roles);

    if (usuario.queseriaId() != null) {
      claims.claim("queseriaId", usuario.queseriaId());
    }

    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
  }
}
