package saas.hotel.istoepousada.service;

import java.time.LocalDate;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import saas.hotel.istoepousada.dto.Relatorio;
import saas.hotel.istoepousada.dto.RelatorioExtratoResponse;
import saas.hotel.istoepousada.dto.enums.Valores;
import saas.hotel.istoepousada.repository.PessoaRepository;
import saas.hotel.istoepousada.repository.RelatorioRepository;

@Service
public class RelatorioService {

  private final RelatorioRepository relatorioRepository;
  private final PessoaRepository pessoaRepository;
  private final NotificacaoService notificacaoService;

  public RelatorioService(
      RelatorioRepository relatorioRepository,
      PessoaRepository pessoaRepository,
      NotificacaoService notificacaoService) {
    this.relatorioRepository = relatorioRepository;
    this.pessoaRepository = pessoaRepository;
    this.notificacaoService = notificacaoService;
  }

  public RelatorioExtratoResponse buscar(
      Long id,
      LocalDate dataInicio,
      LocalDate dataFim,
      Long funcionarioId,
      Long quartoId,
      Long tipoPagamentoId,
      Valores valores,
      Pageable pageable) {
    if (dataInicio == null && dataFim == null) {
      LocalDate hoje = LocalDate.now();
      dataInicio = hoje.minusDays(1);
      dataFim = hoje;
    }
    return relatorioRepository.buscar(
        id, dataInicio, dataFim, funcionarioId, quartoId, tipoPagamentoId, valores, pageable);
  }

  @Transactional
  public Relatorio criar(Relatorio.RelatorioRequest request) {
    validarRequest(request);
    Long funcionarioPessoaId = pessoaRepository.getFuncionarioPessoaIdFromRequest();
    var funcionario = pessoaRepository.findById(funcionarioPessoaId);
    Relatorio salvo = relatorioRepository.insert(request, funcionarioPessoaId);
    notificacaoService.criar(funcionario, "LANÇOU UM RELATÓRIO: " + request.relatorio());
    return salvo;
  }

  @Transactional
  public Relatorio atualizar(Long id, Relatorio.RelatorioRequest request) {
    if (id == null) throw new IllegalArgumentException("id é obrigatório.");
    validarRequest(request);
    Long funcionarioPessoaId = pessoaRepository.getFuncionarioPessoaIdFromRequest();
    var funcionario = pessoaRepository.findById(funcionarioPessoaId);
    Relatorio salvo = relatorioRepository.update(id, request, funcionarioPessoaId);
    notificacaoService.criar(
        funcionario, "ATUALIZOU O RELATÓRIO #" + id + ": " + request.relatorio());
    return salvo;
  }

  private void validarRequest(Relatorio.RelatorioRequest request) {
    if (request == null) throw new IllegalArgumentException("Request é obrigatória.");
    if (!StringUtils.hasText(request.relatorio()))
      throw new IllegalArgumentException("Descrição do relatório é obrigatória.");
    if (request.valor() == null) throw new IllegalArgumentException("valor é obrigatório.");
    if (request.tipoPagamentoId() == null)
      throw new IllegalArgumentException("tipoPagamentoId é obrigatório.");
  }
}
