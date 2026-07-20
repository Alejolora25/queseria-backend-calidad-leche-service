package com.queseria.calidadleche.application.exception;

public class EmailUsuarioYaRegistradoException extends RuntimeException {
  public static final String MESSAGE = "El email ya está registrado";

  public EmailUsuarioYaRegistradoException() {
    super(MESSAGE);
  }
}
