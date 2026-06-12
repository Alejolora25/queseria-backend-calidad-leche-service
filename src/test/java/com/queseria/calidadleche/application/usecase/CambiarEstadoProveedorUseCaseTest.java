package com.queseria.calidadleche.application.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.queseria.calidadleche.domain.model.Proveedor;
import com.queseria.calidadleche.domain.repo.ProveedorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class CambiarEstadoProveedorUseCaseTest {

  @Mock
  private ProveedorRepository proveedorRepo;

  @Test
  void activarDebeGuardarProveedorActivo() {
    Proveedor inactivo = Proveedor.reconstruir(5L, "Proveedor", "CC", "123", false, null, null);
    Proveedor activado = Proveedor.reconstruir(5L, "Proveedor", "CC", "123", true, inactivo.creadoEn(), null);

    when(proveedorRepo.findById(5L)).thenReturn(Mono.just(inactivo));
    when(proveedorRepo.save(any(Proveedor.class))).thenReturn(Mono.just(activado));

    var useCase = new CambiarEstadoProveedorUseCase(proveedorRepo);

    StepVerifier.create(useCase.activar(5L))
        .expectNext(activado)
        .verifyComplete();

    ArgumentCaptor<Proveedor> captor = ArgumentCaptor.forClass(Proveedor.class);
    verify(proveedorRepo).save(captor.capture());
    org.assertj.core.api.Assertions.assertThat(captor.getValue().activo()).isTrue();
  }

  @Test
  void desactivarDebeGuardarProveedorInactivo() {
    Proveedor activo = Proveedor.reconstruir(5L, "Proveedor", "CC", "123", true, null, null);
    Proveedor desactivado = Proveedor.reconstruir(5L, "Proveedor", "CC", "123", false, activo.creadoEn(), null);

    when(proveedorRepo.findById(5L)).thenReturn(Mono.just(activo));
    when(proveedorRepo.save(any(Proveedor.class))).thenReturn(Mono.just(desactivado));

    var useCase = new CambiarEstadoProveedorUseCase(proveedorRepo);

    StepVerifier.create(useCase.desactivar(5L))
        .expectNext(desactivado)
        .verifyComplete();

    ArgumentCaptor<Proveedor> captor = ArgumentCaptor.forClass(Proveedor.class);
    verify(proveedorRepo).save(captor.capture());
    org.assertj.core.api.Assertions.assertThat(captor.getValue().activo()).isFalse();
  }

  @Test
  void activarDebeCompletarVacioCuandoProveedorNoExiste() {
    when(proveedorRepo.findById(99L)).thenReturn(Mono.empty());

    var useCase = new CambiarEstadoProveedorUseCase(proveedorRepo);

    StepVerifier.create(useCase.activar(99L)).verifyComplete();

    verify(proveedorRepo, never()).save(any(Proveedor.class));
  }

  @Test
  void desactivarDebeCompletarVacioCuandoProveedorNoExiste() {
    when(proveedorRepo.findById(99L)).thenReturn(Mono.empty());

    var useCase = new CambiarEstadoProveedorUseCase(proveedorRepo);

    StepVerifier.create(useCase.desactivar(99L)).verifyComplete();

    verify(proveedorRepo, never()).save(any(Proveedor.class));
  }
}
