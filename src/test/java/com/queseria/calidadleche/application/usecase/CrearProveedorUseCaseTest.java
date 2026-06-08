package com.queseria.calidadleche.application.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.queseria.calidadleche.domain.model.Proveedor;
import com.queseria.calidadleche.domain.repo.ProveedorRepository;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class CrearProveedorUseCaseTest {

  @Mock
  private ProveedorRepository proveedorRepo;

  @Test
  void ejecutarDebeCrearProveedorCuandoIdentificacionNoExiste() {
    when(proveedorRepo.findByIdentificacion("900123456")).thenReturn(Mono.empty());
    when(proveedorRepo.save(any(Proveedor.class))).thenAnswer(invocation -> {
      Proveedor p = invocation.getArgument(0);
      return Mono.just(Proveedor.reconstruir(
          10L, p.nombre(), p.tipoIdentificacion(), p.identificacion(), p.activo(),
          p.creadoEn(), p.actualizadoEn()));
    });

    CrearProveedorUseCase useCase = new CrearProveedorUseCase(proveedorRepo);

    StepVerifier.create(useCase.ejecutar("Finca Central", "NIT", "900123456"))
        .expectNextMatches(p ->
            p.id().equals(10L)
                && p.nombre().equals("Finca Central")
                && p.tipoIdentificacion().equals("NIT")
                && p.identificacion().equals("900123456")
                && p.activo())
        .verifyComplete();

    ArgumentCaptor<Proveedor> captor = ArgumentCaptor.forClass(Proveedor.class);
    verify(proveedorRepo).save(captor.capture());
  }

  @Test
  void ejecutarDebeFallarCuandoIdentificacionYaExiste() {
    Proveedor existente = Proveedor.reconstruir(
        2L, "Proveedor Existente", "CC", "123456", true, null, null);
    when(proveedorRepo.findByIdentificacion("123456")).thenReturn(Mono.just(existente));

    CrearProveedorUseCase useCase = new CrearProveedorUseCase(proveedorRepo);

    StepVerifier.create(useCase.ejecutar("Nuevo", "CC", "123456"))
        .expectErrorMatches(error ->
            error instanceof IllegalArgumentException
                && error.getMessage().contains("Identificaci"))
        .verify();

    verify(proveedorRepo, never()).save(any(Proveedor.class));
  }
}
