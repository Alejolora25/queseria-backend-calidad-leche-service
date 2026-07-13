package com.queseria.calidadleche.infrastructure.security;

import java.util.Collection;
import java.util.Locale;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import com.queseria.calidadleche.domain.model.NombreRol;

import reactor.core.publisher.Flux;

public class JwtRolesConverter implements Converter<Jwt, Flux<GrantedAuthority>> {

  @Override
  public Flux<GrantedAuthority> convert(Jwt jwt) {
    Object rolesClaim = jwt.getClaim("roles");
    if (!(rolesClaim instanceof Collection<?> roles)) {
      return Flux.empty();
    }

    return Flux.fromStream(roles.stream()
        .filter(java.util.Objects::nonNull)
        .map(Object::toString)
        .map(String::trim)
        .filter(value -> !value.isEmpty())
        .map(this::toKnownRole)
        .filter(java.util.Objects::nonNull)
        .distinct()
        .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role.name())));
  }

  private NombreRol toKnownRole(String value) {
    try {
      return NombreRol.valueOf(value.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }
}
