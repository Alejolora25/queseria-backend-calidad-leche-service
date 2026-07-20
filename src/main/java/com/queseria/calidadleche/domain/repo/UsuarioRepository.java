package com.queseria.calidadleche.domain.repo;

import com.queseria.calidadleche.domain.model.NombreRol;
import com.queseria.calidadleche.domain.model.Usuario;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

public interface UsuarioRepository {
  Mono<Usuario> save(Usuario usuario);
  Mono<Usuario> findByEmail(String email);
  Mono<Usuario> findById(Long id);
  Mono<Boolean> existsByEmail(String email);
  Mono<Void> asignarRol(Long usuarioId, NombreRol rol);
  Mono<Usuario> saveWithRoles(Usuario usuario);
  Mono<Usuario> replaceRoles(Long usuarioId, java.util.Set<NombreRol> roles);
  Flux<Usuario> searchPaged(String q, Boolean activo, int limit, int offset);
  Mono<Long> countFiltered(String q, Boolean activo);
  Mono<Long> countActiveByRole(NombreRol rol);
}
