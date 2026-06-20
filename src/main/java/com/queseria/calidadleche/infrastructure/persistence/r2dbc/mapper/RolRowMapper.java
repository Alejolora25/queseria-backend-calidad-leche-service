package com.queseria.calidadleche.infrastructure.persistence.r2dbc.mapper;

import com.queseria.calidadleche.domain.model.Rol;
import com.queseria.calidadleche.infrastructure.persistence.r2dbc.row.RolRow;

public final class RolRowMapper {
  private RolRowMapper() {}

  public static Rol toDomain(RolRow row) {
    return Rol.reconstruir(row.getId(), row.getNombre(), row.getDescripcion(), row.getCreatedAt());
  }
}
