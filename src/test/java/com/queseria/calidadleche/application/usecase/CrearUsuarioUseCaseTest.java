package com.queseria.calidadleche.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.queseria.calidadleche.application.exception.EmailUsuarioYaRegistradoException;
import com.queseria.calidadleche.application.exception.OperacionUsuarioNoPermitidaException;
import com.queseria.calidadleche.application.port.PasswordHasher;
import com.queseria.calidadleche.domain.model.NombreRol;
import com.queseria.calidadleche.domain.model.Usuario;
import com.queseria.calidadleche.domain.repo.UsuarioRepository;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class CrearUsuarioUseCaseTest {
  private UsuarioRepository usuarioRepository;
  private PasswordHasher passwordHasher;
  private CrearUsuarioUseCase useCase;

  @BeforeEach
  void setUp() {
    usuarioRepository = Mockito.mock(UsuarioRepository.class);
    passwordHasher = Mockito.mock(PasswordHasher.class);
    useCase = new CrearUsuarioUseCase(usuarioRepository, passwordHasher);
  }

  @Test
  void debeNormalizarEmailHashearPasswordYGuardarRoles() {
    when(usuarioRepository.existsByEmail("operador@queseria.local")).thenReturn(Mono.just(false));
    when(passwordHasher.hash("Operador123*")).thenReturn(Mono.just("hash-bcrypt"));
    when(usuarioRepository.saveWithRoles(any(Usuario.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(useCase.execute(
            "María Operadora",
            "  OPERADOR@QUESERIA.LOCAL ",
            "Operador123*",
            Set.of(NombreRol.OPERADOR)))
        .assertNext(usuario -> {
          assertThat(usuario.email()).isEqualTo("operador@queseria.local");
          assertThat(usuario.passwordHash()).isEqualTo("hash-bcrypt");
          assertThat(usuario.activo()).isTrue();
          assertThat(usuario.roles()).containsExactly(NombreRol.OPERADOR);
          assertThat(usuario.queseriaId()).isNull();
        })
        .verifyComplete();

    verify(passwordHasher).hash("Operador123*");
  }

  @Test
  void debeRechazarEmailDuplicadoSinHashearPassword() {
    when(usuarioRepository.existsByEmail("admin@queseria.local")).thenReturn(Mono.just(true));

    StepVerifier.create(useCase.execute(
            "Otro admin",
            "admin@queseria.local",
            "Admin123*",
            Set.of(NombreRol.ADMIN)))
        .expectError(EmailUsuarioYaRegistradoException.class)
        .verify();

    verify(passwordHasher, never()).hash(any());
    verify(usuarioRepository, never()).saveWithRoles(any());
  }

  @Test
  void debeRechazarMasDeUnRolAntesDeConsultarPersistencia() {
    StepVerifier.create(useCase.execute(
            "Usuario ambiguo",
            "ambiguo@queseria.local",
            "Usuario123*",
            Set.of(NombreRol.OPERADOR, NombreRol.LECTOR)))
        .expectErrorMatches(error -> error instanceof OperacionUsuarioNoPermitidaException
            && error.getMessage().equals("El usuario debe tener exactamente un rol"))
        .verify();

    verify(usuarioRepository, never()).existsByEmail(any());
    verify(passwordHasher, never()).hash(any());
  }
}
