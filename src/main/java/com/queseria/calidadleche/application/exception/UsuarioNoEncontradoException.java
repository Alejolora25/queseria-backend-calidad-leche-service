package com.queseria.calidadleche.application.exception;

public class UsuarioNoEncontradoException extends RuntimeException {
  public static final String MESSAGE = "Usuario no existe";

  public UsuarioNoEncontradoException() {
    super(MESSAGE);
  }
}
