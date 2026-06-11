package com.queseria.calidadleche.application.usecase;

import com.queseria.calidadleche.application.model.AnaliticaMuestraConsulta;
import com.queseria.calidadleche.application.port.AnaliticaConsultaRepository;
import reactor.core.publisher.Mono;

public class BuscarAnaliticaPorMuestraUseCase {

  private final AnaliticaConsultaRepository analiticaConsultaRepository;

  public BuscarAnaliticaPorMuestraUseCase(AnaliticaConsultaRepository analiticaConsultaRepository) {
    this.analiticaConsultaRepository = analiticaConsultaRepository;
  }

  public Mono<AnaliticaMuestraConsulta> execute(Long sampleId) {
    return analiticaConsultaRepository.buscarUltimaPorMuestra(sampleId);
  }
}
