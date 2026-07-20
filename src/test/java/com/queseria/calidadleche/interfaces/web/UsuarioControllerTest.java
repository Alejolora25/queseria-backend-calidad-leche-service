package com.queseria.calidadleche.interfaces.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.queseria.calidadleche.application.usecase.CambiarEstadoUsuarioUseCase;
import com.queseria.calidadleche.application.usecase.CambiarRolesUsuarioUseCase;
import com.queseria.calidadleche.application.usecase.CrearUsuarioUseCase;
import com.queseria.calidadleche.application.usecase.ListarUsuariosUseCase;
import com.queseria.calidadleche.application.exception.EmailUsuarioYaRegistradoException;
import com.queseria.calidadleche.application.exception.UsuarioNoEncontradoException;
import com.queseria.calidadleche.domain.model.NombreRol;
import com.queseria.calidadleche.domain.model.Usuario;
import com.queseria.calidadleche.infrastructure.config.SecurityConfig;
import com.queseria.calidadleche.infrastructure.config.WebFluxCorsConfig;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@WebFluxTest(
    controllers = UsuarioController.class,
    properties = {
        "app.security.jwt.secret=test-jwt-secret-with-at-least-32-characters",
        "app.security.jwt.issuer=queseria-test",
        "app.security.jwt.expiration-minutes=60"
    }
)
@Import({ SecurityConfig.class, WebFluxCorsConfig.class })
class UsuarioControllerTest {
  @Autowired
  private WebTestClient webTestClient;

  @MockitoBean
  private CrearUsuarioUseCase crearUsuarioUseCase;

  @MockitoBean
  private ListarUsuariosUseCase listarUsuariosUseCase;

  @MockitoBean
  private CambiarEstadoUsuarioUseCase cambiarEstadoUsuarioUseCase;

  @MockitoBean
  private CambiarRolesUsuarioUseCase cambiarRolesUsuarioUseCase;

  @Test
  void adminDebeCrearUsuarioSinExponerPasswordHash() {
    when(crearUsuarioUseCase.execute(
        "María Operadora",
        "maria@queseria.local",
        "Operador123*",
        Set.of(NombreRol.OPERADOR)))
        .thenReturn(Mono.just(usuario(2L, true, Set.of(NombreRol.OPERADOR))));

    adminClient().post()
        .uri("/api/v1/usuarios")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {
              "nombre": "María Operadora",
              "email": "maria@queseria.local",
              "password": "Operador123*",
              "roles": ["OPERADOR"]
            }
            """)
        .exchange()
        .expectStatus().isCreated()
        .expectBody()
        .jsonPath("$.id").isEqualTo(2)
        .jsonPath("$.email").isEqualTo("usuario2@queseria.local")
        .jsonPath("$.activo").isEqualTo(true)
        .jsonPath("$.roles[0]").isEqualTo("OPERADOR")
        .jsonPath("$.password").doesNotExist()
        .jsonPath("$.passwordHash").doesNotExist();
  }

  @Test
  void adminDebeListarUsuariosConPaginacion() {
    when(listarUsuariosUseCase.listar("admin", true, 20, 0))
        .thenReturn(Flux.just(usuario(1L, true, Set.of(NombreRol.ADMIN))));
    when(listarUsuariosUseCase.contar("admin", true)).thenReturn(Mono.just(1L));

    adminClient().get()
        .uri(uriBuilder -> uriBuilder.path("/api/v1/usuarios")
            .queryParam("q", "admin")
            .queryParam("activo", true)
            .build())
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.items[0].id").isEqualTo(1)
        .jsonPath("$.items[0].roles[0]").isEqualTo("ADMIN")
        .jsonPath("$.total").isEqualTo(1)
        .jsonPath("$.limit").isEqualTo(20)
        .jsonPath("$.offset").isEqualTo(0);
  }

  @Test
  void adminDebeActivarDesactivarYCambiarRolesUsandoSuSubject() {
    Usuario operador = usuario(2L, false, Set.of(NombreRol.OPERADOR));
    when(cambiarEstadoUsuarioUseCase.activar(1L, 2L)).thenReturn(Mono.just(operador));
    when(cambiarEstadoUsuarioUseCase.desactivar(1L, 2L)).thenReturn(Mono.just(operador));
    when(cambiarRolesUsuarioUseCase.execute(1L, 2L, Set.of(NombreRol.LECTOR)))
        .thenReturn(Mono.just(usuario(2L, true, Set.of(NombreRol.LECTOR))));

    adminClient().patch().uri("/api/v1/usuarios/2/activar").exchange()
        .expectStatus().isOk();
    adminClient().patch().uri("/api/v1/usuarios/2/desactivar").exchange()
        .expectStatus().isOk();
    adminClient().put()
        .uri("/api/v1/usuarios/2/roles")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"roles\":[\"LECTOR\"]}")
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.roles[0]").isEqualTo("LECTOR");

    verify(cambiarEstadoUsuarioUseCase).activar(1L, 2L);
    verify(cambiarEstadoUsuarioUseCase).desactivar(1L, 2L);
    verify(cambiarRolesUsuarioUseCase).execute(1L, 2L, Set.of(NombreRol.LECTOR));
  }

  @Test
  void requestInvalidoDebeResponderBadRequestSinInvocarCasoDeUso() {
    adminClient().post()
        .uri("/api/v1/usuarios")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {
              "nombre": "",
              "email": "email-invalido",
              "password": "corta",
              "roles": []
            }
            """)
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.error").isEqualTo("bad_request")
        .jsonPath("$.fields.nombre").exists()
        .jsonPath("$.fields.email").exists()
        .jsonPath("$.fields.password").exists()
        .jsonPath("$.fields.roles").exists();

