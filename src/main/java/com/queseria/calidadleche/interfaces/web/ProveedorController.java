package com.queseria.calidadleche.interfaces.web;

import com.queseria.calidadleche.application.usecase.CrearProveedorUseCase;
import com.queseria.calidadleche.application.usecase.BuscarProveedorUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import reactor.core.publisher.Flux;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/api/v1/proveedores")
public class ProveedorController {

  public record CrearProveedorReq(@NotBlank String nombre, @NotBlank String tipoIdentificacion, @NotBlank String identificacion) {}
  public record ProveedorResp(Long id, String nombre, String tipoIdentificacion, String identificacion, Boolean activo) {}
  public record PageResp<T>(java.util.List<T> items, long total, int limit, int offset) {}

  private final CrearProveedorUseCase crearUC;
  private final BuscarProveedorUseCase buscarUC;

  public ProveedorController(CrearProveedorUseCase crearUC, BuscarProveedorUseCase buscarUC) {
    this.crearUC = crearUC;
    this.buscarUC = buscarUC;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<ProveedorResp> crear(@Valid @RequestBody CrearProveedorReq req) {
    return crearUC.ejecutar(req.nombre(), req.tipoIdentificacion(), req.identificacion())
      .map(p -> new ProveedorResp(p.id(), p.nombre(), p.tipoIdentificacion(), p.identificacion(), p.activo()));
  }

  @GetMapping("/{id}")
  public Mono<ProveedorResp> obtenerPorId(@PathVariable Long id) {
    return buscarUC.porId(id)
        .map(p -> new ProveedorResp(p.id(), p.nombre(), p.tipoIdentificacion(), p.identificacion(), p.activo()))
        .switchIfEmpty(Mono.error(
            new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Proveedor no existe"
            )
        ));
  }

  @GetMapping(params = "identificacion")
  public Mono<ProveedorResp> obtenerPorIdentificacion(@RequestParam("identificacion") String ident) {
    return buscarUC.porIdentificacion(ident)
        .map(p -> new ProveedorResp(p.id(), p.nombre(), p.tipoIdentificacion(), p.identificacion(), p.activo()))
        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Proveedor no existe")));
  }

  @GetMapping
  public Mono<PageResp<ProveedorResp>> listar(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(required = false) Boolean activo,
      @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit,
      @RequestParam(defaultValue = "0")  @Min(0) int offset
  ) {
    Mono<Long> totalMono = buscarUC.contar(q, activo);

    Flux<ProveedorResp> items = buscarUC.listar(q, activo, limit, offset)
        .map(p -> new ProveedorResp(p.id(), p.nombre(), p.tipoIdentificacion(), p.identificacion(), p.activo()));

    return items.collectList()
        .zipWith(totalMono, (list, total) -> new PageResp<>(list, total, limit, offset));
  }
}