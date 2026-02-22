package com.queseria.calidadleche.domain.repo;

import com.queseria.calidadleche.domain.model.MuestraLeche;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

public interface MuestraRepository {
  Mono<MuestraLeche> save(MuestraLeche muestra);
  Mono<MuestraLeche> findById(Long id);
  Flux<MuestraLeche> findByProveedorAndRango(Long proveedorId, OffsetDateTime desde, OffsetDateTime hasta, int limit, int offset);

  Flux<MuestraLeche> findByProveedorAndRangoPaged(Long proveedorId, OffsetDateTime desde, OffsetDateTime hasta, int limit, int offset);
  Mono<Long> countByProveedorAndRango(Long proveedorId, OffsetDateTime desde, OffsetDateTime hasta);
}