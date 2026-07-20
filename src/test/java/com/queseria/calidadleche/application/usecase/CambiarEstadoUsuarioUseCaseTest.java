package com.queseria.calidadleche.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.queseria.calidadleche.application.exception.OperacionUsuarioNoPermitidaException;
import com.queseria.calidadleche.domain.model.NombreRol;
import com.queseria.calidadleche.domain.model.Usuario;
import com.queseria.calidadleche.domain.repo.UsuarioRepository;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class CambiarEstadoUsuarioUseCaseTest {
  private UsuarioRepository usuarioRepository;
  private CambiarEstadoUsuarioUseCase useCase;

  @BeforeEach
  void setUp() {
    usuarioRepository = Mockito.mock(UsuarioRepository.class);
    useCase = new CambiarEstadoUsuarioUseCase(usuarioRepository);
  }

  @Test
  void noDebePermitirQueUnAdminSeDesactiveASiMismo() {
    when(usuarioRepository.findById(1L)).thenReturn(Mono.just(usuario(1L, true, NombreRol.ADMIN)));

    StepVerifier.create(useCase.desactivar(1L, 1L))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(OperacionUsuarioNoPermitidaException.class);
          assertThat(error.getMessage()).contains("propio usuario");
        })
        .verify();

    verify(usuarioRepository, never()).save(any());
  }

  @Test
  void noDebeDesactivarAlUltimoAdministradorActivo() {
    when(usuarioRepository.findById(2L)).thenReturn(Mono.just(usuario(2L, true, NombreRol.ADMIN)));
    when(usuarioRepository.countActiveByRole(NombreRol.ADMIN)).thenReturn(Mono.just(1L));

    StepVerifier.create(useCase.desactivar(1L, 2L))
        .expectError(OperacionUsuarioNoPermitidaException.class)
        .verify();

    verify(usuarioRepository, never()).save(any());
  }

  @Test
  void debeDesactivarUnOperador() {
    when(usuarioRepository.findById(2L)).thenReturn(Mono.just(usuario(2L, true, NombreRol.OPERADOR)));
    when(usuarioRepository.save(any(Usuario.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(useCase.desactivar(1L, 2L))
        .assertNext(usuario -> assertThat(usuario.activo()).isFalse())
        .verifyComplete();
  }

  private Usuario usuario(Long id, boolean activo, NombreRol rol) {
    OffsetDateTime now = OffsetDateTime.now();
    return Usuario.reconstruir(
        id, "Usuario", "usuario" + id + "@queseria.local", "hash", activo, null,
        Set.of(rol), now, now);
  }
}
