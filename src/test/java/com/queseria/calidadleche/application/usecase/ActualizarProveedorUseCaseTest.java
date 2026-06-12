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
class ActualizarProveedorUseCaseTest {

  @Mock
  private ProveedorRepository proveedorRepo;

  @Test
  void ejecutarDebeActualizarDatosSinCambiarEstado() {
    Proveedor actual = Proveedor.reconstruir(10L, "Viejo", "CC", "123", false, null, null);
    Proveedor guardado = Proveedor.reconstruir(10L, "Nuevo", "NIT", "900", false, actual.creadoEn(), null);

    when(proveedorRepo.findById(10L)).thenReturn(Mono.just(actual));
    when(proveedorRepo.findByIdentificacion("900")).thenReturn(Mono.empty());
    when(proveedorRepo.save(any(Proveedor.class))).thenReturn(Mono.just(guardado));

    var useCase = new ActualizarProveedorUseCase(proveedorRepo);

    StepVerifier.create(useCase.ejecutar(10L, "Nuevo", "NIT", "900"))
        .expectNext(guardado)
        .verifyComplete();

    ArgumentCaptor<Proveedor> captor = ArgumentCaptor.forClass(Proveedor.class);
    verify(proveedorRepo).save(captor.capture());
    Proveedor enviado = captor.getValue();
    org.assertj.core.api.Assertions.assertThat(enviado.activo()).isFalse();
    org.assertj.core.api.Assertions.assertThat(enviado.nombre()).isEqualTo("Nuevo");
    org.assertj.core.api.Assertions.assertThat(enviado.tipoIdentificacion()).isEqualTo("NIT");
    org.assertj.core.api.Assertions.assertThat(enviado.identificacion()).isEqualTo("900");
  }

  @Test
  void ejecutarDebePermitirIdentificacionDelMismoProveedor() {
    Proveedor actual = Proveedor.reconstruir(10L, "Viejo", "CC", "123", true, null, null);
    Proveedor guardado = Proveedor.reconstruir(10L, "Nuevo", "CC", "123", true, actual.creadoEn(), null);

    when(proveedorRepo.findById(10L)).thenReturn(Mono.just(actual));
    when(proveedorRepo.findByIdentificacion("123")).thenReturn(Mono.just(actual));
    when(proveedorRepo.save(any(Proveedor.class))).thenReturn(Mono.just(guardado));

    var useCase = new ActualizarProveedorUseCase(proveedorRepo);

    StepVerifier.create(useCase.ejecutar(10L, "Nuevo", "CC", "123"))
        .expectNext(guardado)
        .verifyComplete();

    verify(proveedorRepo).save(any(Proveedor.class));
  }

  @Test
  void ejecutarDebeFallarCuandoIdentificacionPerteneceAOtroProveedor() {
    Proveedor actual = Proveedor.reconstruir(10L, "Actual", "CC", "123", true, null, null);
    Proveedor otro = Proveedor.reconstruir(11L, "Otro", "NIT", "900", true, null, null);

    when(proveedorRepo.findById(10L)).thenReturn(Mono.just(actual));
    when(proveedorRepo.findByIdentificacion("900")).thenReturn(Mono.just(otro));

    var useCase = new ActualizarProveedorUseCase(proveedorRepo);

    StepVerifier.create(useCase.ejecutar(10L, "Actualizado", "NIT", "900"))
        .expectErrorMatches(error ->
            error instanceof IllegalArgumentException
                && error.getMessage().equals("Identificacion ya existe"))
        .verify();

    verify(proveedorRepo, never()).save(any(Proveedor.class));
  }

  @Test
  void ejecutarDebeCompletarVacioCuandoProveedorNoExiste() {
    when(proveedorRepo.findById(99L)).thenReturn(Mono.empty());

    var useCase = new ActualizarProveedorUseCase(proveedorRepo);

    StepVerifier.create(useCase.ejecutar(99L, "Nuevo", "CC", "1"))
        .verifyComplete();

    verify(proveedorRepo, never()).findByIdentificacion(any());
    verify(proveedorRepo, never()).save(any(Proveedor.class));
  }
}
