package com.queseria.calidadleche.infrastructure.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.queseria.calidadleche.application.port.PasswordHasher;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class BCryptPasswordHasher implements PasswordHasher {
  private final PasswordEncoder passwordEncoder;

  public BCryptPasswordHasher(PasswordEncoder passwordEncoder) {
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public Mono<String> hash(String rawPassword) {
    return Mono.fromCallable(() -> passwordEncoder.encode(rawPassword))
        .subscribeOn(Schedulers.boundedElastic());
  }
}
