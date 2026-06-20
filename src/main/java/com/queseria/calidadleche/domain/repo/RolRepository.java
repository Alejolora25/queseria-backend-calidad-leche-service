package com.queseria.calidadleche.domain.repo;

import com.queseria.calidadleche.domain.model.NombreRol;
import com.queseria.calidadleche.domain.model.Rol;
import reactor.core.publisher.Mono;

public interface RolRepository {
  Mono<Rol> findByNombre(NombreRol nombre);
}
