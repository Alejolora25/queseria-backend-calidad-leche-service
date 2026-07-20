package com.queseria.calidadleche.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anySet;
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

class CambiarRolesUsuarioUseCaseTest {
  private UsuarioRepository usuarioRepository;
  private CambiarRolesUsuarioUseCase useCase;

  @BeforeEach
  void setUp() {
    usuarioRepository = Mockito.mock(UsuarioRepository.class);
    useCase = new CambiarRolesUsuarioUseCase(usuarioRepository);
  }

  @Test
  void noDebePermitirQueUnAdminSeQuiteSuPropioRol() {
    when(usuarioRepository.findById(1L)).thenReturn(Mono.just(usuario(1L, true, Set.of(NombreRol.ADMIN))));

    StepVerifier.create(useCase.execute(1L, 1L, Set.of(NombreRol.LECTOR)))
        .expectError(OperacionUsuarioNoPermitidaException.class)
        .verify();

    verify(usuarioRepository, never()).replaceRoles(Mockito.anyLong(), anySet());
  }

  @Test
  void noDebeRetirarRolAlUltimoAdministradorActivo() {
    when(usuarioRepository.findById(2L)).thenReturn(Mono.just(usuario(2L, true, Set.of(NombreRol.ADMIN))));
    when(usuarioRepository.countActiveByRole(NombreRol.ADMIN)).thenReturn(Mono.just(1L));

    StepVerifier.create(useCase.execute(1L, 2L, Set.of(NombreRol.OPERADOR)))
        .expectError(OperacionUsuarioNoPermitidaException.class)
        .verify();
  }

  @Test
  void debeReemplazarRolesDeUnUsuario() {
    Set<NombreRol> roles = Set.of(NombreRol.OPERADOR);
    when(usuarioRepository.findById(2L))
        .thenReturn(Mono.just(usuario(2L, true, Set.of(NombreRol.LECTOR))));
    when(usuarioRepository.replaceRoles(2L, roles))
        .thenReturn(Mono.just(usuario(2L, true, roles)));

    StepVerifier.create(useCase.execute(1L, 2L, roles))
        .assertNext(usuario -> assertThat(usuario.roles())
            .containsExactly(NombreRol.OPERADOR))
        .verifyComplete();
  }

  @Test
  void debeRechazarMasDeUnRolAntesDeBuscarUsuario() {
    StepVerifier.create(useCase.execute(
            1L, 2L, Set.of(NombreRol.OPERADOR, NombreRol.LECTOR)))
        .expectErrorMatches(error -> error instanceof OperacionUsuarioNoPermitidaException
            && error.getMessage().equals("El usuario debe tener exactamente un rol"))
        .verify();

    verify(usuarioRepository, never()).findById(Mockito.anyLong());
    verify(usuarioRepository, never()).replaceRoles(Mockito.anyLong(), anySet());
  }

  private Usuario usuario(Long id, boolean activo, Set<NombreRol> roles) {
    OffsetDateTime now = OffsetDateTime.now();
    return Usuario.reconstruir(
        id, "Usuario", "usuario" + id + "@queseria.local", "hash", activo, null,
        roles, now, now);
  }
}
