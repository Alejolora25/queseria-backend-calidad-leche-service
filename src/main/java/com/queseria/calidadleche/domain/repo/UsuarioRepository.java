package com.queseria.calidadleche.domain.repo;

import com.queseria.calidadleche.domain.model.NombreRol;
import com.queseria.calidadleche.domain.model.Usuario;
import reactor.core.publisher.Mono;

public interface UsuarioRepository {
  Mono<Usuario> save(Usuario usuario);
  Mono<Usuario> findByEmail(String email);
  Mono<Boolean> existsByEmail(String email);
  Mono<Void> asignarRol(Long usuarioId, NombreRol rol);
}
