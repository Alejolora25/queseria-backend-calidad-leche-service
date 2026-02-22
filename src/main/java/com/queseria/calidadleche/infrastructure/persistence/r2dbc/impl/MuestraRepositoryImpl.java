package com.queseria.calidadleche.infrastructure.persistence.r2dbc.impl;

import com.queseria.calidadleche.domain.model.MuestraLeche;
import com.queseria.calidadleche.domain.repo.MuestraRepository;
import com.queseria.calidadleche.infrastructure.persistence.r2dbc.mapper.MuestraRowMapper;
import com.queseria.calidadleche.infrastructure.persistence.r2dbc.repo.MuestraR2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@Repository
public class MuestraRepositoryImpl implements MuestraRepository {
  private final MuestraR2dbcRepository repo;
  public MuestraRepositoryImpl(MuestraR2dbcRepository repo) { this.repo = repo; }

  @Override public Mono<MuestraLeche> save(MuestraLeche m) {
    return repo.save(MuestraRowMapper.toRow(m)).map(MuestraRowMapper::toDomain);
  }
  @Override public Mono<MuestraLeche> findById(Long id) {
    return repo.findById(id).map(MuestraRowMapper::toDomain);
  }
  @Override public Flux<MuestraLeche> findByProveedorAndRango(Long proveedorId, OffsetDateTime desde, OffsetDateTime hasta, int limit, int offset) {
    // (legacy/simple) sin paginar por DB
    return repo.findByProveedorIdAndFechaMuestraBetween(proveedorId, desde, hasta)
               .map(MuestraRowMapper::toDomain);
  }

  @Override
  public Flux<MuestraLeche> findByProveedorAndRangoPaged(Long proveedorId, OffsetDateTime desde, OffsetDateTime hasta, int limit, int offset) {
    return repo.findPaged(proveedorId, desde, hasta, limit, offset)
               .map(MuestraRowMapper::toDomain);
  }

  @Override
  public Mono<Long> countByProveedorAndRango(Long proveedorId, OffsetDateTime desde, OffsetDateTime hasta) {
    return repo.countByProveedorAndRango(proveedorId, desde, hasta);
  }
}