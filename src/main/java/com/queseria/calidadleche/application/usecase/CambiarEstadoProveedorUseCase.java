package com.queseria.calidadleche.application.usecase;

import com.queseria.calidadleche.domain.model.Proveedor;
import com.queseria.calidadleche.domain.repo.ProveedorRepository;
import reactor.core.publisher.Mono;

public class CambiarEstadoProveedorUseCase {

  private final ProveedorRepository proveedorRepo;

  public CambiarEstadoProveedorUseCase(ProveedorRepository proveedorRepo) {
    this.proveedorRepo = proveedorRepo;
  }

  public Mono<Proveedor> activar(Long id) {
    return proveedorRepo.findById(id)
        .flatMap(proveedor -> proveedorRepo.save(proveedor.activar()));
  }

  public Mono<Proveedor> desactivar(Long id) {
    return proveedorRepo.findById(id)
        .flatMap(proveedor -> proveedorRepo.save(proveedor.desactivar()));
  }
}
