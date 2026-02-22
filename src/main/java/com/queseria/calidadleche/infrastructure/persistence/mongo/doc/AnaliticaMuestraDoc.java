package com.queseria.calidadleche.infrastructure.persistence.mongo.doc;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;


import java.math.BigDecimal;
import java.time.Instant;

import java.util.List;
import java.util.Map;

@Data @Builder @AllArgsConstructor @NoArgsConstructor
@Document("analiticas_muestra")
@CompoundIndexes({
  // Evita duplicados: una analítica por muestra (Postgres.id)
  @CompoundIndex(name = "idx_sampleId_unique", def = "{ 'sampleId': 1 }", unique = true),
  // Consultas típicas: por proveedor y recientes primero
  @CompoundIndex(name = "idx_proveedor_ts", def = "{ 'proveedorId': 1, 'timestamp': -1 }")
})
public class AnaliticaMuestraDoc {

  @Id private String id;

  @Indexed // útil si consultas directo por sampleId sin usar el compuesto
  private Long sampleId;
  @Indexed // también ayuda a filtros simples por proveedor
  private Long proveedorId;
  @Indexed // si haces rangos por fecha sin el compuesto
  private Instant timestamp;

  // ---- Bloque base tal como se midió/registró ----
  private BaseValores base;

  // ---- Evaluación calculada ----
  private EvaluacionDoc evaluacion;

  // ---- Integridad y resumen ----
  private String hashBase;        // sha256 del JSON canónico de 'base'
  private List<String> flags;     // p.ej. ["RECHAZAR", "ALERTA"]
  @Field(targetType = FieldType.DECIMAL128)
  private BigDecimal kpiCalidad;  // opcional

  // -------- Tipos anidados --------
  @Data @Builder @AllArgsConstructor @NoArgsConstructor
  public static class BaseValores {
    @Field(targetType = FieldType.DECIMAL128) private BigDecimal grasa;
    @Field(targetType = FieldType.DECIMAL128) private BigDecimal proteina;
    @Field(targetType = FieldType.DECIMAL128) private BigDecimal lactosa;
    @Field(targetType = FieldType.DECIMAL128) private BigDecimal solidosTotales;
    @Field(targetType = FieldType.DECIMAL128) private BigDecimal densidad;
    @Field(targetType = FieldType.DECIMAL128) private BigDecimal acidezDornic;
    @Field(targetType = FieldType.DECIMAL128) private BigDecimal temperaturaC;
    @Field(targetType = FieldType.DECIMAL128) private BigDecimal sng;
    @Field(targetType = FieldType.DECIMAL128) private BigDecimal aguaPct;
  }

  @Data @Builder @AllArgsConstructor @NoArgsConstructor
  public static class EvaluacionDoc {
    // clave = nombre del parámetro (grasa, proteina, ...),
    // valor = objeto con estado+mensajes
    private Map<String, ResultadoParametroDoc> porParametro;
  }

  @Data @Builder @AllArgsConstructor @NoArgsConstructor
  public static class ResultadoParametroDoc {
    private String estado;            // ACEPTABLE | ALERTA | RECHAZAR | INFO | SIN_DATO
    private List<String> mensajes;
  }
}
