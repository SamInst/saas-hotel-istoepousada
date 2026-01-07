package saas.hotel.istoepousada.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.Veiculo;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;
import saas.hotel.istoepousada.repository.VeiculoRepository;

@Service
public class VeiculoService {
  private final VeiculoRepository veiculoRepository;

  public VeiculoService(VeiculoRepository veiculoRepository) {
    this.veiculoRepository = veiculoRepository;
  }

  @Transactional(readOnly = true)
  public Veiculo buscarPorId(Long id) {
    return veiculoRepository
        .findById(id)
        .orElseThrow(() -> new NotFoundException("Veículo não encontrado: id=" + id));
  }

  @Transactional(readOnly = true)
  public List<Veiculo> listarTodos() {
    return veiculoRepository.findAll();
  }

  @Transactional(readOnly = true)
  public List<Veiculo> listarPorPessoa(Long pessoaId) {
    return veiculoRepository.findAllByPessoaId(pessoaId);
  }

  @Transactional
  public Veiculo salvar(Long pessoa_id, Veiculo veiculo) {
    if (veiculo.id() != null) {
      veiculoRepository
          .findById(veiculo.id())
          .orElseThrow(() -> new NotFoundException("Veículo não encontrado: id=" + veiculo.id()));
    }
    return veiculoRepository.save(pessoa_id, veiculo);
  }

  @Transactional
  public void vincularPessoa(Long pessoaId, Long veiculoId) {
    veiculoRepository
        .findById(veiculoId)
        .orElseThrow(() -> new NotFoundException("Veículo não encontrado: id=" + veiculoId));

    veiculoRepository.vincularPessoa(pessoaId, veiculoId);
  }

  /**
   * Ativa/desativa o vínculo do veículo com a pessoa.
   *
   * <p>Observação: - por causa do UNIQUE(veiculo_id), se o veículo estiver em outra pessoa, o
   * vínculo será movido para a pessoa informada e marcado como ativo/inativo conforme parâmetro.
   */
  @Transactional
  public void setVinculoAtivo(Long pessoaId, Long veiculoId, boolean ativo) {
    veiculoRepository
        .findById(veiculoId)
        .orElseThrow(() -> new NotFoundException("Veículo não encontrado: id=" + veiculoId));

    veiculoRepository.setVinculoAtivo(pessoaId, veiculoId, ativo);
  }
}
