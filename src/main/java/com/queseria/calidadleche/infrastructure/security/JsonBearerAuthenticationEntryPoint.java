package com.queseria.calidadleche.infrastructure.security;

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

public class JsonBearerAuthenticationEntryPoint implements ServerAuthenticationEntryPoint {
  private static final byte[] RESPONSE_BODY = """
      {"error":"unauthorized","message":"Token inválido o expirado"}
      """.strip().getBytes(StandardCharsets.UTF_8);

  @Override
  public Mono<Void> commence(ServerWebExchange exchange, AuthenticationException exception) {
    var response = exchange.getResponse();
    response.setStatusCode(HttpStatus.UNAUTHORIZED);
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    response.getHeaders().set(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
    DataBuffer buffer = response.bufferFactory().wrap(RESPONSE_BODY);
    return response.writeWith(Mono.just(buffer));
  }
}
