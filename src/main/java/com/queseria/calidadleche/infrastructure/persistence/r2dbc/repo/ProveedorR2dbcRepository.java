package com.queseria.calidadleche.infrastructure.persistence.r2dbc.repo;

import com.queseria.calidadleche.infrastructure.persistence.r2dbc.row.ProveedorRow;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProveedorR2dbcRepository extends ReactiveCrudRepository<ProveedorRow, Long> {
  Mono<ProveedorRow> findByIdentificacion(String identificacion);

  @Query("""
    SELECT * FROM proveedores
    WHERE (:q = '' OR nombre ILIKE '%' || :q || '%' OR identificacion ILIKE '%' || :q || '%')
      AND (:activo IS NULL OR activo = :activo)
    ORDER BY id DESC
    LIMIT :limit OFFSET :offset
  """)
  Flux<ProveedorRow> findPaged(String q, Boolean activo, int limit, int offset);

  @Query("""
    SELECT COUNT(*) FROM proveedores
    WHERE (:q = '' OR nombre ILIKE '%' || :q || '%' OR identificacion ILIKE '%' || :q || '%')
      AND (:activo IS NULL OR activo = :activo)
  """)
  Mono<Long> countFiltered(String q, Boolean activo);
}