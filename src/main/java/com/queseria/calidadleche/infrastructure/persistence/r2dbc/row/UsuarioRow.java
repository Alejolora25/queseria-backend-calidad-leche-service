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
@Table("usuarios")
public class UsuarioRow {
  @Id
  private Long id;
  private String nombre;
  private String email;

  @Column("password_hash")
  private String passwordHash;

  private Boolean activo;

  @Column("queseria_id")
  private Long queseriaId;

  @Column("created_at")
  private OffsetDateTime createdAt;

  @Column("updated_at")
  private OffsetDateTime updatedAt;
}
