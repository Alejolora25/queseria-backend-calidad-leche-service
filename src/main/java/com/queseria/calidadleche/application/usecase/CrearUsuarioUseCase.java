package com.queseria.calidadleche.application.usecase;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import com.queseria.calidadleche.application.exception.EmailUsuarioYaRegistradoException;
import com.queseria.calidadleche.application.exception.OperacionUsuarioNoPermitidaException;
import com.queseria.calidadleche.application.port.PasswordHasher;
import com.queseria.calidadleche.domain.model.NombreRol;
import com.queseria.calidadleche.domain.model.Usuario;
import com.queseria.calidadleche.domain.repo.UsuarioRepository;

import reactor.core.publisher.Mono;

public class CrearUsuarioUseCase {
  private final UsuarioRepository usuarioRepository;
  private final PasswordHasher passwordHasher;

  public CrearUsuarioUseCase(UsuarioRepository usuarioRepository, PasswordHasher passwordHasher) {
    this.usuarioRepository = usuarioRepository;
    this.passwordHasher = passwordHasher;
  }

  public Mono<Usuario> execute(String nombre, String email, String password, Set<NombreRol> roles) {
    return Mono.defer(() -> {
      String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
      Set<NombreRol> normalizedRoles = copyAndValidateRole(roles);

      return usuarioRepository.existsByEmail(normalizedEmail)
          .flatMap(exists -> exists
              ? Mono.error(new EmailUsuarioYaRegistradoException())
              : passwordHasher.hash(password)
                  .map(hash -> Usuario.crear(nombre, normalizedEmail, hash, null).conRoles(normalizedRoles))
                  .flatMap(usuarioRepository::saveWithRoles));
    });
  }

  private Set<NombreRol> copyAndValidateRole(Set<NombreRol> roles) {
    if (roles == null || roles.size() != 1) {
      throw new OperacionUsuarioNoPermitidaException("El usuario debe tener exactamente un rol");
    }
    return new LinkedHashSet<>(roles);
  }
}
