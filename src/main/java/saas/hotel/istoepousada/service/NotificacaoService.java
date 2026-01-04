package saas.hotel.istoepousada.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.Notificacao;
import saas.hotel.istoepousada.repository.NotificacaoRepository;

@Service
public class NotificacaoService {

  private final NotificacaoRepository notificacaoRepository;

  public NotificacaoService(NotificacaoRepository notificacaoRepository) {
    this.notificacaoRepository = notificacaoRepository;
  }

  @Transactional(readOnly = true)
  public List<Notificacao> listarUltimas20(Integer quantidade) {
    return notificacaoRepository.listarPorQuantidade(quantidade);
  }

  @Transactional(readOnly = true)
  public List<Notificacao> listarUltimas20PorPessoa(Long pessoaId, Integer quantidade) {
    return notificacaoRepository.listarPorPessoa(pessoaId, quantidade);
  }

  @Transactional
  public Notificacao criar(Long fkPessoa, String nome, String descricao) {
    return notificacaoRepository.criar(fkPessoa, nome, descricao);
  }
}
