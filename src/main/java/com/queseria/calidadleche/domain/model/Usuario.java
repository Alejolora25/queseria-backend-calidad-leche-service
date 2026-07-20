package com.queseria.calidadleche.domain.model;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public final class Usuario {
  private final Long id;
  private final String nombre;
  private final String email;
  private final String passwordHash;
  private final boolean activo;
  private final Long queseriaId;
  private final Set<NombreRol> roles;
  private final OffsetDateTime creadoEn;
  private final OffsetDateTime actualizadoEn;

  private Usuario(
      Long id,
      String nombre,
      String email,
      String passwordHash,
      boolean activo,
      Long queseriaId,
      Set<NombreRol> roles,
      OffsetDateTime creadoEn,
      OffsetDateTime actualizadoEn
  ) {
    this.id = id;
    this.nombre = Objects.requireNonNull(nombre).trim();
    this.email = normalizarEmail(email);
    this.passwordHash = Objects.requireNonNull(passwordHash);
    this.activo = activo;
    this.queseriaId = queseriaId;
    this.roles = Collections.unmodifiableSet(new LinkedHashSet<>(roles == null ? Set.of() : roles));
    this.creadoEn = creadoEn == null ? OffsetDateTime.now() : creadoEn;
    this.actualizadoEn = actualizadoEn == null ? this.creadoEn : actualizadoEn;
  }

  public static Usuario crear(String nombre, String email, String passwordHash, Long queseriaId) {
    return new Usuario(null, nombre, email, passwordHash, true, queseriaId, Set.of(), null, null);
  }

  public static Usuario reconstruir(
      Long id,
      String nombre,
      String email,
      String passwordHash,
      boolean activo,
      Long queseriaId,
      Set<NombreRol> roles,
      OffsetDateTime creadoEn,
      OffsetDateTime actualizadoEn
  ) {
    return new Usuario(id, nombre, email, passwordHash, activo, queseriaId, roles, creadoEn, actualizadoEn);
  }

  public Usuario conRoles(Set<NombreRol> roles) {
    return new Usuario(id, nombre, email, passwordHash, activo, queseriaId, roles, creadoEn, OffsetDateTime.now());
  }

  public Usuario conEstado(boolean activo) {
    return new Usuario(id, nombre, email, passwordHash, activo, queseriaId, roles, creadoEn, OffsetDateTime.now());
  }

  public Long id() { return id; }
  public String nombre() { return nombre; }
  public String email() { return email; }
  public String passwordHash() { return passwordHash; }
  public boolean activo() { return activo; }
  public Long queseriaId() { return queseriaId; }
  public Set<NombreRol> roles() { return roles; }
  public OffsetDateTime creadoEn() { return creadoEn; }
  public OffsetDateTime actualizadoEn() { return actualizadoEn; }

  private static String normalizarEmail(String email) {
    return Objects.requireNonNull(email).trim().toLowerCase();
  }
}
