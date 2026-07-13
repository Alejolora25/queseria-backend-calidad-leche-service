package com.queseria.calidadleche.application.usecase;

import java.util.Locale;

import com.queseria.calidadleche.application.exception.CredencialesInvalidasException;
import com.queseria.calidadleche.application.model.LoginResult;
import com.queseria.calidadleche.application.port.AccessTokenProvider;
import com.queseria.calidadleche.application.port.PasswordVerifier;
import com.queseria.calidadleche.domain.model.Usuario;
import com.queseria.calidadleche.domain.repo.UsuarioRepository;

import reactor.core.publisher.Mono;

public class LoginUseCase {
  private final UsuarioRepository usuarioRepository;
  private final PasswordVerifier passwordVerifier;
  private final AccessTokenProvider accessTokenProvider;

  public LoginUseCase(
      UsuarioRepository usuarioRepository,
      PasswordVerifier passwordVerifier,
      AccessTokenProvider accessTokenProvider
  ) {
    this.usuarioRepository = usuarioRepository;
    this.passwordVerifier = passwordVerifier;
    this.accessTokenProvider = accessTokenProvider;
  }

  public Mono<LoginResult> execute(String email, String password) {
    String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

    return usuarioRepository.findByEmail(normalizedEmail)
        .flatMap(usuario -> authenticate(usuario, password))
        .switchIfEmpty(Mono.defer(() -> rejectAfterDummyCheck(password)));
  }

  private Mono<LoginResult> authenticate(Usuario usuario, String password) {
    if (!usuario.activo()) {
      return rejectAfterDummyCheck(password);
    }

    return passwordVerifier.matches(password, usuario.passwordHash())
        .flatMap(matches -> matches
            ? Mono.just(toResult(usuario))
            : Mono.error(new CredencialesInvalidasException()));
  }

  private Mono<LoginResult> rejectAfterDummyCheck(String password) {
    return passwordVerifier.matchesDummy(password)
        .then(Mono.error(new CredencialesInvalidasException()));
  }

  private LoginResult toResult(Usuario usuario) {
    AccessTokenProvider.GeneratedAccessToken token = accessTokenProvider.generate(usuario);
    return new LoginResult(
        token.value(),
        token.expiresInSeconds(),
        usuario.id(),
        usuario.nombre(),
        usuario.email(),
        usuario.roles(),
        usuario.queseriaId()
    );
  }
}
