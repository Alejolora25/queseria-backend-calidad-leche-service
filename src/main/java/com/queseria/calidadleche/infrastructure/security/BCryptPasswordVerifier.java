package com.queseria.calidadleche.infrastructure.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.queseria.calidadleche.application.port.PasswordVerifier;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class BCryptPasswordVerifier implements PasswordVerifier {
  private static final String DUMMY_PASSWORD = "dummy-password-for-login-timing";

  private final PasswordEncoder passwordEncoder;
  private final String dummyPasswordHash;

  public BCryptPasswordVerifier(PasswordEncoder passwordEncoder) {
    this.passwordEncoder = passwordEncoder;
    this.dummyPasswordHash = passwordEncoder.encode(DUMMY_PASSWORD);
  }

  @Override
  public Mono<Boolean> matches(String rawPassword, String passwordHash) {
    return compare(rawPassword, passwordHash);
  }

  @Override
  public Mono<Boolean> matchesDummy(String rawPassword) {
    return compare(rawPassword, dummyPasswordHash);
  }

  private Mono<Boolean> compare(String rawPassword, String passwordHash) {
    return Mono.fromCallable(() -> passwordEncoder.matches(rawPassword, passwordHash))
        .subscribeOn(Schedulers.boundedElastic());
  }
}
