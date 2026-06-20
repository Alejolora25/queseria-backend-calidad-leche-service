package com.queseria.calidadleche.infrastructure.persistence.r2dbc.repo;

import com.queseria.calidadleche.infrastructure.persistence.r2dbc.row.RolRow;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface RolR2dbcRepository extends ReactiveCrudRepository<RolRow, Long> {
  Mono<RolRow> findByNombre(String nombre);
}
