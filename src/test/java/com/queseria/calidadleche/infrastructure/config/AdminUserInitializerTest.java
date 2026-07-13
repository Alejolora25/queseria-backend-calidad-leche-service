package com.queseria.calidadleche.infrastructure.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.queseria.calidadleche.domain.model.NombreRol;
import com.queseria.calidadleche.domain.model.Usuario;
import com.queseria.calidadleche.domain.repo.UsuarioRepository;

import reactor.core.publisher.Mono;

class AdminUserInitializerTest {

  @Test
  void debeUsarPasswordEncoderInyectadoParaCrearAdministrador() {
    UsuarioRepository repository = org.mockito.Mockito.mock(UsuarioRepository.class);
    Environment environment = org.mockito.Mockito.mock(Environment.class);
    PasswordEncoder passwordEncoder = org.mockito.Mockito.mock(PasswordEncoder.class);
    Usuario saved = Usuario.reconstruir(
        1L, "Administrador", "admin@queseria.local", "hash-bcrypt", true, null,
        java.util.Set.of(), null, null
    );

    when(environment.getProperty("app.security.admin.email")).thenReturn("admin@queseria.local");
    when(environment.getProperty("app.security.admin.password")).thenReturn("Admin123*");
    when(environment.getProperty("app.security.admin.name")).thenReturn("Administrador");
    when(environment.getActiveProfiles()).thenReturn(new String[] { "dev" });
    when(repository.findByEmail("admin@queseria.local")).thenReturn(Mono.empty());
    when(passwordEncoder.encode("Admin123*")).thenReturn("hash-bcrypt");
    when(repository.save(any(Usuario.class))).thenReturn(Mono.just(saved));
    when(repository.asignarRol(1L, NombreRol.ADMIN)).thenReturn(Mono.empty());

    new AdminUserInitializer(repository, environment, passwordEncoder)
        .run(new DefaultApplicationArguments(new String[0]));

    verify(passwordEncoder).encode("Admin123*");
    verify(repository).asignarRol(1L, NombreRol.ADMIN);
  }
}
