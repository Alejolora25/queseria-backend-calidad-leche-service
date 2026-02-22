package com.queseria.calidadleche.infrastructure.persistence.r2dbc.impl;

import com.queseria.calidadleche.domain.model.Proveedor;
import com.queseria.calidadleche.domain.repo.ProveedorRepository;
import com.queseria.calidadleche.infrastructure.persistence.r2dbc.mapper.ProveedorRowMapper;
import com.queseria.calidadleche.infrastructure.persistence.r2dbc.repo.ProveedorR2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class ProveedorRepositoryImpl implements ProveedorRepository {
  private final ProveedorR2dbcRepository repo;
  public ProveedorRepositoryImpl(ProveedorR2dbcRepository repo) { this.repo = repo; }

  @Override public Mono<Proveedor> save(Proveedor p) {
    return repo.save(ProveedorRowMapper.toRow(p)).map(ProveedorRowMapper::toDomain);
  }
  @Override public Mono<Proveedor> findById(Long id) {
    return repo.findById(id).map(ProveedorRowMapper::toDomain);
  }
  @Override public Mono<Proveedor> findByIdentificacion(String identificacion) {
    return repo.findByIdentificacion(identificacion).map(ProveedorRowMapper::toDomain);
  }
  @Override public Flux<Proveedor> search(String q, Boolean activo, int limit, int offset) {
    return repo.findAll().map(ProveedorRowMapper::toDomain);
  }
  @Override
  public Flux<Proveedor> searchPaged(String q, Boolean activo, int limit, int offset) {
    String safeQ = (q == null) ? "" : q.trim();
    return repo.findPaged(safeQ, activo, limit, offset)
        .map(ProveedorRowMapper::toDomain);
  }

  @Override
  public Mono<Long> countFiltered(String q, Boolean activo) {
    String safeQ = (q == null) ? "" : q.trim();
    return repo.countFiltered(safeQ, activo);
  }
}