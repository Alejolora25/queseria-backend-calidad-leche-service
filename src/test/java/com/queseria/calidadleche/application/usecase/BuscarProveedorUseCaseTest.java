package com.queseria.calidadleche.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.queseria.calidadleche.domain.model.Proveedor;
import com.queseria.calidadleche.domain.repo.ProveedorRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class BuscarProveedorUseCaseTest {

  @Mock
  private ProveedorRepository repo;

  @Test
  void porIdDebeDelegarEnRepositorio() {
    Proveedor proveedor = Proveedor.reconstruir(1L, "Proveedor", "CC", "111", true, null, null);
    when(repo.findById(1L)).thenReturn(Mono.just(proveedor));

    BuscarProveedorUseCase useCase = new BuscarProveedorUseCase(repo);

    StepVerifier.create(useCase.porId(1L))
        .expectNext(proveedor)
        .verifyComplete();

    verify(repo).findById(1L);
  }

  @Test
  void porIdentificacionDebeDelegarEnRepositorio() {
    Proveedor proveedor = Proveedor.reconstruir(2L, "Proveedor", "NIT", "900", true, null, null);
    when(repo.findByIdentificacion("900")).thenReturn(Mono.just(proveedor));

    BuscarProveedorUseCase useCase = new BuscarProveedorUseCase(repo);

    StepVerifier.create(useCase.porIdentificacion("900"))
        .expectNext(proveedor)
        .verifyComplete();

    verify(repo).findByIdentificacion("900");
  }

  @Test
  void listarDebeUsarBusquedaPaginada() {
    Proveedor p1 = Proveedor.reconstruir(1L, "A", "CC", "1", true, null, null);
    Proveedor p2 = Proveedor.reconstruir(2L, "B", "CC", "2", true, null, null);
    when(repo.searchPaged("fin", true, 20, 40)).thenReturn(Flux.just(p1, p2));

    BuscarProveedorUseCase useCase = new BuscarProveedorUseCase(repo);

    StepVerifier.create(useCase.listar("fin", true, 20, 40))
        .recordWith(java.util.ArrayList::new)
        .expectNextCount(2)
        .consumeRecordedWith(items -> assertThat(items).containsExactly(p1, p2))
        .verifyComplete();

    verify(repo).searchPaged("fin", true, 20, 40);
  }

  @Test
  void contarDebeDelegarFiltro() {
    when(repo.countFiltered("", null)).thenReturn(Mono.just(5L));

    BuscarProveedorUseCase useCase = new BuscarProveedorUseCase(repo);

    StepVerifier.create(useCase.contar("", null))
        .expectNext(5L)
        .verifyComplete();

    verify(repo).countFiltered("", null);
  }
}
