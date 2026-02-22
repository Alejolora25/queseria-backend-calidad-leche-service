package com.queseria.calidadleche.domain.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class EvaluacionCalidadService {

  public record ResultadoParametro(String estado, List<String> mensajes) {}
  public record EvaluacionMuestra(Map<String, ResultadoParametro> porParametro) {}

  // Rangos default globales (David)
  private static final BigDecimal G_MIN_OK   = bd("3.7");
  private static final BigDecimal G_MAX_OK   = bd("4.5");
  private static final BigDecimal G_MIN_WARN = bd("3.4");
  private static final BigDecimal P_MIN_OK   = bd("3.1");
  private static final BigDecimal P_MAX_OK   = bd("3.3");
  private static final BigDecimal P_MIN_WARN = bd("2.8");

  private static final BigDecimal DQ_MIN_OK  = bd("31");   // °Q
  private static final BigDecimal DQ_MAX_OK  = bd("33");
  private static final BigDecimal DQ_MIN_WARN= bd("29");
  private static final BigDecimal DQ_MAX_WARN= bd("30.8");

  private static final BigDecimal ST_MIN_OK  = bd("12.5");
  private static final BigDecimal ST_MAX_OK  = bd("14");
  private static final BigDecimal ST_MIN_WARN= bd("12.0");

  private static final BigDecimal SNG_MIN_OK = bd("8.5");
  private static final BigDecimal SNG_MAX_OK = bd("9.5");
  private static final BigDecimal SNG_MIN_WARN=bd("8.2");

  private static final BigDecimal DORNIC_MIN_OK = bd("13");
  private static final BigDecimal DORNIC_MAX_OK = bd("17");

  private static final BigDecimal CERO = BigDecimal.ZERO;

  public EvaluacionMuestra evaluar(
      BigDecimal grasa, BigDecimal proteina,
      BigDecimal densidad_gml, BigDecimal temperaturaC,
      BigDecimal solidosTotales, BigDecimal sngOpcional,
      BigDecimal acidezDornic, BigDecimal aguaPctOpcional
  ) {
    Map<String, ResultadoParametro> r = new LinkedHashMap<>();

    // °Q crudo y corregido (referencia 15–20 °C; si >20 sumo, si <15 resto; entre 15 y 20 no corrijo)
    BigDecimal dqCrudo = densidadAGradosQ(densidad_gml);
    BigDecimal dqCorr  = corregirDQPorTemperatura(dqCrudo, temperaturaC);

    // % ácido láctico
    BigDecimal acidoPct = (acidezDornic != null)
        ? acidezDornic.multiply(bd("0.1")).setScale(2, RoundingMode.HALF_UP)
        : null;

    // SNG calculado si viene nulo: SNG = ST - grasa
    BigDecimal sng = (sngOpcional != null) ? sngOpcional
                                           : (solidosTotales != null && grasa != null
                                              ? solidosTotales.subtract(grasa).max(CERO) : null);

    // 1) Grasa
    r.put("grasa", evaluarRango(grasa, G_MIN_OK, G_MAX_OK, G_MIN_WARN, null,
        List.of(
          "Puede ser leche descremada; revisar densidad y % de adición de agua",
          "Muestra mal tomada; agitar y repetir",
          "Revisar en finca: raza, alimentación; tomar muestra en sitio"
        ),
        List.of(
          "Valor alto: revisar % de agua y muestreo",
          "Revisar en finca: raza/alimentación; tomar muestra en sitio"
        )
    ));

    // 2) Proteína
    r.put("proteina", evaluarRango(proteina, P_MIN_OK, P_MAX_OK, P_MIN_WARN, null,
        List.of(
          "Puede ser leche descremada; revisar densidad y % de adición de agua",
          "Muestra mal tomada; agitar y repetir",
          "Revisar en finca: raza, alimentación; tomar muestra en sitio"
        ),
        List.of(
          "Valor alto: revisar % de agua y muestreo",
          "Revisar en finca: raza/alimentación; tomar muestra en sitio"
        )
    ));

    // 3) Densidad (°Q corregido)
    r.put("densidad_dq", evaluarDensidad(dqCorr));

    // 4) Sólidos Totales
    r.put("solidos_totales", evaluarST(solidosTotales));

    // 5) SNG
    r.put("sng", evaluarSNG(sng));

    // 6) Acidez (Dornic)
    r.put("acidez_dornic", evaluarDornic(acidezDornic));

    // 7) % Agua (si viene y es >0, rechazar; si no viene, aceptable con nota)
    r.put("agua_pct", evaluarAgua(aguaPctOpcional));

    // 8) Informativos adicionales
    if (acidoPct != null) {
      r.put("acido_lactico_pct", new ResultadoParametro("INFO",
          List.of("Ácido láctico estimado: " + acidoPct + "%")));
    }
    r.put("densidad_dq_crudo", new ResultadoParametro("INFO",
        List.of("°Q crudo: " + round2(dqCrudo))));
    r.put("densidad_dq_corregido", new ResultadoParametro("INFO",
        List.of("°Q corregido: " + round2(dqCorr))));

    return new EvaluacionMuestra(r);
  }

  private ResultadoParametro evaluarDensidad(BigDecimal dqCorr) {
    if (dqCorr == null) return new ResultadoParametro("SIN_DATO", List.of("Sin densidad"));
    if (dqCorr.compareTo(DQ_MIN_OK) >= 0 && dqCorr.compareTo(DQ_MAX_OK) <= 0) {
      return new ResultadoParametro("ACEPTABLE", List.of());
    }
    if (dqCorr.compareTo(DQ_MIN_WARN) >= 0 && dqCorr.compareTo(DQ_MAX_WARN) <= 0) {
      return new ResultadoParametro("ALERTA", List.of(
          "Revisar % de adición de agua",
          "Muestra mal tomada; agitar y repetir",
          "Revisar en finca: raza/alimentación; tomar muestra en sitio"
      ));
    }
    if (dqCorr.compareTo(DQ_MIN_WARN) < 0) {
      return new ResultadoParametro("RECHAZAR", List.of(
          "Densidad baja: posible adulteración con agua",
          "Revisar % de adición de agua y muestreo"
      ));
    }
    // > 33
    return new ResultadoParametro("ALERTA", List.of(
        "Densidad alta: revisar % de grasa (posible descremado)",
        "Muestra mal tomada; agitar y repetir"
    ));
  }

  private ResultadoParametro evaluarST(BigDecimal st) {
    if (st == null) return new ResultadoParametro("SIN_DATO", List.of("Sin Sólidos Totales"));
    if (between(st, ST_MIN_OK, ST_MAX_OK)) return new ResultadoParametro("ACEPTABLE", List.of());
    if (st.compareTo(ST_MIN_WARN) >= 0 && st.compareTo(ST_MIN_OK) < 0) {
      return new ResultadoParametro("ALERTA", List.of(
          "Revisar % de adición de agua",
          "Muestra mal tomada; agitar y repetir",
          "Revisar en finca: raza/alimentación; tomar muestra en sitio"
      ));
    }
    if (st.compareTo(ST_MIN_WARN) < 0) {
      return new ResultadoParametro("RECHAZAR", List.of(
          "Sólidos totales bajos: posible adulteración",
          "Revisar % de adición de agua"
      ));
    }
    // > 14
    return new ResultadoParametro("ALERTA", List.of(
        "Valor alto: leche de muy buena calidad (proteínas, grasa, lactosa)",
        "Verificar muestreo correcto"
    ));
  }

  private ResultadoParametro evaluarSNG(BigDecimal sng) {
    if (sng == null) return new ResultadoParametro("SIN_DATO", List.of("Sin SNG (se puede calcular como ST - grasa)"));
    if (between(sng, SNG_MIN_OK, SNG_MAX_OK)) return new ResultadoParametro("ACEPTABLE", List.of());
    if (sng.compareTo(SNG_MIN_WARN) >= 0 && sng.compareTo(SNG_MIN_OK) < 0) {
      return new ResultadoParametro("ALERTA", List.of(
          "Revisar % de adición de agua",
          "Muestra mal tomada; agitar y repetir",
          "Revisar en finca"
      ));
    }
    if (sng.compareTo(SNG_MIN_WARN) < 0) {
      return new ResultadoParametro("RECHAZAR", List.of(
          "SNG bajo: posible adulteración con agua",
          "Revisar muestreo y densidad"
      ));
    }
    // > 9.5
    return new ResultadoParametro("ALERTA", List.of(
        "Valor alto: buena calidad (lactosa/minerales)",
        "Verificar muestreo"
    ));
  }

  private ResultadoParametro evaluarDornic(BigDecimal d) {
    if (d == null) return new ResultadoParametro("SIN_DATO", List.of("Sin acidez (°D)"));
    if (between(d, DORNIC_MIN_OK, DORNIC_MAX_OK)) return new ResultadoParametro("ACEPTABLE", List.of());
    if (d.compareTo(DORNIC_MAX_OK) > 0) {
      return new ResultadoParametro("RECHAZAR", List.of("Leche ácida (>17 °D)"));
    }
    // < 13
    return new ResultadoParametro("ALERTA", List.of(
        "Acidez baja: posible adición de agua o neutralización",
        "Realizar prueba de neutralizantes y revisar adición de agua"
    ));
  }

  private ResultadoParametro evaluarAgua(BigDecimal agua) {
    if (agua == null) {
      return new ResultadoParametro("ACEPTABLE", List.of("Sin dato de equipo (agua%)"));
    }
    if (agua.compareTo(CERO) > 0) {
      return new ResultadoParametro("RECHAZAR", List.of("Adición de agua > 0%"));
        }
        return new ResultadoParametro("ACEPTABLE", List.of());
    }

    private ResultadoParametro evaluarRango(
        BigDecimal valor,
        BigDecimal minOk, BigDecimal maxOk,
        BigDecimal minWarnNullable, BigDecimal maxWarnNullable,
        List<String> mensajesPorDebajo,
        List<String> mensajesPorEncima
    ) {
    if (valor == null) return new ResultadoParametro("SIN_DATO", List.of("Sin dato"));

    boolean enOk = between(valor, minOk, maxOk);
    if (enOk) return new ResultadoParametro("ACEPTABLE", List.of());

    // ALERTA por debajo
    if (minWarnNullable != null && valor.compareTo(minWarnNullable) >= 0 && valor.compareTo(minOk) < 0) {
        return new ResultadoParametro("ALERTA", mensajesPorDebajo);
    }
    // ALERTA por encima
    if (maxWarnNullable != null && valor.compareTo(maxOk) > 0 && valor.compareTo(maxWarnNullable) <= 0) {
        return new ResultadoParametro("ALERTA", mensajesPorEncima);
    }

    // RECHAZO por debajo del minWarn
    if (minWarnNullable != null && valor.compareTo(minWarnNullable) < 0) {
        var msgs = new java.util.ArrayList<String>(mensajesPorDebajo);
        msgs.add(0, "Por debajo del mínimo permitido");
        return new ResultadoParametro("RECHAZAR", msgs);
    }

    // RECHAZO por encima del maxWarn (si lo definimos), o ALERTA fuerte si no lo definimos
    if (maxWarnNullable != null && valor.compareTo(maxWarnNullable) > 0) {
        var msgs = new java.util.ArrayList<String>(mensajesPorEncima);
        msgs.add(0, "Por encima del máximo permitido");
        return new ResultadoParametro("RECHAZAR", msgs);
    }

    // Si no hay warn superior definido, todo > maxOk lo tratamos como ALERTA
    return new ResultadoParametro("ALERTA", mensajesPorEncima);
    }

  // Utilidades
  private static BigDecimal densidadAGradosQ(BigDecimal densidad) {
    if (densidad == null) return null;
    // °Q = (g/mL - 1) * 1000
    return densidad.subtract(bd("1.0")).multiply(bd("1000")).setScale(2, RoundingMode.HALF_UP);
  }

  private static BigDecimal corregirDQPorTemperatura(BigDecimal dq, BigDecimal t) {
    if (dq == null || t == null) return dq;
    // Sin corrección entre 15–20 °C
    if (t.compareTo(bd("15")) >= 0 && t.compareTo(bd("20")) <= 0) return dq;
    if (t.compareTo(bd("20")) > 0) {
      // por encima, sumar 0.2 por °C
      BigDecimal delta = t.subtract(bd("20")).multiply(bd("0.2"));
      return dq.add(delta).setScale(2, RoundingMode.HALF_UP);
    } else {
      // por debajo de 15, restar 0.2 por °C
      BigDecimal delta = bd("15").subtract(t).multiply(bd("0.2"));
      return dq.subtract(delta).setScale(2, RoundingMode.HALF_UP);
    }
  }

  private static boolean between(BigDecimal v, BigDecimal min, BigDecimal max) {
    return v.compareTo(min) >= 0 && v.compareTo(max) <= 0;
  }
  private static BigDecimal bd(String s){ return new BigDecimal(s); }
  private static String round2(BigDecimal v){ return v==null? "": v.setScale(2, RoundingMode.HALF_UP).toPlainString(); }
}