    verifyNoInteractions(crearUsuarioUseCase);
  }

  @Test
  void masDeUnRolDebeResponderBadRequestSinInvocarCasoDeUso() {
    adminClient().post()
        .uri("/api/v1/usuarios")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {
              "nombre": "Usuario ambiguo",
              "email": "ambiguo@queseria.local",
              "password": "Usuario123*",
              "roles": ["OPERADOR", "LECTOR"]
            }
            """)
        .exchange()
        .expectStatus().isBadRequest()
        .expectBody()
        .jsonPath("$.error").isEqualTo("bad_request")
        .jsonPath("$.fields.roles[0]").isEqualTo("El usuario debe tener exactamente un rol");

    verifyNoInteractions(crearUsuarioUseCase);
  }

  @Test
  void emailDuplicadoDebeResponderConflict() {
    when(crearUsuarioUseCase.execute(
        "Administrador",
        "admin@queseria.local",
        "Admin123*",
        Set.of(NombreRol.ADMIN)))
        .thenReturn(Mono.error(new EmailUsuarioYaRegistradoException()));

    adminClient().post()
        .uri("/api/v1/usuarios")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue("""
            {
              "nombre": "Administrador",
              "email": "admin@queseria.local",
              "password": "Admin123*",
              "roles": ["ADMIN"]
            }
            """)
        .exchange()
        .expectStatus().isEqualTo(409)
        .expectBody()
        .jsonPath("$.error").isEqualTo("conflict")
        .jsonPath("$.message").isEqualTo("El email ya está registrado");
  }

  @Test
  void usuarioInexistenteDebeResponderNotFound() {
    when(cambiarEstadoUsuarioUseCase.activar(1L, 99L))
        .thenReturn(Mono.error(new UsuarioNoEncontradoException()));

    adminClient().patch()
        .uri("/api/v1/usuarios/99/activar")
        .exchange()
        .expectStatus().isNotFound()
        .expectBody()
        .jsonPath("$.error").isEqualTo("not_found")
        .jsonPath("$.message").isEqualTo("Usuario no existe");
  }

  @Test
  void usuarioSinTokenDebeRecibirUnauthorized() {
    webTestClient.get()
        .uri("/api/v1/usuarios")
        .exchange()
        .expectStatus().isUnauthorized();
  }

  @Test
  void lectorAutenticadoDebeRecibirForbidden() {
    webTestClient.mutateWith(mockJwt()
            .jwt(jwt -> jwt.subject("3").claim("roles", List.of("LECTOR")))
            .authorities(new SimpleGrantedAuthority("ROLE_LECTOR")))
        .get()
        .uri("/api/v1/usuarios")
        .exchange()
        .expectStatus().isForbidden();

    verifyNoInteractions(listarUsuariosUseCase);
  }

  private WebTestClient adminClient() {
    return webTestClient.mutateWith(mockJwt()
        .jwt(jwt -> jwt.subject("1").claim("roles", List.of("ADMIN")))
        .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")));
  }

  private Usuario usuario(Long id, boolean activo, Set<NombreRol> roles) {
    OffsetDateTime now = OffsetDateTime.now();
    return Usuario.reconstruir(
        id,
        id == 1 ? "Administrador" : "María Operadora",
        "usuario" + id + "@queseria.local",
        "hash-que-no-debe-salir",
        activo,
        null,
        roles,
        now,
        now);
  }
}
