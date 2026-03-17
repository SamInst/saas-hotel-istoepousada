package saas.hotel.istoepousada.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import saas.hotel.istoepousada.dto.Relatorio;
import saas.hotel.istoepousada.repository.RelatorioRepository;

@Service
public class RelatorioService {
  private final RelatorioRepository relatorioRepository;

  public RelatorioService(RelatorioRepository relatorioRepository) {
    this.relatorioRepository = relatorioRepository;
  }

  public Relatorio.Extrato buscar(Relatorio.Buscar relatorio) {
    return relatorioRepository.buscar(relatorio);
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
