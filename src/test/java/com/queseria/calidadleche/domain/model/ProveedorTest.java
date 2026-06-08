package com.queseria.calidadleche.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

class ProveedorTest {

  @Test
  void crearDebeConservarTipoEIdentificacionEnElOrdenCorrecto() {
    Proveedor proveedor = Proveedor.crear("  Finca La Esperanza  ", "NIT", "900123456");

    assertThat(proveedor.id()).isNull();
    assertThat(proveedor.nombre()).isEqualTo("Finca La Esperanza");
    assertThat(proveedor.tipoIdentificacion()).isEqualTo("NIT");
    assertThat(proveedor.identificacion()).isEqualTo("900123456");
    assertThat(proveedor.activo()).isTrue();
    assertThat(proveedor.creadoEn()).isNotNull();
    assertThat(proveedor.actualizadoEn()).isEqualTo(proveedor.creadoEn());
  }

  @Test
  void reconstruirDebeMantenerDatosPersistidos() {
    OffsetDateTime creadoEn = OffsetDateTime.parse("2026-01-10T08:00:00-05:00");
    OffsetDateTime actualizadoEn = OffsetDateTime.parse("2026-01-11T09:00:00-05:00");

    Proveedor proveedor = Proveedor.reconstruir(
        7L, "Proveedor Norte", "CC", "123456", false, creadoEn, actualizadoEn);

    assertThat(proveedor.id()).isEqualTo(7L);
    assertThat(proveedor.nombre()).isEqualTo("Proveedor Norte");
    assertThat(proveedor.tipoIdentificacion()).isEqualTo("CC");
    assertThat(proveedor.identificacion()).isEqualTo("123456");
    assertThat(proveedor.activo()).isFalse();
    assertThat(proveedor.creadoEn()).isEqualTo(creadoEn);
    assertThat(proveedor.actualizadoEn()).isEqualTo(actualizadoEn);
  }

  @Test
  void desactivarDebeRetornarProveedorInactivoSinCambiarIdentidad() {
    OffsetDateTime creadoEn = OffsetDateTime.parse("2026-01-10T08:00:00-05:00");
    Proveedor proveedor = Proveedor.reconstruir(
        3L, "Proveedor Sur", "CC", "444", true, creadoEn, creadoEn);

    Proveedor desactivado = proveedor.desactivar();

    assertThat(desactivado.id()).isEqualTo(3L);
    assertThat(desactivado.nombre()).isEqualTo("Proveedor Sur");
    assertThat(desactivado.tipoIdentificacion()).isEqualTo("CC");
    assertThat(desactivado.identificacion()).isEqualTo("444");
    assertThat(desactivado.activo()).isFalse();
    assertThat(desactivado.creadoEn()).isEqualTo(creadoEn);
    assertThat(desactivado.actualizadoEn()).isAfter(creadoEn);
  }
}
