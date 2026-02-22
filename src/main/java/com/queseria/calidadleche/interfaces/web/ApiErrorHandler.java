package com.queseria.calidadleche.interfaces.web;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;



import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiErrorHandler {

  // 400 - Body @Valid (WebFlux)
  @ExceptionHandler(WebExchangeBindException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, Object> handleWebExchangeBindException(WebExchangeBindException ex) {
    var fieldErrors = ex.getFieldErrors().stream()
        .collect(Collectors.groupingBy(
            fe -> fe.getField(),
            Collectors.mapping(fe -> fe.getDefaultMessage(), Collectors.toList())
        ));

    Map<String, Object> body = new HashMap<>();
    body.put("error", "bad_request");
    body.put("message", "Errores de validación");
    body.put("fields", fieldErrors);
    return body;
  }

  // 400 - Body @Valid (MVC-style; por compatibilidad)
  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
    var fieldErrors = ex.getBindingResult().getFieldErrors().stream()
        .collect(Collectors.groupingBy(
            fe -> fe.getField(),
            Collectors.mapping(fe -> fe.getDefaultMessage(), Collectors.toList())
        ));

    Map<String, Object> body = new HashMap<>();
    body.put("error", "bad_request");
    body.put("message", "Errores de validación");
    body.put("fields", fieldErrors);
    return body;
  }

  // 400 - Query params / path variables
  @ExceptionHandler(ConstraintViolationException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, Object> handleConstraintViolation(ConstraintViolationException ex) {
    // arma un mapa field -> [mensajes]
    var fields = new java.util.LinkedHashMap<String, java.util.List<String>>();
    ex.getConstraintViolations().forEach(v -> {
      // propertyPath puede ser "historico.limit" o solo "limit"; extraemos el último nodo
      String path = v.getPropertyPath() == null ? "" : v.getPropertyPath().toString();
      String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
      fields.computeIfAbsent(field, k -> new java.util.ArrayList<>()).add(v.getMessage());
    });
    return Map.of(
        "error", "bad_request",
        "message", "Errores de validación en parámetros",
        "fields", fields
    );
  }

  // 400 - Tipos inválidos / conversión (e.g. fecha mal formateada)
  @ExceptionHandler(ServerWebInputException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, Object> handleServerWebInput(ServerWebInputException ex) {
    return Map.of(
        "error", "bad_request",
        "message", ex.getReason() != null ? ex.getReason() : "Entrada inválida"
    );
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.CONFLICT) // 409
  public Map<String, Object> conflict(IllegalArgumentException ex) {
    return Map.of("error", "conflict", "message", ex.getMessage());
  }

  @ExceptionHandler(ResponseStatusException.class)
  @ResponseStatus
  public Map<String, Object> notFound(ResponseStatusException ex) {
    // Por defecto
    Map<String, Object> body = new java.util.LinkedHashMap<>();
    body.put("error", ex.getStatusCode().toString());
    body.put("message", ex.getReason());

    // Mapeo simple de códigos semánticos (opcional)
    if (ex.getStatusCode().value() == 404 && "Proveedor no existe".equals(ex.getReason())) {
      body.put("code", "PROVEEDOR_NOT_FOUND");
    }
    return body;
  }
}