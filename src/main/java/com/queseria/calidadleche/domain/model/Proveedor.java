package com.queseria.calidadleche.domain.model;

import java.time.OffsetDateTime;
import java.util.Objects;

public final class Proveedor {
  private final Long id;
  private final String nombre;
  private final String tipoIdentificacion;
  private final String identificacion;
  private final boolean activo;
  private final OffsetDateTime creadoEn;
  private final OffsetDateTime actualizadoEn;

  private Proveedor(Long id, String nombre, String identificacion, String tipoIdentificacion, boolean activo,
                    OffsetDateTime creadoEn, OffsetDateTime actualizadoEn) {
    this.id = id; // null => INSERT; la BD genera el id
    this.nombre = Objects.requireNonNull(nombre).trim();
    this.tipoIdentificacion = Objects.requireNonNull(tipoIdentificacion).trim(); 
    this.identificacion = Objects.requireNonNull(identificacion).trim();
    this.activo = activo;
    this.creadoEn = creadoEn == null ? OffsetDateTime.now() : creadoEn;
    this.actualizadoEn = actualizadoEn == null ? this.creadoEn : actualizadoEn;
  }

  public static Proveedor crear(String nombre, String tipoIdentificacion, String identificacion) {
    return new Proveedor(null, nombre, tipoIdentificacion, identificacion, true, null, null);
  }

  public static Proveedor reconstruir(Long id, String nombre, String tipoIdentificacion, String identificacion,
                                      boolean activo, OffsetDateTime creadoEn, OffsetDateTime actualizadoEn) {
    return new Proveedor(id, nombre, tipoIdentificacion, identificacion, activo, creadoEn, actualizadoEn);
  }

  public Proveedor desactivar() {
    return new Proveedor(id, nombre, tipoIdentificacion, identificacion, false, creadoEn, OffsetDateTime.now());
  }

  public Long id() { return id; }
  public String nombre() { return nombre; }
  public String tipoIdentificacion() { return tipoIdentificacion; }
  public String identificacion() { return identificacion; }
  public boolean activo() { return activo; }
  public OffsetDateTime creadoEn() { return creadoEn; }
  public OffsetDateTime actualizadoEn() { return actualizadoEn; }
}