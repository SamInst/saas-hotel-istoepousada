package saas.hotel.istoepousada.service;

import java.io.IOException;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import saas.hotel.istoepousada.dto.Pagamento;
import saas.hotel.istoepousada.dto.Quarto;
import saas.hotel.istoepousada.dto.Relatorio;
import saas.hotel.istoepousada.repository.PagamentoRepository;
import saas.hotel.istoepousada.repository.RelatorioRepository;

@Service
public class RelatorioService {
  private static final Logger log = LoggerFactory.getLogger(RelatorioService.class);
  private final RelatorioRepository relatorioRepository;
  private final ArquivoService arquivoService;
  private final PagamentoRepository pagamentoRepository;

  public RelatorioService(
      RelatorioRepository relatorioRepository,
      ArquivoService arquivoService,
      PagamentoRepository pagamentoRepository) {
    this.relatorioRepository = relatorioRepository;
    this.arquivoService = arquivoService;
    this.pagamentoRepository = pagamentoRepository;
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
    return relatorioRepository.buscar(
        id,
        data_inicio,
        data_fim,
        funcionario_id,
        quarto_id,
        tipo_pagamento_id,
        registro,
        despesa_pessoal,
        page,
        size);
  }

  @Transactional
  public Relatorio criar(Relatorio.Request relatorio, MultipartFile arquivo) throws IOException {
    validarRequest(relatorio);
    var novoRelatorio = relatorioRepository.insert(relatorio);
    log.info("Relatório criado com sucesso: {}", novoRelatorio.id());

    if (arquivo != null) {
      String path = arquivoService.salvarComprovante(arquivo);
      arquivoService.setPath(path, novoRelatorio.pagamento().uuid());
      log.info(
          "Arquivo de comprovante salvo com sucesso para o pagamento {}",
          novoRelatorio.pagamento().uuid());
    }
    return novoRelatorio;
  }

  @Transactional
  public Relatorio atualizar(Relatorio.Update relatorio, MultipartFile arquivo) throws IOException {
    validarUpdate(relatorio);
    String novoPath;

    if (arquivo != null) {
      var pathAntigo = arquivoService.buscaPathArquivoByPagamentoUUID(relatorio.pagamento().uuid());
      arquivoService.deletarArquivo(pathAntigo);

      novoPath = arquivoService.salvarComprovante(arquivo);
      arquivoService.setPath(novoPath, relatorio.pagamento().uuid());
    }
    return relatorioRepository.update(
        new Relatorio.Update(
            relatorio.id(),
            relatorio.descricao(),
            new Pagamento.Update(
                relatorio.pagamento().uuid(),
                new Pagamento.TipoPagamento.Id(relatorio.pagamento().tipo_pagamento().id()),
                relatorio.pagamento().nome_pagador(),
                relatorio.pagamento().descricao(),
                relatorio.pagamento().valor(),
                relatorio.pagamento().desconto() == null
                    ? null
                    : new Pagamento.Desconto.Update(
                        relatorio.pagamento().desconto().uuid(),
                        relatorio.pagamento().desconto().porcentagem(),
                        relatorio.pagamento().desconto().valor()),
                relatorio.pagamento().arquivo()),
            relatorio.quarto() == null ? null : new Quarto.Id(relatorio.quarto().id()),
            relatorio.despesa_pessoal()));
  }

  private void validarRequest(Relatorio.Request request) {
    if (request == null) throw new IllegalArgumentException("Request é obrigatória.");
    if (!StringUtils.hasText(request.relatorio()))
      throw new IllegalArgumentException("Descrição do relatório é obrigatória.");
    if (request.pagamento() == null)
      throw new IllegalArgumentException("E Necessário adicionar um pagamento");
  }

  private void validarUpdate(Relatorio.Update relatorio) {
    if (relatorio == null) throw new IllegalArgumentException("Request é obrigatória.");

    if (relatorio.id() == null) throw new IllegalArgumentException("uuid é obrigatório.");

    if (!StringUtils.hasText(relatorio.descricao()))
      throw new IllegalArgumentException("Descrição do relatório é obrigatória.");
    if (relatorio.pagamento() == null)
      throw new IllegalArgumentException("E Necessário adicionar um pagamento");
  }
}
