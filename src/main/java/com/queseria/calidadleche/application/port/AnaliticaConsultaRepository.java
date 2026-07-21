package com.queseria.calidadleche.application.port;

import com.queseria.calidadleche.application.model.AnaliticaMuestraConsulta;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import reactor.core.publisher.Flux;

public interface AnaliticaConsultaRepository {

  Mono<AnaliticaMuestraConsulta> buscarUltimaPorMuestra(Long sampleId);

  Flux<MetadataAnalitica> buscarMetadatosPorMuestras(Collection<Long> sampleIds);

  Mono<Map<String, Object>> obtenerResumenProveedor(Long proveedorId, Instant desde, Instant hasta);

  record MetadataAnalitica(Long sampleId, Instant fechaMuestra, String estadoGeneral) {}
}
