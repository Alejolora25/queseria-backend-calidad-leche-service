package com.queseria.calidadleche.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class ObtenerResumenAnaliticaProveedorUseCaseTest {

  @Mock private AnaliticaConsultaRepository analiticaConsultaRepository;
  @Mock private AnaliticaRepository analiticaRepository;
  @Mock private MuestraRepository muestraRepository;

  private ObtenerResumenAnaliticaProveedorUseCase useCase;
  private final Instant desde = Instant.parse("2026-01-01T00:00:00Z");
  private final Instant hasta = Instant.parse("2026-01-31T23:59:59Z");

  @BeforeEach
  void setUp() {
    useCase = new ObtenerResumenAnaliticaProveedorUseCase(
        analiticaConsultaRepository,
        analiticaRepository,
        muestraRepository,
        new EvaluacionCalidadService()
    );
  }

  @Test
  void executeDebeUsarMongoCuandoTodasLasAnaliticasEstanSincronizadas() {
    var referencia = referencia();
    Map<String, Object> resumenMongo = Map.of("origen", "mongo");
    when(muestraRepository.findReferenciasByProveedorAndRango(any(), any(), any()))
        .thenReturn(Flux.just(referencia));
    when(analiticaConsultaRepository.buscarMetadatosPorMuestras(any(Collection.class)))
        .thenReturn(Flux.just(new AnaliticaConsultaRepository.MetadataAnalitica(
            referencia.id(), referencia.fechaMuestra().toInstant(), "ACEPTABLE"
        )));
    when(analiticaConsultaRepository.obtenerResumenProveedor(4L, desde, hasta))
        .thenReturn(Mono.just(resumenMongo));

    StepVerifier.create(useCase.execute(4L, desde, hasta))
        .expectNext(resumenMongo)
        .verifyComplete();

    verify(analiticaRepository, never()).saveAnalisis(any(), any());
  }

  @Test
  void executeDebeRepararLasAnaliticasFaltantesAntesDeResumirEnMongo() {
    var referencia = referencia();
    MuestraLeche muestra = muestra();
    Map<String, Object> resumenMongo = Map.of("origen", "mongo reparado");
    when(muestraRepository.findReferenciasByProveedorAndRango(any(), any(), any()))
        .thenReturn(Flux.just(referencia));
    when(analiticaConsultaRepository.buscarMetadatosPorMuestras(any(Collection.class)))
        .thenReturn(Flux.empty());
    when(muestraRepository.findById(10L)).thenReturn(Mono.just(muestra));
    when(analiticaRepository.saveAnalisis(any(), any())).thenReturn(Mono.empty());
    when(analiticaConsultaRepository.obtenerResumenProveedor(4L, desde, hasta))
        .thenReturn(Mono.just(resumenMongo));

    StepVerifier.create(useCase.execute(4L, desde, hasta))
        .expectNext(resumenMongo)
        .verifyComplete();

    verify(analiticaRepository).saveAnalisis(any(), any());
  }

  @Test
  void executeDebeCalcularDesdePostgresCuandoMongoFalla() {
    when(muestraRepository.findReferenciasByProveedorAndRango(any(), any(), any()))
        .thenReturn(Flux.just(referencia()));
    when(analiticaConsultaRepository.buscarMetadatosPorMuestras(any(Collection.class)))
        .thenReturn(Flux.error(new IllegalStateException("mongo caído")));
    when(muestraRepository.findByProveedorAndRango(any(), any(), any(), any(Integer.class), any(Integer.class)))
        .thenReturn(Flux.just(muestra()));

    StepVerifier.create(useCase.execute(4L, desde, hasta))
        .assertNext(resumen -> {
          Map<?, ?> distribucion = (Map<?, ?>) resumen.get("distribucionEstados");
          assertThat(distribucion.get("ACEPTABLE")).isEqualTo(1L);
          assertThat(distribucion.get("totalEstados")).isEqualTo(1L);
        })
        .verifyComplete();
  }

  @Test
  void executeDebeResponderResumenVacioCuandoNoHayMuestras() {
    when(muestraRepository.findReferenciasByProveedorAndRango(any(), any(), any()))
        .thenReturn(Flux.empty());

    StepVerifier.create(useCase.execute(4L, desde, hasta))
        .assertNext(resumen -> {
          Map<?, ?> distribucion = (Map<?, ?>) resumen.get("distribucionEstados");
          assertThat(distribucion.get("totalEstados")).isEqualTo(0L);
        })
        .verifyComplete();

    verify(analiticaConsultaRepository, never()).obtenerResumenProveedor(any(), any(), any());
  }

  private MuestraRepository.ReferenciaMuestra referencia() {
    return new MuestraRepository.ReferenciaMuestra(
        10L,
        OffsetDateTime.parse("2026-01-10T08:00:00-05:00")
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
