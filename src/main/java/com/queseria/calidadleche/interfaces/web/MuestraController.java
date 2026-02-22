package com.queseria.calidadleche.interfaces.web;

import com.queseria.calidadleche.application.usecase.RegistrarMuestraUseCase;
import com.queseria.calidadleche.domain.model.*;
import com.queseria.calidadleche.domain.service.EvaluacionCalidadService;
import com.queseria.calidadleche.domain.service.PersistirAnaliticaService;
import com.queseria.calidadleche.domain.repo.MuestraRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Validated
@RestController
@RequestMapping("/api/v1/muestras")
public class MuestraController {

  // ======= DTOs =======

  // ---- Page DTO para la respuesta paginada ----
  public record PageResp<T>(java.util.List<T> items, long total, int limit, int offset) {}
  // ---- Crear Muestra DTOs ----
  public record ComposicionReq(
      @NotNull BigDecimal grasa,
      @NotNull BigDecimal proteina,
      BigDecimal lactosa,
      @NotNull BigDecimal solidosTotales
  ) {}

  public record FisicoQuimicoReq(
      @NotNull BigDecimal densidad,       // g/mL
      @NotNull BigDecimal acidezDornic,   // °D
      @NotNull BigDecimal temperaturaC
  ) {}

  public record HigieneReq(Integer ufcBacterias, Integer ccSomaticas) {}

  public record CrearMuestraReq(
      @NotNull Long proveedorId,
      @NotNull OffsetDateTime fechaMuestra,
      BigDecimal volumenLitros,
      BigDecimal precioLitro,
      String observaciones,
      @Valid @NotNull ComposicionReq composicion,
      @Valid @NotNull FisicoQuimicoReq fisicoQuimico,
      @Valid HigieneReq higiene,
      BigDecimal sng,            // opcional (si no, se calcula como ST - grasa)
      BigDecimal aguaPct         // opcional
  ) {}

  public record ResultadoParametroResp(String estado, java.util.List<String> mensajes) {}
  public record EvaluacionResp(java.util.Map<String, ResultadoParametroResp> porParametro) {}
  public record MuestraResp(
      Long id, Long proveedorId, OffsetDateTime fechaMuestra,
      BigDecimal volumenLitros, BigDecimal precioLitro, String observaciones,
      BigDecimal grasa, BigDecimal proteina, BigDecimal lactosa, BigDecimal solidosTotales,
      BigDecimal densidad, BigDecimal acidezDornic, BigDecimal temperaturaC,
      Integer ufcBacterias, Integer ccSomaticas,
      EvaluacionResp evaluacion
  ) {}

  private final RegistrarMuestraUseCase registrarUC;
  private final EvaluacionCalidadService evalService;
  private final PersistirAnaliticaService persistirAnaliticaService;
  private final MuestraRepository muestraRepository;

  public MuestraController(RegistrarMuestraUseCase registrarUC, 
                           EvaluacionCalidadService evalService,
                           PersistirAnaliticaService persistirAnaliticaService,
                           MuestraRepository muestraRepository) {
    this.registrarUC = registrarUC;
    this.evalService = evalService;
    this.persistirAnaliticaService = persistirAnaliticaService;
    this.muestraRepository = muestraRepository;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<MuestraResp> crear(@Valid @RequestBody CrearMuestraReq req) {
    var comp = new Composicion(req.composicion().grasa(), req.composicion().proteina(),
        req.composicion().lactosa(), req.composicion().solidosTotales());
    var fq   = new FisicoQuimico(req.fisicoQuimico().densidad(),
        req.fisicoQuimico().acidezDornic(), req.fisicoQuimico().temperaturaC());
    var hig  = (req.higiene()==null) ? new Higiene(0,0) :
        new Higiene(req.higiene().ufcBacterias()==null?0:req.higiene().ufcBacterias(),
                    req.higiene().ccSomaticas()==null?0:req.higiene().ccSomaticas());

    var muestra = MuestraLeche.registrar(
        req.proveedorId(), req.fechaMuestra(),
        req.volumenLitros(), req.precioLitro(), req.observaciones(),
        comp, fq, hig,
        req.aguaPct(),   
        req.sng(),       
        null,            
        null            
    );

    return registrarUC.ejecutar(muestra)
    .flatMap(saved -> {
      // 1) Evaluar al vuelo
      var ev = evalService.evaluar(
          comp.grasa(), comp.proteina(),
          fq.densidad(), fq.temperaturaC(),
          comp.solidosTotales(), req.sng(),
          fq.acidezDornic(), req.aguaPct()
      );

      // 2) Persistir analítica en Mongo y luego construir la respuesta
      return persistirAnaliticaService.persistirAnalisis(saved, ev)
          .then(Mono.fromSupplier(() -> {
            var porParamResp = new java.util.LinkedHashMap<String, ResultadoParametroResp>();
            ev.porParametro().forEach((k,v) ->
                porParamResp.put(k, new ResultadoParametroResp(v.estado(), v.mensajes()))
            );
            var evaluacionResp = new EvaluacionResp(porParamResp);

            return new MuestraResp(
                saved.id(), saved.proveedorId(), saved.fechaMuestra(),
                saved.volumenLitros(), saved.precioLitro(), saved.observaciones(),
                comp.grasa(), comp.proteina(), comp.lactosa(), comp.solidosTotales(),
                fq.densidad(), fq.acidezDornic(), fq.temperaturaC(),
                hig.ufcBacterias(), hig.ccSomaticas(),
                evaluacionResp
            );
          }));
    });
  }

  // ======= NUEVO: GET histórico paginado =======
  @GetMapping
  public Mono<PageResp<MuestraResp>> historico(
      @RequestParam Long proveedorId,
      @RequestParam OffsetDateTime desde,
      @RequestParam OffsetDateTime hasta,
      @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit,
      @RequestParam(defaultValue = "0")  @Min(0) int offset
  ) {

    // ✅ sanity check de rango de fechas
    if (desde.isAfter(hasta)) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST,
            "'desde' no puede ser posterior a 'hasta'"
        );
    }

    Mono<Long> totalMono = muestraRepository.countByProveedorAndRango(proveedorId, desde, hasta);

    Flux<MuestraResp> items = muestraRepository
        .findByProveedorAndRangoPaged(proveedorId, desde, hasta, limit, offset)
        .map(m -> new MuestraResp(
            m.id(), m.proveedorId(), m.fechaMuestra(),
            m.volumenLitros(), m.precioLitro(), m.observaciones(),
            m.composicion().grasa(), m.composicion().proteina(), m.composicion().lactosa(), m.composicion().solidosTotales(),
            m.fisicoQuimico().densidad(), m.fisicoQuimico().acidezDornic(), m.fisicoQuimico().temperaturaC(),
            m.higiene().ufcBacterias(), m.higiene().ccSomaticas(),
            null // evaluacion no aplica en el histórico de Postgres
        ));

    return items.collectList()
        .zipWith(totalMono, (list, total) -> new PageResp<>(list, total, limit, offset));
  }
}
