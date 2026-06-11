package com.queseria.calidadleche.application.usecase;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.queseria.calidadleche.application.port.AnaliticaConsultaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class ObtenerResumenAnaliticaProveedorUseCaseTest {

  @Mock
  private AnaliticaConsultaRepository analiticaConsultaRepository;

  @Test
  void executeDebeDelegarResumenPorProveedorYRango() {
    Instant desde = Instant.parse("2026-01-01T00:00:00Z");
    Instant hasta = Instant.parse("2026-01-31T23:59:59Z");
    Map<String, Object> resumen = Map.of(
        "promedios", Map.of("grasa", 3.8),
        "distribucionEstados", Map.of("ACEPTABLE", 2L)
    );
    when(analiticaConsultaRepository.obtenerResumenProveedor(4L, desde, hasta))
        .thenReturn(Mono.just(resumen));

    var useCase = new ObtenerResumenAnaliticaProveedorUseCase(analiticaConsultaRepository);

    StepVerifier.create(useCase.execute(4L, desde, hasta))
        .expectNext(resumen)
        .verifyComplete();

    verify(analiticaConsultaRepository).obtenerResumenProveedor(4L, desde, hasta);
  }
}
