package com.queseria.calidadleche.interfaces.web;

import java.util.List;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.queseria.calidadleche.application.model.LoginResult;
import com.queseria.calidadleche.application.usecase.LoginUseCase;
import com.queseria.calidadleche.domain.model.NombreRol;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final LoginUseCase loginUseCase;

  public AuthController(LoginUseCase loginUseCase) {
    this.loginUseCase = loginUseCase;
  }

  @PostMapping("/login")
  public Mono<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    return loginUseCase.execute(request.email(), request.password())
        .map(this::toResponse);
  }

  private LoginResponse toResponse(LoginResult result) {
    List<String> roles = result.roles().stream()
        .map(NombreRol::name)
        .sorted()
        .toList();
    UsuarioResponse usuario = new UsuarioResponse(
        result.usuarioId(),
        result.nombre(),
        result.email(),
        roles,
        result.queseriaId()
    );
    return new LoginResponse(
        result.accessToken(),
        "Bearer",
        result.expiresInSeconds(),
        usuario
    );
  }

  public record LoginRequest(
      @NotBlank(message = "El email es obligatorio")
      @Email(message = "El email no tiene un formato válido")
      String email,
      @NotBlank(message = "La contraseña es obligatoria")
      String password
  ) {}

  public record LoginResponse(
      String accessToken,
      String tokenType,
      long expiresIn,
      UsuarioResponse usuario
  ) {}

  public record UsuarioResponse(
      Long id,
      String nombre,
      String email,
      List<String> roles,
      Long queseriaId
  ) {}
}
