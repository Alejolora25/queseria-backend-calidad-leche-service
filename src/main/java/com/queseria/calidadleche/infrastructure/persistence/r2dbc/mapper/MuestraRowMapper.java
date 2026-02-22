package com.queseria.calidadleche.infrastructure.persistence.r2dbc.mapper;

import com.queseria.calidadleche.domain.model.*;
import com.queseria.calidadleche.infrastructure.persistence.r2dbc.row.MuestraLecheRow;

public final class MuestraRowMapper {
  private MuestraRowMapper(){}

  public static MuestraLeche toDomain(MuestraLecheRow r) {
    var comp = new Composicion(r.getGrasa(), r.getProteina(), r.getLactosa(), r.getSolidosTotales());
    var fq   = new FisicoQuimico(r.getDensidad(), r.getAcidezDornic(), r.getTemperaturaC());
    var hig  = new Higiene(r.getUfcBacterias(), r.getCcSomaticas());
    return MuestraLeche.reconstruir(
      r.getId(), r.getProveedorId(), r.getFechaMuestra(),
      r.getVolumenLitros(), r.getPrecioLitro(), r.getObservaciones(),
      comp, fq, hig,
      r.getAguaPct(),                          
    r.getSng(),                             
    r.getEquipo() == null ? null : Equipo.valueOf(r.getEquipo()),
    r.getCondicion() == null ? null : CondicionMuestra.valueOf(r.getCondicion())
    );
  }

  public static MuestraLecheRow toRow(MuestraLeche m) {
    return MuestraLecheRow.builder()
        .id(m.id()) // null => INSERT
        .proveedorId(m.proveedorId())
        .fechaMuestra(m.fechaMuestra())
        .volumenLitros(m.volumenLitros())
        .precioLitro(m.precioLitro())
        .temperaturaC(m.fisicoQuimico().temperaturaC())
        .grasa(m.composicion().grasa())
        .proteina(m.composicion().proteina())
        .lactosa(m.composicion().lactosa())
        .solidosTotales(m.composicion().solidosTotales())
        .densidad(m.fisicoQuimico().densidad())
        .acidezDornic(m.fisicoQuimico().acidezDornic())
        .ufcBacterias(m.higiene().ufcBacterias())
        .ccSomaticas(m.higiene().ccSomaticas())
        .observaciones(m.observaciones())
        .aguaPct(m.aguaPct())
        .sng(m.sng())
        .equipo(m.equipo() == null ? null : m.equipo().name())
        .condicion(m.condicion() == null ? null : m.condicion().name())
        .build();
  }
}