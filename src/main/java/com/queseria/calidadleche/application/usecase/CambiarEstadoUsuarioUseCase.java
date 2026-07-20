package com.queseria.calidadleche.application.usecase;

import com.queseria.calidadleche.application.exception.OperacionUsuarioNoPermitidaException;
import com.queseria.calidadleche.application.exception.UsuarioNoEncontradoException;
import com.queseria.calidadleche.domain.model.NombreRol;
import com.queseria.calidadleche.domain.model.Usuario;
import com.queseria.calidadleche.domain.repo.UsuarioRepository;

import reactor.core.publisher.Mono;

public class CambiarEstadoUsuarioUseCase {
  private final UsuarioRepository usuarioRepository;

  public CambiarEstadoUsuarioUseCase(UsuarioRepository usuarioRepository) {
    this.usuarioRepository = usuarioRepository;
  }

  public Mono<Usuario> activar(Long actorId, Long usuarioId) {
    return cambiar(actorId, usuarioId, true);
  }

  public Mono<Usuario> desactivar(Long actorId, Long usuarioId) {
    return cambiar(actorId, usuarioId, false);
  }

  private Mono<Usuario> cambiar(Long actorId, Long usuarioId, boolean activo) {
    return usuarioRepository.findById(usuarioId)
        .switchIfEmpty(Mono.error(new UsuarioNoEncontradoException()))
        .flatMap(usuario -> validarCambio(actorId, usuario, activo));
  }

  private Mono<Usuario> validarCambio(Long actorId, Usuario usuario, boolean activo) {
    if (usuario.activo() == activo) {
      return Mono.just(usuario);
    }
    if (!activo && usuario.id().equals(actorId)) {
      return Mono.error(new OperacionUsuarioNoPermitidaException(
          "No puedes desactivar tu propio usuario"));
    }
    if (!activo && usuario.roles().contains(NombreRol.ADMIN)) {
      return usuarioRepository.countActiveByRole(NombreRol.ADMIN)
          .flatMap(total -> total <= 1
              ? Mono.error(new OperacionUsuarioNoPermitidaException(
                  "No se puede desactivar al último administrador activo"))
              : usuarioRepository.save(usuario.conEstado(false)));
    }
    return usuarioRepository.save(usuario.conEstado(activo));
  }
}
