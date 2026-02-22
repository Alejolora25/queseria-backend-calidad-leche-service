package com.queseria.calidadleche.infrastructure.persistence.mongo.repo;

import com.queseria.calidadleche.infrastructure.persistence.mongo.doc.AnaliticaMuestraDoc;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;

public interface AnaliticaMuestraMongoRepository
    extends ReactiveMongoRepository<AnaliticaMuestraDoc, String> {

  Mono<AnaliticaMuestraDoc> findFirstBySampleIdOrderByTimestampDesc(Long sampleId);

  Flux<AnaliticaMuestraDoc> findByProveedorIdAndTimestampBetween(
      Long proveedorId, Instant desde, Instant hasta
  );

  Flux<AnaliticaMuestraDoc> findBySampleId(Long sampleId);
}
