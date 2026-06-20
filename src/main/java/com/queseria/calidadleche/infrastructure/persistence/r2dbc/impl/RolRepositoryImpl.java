package com.queseria.calidadleche.infrastructure.persistence.r2dbc.impl;

import com.queseria.calidadleche.domain.model.NombreRol;
import com.queseria.calidadleche.domain.model.Rol;
import com.queseria.calidadleche.domain.repo.RolRepository;
import com.queseria.calidadleche.infrastructure.persistence.r2dbc.mapper.RolRowMapper;
import com.queseria.calidadleche.infrastructure.persistence.r2dbc.repo.RolR2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class RolRepositoryImpl implements RolRepository {
  private final RolR2dbcRepository repo;

  public RolRepositoryImpl(RolR2dbcRepository repo) {
    this.repo = repo;
  }

  @Override
  public Mono<Rol> findByNombre(NombreRol nombre) {
    return repo.findByNombre(nombre.name()).map(RolRowMapper::toDomain);
  }
}
