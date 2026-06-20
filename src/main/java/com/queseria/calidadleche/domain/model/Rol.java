package com.queseria.calidadleche.domain.model;

import java.time.OffsetDateTime;
import java.util.Objects;

public final class Rol {
  private final Long id;
  private final NombreRol nombre;
  private final String descripcion;
  private final OffsetDateTime creadoEn;

  private Rol(Long id, NombreRol nombre, String descripcion, OffsetDateTime creadoEn) {
    this.id = id;
    this.nombre = Objects.requireNonNull(nombre);
    this.descripcion = descripcion;
    this.creadoEn = creadoEn == null ? OffsetDateTime.now() : creadoEn;
  }

  public static Rol reconstruir(Long id, String nombre, String descripcion, OffsetDateTime creadoEn) {
    return new Rol(id, NombreRol.valueOf(Objects.requireNonNull(nombre).trim()), descripcion, creadoEn);
  }

  public Long id() { return id; }
  public NombreRol nombre() { return nombre; }
  public String descripcion() { return descripcion; }
  public OffsetDateTime creadoEn() { return creadoEn; }
}
