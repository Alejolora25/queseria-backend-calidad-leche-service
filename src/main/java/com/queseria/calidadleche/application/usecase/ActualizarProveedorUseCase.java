package com.queseria.calidadleche.application.usecase;

import com.queseria.calidadleche.domain.model.Proveedor;
import com.queseria.calidadleche.domain.repo.ProveedorRepository;
import reactor.core.publisher.Mono;

import java.util.Objects;

public class ActualizarProveedorUseCase {

  private final ProveedorRepository proveedorRepo;

  public ActualizarProveedorUseCase(ProveedorRepository proveedorRepo) {
    this.proveedorRepo = proveedorRepo;
  }

  public Mono<Proveedor> ejecutar(Long id, String nombre, String tipoIdentificacion, String identificacion) {
    return proveedorRepo.findById(id)
        .flatMap(actual -> validarIdentificacionDisponible(id, identificacion)
            .then(Mono.defer(() -> proveedorRepo.save(
                actual.actualizarDatos(nombre, tipoIdentificacion, identificacion)
            ))));
  }

  private Mono<Void> validarIdentificacionDisponible(Long id, String identificacion) {
    return proveedorRepo.findByIdentificacion(identificacion)
        .filter(existente -> !Objects.equals(existente.id(), id))
        .flatMap(existente -> Mono.<Void>error(new IllegalArgumentException("Identificacion ya existe")))
        .then();
  }
}
