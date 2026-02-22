package com.queseria.calidadleche.infrastructure.config;

import com.queseria.calidadleche.application.usecase.BuscarProveedorUseCase;
import com.queseria.calidadleche.application.usecase.CrearProveedorUseCase;
import com.queseria.calidadleche.application.usecase.RegistrarMuestraUseCase;
import com.queseria.calidadleche.domain.repo.MuestraRepository;
import com.queseria.calidadleche.domain.repo.ProveedorRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {
  @Bean CrearProveedorUseCase crearProveedorUseCase(ProveedorRepository repo) {
    return new CrearProveedorUseCase(repo);
  }
  @Bean RegistrarMuestraUseCase registrarMuestraUseCase(MuestraRepository repo) {
    return new RegistrarMuestraUseCase(repo);
  }
  @Bean BuscarProveedorUseCase buscarProveedorUseCase(ProveedorRepository repo) {
    return new BuscarProveedorUseCase(repo);
  }
}