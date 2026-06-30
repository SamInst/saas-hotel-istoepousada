package saas.hotel.istoepousada.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import saas.hotel.istoepousada.dto.ConfirmacaoPresenca;
import saas.hotel.istoepousada.repository.ConfirmacaoPresencaRepository;

@Service
public class ConfirmacaoPresencaService {

  private final ConfirmacaoPresencaRepository repository;

  public ConfirmacaoPresencaService(ConfirmacaoPresencaRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public ConfirmacaoPresenca confirmar(ConfirmacaoPresenca.Request request) {
    if (request == null || !StringUtils.hasText(request.nome())) {
      throw new IllegalArgumentException("Nome é obrigatório para confirmar presença.");
    }
    return repository.insert(request.nome().trim());
  }

  public List<ConfirmacaoPresenca> listar() {
    return repository.listar();
  }

  public long total() {
    return repository.total();
  }
}
