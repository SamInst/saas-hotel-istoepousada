package saas.hotel.istoepousada.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.Pagamento;
import saas.hotel.istoepousada.repository.PagamentoRepository;

@Service
public class PagamentoService {
  private final PagamentoRepository pagamentoRepository;
  private final PessoaService pessoaService;

  public PagamentoService(PagamentoRepository pagamentoRepository, PessoaService pessoaService) {
    this.pagamentoRepository = pagamentoRepository;
    this.pessoaService = pessoaService;
  }

  @Transactional
  public Pagamento criar(Pagamento.Request req) {
    return pagamentoRepository.create(req, getFuncionarioId());
  }

  @Transactional(readOnly = true)
  public Pagamento buscar(UUID id) {
    return pagamentoRepository.findById(id);
  }

  @Transactional(readOnly = true)
  public List<Pagamento> buscarPorHospedagemId(Long id) {
    return pagamentoRepository.findByHospedagemId(id);
  }

  @Transactional(readOnly = true)
  public Map<Long, List<Pagamento>> buscarPorHospedagemIds(List<Long> ids) {
    return pagamentoRepository.findByHospedagemIds(ids);
  }

  @Transactional(readOnly = true)
  public List<Pagamento> listar() {
    return pagamentoRepository.findAll();
  }

  @Transactional
  public Pagamento atualizar(Pagamento.Update pagamento) {
    return pagamentoRepository.update(pagamento, getFuncionarioId());
  }

  @Transactional
  public void cancelarPagamento(UUID id, String motivo) {
    pagamentoRepository.cancelarPagamento(id, motivo, getFuncionarioId());
  }

  private Long getFuncionarioId() {
    return pessoaService.getFuncionarioIdFromRequest();
  }
}
