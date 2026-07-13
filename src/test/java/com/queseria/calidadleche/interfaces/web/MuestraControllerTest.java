package com.queseria.calidadleche.interfaces.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.queseria.calidadleche.application.usecase.RegistrarMuestraConEvaluacionUseCase;
import com.queseria.calidadleche.domain.model.MuestraLeche;
import com.queseria.calidadleche.domain.repo.MuestraRepository;
import com.queseria.calidadleche.domain.service.EvaluacionCalidadService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;

@WebFluxTest(controllers = MuestraController.class)
class MuestraControllerTest {

  @Autowired
  private WebTestClient webTestClient;

  @MockitoBean
  private RegistrarMuestraConEvaluacionUseCase registrarMuestraConEvaluacionUseCase;

  @MockitoBean
  private MuestraRepository muestraRepository;

  @Test
  void crearDebeCalcularSolidosTotalesDesdeGrasaYSng() {
    when(registrarMuestraConEvaluacionUseCase.ejecutar(any(MuestraLeche.class)))
        .thenAnswer(invocation -> {
          MuestraLeche muestra = invocation.getArgument(0);
          MuestraLeche guardada = MuestraLeche.reconstruir(
              30L,
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
          var evaluacion = new EvaluacionCalidadService.EvaluacionMuestra(Map.of());
          return Mono.just(new RegistrarMuestraConEvaluacionUseCase.Resultado(guardada, evaluacion));
        });

    webTestClient.post()
        .uri("/api/v1/muestras")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {
              "proveedorId": 4,
              "fechaMuestra": "2026-01-10T08:00:00-05:00",
              "volumenLitros": 120.5,
              "precioLitro": 1800,
              "observaciones": "Muestra inicial",
              "composicion": {
                "grasa": 5.0,
                "proteina": 3.2,
                "lactosa": 4.7,
                "solidosTotales": 99.0
              },
              "fisicoQuimico": {
                "densidad": 1.032,
                "acidezDornic": 15,
                "temperaturaC": 18
              },
              "higiene": {
                "ufcBacterias": 1000,
                "ccSomaticas": 200000
              },
              "sng": 4.5,
              "aguaPct": 0
            }
            """)
        .exchange()
        .expectStatus().isCreated()
        .expectBody()
        .jsonPath("$.id").isEqualTo(30)
        .jsonPath("$.solidosTotales").isEqualTo(9.5);

    ArgumentCaptor<MuestraLeche> muestraCaptor = ArgumentCaptor.forClass(MuestraLeche.class);
    verify(registrarMuestraConEvaluacionUseCase).ejecutar(muestraCaptor.capture());

    MuestraLeche muestra = muestraCaptor.getValue();
    assertThat(muestra.sng()).isEqualByComparingTo(new BigDecimal("4.5"));
    assertThat(muestra.composicion().solidosTotales()).isEqualByComparingTo(new BigDecimal("9.5"));
  }

  @Test
  void crearDebeResponderBadRequestCuandoSngNoVieneInformado() {
    webTestClient.post()
        .uri("/api/v1/muestras")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {
              "proveedorId": 4,
              "fechaMuestra": "2026-01-10T08:00:00-05:00",
              "composicion": {
                "grasa": 5.0,
                "proteina": 3.2,
                "lactosa": 4.7,
                "solidosTotales": 9.5
              },
              "fisicoQuimico": {
                "densidad": 1.032,
                "acidezDornic": 15,
                "temperaturaC": 18
              }
            }
            """)
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.error").isEqualTo("bad_request")
        .jsonPath("$.fields.sng").exists();
  }
}
