package com.queseria.calidadleche.infrastructure.persistence.r2dbc.row;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import java.time.OffsetDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Table("proveedores")
public class ProveedorRow {
  @Id
  private Long id;
  private String nombre;

  @Column("tipo_identificacion")  // <--- NUEVO
  private String tipoIdentificacion;
  
  private String identificacion;
  private Boolean activo;

  @Column("created_at")
  private OffsetDateTime createdAt;

  @Column("updated_at")
  private OffsetDateTime updatedAt;
}