package saas.hotel.istoepousada.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import saas.hotel.istoepousada.dto.Relatorio;
import saas.hotel.istoepousada.repository.RelatorioRepository;

import java.time.LocalDate;

@Service
public class RelatorioService {
  private final RelatorioRepository relatorioRepository;

  public RelatorioService(RelatorioRepository relatorioRepository) {
    this.relatorioRepository = relatorioRepository;
  }

  public Relatorio.Extrato buscar(
          Long id,
          LocalDate data_inicio,
          LocalDate data_fim,
          Long funcionario_id,
          Long quarto_id,
          Long tipo_pagamento_id,
          Relatorio.Registro registro,
          Boolean despesa_pessoal,
          int page,
          int size) {
    return relatorioRepository.buscar(id, data_inicio, data_fim, funcionario_id, quarto_id, tipo_pagamento_id, registro, despesa_pessoal, page, size);
  }

  @Transactional
  public Relatorio criar(Relatorio.Request request) {
    validarRequest(request);
    return relatorioRepository.insert(request);
  }

  @Transactional
  public Relatorio atualizar(Relatorio.Update relatorio) {
    if (relatorio.id() == null) throw new IllegalArgumentException("uuid é obrigatório.");
    return relatorioRepository.update(relatorio);
  }

  private void validarRequest(Relatorio.Request request) {
    if (request == null) throw new IllegalArgumentException("Request é obrigatória.");
    if (!StringUtils.hasText(request.relatorio()))
      throw new IllegalArgumentException("Descrição do relatório é obrigatória.");
  }
}
