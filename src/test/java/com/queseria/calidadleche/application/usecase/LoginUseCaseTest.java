package com.queseria.calidadleche.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.queseria.calidadleche.application.exception.CredencialesInvalidasException;
import com.queseria.calidadleche.application.port.AccessTokenProvider;
import com.queseria.calidadleche.application.port.PasswordVerifier;
import com.queseria.calidadleche.domain.model.NombreRol;
import com.queseria.calidadleche.domain.model.Usuario;
import com.queseria.calidadleche.domain.repo.UsuarioRepository;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class LoginUseCaseTest {
  private UsuarioRepository usuarioRepository;
  private PasswordVerifier passwordVerifier;
  private AccessTokenProvider accessTokenProvider;
  private LoginUseCase loginUseCase;

  @BeforeEach
  void setUp() {
    usuarioRepository = Mockito.mock(UsuarioRepository.class);
    passwordVerifier = Mockito.mock(PasswordVerifier.class);
    accessTokenProvider = Mockito.mock(AccessTokenProvider.class);
    loginUseCase = new LoginUseCase(usuarioRepository, passwordVerifier, accessTokenProvider);
  }

  @Test
  void debeAutenticarYGenerarTokenConEmailNormalizado() {
    Usuario usuario = usuario(true);
    when(usuarioRepository.findByEmail("admin@queseria.local")).thenReturn(Mono.just(usuario));
    when(passwordVerifier.matches("Admin123*", "hash-bcrypt")).thenReturn(Mono.just(true));
    when(accessTokenProvider.generate(usuario))
        .thenReturn(new AccessTokenProvider.GeneratedAccessToken("jwt-generado", 3600));

    StepVerifier.create(loginUseCase.execute("  ADMIN@QUESERIA.LOCAL ", "Admin123*"))
        .assertNext(result -> {
          assertThat(result.accessToken()).isEqualTo("jwt-generado");
          assertThat(result.expiresInSeconds()).isEqualTo(3600);
          assertThat(result.usuarioId()).isEqualTo(1L);
          assertThat(result.nombre()).isEqualTo("Administrador");
          assertThat(result.email()).isEqualTo("admin@queseria.local");
          assertThat(result.roles()).containsExactlyInAnyOrder(NombreRol.ADMIN, NombreRol.OPERADOR);
          assertThat(result.queseriaId()).isEqualTo(9L);
        })
        .verifyComplete();

    verify(usuarioRepository).findByEmail("admin@queseria.local");
  }

  @Test
  void debeRechazarConElMismoMensajeCuandoLaContrasenaEsIncorrecta() {
    Usuario usuario = usuario(true);
    when(usuarioRepository.findByEmail("admin@queseria.local")).thenReturn(Mono.just(usuario));
    when(passwordVerifier.matches("incorrecta", "hash-bcrypt")).thenReturn(Mono.just(false));

    verifyInvalidCredentials(loginUseCase.execute("admin@queseria.local", "incorrecta"));

    verifyNoInteractions(accessTokenProvider);
  }

  @Test
  void debeHacerComparacionFicticiaYRechazarCuandoElUsuarioNoExiste() {
    when(usuarioRepository.findByEmail("nadie@queseria.local")).thenReturn(Mono.empty());
    when(passwordVerifier.matchesDummy("cualquier-clave")).thenReturn(Mono.just(false));

    verifyInvalidCredentials(loginUseCase.execute("nadie@queseria.local", "cualquier-clave"));

    verify(passwordVerifier).matchesDummy("cualquier-clave");
    verifyNoInteractions(accessTokenProvider);
  }

  @Test
  void debeRechazarUsuarioInactivoConComparacionFicticia() {
    Usuario usuario = usuario(false);
    when(usuarioRepository.findByEmail("admin@queseria.local")).thenReturn(Mono.just(usuario));
    when(passwordVerifier.matchesDummy("Admin123*")).thenReturn(Mono.just(false));

    verifyInvalidCredentials(loginUseCase.execute("admin@queseria.local", "Admin123*"));

    verify(passwordVerifier).matchesDummy("Admin123*");
    verify(passwordVerifier, never()).matches("Admin123*", "hash-bcrypt");
    verifyNoInteractions(accessTokenProvider);
  }

  private void verifyInvalidCredentials(Mono<?> result) {
    StepVerifier.create(result)
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(CredencialesInvalidasException.class);
          assertThat(error).hasMessage("Credenciales inválidas");
        })
        .verify();
  }

  private Usuario usuario(boolean activo) {
    OffsetDateTime now = OffsetDateTime.now();
    return Usuario.reconstruir(
        1L,
        "Administrador",
        "admin@queseria.local",
        "hash-bcrypt",
        activo,
        9L,
        Set.of(NombreRol.ADMIN, NombreRol.OPERADOR),
        now,
        now
    );
  }
}
