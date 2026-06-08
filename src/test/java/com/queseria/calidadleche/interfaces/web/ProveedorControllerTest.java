package com.queseria.calidadleche.interfaces.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.queseria.calidadleche.application.usecase.BuscarProveedorUseCase;
import com.queseria.calidadleche.application.usecase.CrearProveedorUseCase;
import com.queseria.calidadleche.domain.model.Proveedor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = ProveedorController.class)
class ProveedorControllerTest {

  @Autowired
  private WebTestClient webTestClient;

  @MockitoBean
  private CrearProveedorUseCase crearProveedorUseCase;

  @MockitoBean
  private BuscarProveedorUseCase buscarProveedorUseCase;

  @Test
  void crearDebeResponderCreatedConProveedor() {
    Proveedor proveedor = Proveedor.reconstruir(
        1L, "Finca Central", "NIT", "900123456", true, null, null);
    when(crearProveedorUseCase.ejecutar("Finca Central", "NIT", "900123456"))
        .thenReturn(Mono.just(proveedor));

    webTestClient.post()
        .uri("/api/v1/proveedores")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {
              "nombre": "Finca Central",
              "tipoIdentificacion": "NIT",
              "identificacion": "900123456"
            }
            """)
        .exchange()
        .expectStatus().isCreated()
        .expectBody()
        .jsonPath("$.id").isEqualTo(1)
        .jsonPath("$.nombre").isEqualTo("Finca Central")
        .jsonPath("$.tipoIdentificacion").isEqualTo("NIT")
        .jsonPath("$.identificacion").isEqualTo("900123456")
        .jsonPath("$.activo").isEqualTo(true);

    verify(crearProveedorUseCase).ejecutar("Finca Central", "NIT", "900123456");
  }

  @Test
  void crearDebeResponderBadRequestCuandoFaltanCampos() {
    webTestClient.post()
        .uri("/api/v1/proveedores")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {
              "nombre": "",
              "tipoIdentificacion": "",
              "identificacion": ""
            }
            """)
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.error").isEqualTo("bad_request")
        .jsonPath("$.fields.nombre").exists()
        .jsonPath("$.fields.tipoIdentificacion").exists()
        .jsonPath("$.fields.identificacion").exists();
  }

  @Test
  void obtenerPorIdDebeResponderOkCuandoExiste() {
    Proveedor proveedor = Proveedor.reconstruir(
        7L, "Proveedor Norte", "CC", "123456", true, null, null);
    when(buscarProveedorUseCase.porId(7L)).thenReturn(Mono.just(proveedor));

    webTestClient.get()
        .uri("/api/v1/proveedores/7")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.id").isEqualTo(7)
        .jsonPath("$.nombre").isEqualTo("Proveedor Norte")
        .jsonPath("$.tipoIdentificacion").isEqualTo("CC")
        .jsonPath("$.identificacion").isEqualTo("123456")
        .jsonPath("$.activo").isEqualTo(true);

    verify(buscarProveedorUseCase).porId(7L);
  }

  @Test
  void obtenerPorIdDebeResponderNotFoundCuandoNoExiste() {
    when(buscarProveedorUseCase.porId(99L)).thenReturn(Mono.empty());

    webTestClient.get()
        .uri("/api/v1/proveedores/99")
        .exchange()
        .expectStatus().isNotFound()
        .expectBody()
        .jsonPath("$.error").isEqualTo("404 NOT_FOUND")
        .jsonPath("$.message").isEqualTo("Proveedor no existe")
        .jsonPath("$.code").isEqualTo("PROVEEDOR_NOT_FOUND");
  }

  @Test
  void obtenerPorIdentificacionDebeResponderOkCuandoExiste() {
    Proveedor proveedor = Proveedor.reconstruir(
        4L, "Proveedor Sur", "NIT", "800", true, null, null);
    when(buscarProveedorUseCase.porIdentificacion("800")).thenReturn(Mono.just(proveedor));

    webTestClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/api/v1/proveedores")
            .queryParam("identificacion", "800")
            .build())
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.id").isEqualTo(4)
        .jsonPath("$.tipoIdentificacion").isEqualTo("NIT")
        .jsonPath("$.identificacion").isEqualTo("800");

    verify(buscarProveedorUseCase).porIdentificacion("800");
  }

  @Test
  void obtenerPorIdentificacionDebeResponderNotFoundCuandoNoExiste() {
    when(buscarProveedorUseCase.porIdentificacion("no-existe")).thenReturn(Mono.empty());

    webTestClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/api/v1/proveedores")
            .queryParam("identificacion", "no-existe")
            .build())
        .exchange()
        .expectStatus().isNotFound()
        .expectBody()
        .jsonPath("$.code").isEqualTo("PROVEEDOR_NOT_FOUND");
  }

  @Test
  void listarDebeResponderPaginaDeProveedores() {
    Proveedor p1 = Proveedor.reconstruir(2L, "Proveedor B", "CC", "2", true, null, null);
    Proveedor p2 = Proveedor.reconstruir(1L, "Proveedor A", "CC", "1", true, null, null);

    when(buscarProveedorUseCase.listar("prov", true, 20, 0)).thenReturn(Flux.just(p1, p2));
    when(buscarProveedorUseCase.contar("prov", true)).thenReturn(Mono.just(2L));

    webTestClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/api/v1/proveedores")
            .queryParam("q", "prov")
            .queryParam("activo", "true")
            .queryParam("limit", "20")
            .queryParam("offset", "0")
            .build())
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.total").isEqualTo(2)
        .jsonPath("$.limit").isEqualTo(20)
        .jsonPath("$.offset").isEqualTo(0)
        .jsonPath("$.items.length()").isEqualTo(2)
        .jsonPath("$.items[0].id").isEqualTo(2)
        .jsonPath("$.items[1].id").isEqualTo(1);

    verify(buscarProveedorUseCase).listar("prov", true, 20, 0);
    verify(buscarProveedorUseCase).contar("prov", true);
  }

  @Test
  void listarDebeResponderBadRequestCuandoLimitEsMayorAlMaximo() {
    webTestClient.get()
        .uri("/api/v1/proveedores?limit=201")
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.error").isEqualTo("bad_request")
        .jsonPath("$.fields.limit").exists();
  }

  @Test
  void listarDebeResponderBadRequestCuandoOffsetEsNegativo() {
    webTestClient.get()
        .uri("/api/v1/proveedores?offset=-1")
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.error").isEqualTo("bad_request")
        .jsonPath("$.fields.offset").exists();
  }
}
