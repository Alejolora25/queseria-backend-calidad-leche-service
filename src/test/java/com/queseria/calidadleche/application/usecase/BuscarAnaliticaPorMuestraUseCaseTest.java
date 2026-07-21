package com.queseria.calidadleche.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.queseria.calidadleche.application.model.AnaliticaMuestraConsulta;
import com.queseria.calidadleche.application.port.AnaliticaConsultaRepository;
import com.queseria.calidadleche.application.port.AnaliticaRepository;
import com.queseria.calidadleche.domain.model.Composicion;
import com.queseria.calidadleche.domain.model.FisicoQuimico;
import com.queseria.calidadleche.domain.model.Higiene;
import com.queseria.calidadleche.domain.model.MuestraLeche;
import com.queseria.calidadleche.domain.repo.MuestraRepository;
import com.queseria.calidadleche.domain.service.EvaluacionCalidadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class BuscarAnaliticaPorMuestraUseCaseTest {

  @Mock private AnaliticaConsultaRepository analiticaConsultaRepository;
  @Mock private AnaliticaRepository analiticaRepository;
  @Mock private MuestraRepository muestraRepository;

  private BuscarAnaliticaPorMuestraUseCase useCase;

  @BeforeEach
  void setUp() {
    useCase = new BuscarAnaliticaPorMuestraUseCase(
        analiticaConsultaRepository,
        analiticaRepository,
        muestraRepository,
        new EvaluacionCalidadService()
    );
  }

  @Test
  void executeDebeDevolverLaAnaliticaExistenteSinConsultarPostgres() {
    AnaliticaMuestraConsulta analitica = analitica();
    when(analiticaConsultaRepository.buscarUltimaPorMuestra(10L)).thenReturn(Mono.just(analitica));

    StepVerifier.create(useCase.execute(10L))
        .expectNext(analitica)
        .verifyComplete();

    verifyNoInteractions(analiticaRepository, muestraRepository);
  }

  @Test
  void executeDebeRecalcularYGuardarCuandoLaAnaliticaNoExiste() {
    MuestraLeche muestra = muestra();
    AnaliticaMuestraConsulta analitica = analitica();
    when(analiticaConsultaRepository.buscarUltimaPorMuestra(10L))
        .thenReturn(Mono.empty(), Mono.just(analitica));
    when(muestraRepository.findById(10L)).thenReturn(Mono.just(muestra));
    when(analiticaRepository.saveAnalisis(any(), any())).thenReturn(Mono.empty());

    StepVerifier.create(useCase.execute(10L))
        .expectNext(analitica)
        .verifyComplete();

    verify(muestraRepository).findById(10L);
    verify(analiticaRepository).saveAnalisis(any(), any());
  }

  @Test
  void executeDebeDevolverCalculoTemporalCuandoMongoNoEstaDisponible() {
    MuestraLeche muestra = muestra();
    when(analiticaConsultaRepository.buscarUltimaPorMuestra(10L))
        .thenReturn(Mono.error(new IllegalStateException("mongo caído")));
    when(muestraRepository.findById(10L)).thenReturn(Mono.just(muestra));

    StepVerifier.create(useCase.execute(10L))
        .assertNext(analitica -> {
          assertThat(analitica.id()).isNull();
          assertThat(analitica.sampleId()).isEqualTo(10L);
          assertThat(analitica.timestamp()).isEqualTo(muestra.fechaMuestra().toInstant());
          assertThat(analitica.evaluacion().porParametro()).containsKey("grasa");
        })
        .verifyComplete();

    verifyNoInteractions(analiticaRepository);
  }

  @Test
  void executeDebeQuedarVacioCuandoNoExisteLaMuestra() {
    when(analiticaConsultaRepository.buscarUltimaPorMuestra(99L)).thenReturn(Mono.empty());
    when(muestraRepository.findById(99L)).thenReturn(Mono.empty());

    StepVerifier.create(useCase.execute(99L)).verifyComplete();
  }

  private AnaliticaMuestraConsulta analitica() {
    return new AnaliticaMuestraConsulta(
        "mongo-id",
        10L,
        4L,
        Instant.parse("2026-01-10T13:00:00Z"),
        null,
        null,
        "hash",
        List.of("ACEPTABLE"),
        BigDecimal.ONE
    );
  }

  private MuestraLeche muestra() {
    return MuestraLeche.reconstruir(
        10L,
        4L,
        OffsetDateTime.parse("2026-01-10T08:00:00-05:00"),
        new BigDecimal("120.50"),
        new BigDecimal("1800"),
        null,
        new Composicion(new BigDecimal("4.0"), new BigDecimal("3.2"), BigDecimal.ZERO, new BigDecimal("12.8")),
        new FisicoQuimico(new BigDecimal("1.032"), new BigDecimal("15"), new BigDecimal("18")),
        new Higiene(0, 0),
        BigDecimal.ZERO,
        new BigDecimal("8.8"),
        null,
        null
    );
  }
}
