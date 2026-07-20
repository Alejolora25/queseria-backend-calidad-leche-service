package com.queseria.calidadleche.application.port;

import reactor.core.publisher.Mono;

public interface PasswordHasher {
  Mono<String> hash(String rawPassword);
}
