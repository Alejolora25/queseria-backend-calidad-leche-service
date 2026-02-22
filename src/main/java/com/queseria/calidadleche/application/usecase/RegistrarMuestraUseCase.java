package com.queseria.calidadleche.application.usecase;

import com.queseria.calidadleche.domain.model.MuestraLeche;
import com.queseria.calidadleche.domain.repo.MuestraRepository;
import reactor.core.publisher.Mono;

public class RegistrarMuestraUseCase {
  private final MuestraRepository muestraRepo;
  public RegistrarMuestraUseCase(MuestraRepository muestraRepo) { this.muestraRepo = muestraRepo; }
  public Mono<MuestraLeche> ejecutar(MuestraLeche muestra) {
    return muestraRepo.save(muestra);
  }
}