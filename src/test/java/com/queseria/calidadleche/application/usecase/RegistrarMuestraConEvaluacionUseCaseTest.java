package com.queseria.calidadleche.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.queseria.calidadleche.application.port.AnaliticaRepository;
import com.queseria.calidadleche.domain.model.Composicion;
import com.queseria.calidadleche.domain.model.FisicoQuimico;
import com.queseria.calidadleche.domain.model.Higiene;
import com.queseria.calidadleche.domain.model.MuestraLeche;
import com.queseria.calidadleche.domain.repo.MuestraRepository;
import com.queseria.calidadleche.domain.service.EvaluacionCalidadService;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class RegistrarMuestraConEvaluacionUseCaseTest {

  @Mock
  private MuestraRepository muestraRepo;

  @Mock
  private AnaliticaRepository analiticaRepository;

  private final EvaluacionCalidadService evaluacionService = new EvaluacionCalidadService();

  @Test
  void ejecutarDebeGuardarEvaluarPersistirAnaliticaYRetornarResultado() {
    MuestraLeche nueva = muestraNueva();
    MuestraLeche guardada = muestraGuardada(nueva);

    when(muestraRepo.save(nueva)).thenReturn(Mono.just(guardada));
    when(analiticaRepository.saveAnalisis(any(MuestraLeche.class), any()))
        .thenReturn(Mono.empty());

    var useCase = new RegistrarMuestraConEvaluacionUseCase(
        muestraRepo,
        evaluacionService,
        analiticaRepository
    );

    StepVerifier.create(useCase.ejecutar(nueva))
        .assertNext(resultado -> {
          assertThat(resultado.muestra()).isEqualTo(guardada);
          assertThat(resultado.evaluacion().porParametro())
              .containsKeys("grasa", "proteina", "densidad_dq", "agua_pct");
          assertThat(resultado.evaluacion().porParametro().get("grasa").estado())
              .isEqualTo("ACEPTABLE");
        })
        .verifyComplete();

    ArgumentCaptor<EvaluacionCalidadService.EvaluacionMuestra> evaluacionCaptor =
        ArgumentCaptor.forClass(EvaluacionCalidadService.EvaluacionMuestra.class);
    verify(muestraRepo).save(nueva);
    verify(analiticaRepository).saveAnalisis(org.mockito.ArgumentMatchers.eq(guardada), evaluacionCaptor.capture());
    assertThat(evaluacionCaptor.getValue().porParametro().get("agua_pct").estado())
        .isEqualTo("ACEPTABLE");
  }

  @Test
  void ejecutarDebeResponderExitoCuandoMongoFallaDespuesDeGuardarLaMuestra() {
    MuestraLeche nueva = muestraNueva();
    MuestraLeche guardada = muestraGuardada(nueva);

    when(muestraRepo.save(nueva)).thenReturn(Mono.just(guardada));
    when(analiticaRepository.saveAnalisis(any(MuestraLeche.class), any()))
        .thenReturn(Mono.error(new IllegalStateException("mongo no disponible")));

    var useCase = new RegistrarMuestraConEvaluacionUseCase(
        muestraRepo,
        evaluacionService,
        analiticaRepository
    );

    StepVerifier.create(useCase.ejecutar(nueva))
        .assertNext(resultado -> {
          assertThat(resultado.muestra()).isEqualTo(guardada);
          assertThat(resultado.evaluacion().porParametro()).isNotEmpty();
        })
        .verifyComplete();

    verify(muestraRepo).save(nueva);
    verify(analiticaRepository).saveAnalisis(any(MuestraLeche.class), any());
  }

  private MuestraLeche muestraNueva() {
    return MuestraLeche.registrar(
        4L,
        OffsetDateTime.parse("2026-01-10T08:00:00-05:00"),
        bd("120.50"),
        bd("1800"),
        "Muestra inicial",
        new Composicion(bd("4.0"), bd("3.2"), bd("4.7"), bd("13.0")),
        new FisicoQuimico(bd("1.032"), bd("15"), bd("18")),
        new Higiene(1000, 200000),
        bd("0"),
        null,
        null,
        null);
  }

  private MuestraLeche muestraGuardada(MuestraLeche muestra) {
    return MuestraLeche.reconstruir(
        20L,
        muestra.proveedorId(),
        muestra.fechaMuestra(),
        muestra.volumenLitros(),
        muestra.precioLitro(),
        muestra.observaciones(),
        muestra.composicion(),
        muestra.fisicoQuimico(),
        muestra.higiene(),
        muestra.aguaPct(),
        muestra.sng(),
        muestra.equipo(),
        muestra.condicion()
    );
  }

  private static BigDecimal bd(String value) {
    return new BigDecimal(value);
  }
}
