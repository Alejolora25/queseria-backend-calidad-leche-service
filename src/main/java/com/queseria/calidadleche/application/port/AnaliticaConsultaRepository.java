package com.queseria.calidadleche.application.port;

import com.queseria.calidadleche.application.model.AnaliticaMuestraConsulta;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

public interface AnaliticaConsultaRepository {

  Mono<AnaliticaMuestraConsulta> buscarUltimaPorMuestra(Long sampleId);

  Mono<Map<String, Object>> obtenerResumenProveedor(Long proveedorId, Instant desde, Instant hasta);
}
