package com.queseria.calidadleche.infrastructure.config;

import com.queseria.calidadleche.application.port.AnaliticaRepository;
import com.queseria.calidadleche.application.port.AnaliticaConsultaRepository;
import com.queseria.calidadleche.application.port.AccessTokenProvider;
import com.queseria.calidadleche.application.port.PasswordVerifier;
import com.queseria.calidadleche.application.usecase.ActualizarProveedorUseCase;
import com.queseria.calidadleche.application.usecase.BuscarAnaliticaPorMuestraUseCase;
import com.queseria.calidadleche.application.usecase.BuscarProveedorUseCase;
import com.queseria.calidadleche.application.usecase.CambiarEstadoProveedorUseCase;
import com.queseria.calidadleche.application.usecase.CrearProveedorUseCase;
import com.queseria.calidadleche.application.usecase.ObtenerResumenAnaliticaProveedorUseCase;
import com.queseria.calidadleche.application.usecase.RegistrarMuestraConEvaluacionUseCase;
import com.queseria.calidadleche.application.usecase.RegistrarMuestraUseCase;
import com.queseria.calidadleche.application.usecase.LoginUseCase;
import com.queseria.calidadleche.domain.repo.MuestraRepository;
import com.queseria.calidadleche.domain.repo.ProveedorRepository;
import com.queseria.calidadleche.domain.repo.UsuarioRepository;
import com.queseria.calidadleche.domain.service.EvaluacionCalidadService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {
  @Bean LoginUseCase loginUseCase(
      UsuarioRepository usuarioRepository,
      PasswordVerifier passwordVerifier,
      AccessTokenProvider accessTokenProvider
  ) {
    return new LoginUseCase(usuarioRepository, passwordVerifier, accessTokenProvider);
  }
  @Bean CrearProveedorUseCase crearProveedorUseCase(ProveedorRepository repo) {
    return new CrearProveedorUseCase(repo);
  }
  @Bean ActualizarProveedorUseCase actualizarProveedorUseCase(ProveedorRepository repo) {
    return new ActualizarProveedorUseCase(repo);
  }
  @Bean CambiarEstadoProveedorUseCase cambiarEstadoProveedorUseCase(ProveedorRepository repo) {
    return new CambiarEstadoProveedorUseCase(repo);
  }
  @Bean RegistrarMuestraUseCase registrarMuestraUseCase(MuestraRepository repo) {
    return new RegistrarMuestraUseCase(repo);
  }
  @Bean RegistrarMuestraConEvaluacionUseCase registrarMuestraConEvaluacionUseCase(
      MuestraRepository muestraRepo,
      EvaluacionCalidadService evaluacionService,
      AnaliticaRepository analiticaRepository
  ) {
    return new RegistrarMuestraConEvaluacionUseCase(muestraRepo, evaluacionService, analiticaRepository);
  }
  @Bean BuscarAnaliticaPorMuestraUseCase buscarAnaliticaPorMuestraUseCase(
      AnaliticaConsultaRepository analiticaConsultaRepository
  ) {
    return new BuscarAnaliticaPorMuestraUseCase(analiticaConsultaRepository);
  }
  @Bean ObtenerResumenAnaliticaProveedorUseCase obtenerResumenAnaliticaProveedorUseCase(
      AnaliticaConsultaRepository analiticaConsultaRepository
  ) {
    return new ObtenerResumenAnaliticaProveedorUseCase(analiticaConsultaRepository);
  }
  @Bean BuscarProveedorUseCase buscarProveedorUseCase(ProveedorRepository repo) {
    return new BuscarProveedorUseCase(repo);
  }
}
