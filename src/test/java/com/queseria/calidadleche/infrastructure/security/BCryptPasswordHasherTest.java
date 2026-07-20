package com.queseria.calidadleche.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import reactor.test.StepVerifier;

class BCryptPasswordHasherTest {
  @Test
  void debeGenerarHashBCryptSinDevolverLaPasswordPlana() {
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    BCryptPasswordHasher hasher = new BCryptPasswordHasher(encoder);

    StepVerifier.create(hasher.hash("Admin123*"))
        .assertNext(hash -> {
          assertThat(hash).startsWith("$2");
          assertThat(hash).isNotEqualTo("Admin123*");
          assertThat(encoder.matches("Admin123*", hash)).isTrue();
        })
        .verifyComplete();
  }
}
