package com.queseria.calidadleche.application.usecase;

import com.queseria.calidadleche.domain.model.Proveedor;
import com.queseria.calidadleche.domain.repo.ProveedorRepository;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

public class BuscarProveedorUseCase {
  private final ProveedorRepository repo;
  public BuscarProveedorUseCase(ProveedorRepository repo) { this.repo = repo; }

  public Mono<Proveedor> porId(Long id) { return repo.findById(id); }
  public Mono<Proveedor> porIdentificacion(String ident) { return repo.findByIdentificacion(ident); }
  public Flux<Proveedor> listar(String q, Boolean activo, int limit, int offset) {
    return repo.searchPaged(q, activo, limit, offset);
  }

  public Mono<Long> contar(String q, Boolean activo) {
    return repo.countFiltered(q, activo);
  }
}