package com.queseria.calidadleche.application.usecase;

import com.queseria.calidadleche.domain.model.Usuario;
import com.queseria.calidadleche.domain.repo.UsuarioRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ListarUsuariosUseCase {
  private final UsuarioRepository usuarioRepository;

  public ListarUsuariosUseCase(UsuarioRepository usuarioRepository) {
    this.usuarioRepository = usuarioRepository;
  }

  public Flux<Usuario> listar(String q, Boolean activo, int limit, int offset) {
    return usuarioRepository.searchPaged(q, activo, limit, offset);
  }

  public Mono<Long> contar(String q, Boolean activo) {
    return usuarioRepository.countFiltered(q, activo);
  }
}
