package com.queseria.calidadleche.application.usecase;

import com.queseria.calidadleche.domain.model.Proveedor;
import com.queseria.calidadleche.domain.repo.ProveedorRepository;
import reactor.core.publisher.Mono;

public class CrearProveedorUseCase {
  private final ProveedorRepository proveedorRepo;
  public CrearProveedorUseCase(ProveedorRepository proveedorRepo) { this.proveedorRepo = proveedorRepo; }

  public Mono<Proveedor> ejecutar(String nombre, String tipoIdentificacion, String identificacion) {
    var nuevo = Proveedor.crear(nombre, tipoIdentificacion, identificacion);
    return proveedorRepo.findByIdentificacion(identificacion)
      .flatMap(ex -> Mono.<Proveedor>error(new IllegalArgumentException("Identificación ya existe")))
      .switchIfEmpty(proveedorRepo.save(nuevo));
  }
}
