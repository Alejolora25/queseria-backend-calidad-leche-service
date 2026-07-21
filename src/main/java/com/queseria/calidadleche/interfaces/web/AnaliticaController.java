package com.queseria.calidadleche.interfaces.web;

import com.queseria.calidadleche.application.model.AnaliticaMuestraConsulta;
import com.queseria.calidadleche.application.usecase.BuscarAnaliticaPorMuestraUseCase;
import com.queseria.calidadleche.application.usecase.ObtenerResumenAnaliticaProveedorUseCase;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analiticas")
public class AnaliticaController {

  private final BuscarAnaliticaPorMuestraUseCase buscarAnaliticaPorMuestraUseCase;
  private final ObtenerResumenAnaliticaProveedorUseCase obtenerResumenAnaliticaProveedorUseCase;

  public AnaliticaController(
      BuscarAnaliticaPorMuestraUseCase buscarAnaliticaPorMuestraUseCase,
      ObtenerResumenAnaliticaProveedorUseCase obtenerResumenAnaliticaProveedorUseCase
  ) {
    this.buscarAnaliticaPorMuestraUseCase = buscarAnaliticaPorMuestraUseCase;
    this.obtenerResumenAnaliticaProveedorUseCase = obtenerResumenAnaliticaProveedorUseCase;
  }

  @GetMapping("/muestra/{sampleId}")
  public Mono<AnaliticaMuestraConsulta> porMuestra(@PathVariable Long sampleId) {
    return buscarAnaliticaPorMuestraUseCase.execute(sampleId)
        .switchIfEmpty(Mono.error(new ResponseStatusException(
            HttpStatus.NOT_FOUND,
            "La muestra no existe"
        )));
  }

  @GetMapping("/proveedor/{proveedorId}/resumen")
  public Mono<Map<String, Object>> resumenProveedor(
      @PathVariable Long proveedorId,
      @RequestParam Instant desde,
      @RequestParam Instant hasta
  ) {
    return obtenerResumenAnaliticaProveedorUseCase.execute(proveedorId, desde, hasta);
  }
}
