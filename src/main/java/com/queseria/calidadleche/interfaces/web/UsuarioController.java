package com.queseria.calidadleche.interfaces.web;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.queseria.calidadleche.application.usecase.CambiarEstadoUsuarioUseCase;
import com.queseria.calidadleche.application.usecase.CambiarRolesUsuarioUseCase;
import com.queseria.calidadleche.application.usecase.CrearUsuarioUseCase;
import com.queseria.calidadleche.application.usecase.ListarUsuariosUseCase;
import com.queseria.calidadleche.domain.model.NombreRol;
import com.queseria.calidadleche.domain.model.Usuario;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Validated
@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {
  private final CrearUsuarioUseCase crearUsuarioUseCase;
  private final ListarUsuariosUseCase listarUsuariosUseCase;
  private final CambiarEstadoUsuarioUseCase cambiarEstadoUsuarioUseCase;
  private final CambiarRolesUsuarioUseCase cambiarRolesUsuarioUseCase;

  public UsuarioController(
      CrearUsuarioUseCase crearUsuarioUseCase,
      ListarUsuariosUseCase listarUsuariosUseCase,
      CambiarEstadoUsuarioUseCase cambiarEstadoUsuarioUseCase,
      CambiarRolesUsuarioUseCase cambiarRolesUsuarioUseCase
  ) {
    this.crearUsuarioUseCase = crearUsuarioUseCase;
    this.listarUsuariosUseCase = listarUsuariosUseCase;
    this.cambiarEstadoUsuarioUseCase = cambiarEstadoUsuarioUseCase;
    this.cambiarRolesUsuarioUseCase = cambiarRolesUsuarioUseCase;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<UsuarioResponse> crear(@Valid @RequestBody CrearUsuarioRequest request) {
    return crearUsuarioUseCase.execute(
            request.nombre(), request.email(), request.password(), request.roles())
        .map(this::toResponse);
  }

  @GetMapping
  public Mono<PageResponse<UsuarioResponse>> listar(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(required = false) Boolean activo,
      @RequestParam(defaultValue = "20") @Min(1) @Max(200) int limit,
      @RequestParam(defaultValue = "0") @Min(0) int offset
  ) {
    Flux<UsuarioResponse> items = listarUsuariosUseCase.listar(q, activo, limit, offset)
        .map(this::toResponse);
    return items.collectList()
        .zipWith(
            listarUsuariosUseCase.contar(q, activo),
            (list, total) -> new PageResponse<>(list, total, limit, offset));
  }

  @PatchMapping("/{id}/activar")
  public Mono<UsuarioResponse> activar(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long id
  ) {
    return cambiarEstadoUsuarioUseCase.activar(actorId(jwt), id)
        .map(this::toResponse);
  }

  @PatchMapping("/{id}/desactivar")
  public Mono<UsuarioResponse> desactivar(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long id
  ) {
    return cambiarEstadoUsuarioUseCase.desactivar(actorId(jwt), id)
        .map(this::toResponse);
  }

  @PutMapping("/{id}/roles")
  public Mono<UsuarioResponse> cambiarRoles(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable Long id,
      @Valid @RequestBody CambiarRolesRequest request
  ) {
    return cambiarRolesUsuarioUseCase.execute(actorId(jwt), id, request.roles())
        .map(this::toResponse);
  }

  private Long actorId(Jwt jwt) {
    return Long.valueOf(jwt.getSubject());
  }

  private UsuarioResponse toResponse(Usuario usuario) {
    List<String> roles = usuario.roles().stream()
        .map(NombreRol::name)
        .sorted()
        .toList();
    return new UsuarioResponse(
        usuario.id(),
        usuario.nombre(),
        usuario.email(),
        usuario.activo(),
        roles,
        usuario.queseriaId(),
        usuario.creadoEn(),
        usuario.actualizadoEn());
  }

  public record CrearUsuarioRequest(
      @NotBlank(message = "El nombre es obligatorio")
      @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
      String nombre,
      @NotBlank(message = "El email es obligatorio")
      @Email(message = "El email no tiene un formato válido")
      @Size(max = 180, message = "El email no puede superar 180 caracteres")
      String email,
      @NotBlank(message = "La contraseña es obligatoria")
      @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
      String password,
      @NotNull(message = "El usuario debe tener exactamente un rol")
      @Size(min = 1, max = 1, message = "El usuario debe tener exactamente un rol")
      Set<@NotNull NombreRol> roles
  ) {}

  public record CambiarRolesRequest(
      @NotNull(message = "El usuario debe tener exactamente un rol")
      @Size(min = 1, max = 1, message = "El usuario debe tener exactamente un rol")
      Set<@NotNull NombreRol> roles
  ) {}

  public record UsuarioResponse(
      Long id,
      String nombre,
      String email,
      boolean activo,
      List<String> roles,
      Long queseriaId,
      OffsetDateTime creadoEn,
      OffsetDateTime actualizadoEn
  ) {}

  public record PageResponse<T>(List<T> items, long total, int limit, int offset) {}
}
