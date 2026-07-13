package com.queseria.calidadleche.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import reactor.test.StepVerifier;

class BCryptPasswordVerifierTest {

  @Test
  void debeCompararContrasenaRealConBCrypt() {
    PasswordEncoder encoder = new BCryptPasswordEncoder();
    BCryptPasswordVerifier verifier = new BCryptPasswordVerifier(encoder);
    String hash = encoder.encode("Admin123*");

    StepVerifier.create(verifier.matches("Admin123*", hash))
        .expectNext(true)
        .verifyComplete();

    StepVerifier.create(verifier.matches("incorrecta", hash))
        .expectNext(false)
        .verifyComplete();
  }

  @Test
  void debeEjecutarComparacionFicticiaSinAutenticar() {
    BCryptPasswordVerifier verifier = new BCryptPasswordVerifier(new BCryptPasswordEncoder());

    StepVerifier.create(verifier.matchesDummy("cualquier-clave"))
        .expectNext(false)
        .verifyComplete();
  }
}
