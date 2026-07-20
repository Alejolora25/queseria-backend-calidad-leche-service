package com.queseria.calidadleche.application.usecase;

import java.util.LinkedHashSet;
import java.util.Set;

import com.queseria.calidadleche.application.exception.OperacionUsuarioNoPermitidaException;
import com.queseria.calidadleche.application.exception.UsuarioNoEncontradoException;
import com.queseria.calidadleche.domain.model.NombreRol;
import com.queseria.calidadleche.domain.model.Usuario;
import com.queseria.calidadleche.domain.repo.UsuarioRepository;

import reactor.core.publisher.Mono;

public class CambiarRolesUsuarioUseCase {
  private final UsuarioRepository usuarioRepository;

  public CambiarRolesUsuarioUseCase(UsuarioRepository usuarioRepository) {
    this.usuarioRepository = usuarioRepository;
  }

  public Mono<Usuario> execute(Long actorId, Long usuarioId, Set<NombreRol> roles) {
    if (roles == null || roles.size() != 1) {
      return Mono.error(new OperacionUsuarioNoPermitidaException(
          "El usuario debe tener exactamente un rol"));
    }
    Set<NombreRol> newRoles = new LinkedHashSet<>(roles);

    return usuarioRepository.findById(usuarioId)
        .switchIfEmpty(Mono.error(new UsuarioNoEncontradoException()))
        .flatMap(usuario -> validarCambio(actorId, usuario, newRoles));
  }

  private Mono<Usuario> validarCambio(Long actorId, Usuario usuario, Set<NombreRol> newRoles) {
    boolean removesAdmin = usuario.roles().contains(NombreRol.ADMIN)
        && !newRoles.contains(NombreRol.ADMIN);

    if (removesAdmin && usuario.id().equals(actorId)) {
      return Mono.error(new OperacionUsuarioNoPermitidaException(
          "No puedes quitarte tu propio rol ADMIN"));
    }
    if (removesAdmin && usuario.activo()) {
      return usuarioRepository.countActiveByRole(NombreRol.ADMIN)
          .flatMap(total -> total <= 1
              ? Mono.error(new OperacionUsuarioNoPermitidaException(
                  "No se puede retirar el rol al último administrador activo"))
              : usuarioRepository.replaceRoles(usuario.id(), newRoles));
    }
    return usuarioRepository.replaceRoles(usuario.id(), newRoles);
  }
}
