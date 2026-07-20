package com.queseria.calidadleche.infrastructure.persistence.r2dbc.repo;

import com.queseria.calidadleche.infrastructure.persistence.r2dbc.row.UsuarioRow;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.r2dbc.repository.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UsuarioR2dbcRepository extends ReactiveCrudRepository<UsuarioRow, Long> {
  Mono<UsuarioRow> findByEmail(String email);
  Mono<Boolean> existsByEmail(String email);

  @Query("""
      select * from usuarios u
      where (:q = '' or lower(u.nombre) like lower(concat('%', :q, '%'))
        or lower(u.email) like lower(concat('%', :q, '%')))
        and (:activo is null or u.activo = :activo)
      order by lower(u.nombre), u.id
      limit :limit offset :offset
      """)
  Flux<UsuarioRow> searchPaged(String q, Boolean activo, int limit, int offset);

  @Query("""
      select count(*) from usuarios u
      where (:q = '' or lower(u.nombre) like lower(concat('%', :q, '%'))
        or lower(u.email) like lower(concat('%', :q, '%')))
        and (:activo is null or u.activo = :activo)
      """)
  Mono<Long> countFiltered(String q, Boolean activo);
}
