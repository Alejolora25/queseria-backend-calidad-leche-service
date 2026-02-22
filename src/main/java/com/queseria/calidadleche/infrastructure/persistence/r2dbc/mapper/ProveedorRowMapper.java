package com.queseria.calidadleche.infrastructure.persistence.r2dbc.mapper;

import com.queseria.calidadleche.domain.model.Proveedor;
import com.queseria.calidadleche.infrastructure.persistence.r2dbc.row.ProveedorRow;

public final class ProveedorRowMapper {
  private ProveedorRowMapper(){}

  public static Proveedor toDomain(ProveedorRow r) {
    return Proveedor.reconstruir(
      r.getId(), r.getNombre(), r.getTipoIdentificacion(), r.getIdentificacion(),
      Boolean.TRUE.equals(r.getActivo()),
      r.getCreatedAt(), r.getUpdatedAt()
    );
  }

  public static ProveedorRow toRow(Proveedor p) {
    return ProveedorRow.builder()
        .id(p.id()) // null => INSERT (la BD genera)
        .nombre(p.nombre())
        .tipoIdentificacion(p.tipoIdentificacion())
        .identificacion(p.identificacion())
        .activo(p.activo())
        .createdAt(p.creadoEn())
        .updatedAt(p.actualizadoEn())
        .build();
  }
}