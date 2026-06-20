package com.queseria.calidadleche.infrastructure.persistence.r2dbc.mapper;

import com.queseria.calidadleche.domain.model.NombreRol;
import com.queseria.calidadleche.domain.model.Usuario;
import com.queseria.calidadleche.infrastructure.persistence.r2dbc.row.UsuarioRow;

import java.util.Set;

public final class UsuarioRowMapper {
  private UsuarioRowMapper() {}

  public static Usuario toDomain(UsuarioRow row, Set<NombreRol> roles) {
    return Usuario.reconstruir(
        row.getId(),
        row.getNombre(),
        row.getEmail(),
        row.getPasswordHash(),
        Boolean.TRUE.equals(row.getActivo()),
        row.getQueseriaId(),
        roles,
        row.getCreatedAt(),
        row.getUpdatedAt()
    );
  }

  public static UsuarioRow toRow(Usuario usuario) {
    return UsuarioRow.builder()
        .id(usuario.id())
        .nombre(usuario.nombre())
        .email(usuario.email())
        .passwordHash(usuario.passwordHash())
        .activo(usuario.activo())
        .queseriaId(usuario.queseriaId())
        .createdAt(usuario.creadoEn())
        .updatedAt(usuario.actualizadoEn())
        .build();
  }
}
