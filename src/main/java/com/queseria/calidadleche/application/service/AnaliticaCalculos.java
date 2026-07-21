package com.queseria.calidadleche.application.service;

import com.queseria.calidadleche.application.model.AnaliticaMuestraConsulta;
import com.queseria.calidadleche.domain.model.MuestraLeche;
import com.queseria.calidadleche.domain.service.EvaluacionCalidadService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class AnaliticaCalculos {

  private AnaliticaCalculos() {}

  public static EvaluacionCalidadService.EvaluacionMuestra evaluar(
      MuestraLeche muestra,
      EvaluacionCalidadService evaluacionService
  ) {
    return evaluacionService.evaluar(
        muestra.composicion().grasa(),
        muestra.composicion().proteina(),
        muestra.fisicoQuimico().densidad(),
        muestra.fisicoQuimico().temperaturaC(),
        muestra.composicion().solidosTotales(),
        muestra.sng(),
        muestra.fisicoQuimico().acidezDornic(),
        muestra.aguaPct()
    );
  }

  public static List<String> flags(EvaluacionCalidadService.EvaluacionMuestra evaluacion) {
    Set<String> flags = new LinkedHashSet<>();
    evaluacion.porParametro().values().stream()
        .map(EvaluacionCalidadService.ResultadoParametro::estado)
        .filter(Objects::nonNull)
        .filter(estado -> !estado.equalsIgnoreCase("INFO") && !estado.equalsIgnoreCase("SIN_DATO"))
        .forEach(flags::add);
    return new ArrayList<>(flags);
  }

  public static String estadoGeneral(EvaluacionCalidadService.EvaluacionMuestra evaluacion) {
    List<String> estados = evaluacion.porParametro().values().stream()
        .map(EvaluacionCalidadService.ResultadoParametro::estado)
        .filter(Objects::nonNull)
        .toList();

    if (estados.stream().anyMatch("RECHAZAR"::equalsIgnoreCase)) return "RECHAZAR";
    if (estados.stream().anyMatch("ALERTA"::equalsIgnoreCase)) return "ALERTA";
    return "ACEPTABLE";
  }

  public static BigDecimal kpi(EvaluacionCalidadService.EvaluacionMuestra evaluacion) {
    int total = 0;
    int puntos = 0;

    for (var resultado : evaluacion.porParametro().values()) {
      String estado = resultado.estado();
      if (estado == null || estado.equals("INFO") || estado.equals("SIN_DATO")) continue;
      total++;
      if (estado.equals("ACEPTABLE")) puntos += 2;
      else if (estado.equals("ALERTA")) puntos += 1;
    }

    if (total == 0) return BigDecimal.ONE;
    return BigDecimal.valueOf(puntos)
        .divide(BigDecimal.valueOf(total * 2L), 2, RoundingMode.HALF_UP);
  }

  public static AnaliticaMuestraConsulta consultaTemporal(
      MuestraLeche muestra,
      EvaluacionCalidadService.EvaluacionMuestra evaluacion
  ) {
    Map<String, AnaliticaMuestraConsulta.ResultadoParametro> resultados = new LinkedHashMap<>();
    evaluacion.porParametro().forEach((clave, resultado) -> resultados.put(
        clave,
        new AnaliticaMuestraConsulta.ResultadoParametro(resultado.estado(), resultado.mensajes())
    ));

    return new AnaliticaMuestraConsulta(
        null,
        muestra.id(),
        muestra.proveedorId(),
        muestra.fechaMuestra().toInstant(),
        new AnaliticaMuestraConsulta.BaseValores(
            muestra.composicion().grasa(),
            muestra.composicion().proteina(),
            muestra.composicion().lactosa(),
            muestra.composicion().solidosTotales(),
            muestra.fisicoQuimico().densidad(),
            muestra.fisicoQuimico().acidezDornic(),
            muestra.fisicoQuimico().temperaturaC(),
            muestra.sng(),
            muestra.aguaPct()
        ),
        new AnaliticaMuestraConsulta.Evaluacion(resultados),
        null,
        flags(evaluacion),
        kpi(evaluacion)
    );
  }

  public static Map<String, Object> resumenDesdeMuestras(
      List<MuestraLeche> muestras,
      EvaluacionCalidadService evaluacionService
  ) {
    Promedio grasa = new Promedio();
    Promedio proteina = new Promedio();
    Promedio solidosTotales = new Promedio();
    Promedio sng = new Promedio();
    Promedio kpi = new Promedio();
    long aceptable = 0;
    long alerta = 0;
    long rechazar = 0;

    for (MuestraLeche muestra : muestras) {
      var evaluacion = evaluar(muestra, evaluacionService);
      grasa.agregar(muestra.composicion().grasa());
      proteina.agregar(muestra.composicion().proteina());
      solidosTotales.agregar(muestra.composicion().solidosTotales());
      sng.agregar(muestra.sng());
      kpi.agregar(kpi(evaluacion));

      switch (estadoGeneral(evaluacion)) {
        case "RECHAZAR" -> rechazar++;
        case "ALERTA" -> alerta++;
        default -> aceptable++;
      }
    }

    Map<String, Object> promedios = new LinkedHashMap<>();
    promedios.put("grasa", grasa.valor());
    promedios.put("proteina", proteina.valor());
    promedios.put("solidosTotales", solidosTotales.valor());
    promedios.put("sng", sng.valor());
    promedios.put("kpi", kpi.valor());

    Map<String, Object> distribucion = new LinkedHashMap<>();
    distribucion.put("ACEPTABLE", aceptable);
    distribucion.put("ALERTA", alerta);
    distribucion.put("RECHAZAR", rechazar);
    distribucion.put("totalEstados", (long) muestras.size());

    Map<String, Object> resumen = new LinkedHashMap<>();
    resumen.put("promedios", promedios);
    resumen.put("distribucionEstados", distribucion);
    return resumen;
  }

  private static final class Promedio {
    private BigDecimal suma = BigDecimal.ZERO;
    private long cantidad;

    void agregar(BigDecimal valor) {
      if (valor == null) return;
      suma = suma.add(valor);
      cantidad++;
    }

    BigDecimal valor() {
      if (cantidad == 0) return null;
      return suma.divide(BigDecimal.valueOf(cantidad), 4, RoundingMode.HALF_UP)
          .stripTrailingZeros();
    }
  }
}
