package com.queseria.calidadleche.interfaces.web;

import com.queseria.calidadleche.application.usecase.RegistrarMuestraConEvaluacionUseCase;
import com.queseria.calidadleche.domain.model.Composicion;
import com.queseria.calidadleche.domain.model.FisicoQuimico;
import com.queseria.calidadleche.domain.model.Higiene;
import com.queseria.calidadleche.domain.model.MuestraLeche;
import com.queseria.calidadleche.domain.repo.MuestraRepository;
import com.queseria.calidadleche.domain.service.EvaluacionCalidadService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/v1/muestras")
public class MuestraController {

  public record PageResp<T>(List<T> items, long total, int limit, int offset) {}

  public record ComposicionReq(
      @NotNull BigDecimal grasa,
      @NotNull BigDecimal proteina,
      BigDecimal lactosa,
      @NotNull BigDecimal solidosTotales
  ) {}

  public record FisicoQuimicoReq(
      @NotNull BigDecimal densidad,
      @NotNull BigDecimal acidezDornic,
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
      @NotNull BigDecimal sng,
      BigDecimal aguaPct
  ) {}

  public record ResultadoParametroResp(String estado, List<String> mensajes) {}
  public record EvaluacionResp(Map<String, ResultadoParametroResp> porParametro) {}
  public record MuestraResp(
      Long id, Long proveedorId, OffsetDateTime fechaMuestra,
      BigDecimal volumenLitros, BigDecimal precioLitro, String observaciones,
      BigDecimal grasa, BigDecimal proteina, BigDecimal lactosa, BigDecimal solidosTotales,
      BigDecimal densidad, BigDecimal acidezDornic, BigDecimal temperaturaC,
      Integer ufcBacterias, Integer ccSomaticas,
      EvaluacionResp evaluacion
  ) {}

  private final RegistrarMuestraConEvaluacionUseCase registrarConEvaluacionUC;
  private final MuestraRepository muestraRepository;

  public MuestraController(
      RegistrarMuestraConEvaluacionUseCase registrarConEvaluacionUC,
      MuestraRepository muestraRepository
  ) {
    this.registrarConEvaluacionUC = registrarConEvaluacionUC;
    this.muestraRepository = muestraRepository;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<MuestraResp> crear(@Valid @RequestBody CrearMuestraReq req) {
    MuestraLeche muestra = toDomain(req);

    return registrarConEvaluacionUC.ejecutar(muestra)
        .map(resultado -> toResp(resultado.muestra(), resultado.evaluacion()));
  }

  @GetMapping
  public Mono<PageResp<MuestraResp>> historico(
      @RequestParam Long proveedorId,
      @RequestParam OffsetDateTime desde,
      @RequestParam OffsetDateTime hasta,
      @RequestParam(defaultValue = "50") @Min(1) @Max(200) int limit,
      @RequestParam(defaultValue = "0") @Min(0) int offset
  ) {
    if (desde.isAfter(hasta)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "'desde' no puede ser posterior a 'hasta'"
      );
    }

    Mono<Long> totalMono = muestraRepository.countByProveedorAndRango(proveedorId, desde, hasta);

    Flux<MuestraResp> items = muestraRepository
        .findByProveedorAndRangoPaged(proveedorId, desde, hasta, limit, offset)
        .map(m -> toResp(m, null));

    return items.collectList()
        .zipWith(totalMono, (list, total) -> new PageResp<>(list, total, limit, offset));
  }

  private MuestraLeche toDomain(CrearMuestraReq req) {
    BigDecimal solidosTotalesCalculados = req.composicion().grasa().add(req.sng());

    var comp = new Composicion(
        req.composicion().grasa(),
        req.composicion().proteina(),
        req.composicion().lactosa(),
        solidosTotalesCalculados
    );

    var fq = new FisicoQuimico(
        req.fisicoQuimico().densidad(),
        req.fisicoQuimico().acidezDornic(),
        req.fisicoQuimico().temperaturaC()
    );

    var hig = req.higiene() == null
        ? new Higiene(0, 0)
        : new Higiene(
            req.higiene().ufcBacterias() == null ? 0 : req.higiene().ufcBacterias(),
            req.higiene().ccSomaticas() == null ? 0 : req.higiene().ccSomaticas()
        );

    return MuestraLeche.registrar(
        req.proveedorId(),
        req.fechaMuestra(),
        req.volumenLitros(),
        req.precioLitro(),
        req.observaciones(),
        comp,
        fq,
        hig,
        req.aguaPct(),
        req.sng(),
        null,
        null
    );
  }

  private MuestraResp toResp(
      MuestraLeche muestra,
      EvaluacionCalidadService.EvaluacionMuestra evaluacion
  ) {
    EvaluacionResp evaluacionResp = null;

    if (evaluacion != null) {
      var porParamResp = new LinkedHashMap<String, ResultadoParametroResp>();
      evaluacion.porParametro().forEach((k, v) ->
          porParamResp.put(k, new ResultadoParametroResp(v.estado(), v.mensajes()))
      );
      evaluacionResp = new EvaluacionResp(porParamResp);
    }

    return new MuestraResp(
        muestra.id(),
        muestra.proveedorId(),
        muestra.fechaMuestra(),
        muestra.volumenLitros(),
        muestra.precioLitro(),
        muestra.observaciones(),
        muestra.composicion().grasa(),
        muestra.composicion().proteina(),
        muestra.composicion().lactosa(),
        muestra.composicion().solidosTotales(),
        muestra.fisicoQuimico().densidad(),
        muestra.fisicoQuimico().acidezDornic(),
        muestra.fisicoQuimico().temperaturaC(),
        muestra.higiene().ufcBacterias(),
        muestra.higiene().ccSomaticas(),
        evaluacionResp
    );
  }
}
