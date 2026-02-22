package com.queseria.calidadleche.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

public final class MuestraLeche {
  private final Long id;
  private final Long proveedorId;
  private final OffsetDateTime fechaMuestra;
  private final BigDecimal volumenLitros;
  private final BigDecimal precioLitro;
  private final String observaciones;
  private final Composicion composicion;
  private final FisicoQuimico fisicoQuimico;
  private final Higiene higiene;
  private final BigDecimal aguaPct;     // puede ser null (equipo no lo calculó)
  private final BigDecimal sng;         // puede ser null (si equipo lo mandó; si no, derivamos)
  private final Equipo equipo;          // MANUAL por defecto
  private final CondicionMuestra condicion; // TIBIA por defecto

  private MuestraLeche(Long id, Long proveedorId, OffsetDateTime fechaMuestra,
                       BigDecimal volumenLitros, BigDecimal precioLitro, String observaciones,
                       Composicion composicion, FisicoQuimico fisicoQuimico, Higiene higiene,
                      BigDecimal aguaPct, BigDecimal sng, Equipo equipo, CondicionMuestra condicion) {
    this.id = id; // null => INSERT
    this.proveedorId = Objects.requireNonNull(proveedorId);
    this.fechaMuestra = Objects.requireNonNull(fechaMuestra);
    this.volumenLitros = volumenLitros == null ? BigDecimal.ZERO : volumenLitros;
    this.precioLitro = precioLitro;
    this.observaciones = observaciones;
    this.composicion = Objects.requireNonNull(composicion);
    this.fisicoQuimico = Objects.requireNonNull(fisicoQuimico);
    this.higiene = Objects.requireNonNull(higiene);
    this.aguaPct = aguaPct;
    this.sng = sng;
    this.equipo = equipo == null ? Equipo.MANUAL : equipo;
    this.condicion = condicion == null ? CondicionMuestra.TIBIA : condicion;
  }

  public static MuestraLeche registrar(Long proveedorId, OffsetDateTime fechaMuestra,
                                       BigDecimal volumenLitros, BigDecimal precioLitro, String observaciones,
                                       Composicion composicion, FisicoQuimico fisicoQuimico, Higiene higiene,
                                      BigDecimal aguaPct, BigDecimal sng, Equipo equipo, CondicionMuestra condicion) {
    return new MuestraLeche(null, proveedorId, fechaMuestra, volumenLitros, precioLitro, observaciones,
                            composicion, fisicoQuimico, higiene, aguaPct, sng, equipo, condicion);
  }

  public static MuestraLeche reconstruir(Long id, Long proveedorId, OffsetDateTime fechaMuestra,
                                         BigDecimal volumenLitros, BigDecimal precioLitro, String observaciones,
                                         Composicion composicion, FisicoQuimico fisicoQuimico, Higiene higiene,
                                        BigDecimal aguaPct, BigDecimal sng, Equipo equipo, CondicionMuestra condicion) {
    return new MuestraLeche(id, proveedorId, fechaMuestra, volumenLitros, precioLitro, observaciones,
                            composicion, fisicoQuimico, higiene, aguaPct, sng, equipo, condicion);
  }

  public Long id() { return id; }
  public Long proveedorId() { return proveedorId; }
  public OffsetDateTime fechaMuestra() { return fechaMuestra; }
  public BigDecimal volumenLitros() { return volumenLitros; }
  public BigDecimal precioLitro() { return precioLitro; }
  public String observaciones() { return observaciones; }
  public Composicion composicion() { return composicion; }
  public FisicoQuimico fisicoQuimico() { return fisicoQuimico; }
  public Higiene higiene() { return higiene; }
  public BigDecimal aguaPct() { return aguaPct; }
  public BigDecimal sng() { return sng; }
  public Equipo equipo() { return equipo; }
  public CondicionMuestra condicion() { return condicion; }
}