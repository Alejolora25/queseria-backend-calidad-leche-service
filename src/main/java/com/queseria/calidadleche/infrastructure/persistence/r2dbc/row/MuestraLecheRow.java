package com.queseria.calidadleche.infrastructure.persistence.r2dbc.row;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Table("muestras_leche")
public class MuestraLecheRow {
  @Id
  private Long id;

  @Column("proveedor_id")
  private Long proveedorId;

  @Column("fecha_muestra")
  private OffsetDateTime fechaMuestra;

  @Column("volumen_litros")
  private BigDecimal volumenLitros;

  @Column("precio_litro")
  private BigDecimal precioLitro;

  @Column("temperatura_c")
  private BigDecimal temperaturaC;

  // composición
  private BigDecimal grasa, proteina, lactosa;

  @Column("solidos_totales")
  private BigDecimal solidosTotales;

  // físico-químico (g/mL y °D)
  private BigDecimal densidad;

  @Column("acidez_dornic")
  private BigDecimal acidezDornic;

  // higiene
  @Column("ufc_bacterias")
  private Integer ufcBacterias;

  @Column("cc_somaticas")
  private Integer ccSomaticas;

  private String observaciones;

  @Column("agua_pct") private BigDecimal aguaPct;
  private BigDecimal sng;
  private String equipo;     // persistimos como String del enum
  private String condicion;  // persistimos como String del enum
}