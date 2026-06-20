package com.queseria.calidadleche.infrastructure.config;

import com.queseria.calidadleche.domain.model.NombreRol;
import com.queseria.calidadleche.domain.model.Usuario;
import com.queseria.calidadleche.domain.repo.UsuarioRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;

@Component
public class AdminUserInitializer implements ApplicationRunner {
  private final UsuarioRepository usuarioRepository;
  private final Environment environment;
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  public AdminUserInitializer(UsuarioRepository usuarioRepository, Environment environment) {
    this.usuarioRepository = usuarioRepository;
    this.environment = environment;
  }

  @Override
  public void run(ApplicationArguments args) {
    ensureAdminUser().block(Duration.ofSeconds(30));
  }

  private Mono<Void> ensureAdminUser() {
    String email = property("app.security.admin.email");
    String password = property("app.security.admin.password");
    String name = property("app.security.admin.name");

    if (isBlank(email) || isBlank(password) || isBlank(name)) {
      if (isProdProfile()) {
        return Mono.error(new IllegalStateException(
            "ADMIN_EMAIL, ADMIN_PASSWORD y ADMIN_NAME son obligatorios en produccion"
        ));
      }
      return Mono.empty();
    }

    return usuarioRepository.findByEmail(email)
        .flatMap(existing -> usuarioRepository.asignarRol(existing.id(), NombreRol.ADMIN).thenReturn(existing))
        .switchIfEmpty(Mono.defer(() -> {
          String passwordHash = passwordEncoder.encode(password);
          Usuario admin = Usuario.crear(name, email, passwordHash, null);
          return usuarioRepository.save(admin)
              .flatMap(saved -> usuarioRepository.asignarRol(saved.id(), NombreRol.ADMIN).thenReturn(saved));
        }))
        .then();
  }

  private String property(String key) {
    String value = environment.getProperty(key);
    return value == null ? "" : value.trim();
  }

  private boolean isProdProfile() {
    return Arrays.asList(environment.getActiveProfiles()).contains("prod");
  }

  private boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }
}
