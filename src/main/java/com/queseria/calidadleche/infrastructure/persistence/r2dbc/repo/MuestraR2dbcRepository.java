package com.queseria.calidadleche.infrastructure.persistence.r2dbc.repo;

import com.queseria.calidadleche.infrastructure.persistence.r2dbc.row.MuestraLecheRow;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

public interface MuestraR2dbcRepository extends ReactiveCrudRepository<MuestraLecheRow, Long> {
  Flux<MuestraLecheRow> findByProveedorIdAndFechaMuestraBetween(Long proveedorId, OffsetDateTime desde, OffsetDateTime hasta);

  @Query("""
    SELECT * FROM muestras_leche
    WHERE proveedor_id = :proveedorId
      AND fecha_muestra BETWEEN :desde AND :hasta
    ORDER BY fecha_muestra DESC
    LIMIT :limit OFFSET :offset
  """)
  Flux<MuestraLecheRow> findPaged(Long proveedorId, OffsetDateTime desde, OffsetDateTime hasta, int limit, int offset);

  @Query("""
    SELECT COUNT(*) FROM muestras_leche
    WHERE proveedor_id = :proveedorId
      AND fecha_muestra BETWEEN :desde AND :hasta
  """)
  Mono<Long> countByProveedorAndRango(Long proveedorId, OffsetDateTime desde, OffsetDateTime hasta);
}
