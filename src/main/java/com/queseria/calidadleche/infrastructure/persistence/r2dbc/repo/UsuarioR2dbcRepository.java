package com.queseria.calidadleche.infrastructure.persistence.r2dbc.repo;

import com.queseria.calidadleche.infrastructure.persistence.r2dbc.row.UsuarioRow;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UsuarioR2dbcRepository extends ReactiveCrudRepository<UsuarioRow, Long> {
  Mono<UsuarioRow> findByEmail(String email);
  Mono<Boolean> existsByEmail(String email);
}
