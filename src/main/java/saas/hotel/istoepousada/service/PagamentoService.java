package saas.hotel.istoepousada.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.Pagamento;
import saas.hotel.istoepousada.repository.PagamentoRepository;

@Service
public class PagamentoService {
  private final PagamentoRepository pagamentoRepository;

  public PagamentoService(PagamentoRepository pagamentoRepository) {
    this.pagamentoRepository = pagamentoRepository;
  }

  @Transactional
  public Pagamento criar(Pagamento.Request req) {
    return pagamentoRepository.create(req);
  }

  @Transactional(readOnly = true)
  public Pagamento buscar(UUID id) {
    return pagamentoRepository.findById(id);
  }

  @Transactional(readOnly = true)
  public List<Pagamento> listar() {
    return pagamentoRepository.findAll();
  }

  @Transactional
  public Pagamento atualizar(Pagamento.Update pagamento) {
    return pagamentoRepository.update(pagamento);
  }

  @Transactional
  public void cancelarPagamento(UUID id) {
    pagamentoRepository.cancelarPagamento(id);
  }
}
