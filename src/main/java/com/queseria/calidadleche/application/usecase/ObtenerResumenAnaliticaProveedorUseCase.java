package com.queseria.calidadleche.application.usecase;

import com.queseria.calidadleche.application.port.AnaliticaConsultaRepository;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

public class ObtenerResumenAnaliticaProveedorUseCase {

  private final AnaliticaConsultaRepository analiticaConsultaRepository;

  public ObtenerResumenAnaliticaProveedorUseCase(AnaliticaConsultaRepository analiticaConsultaRepository) {
    this.analiticaConsultaRepository = analiticaConsultaRepository;
  }

  public Mono<Map<String, Object>> execute(Long proveedorId, Instant desde, Instant hasta) {
    return analiticaConsultaRepository.obtenerResumenProveedor(proveedorId, desde, hasta);
  }
}
