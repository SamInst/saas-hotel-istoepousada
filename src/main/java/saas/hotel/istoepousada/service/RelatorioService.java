package saas.hotel.istoepousada.service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import saas.hotel.istoepousada.dto.Pagamento;
import saas.hotel.istoepousada.dto.Quarto;
import saas.hotel.istoepousada.dto.Relatorio;
import saas.hotel.istoepousada.dto.RelatorioHistorico;
import saas.hotel.istoepousada.repository.PagamentoRepository;
import saas.hotel.istoepousada.repository.RelatorioHistoricoRepository;
import saas.hotel.istoepousada.repository.RelatorioRepository;

@Service
public class RelatorioService {
  private static final Logger log = LoggerFactory.getLogger(RelatorioService.class);
  private final RelatorioRepository relatorioRepository;
  private final ArquivoService arquivoService;
  private final PagamentoRepository pagamentoRepository;
  private final PessoaService pessoaService;
  private final RelatorioHistoricoRepository relatorioHistoricoRepository;

  public RelatorioService(
      RelatorioRepository relatorioRepository,
      ArquivoService arquivoService,
      PagamentoRepository pagamentoRepository,
      PessoaService pessoaService,
      RelatorioHistoricoRepository relatorioHistoricoRepository) {
    this.relatorioRepository = relatorioRepository;
    this.arquivoService = arquivoService;
    this.pagamentoRepository = pagamentoRepository;
    this.pessoaService = pessoaService;
    this.relatorioHistoricoRepository = relatorioHistoricoRepository;
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
    var pagamento = pagamentoRepository.create(relatorio.pagamento(), getFuncionarioId());
    var novoRelatorio = relatorioRepository.insert(relatorio, pagamento, getFuncionarioId());
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

    // Snapshot do estado ANTES de qualquer alteração (o pagamento é atualizado
    // logo abaixo, então precisamos capturar o "antes" aqui no topo).
    Relatorio antigo = relatorioRepository.buscarPorId(relatorio.id());

    String novoPath;

    if (arquivo != null) {
      var pathAntigo = arquivoService.buscaPathArquivoByPagamentoUUID(relatorio.pagamento().uuid());
      arquivoService.deletarArquivo(pathAntigo);

      novoPath = arquivoService.salvarComprovante(arquivo);
      arquivoService.setPath(novoPath, relatorio.pagamento().uuid());
    }
    var pagamentoUpdate =
        new Pagamento.Update(
            relatorio.pagamento().uuid(),
            new Pagamento.TipoPagamento.Id(relatorio.pagamento().tipo_pagamento().id()),
            relatorio.pagamento().nome_pagador(),
            relatorio.pagamento().descricao(),
            relatorio.pagamento().valor(),
            relatorio.pagamento().arquivo());
    if (relatorio.pagamento().valor() != null) {
      pagamentoRepository.update(pagamentoUpdate, getFuncionarioId());
    }
    Relatorio novo =
        relatorioRepository.update(
            new Relatorio.Update(
                relatorio.id(),
                relatorio.descricao(),
                pagamentoUpdate,
                relatorio.quarto() == null ? null : new Quarto.Id(relatorio.quarto().id()),
                relatorio.despesa_pessoal()),
            getFuncionarioId());

    // Registra quem alterou e o que mudou (só grava se houve alteração).
    List<RelatorioHistorico.Alteracao> alteracoes = diff(antigo, novo);
    boolean comprovanteTrocado = arquivo != null;
    if (comprovanteTrocado) {
      alteracoes.add(new RelatorioHistorico.Alteracao("Comprovante", null, "Arquivo atualizado"));
    }
    relatorioHistoricoRepository.registrar(
        relatorio.id(), getFuncionarioId(), "UPDATE", alteracoes);

    return novo;
  }

  public List<RelatorioHistorico> buscarHistorico(Long relatorioId) {
    return relatorioHistoricoRepository.buscarPorRelatorio(relatorioId);
  }

  /** Compara o relatório antes/depois e devolve a lista de campos alterados. */
  private List<RelatorioHistorico.Alteracao> diff(Relatorio antigo, Relatorio novo) {
    List<RelatorioHistorico.Alteracao> alteracoes = new ArrayList<>();
    if (antigo == null || novo == null) return alteracoes;

    addSeMudou(alteracoes, "Descrição", antigo.relatorio(), novo.relatorio());
    addSeMudou(alteracoes, "Valor", fmtValor(antigo.valor()), fmtValor(novo.valor()));
    addSeMudou(
        alteracoes,
        "Despesa interna",
        fmtBool(antigo.despesa_pessoal()),
        fmtBool(novo.despesa_pessoal()));
    addSeMudou(alteracoes, "Quarto", fmtQuarto(antigo.quarto()), fmtQuarto(novo.quarto()));

    Pagamento pAntigo = antigo.pagamento();
    Pagamento pNovo = novo.pagamento();
    addSeMudou(alteracoes, "Pagador", nomePagador(pAntigo), nomePagador(pNovo));
    addSeMudou(alteracoes, "Forma de pagamento", tipoPagamento(pAntigo), tipoPagamento(pNovo));
    addSeMudou(alteracoes, "Descrição do pagamento", descPagamento(pAntigo), descPagamento(pNovo));

    return alteracoes;
  }

  private void addSeMudou(
      List<RelatorioHistorico.Alteracao> lista, String campo, String de, String para) {
    if (!Objects.equals(de, para)) {
      lista.add(new RelatorioHistorico.Alteracao(campo, de, para));
    }
  }

  private String fmtValor(Float v) {
    return v == null ? "—" : String.format("R$ %.2f", v);
  }

  private String fmtBool(Boolean b) {
    return Boolean.TRUE.equals(b) ? "Sim" : "Não";
  }

  private String fmtQuarto(Quarto.Descricao q) {
    return q == null ? "Nenhum" : "Quarto " + q.id() + " - " + q.descricao();
  }

  private String nomePagador(Pagamento p) {
    return p == null ? null : p.nome_pagador();
  }

  private String tipoPagamento(Pagamento p) {
    return p == null || p.tipo_pagamento() == null ? null : p.tipo_pagamento().descricao();
  }

  private String descPagamento(Pagamento p) {
    return p == null ? null : p.descricao();
  }

  private Long getFuncionarioId() {
    return pessoaService.getFuncionarioIdFromRequest();
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
