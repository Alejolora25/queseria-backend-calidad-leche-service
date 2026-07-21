package com.queseria.calidadleche.interfaces.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.queseria.calidadleche.application.model.AnaliticaMuestraConsulta;
import com.queseria.calidadleche.application.usecase.BuscarAnaliticaPorMuestraUseCase;
import com.queseria.calidadleche.application.usecase.ObtenerResumenAnaliticaProveedorUseCase;
import com.queseria.calidadleche.infrastructure.config.SecurityConfig;
import com.queseria.calidadleche.infrastructure.config.WebFluxCorsConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@WebFluxTest(controllers = AnaliticaController.class)
@Import({ SecurityConfig.class, WebFluxCorsConfig.class })
@WithMockUser(roles = "ADMIN")
class AnaliticaControllerTest {

  @Autowired
  private WebTestClient webTestClient;

  @MockitoBean
  private BuscarAnaliticaPorMuestraUseCase buscarAnaliticaPorMuestraUseCase;

  @MockitoBean
  private ObtenerResumenAnaliticaProveedorUseCase obtenerResumenAnaliticaProveedorUseCase;

  @Test
  void porMuestraDebeResponderAnalitica() {
    AnaliticaMuestraConsulta analitica = new AnaliticaMuestraConsulta(
        "mongo-id",
        15L,
        7L,
        Instant.parse("2026-01-10T13:00:00Z"),
        new AnaliticaMuestraConsulta.BaseValores(
            bd("4.0"),
            bd("3.2"),
            bd("4.7"),
            bd("13.0"),
            bd("1.032"),
            bd("15"),
            bd("18"),
            bd("8.5"),
            bd("0")
        ),
        new AnaliticaMuestraConsulta.Evaluacion(Map.of(
            "grasa",
            new AnaliticaMuestraConsulta.ResultadoParametro("ACEPTABLE", List.of("OK"))
        )),
        "hash",
        List.of("ACEPTABLE"),
        bd("1.00")
    );
    when(buscarAnaliticaPorMuestraUseCase.execute(15L)).thenReturn(Mono.just(analitica));

    webTestClient.get()
        .uri("/api/v1/analiticas/muestra/15")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.id").isEqualTo("mongo-id")
        .jsonPath("$.sampleId").isEqualTo(15)
        .jsonPath("$.proveedorId").isEqualTo(7)
        .jsonPath("$.base.grasa").isEqualTo(4.0)
        .jsonPath("$.evaluacion.porParametro.grasa.estado").isEqualTo("ACEPTABLE")
        .jsonPath("$.flags[0]").isEqualTo("ACEPTABLE")
        .jsonPath("$.kpiCalidad").isEqualTo(1.00);

    verify(buscarAnaliticaPorMuestraUseCase).execute(15L);
  }

  @Test
  void porMuestraDebeResponderNotFoundCuandoLaMuestraNoExiste() {
    when(buscarAnaliticaPorMuestraUseCase.execute(99L)).thenReturn(Mono.empty());

    webTestClient.get()
        .uri("/api/v1/analiticas/muestra/99")
        .exchange()
        .expectStatus().isNotFound()
        .expectBody()
        .jsonPath("$.message").isEqualTo("La muestra no existe");
  }

  @Test
  void resumenProveedorDebeResponderKpis() {
    Instant desde = Instant.parse("2026-01-01T00:00:00Z");
    Instant hasta = Instant.parse("2026-01-31T23:59:59Z");
    Map<String, Object> resumen = Map.of(
        "promedios", Map.of(
            "grasa", 4.0,
            "proteina", 3.2,
            "solidosTotales", 13.0,
            "sng", 8.5,
            "kpi", 1.0
        ),
        "distribucionEstados", Map.of(
            "ACEPTABLE", 3L,
            "ALERTA", 1L,
            "RECHAZAR", 0L,
            "totalEstados", 4L
        )
    );
    when(obtenerResumenAnaliticaProveedorUseCase.execute(7L, desde, hasta))
        .thenReturn(Mono.just(resumen));

    webTestClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/api/v1/analiticas/proveedor/7/resumen")
            .queryParam("desde", desde.toString())
            .queryParam("hasta", hasta.toString())
            .build())
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.promedios.grasa").isEqualTo(4.0)
        .jsonPath("$.promedios.proteina").isEqualTo(3.2)
        .jsonPath("$.distribucionEstados.ACEPTABLE").isEqualTo(3)
        .jsonPath("$.distribucionEstados.totalEstados").isEqualTo(4);

    verify(obtenerResumenAnaliticaProveedorUseCase).execute(7L, desde, hasta);
  }

  private static BigDecimal bd(String value) {
    return new BigDecimal(value);
  }
}
