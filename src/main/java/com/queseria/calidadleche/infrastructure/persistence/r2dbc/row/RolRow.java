package com.queseria.calidadleche.infrastructure.persistence.r2dbc.row;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
@Table("roles")
public class RolRow {
  @Id
  private Long id;
  private String nombre;
  private String descripcion;

  @Column("created_at")
  private OffsetDateTime createdAt;
}
