package com.queseria.calidadleche.application.port;

import reactor.core.publisher.Mono;

public interface PasswordVerifier {
  Mono<Boolean> matches(String rawPassword, String passwordHash);
  Mono<Boolean> matchesDummy(String rawPassword);
}
