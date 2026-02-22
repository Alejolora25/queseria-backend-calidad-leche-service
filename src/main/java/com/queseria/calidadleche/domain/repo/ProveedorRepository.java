package com.queseria.calidadleche.domain.repo;

import com.queseria.calidadleche.domain.model.Proveedor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProveedorRepository {
  Mono<Proveedor> save(Proveedor proveedor);
  Mono<Proveedor> findById(Long id);
  Mono<Proveedor> findByIdentificacion(String identificacion);
  Flux<Proveedor> search(String q, Boolean activo, int limit, int offset);
  Flux<Proveedor> searchPaged(String q, Boolean activo, int limit, int offset);
  Mono<Long> countFiltered(String q, Boolean activo);
}