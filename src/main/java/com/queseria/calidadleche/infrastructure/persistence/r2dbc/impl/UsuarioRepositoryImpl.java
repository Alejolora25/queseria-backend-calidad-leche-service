package com.queseria.calidadleche.infrastructure.persistence.r2dbc.impl;

import com.queseria.calidadleche.domain.model.NombreRol;
import com.queseria.calidadleche.domain.model.Usuario;
import com.queseria.calidadleche.domain.repo.UsuarioRepository;
import com.queseria.calidadleche.infrastructure.persistence.r2dbc.mapper.UsuarioRowMapper;
import com.queseria.calidadleche.infrastructure.persistence.r2dbc.repo.UsuarioR2dbcRepository;
import com.queseria.calidadleche.infrastructure.persistence.r2dbc.row.UsuarioRow;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.Locale;
import java.util.Set;

@Repository
public class UsuarioRepositoryImpl implements UsuarioRepository {
  private final UsuarioR2dbcRepository repo;
  private final DatabaseClient databaseClient;

  public UsuarioRepositoryImpl(UsuarioR2dbcRepository repo, DatabaseClient databaseClient) {
    this.repo = repo;
    this.databaseClient = databaseClient;
  }

  @Override
  public Mono<Usuario> save(Usuario usuario) {
    return repo.save(UsuarioRowMapper.toRow(usuario))
        .flatMap(this::toDomainWithRoles);
  }

  @Override
  public Mono<Usuario> findByEmail(String email) {
    return repo.findByEmail(normalizarEmail(email))
        .flatMap(this::toDomainWithRoles);
  }

  @Override
  public Mono<Boolean> existsByEmail(String email) {
    return repo.existsByEmail(normalizarEmail(email));
  }

  @Override
  public Mono<Void> asignarRol(Long usuarioId, NombreRol rol) {
    return databaseClient.sql("""
          insert into usuario_roles (usuario_id, rol_id)
          select :usuarioId, r.id
          from roles r
          where r.nombre = :rol
          on conflict (usuario_id, rol_id) do nothing
        """)
        .bind("usuarioId", usuarioId)
        .bind("rol", rol.name())
        .fetch()
        .rowsUpdated()
        .then();
  }

  private Mono<Usuario> toDomainWithRoles(UsuarioRow row) {
    return rolesByUsuarioId(row.getId())
        .map(roles -> UsuarioRowMapper.toDomain(row, roles));
  }

  private Mono<Set<NombreRol>> rolesByUsuarioId(Long usuarioId) {
    return databaseClient.sql("""
          select r.nombre
          from roles r
          join usuario_roles ur on ur.rol_id = r.id
          where ur.usuario_id = :usuarioId
          order by r.nombre
        """)
        .bind("usuarioId", usuarioId)
        .map((row, metadata) -> NombreRol.valueOf(row.get("nombre", String.class)))
        .all()
        .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
  }

  private String normalizarEmail(String email) {
    return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
  }
}
