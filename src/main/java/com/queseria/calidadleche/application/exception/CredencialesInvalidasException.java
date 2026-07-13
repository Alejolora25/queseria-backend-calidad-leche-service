package com.queseria.calidadleche.application.exception;

public class CredencialesInvalidasException extends RuntimeException {
  public static final String MESSAGE = "Credenciales inválidas";

  public CredencialesInvalidasException() {
    super(MESSAGE);
  }
}
