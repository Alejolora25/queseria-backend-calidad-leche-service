package com.queseria.calidadleche.application.usecase;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.queseria.calidadleche.application.model.AnaliticaMuestraConsulta;
import com.queseria.calidadleche.application.port.AnaliticaConsultaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

@ExtendWith(MockitoExtension.class)
class BuscarAnaliticaPorMuestraUseCaseTest {

  @Mock
  private AnaliticaConsultaRepository analiticaConsultaRepository;

  @Test
  void executeDebeDelegarBusquedaPorMuestra() {
    AnaliticaMuestraConsulta analitica = new AnaliticaMuestraConsulta(
        "mongo-id",
        10L,
        4L,
        Instant.parse("2026-01-10T13:00:00Z"),
        null,
        null,
        "hash",
        java.util.List.of("ACEPTABLE"),
        java.math.BigDecimal.ONE
    );
    when(analiticaConsultaRepository.buscarUltimaPorMuestra(10L))
        .thenReturn(Mono.just(analitica));

    var useCase = new BuscarAnaliticaPorMuestraUseCase(analiticaConsultaRepository);

    StepVerifier.create(useCase.execute(10L))
        .expectNext(analitica)
        .verifyComplete();

    verify(analiticaConsultaRepository).buscarUltimaPorMuestra(10L);
  }
}
