package com.queseria.calidadleche.application.usecase;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.queseria.calidadleche.domain.model.Composicion;
import com.queseria.calidadleche.domain.model.FisicoQuimico;
import com.queseria.calidadleche.domain.model.Higiene;
import com.queseria.calidadleche.domain.model.MuestraLeche;
import com.queseria.calidadleche.domain.repo.MuestraRepository;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class RegistrarMuestraUseCaseTest {

  @Mock
  private MuestraRepository muestraRepo;

  @Test
  void ejecutarDebeGuardarLaMuestraEnRepositorio() {
    MuestraLeche muestra = muestraValida();
    MuestraLeche guardada = MuestraLeche.reconstruir(
        15L, muestra.proveedorId(), muestra.fechaMuestra(),
        muestra.volumenLitros(), muestra.precioLitro(), muestra.observaciones(),
        muestra.composicion(), muestra.fisicoQuimico(), muestra.higiene(),
        muestra.aguaPct(), muestra.sng(), muestra.equipo(), muestra.condicion());

    when(muestraRepo.save(muestra)).thenReturn(Mono.just(guardada));

    RegistrarMuestraUseCase useCase = new RegistrarMuestraUseCase(muestraRepo);

    StepVerifier.create(useCase.ejecutar(muestra))
        .expectNext(guardada)
        .verifyComplete();

    verify(muestraRepo).save(muestra);
  }

  private MuestraLeche muestraValida() {
    return MuestraLeche.registrar(
        4L,
        OffsetDateTime.parse("2026-01-10T08:00:00-05:00"),
        bd("120.50"),
        bd("1800"),
        "Muestra inicial",
        new Composicion(bd("4.0"), bd("3.2"), bd("4.7"), bd("13.0")),
        new FisicoQuimico(bd("1.032"), bd("15"), bd("18")),
        new Higiene(1000, 200000),
        null,
        null,
        null,
        null);
  }

  private static BigDecimal bd(String value) {
    return new BigDecimal(value);
  }
}
