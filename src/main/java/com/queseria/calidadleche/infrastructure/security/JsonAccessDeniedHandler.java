package com.queseria.calidadleche.infrastructure.security;

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

public class JsonAccessDeniedHandler implements ServerAccessDeniedHandler {
  private static final byte[] RESPONSE_BODY = """
      {"error":"forbidden","message":"No tienes permisos para realizar esta acción"}
      """.strip().getBytes(StandardCharsets.UTF_8);

  @Override
  public Mono<Void> handle(ServerWebExchange exchange, AccessDeniedException denied) {
    var response = exchange.getResponse();
    response.setStatusCode(HttpStatus.FORBIDDEN);
    response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
    DataBuffer buffer = response.bufferFactory().wrap(RESPONSE_BODY);
    return response.writeWith(Mono.just(buffer));
  }
}
