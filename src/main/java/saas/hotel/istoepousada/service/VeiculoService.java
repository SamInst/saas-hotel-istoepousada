package saas.hotel.istoepousada.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.Veiculo;
import saas.hotel.istoepousada.repository.PessoaRepository;
import saas.hotel.istoepousada.repository.VeiculoRepository;

@Service
public class VeiculoService {
  private final VeiculoRepository veiculoRepository;
  private final PessoaRepository pessoaRepository;

  public VeiculoService(VeiculoRepository veiculoRepository, PessoaRepository pessoaRepository) {
    this.veiculoRepository = veiculoRepository;
    this.pessoaRepository = pessoaRepository;
  }

  @Transactional(readOnly = true)
  public Veiculo findById(Long id) {
    return veiculoRepository.findById(id);
  }

  @Transactional(readOnly = true)
  public List<Veiculo> findAllByPessoaId(Long pessoa_id) {
    pessoaRepository.findById(pessoa_id);
    return veiculoRepository.findAllByPessoaId(pessoa_id);
  }

  @Transactional
  public Veiculo create(Veiculo.Request veiculo) {
    var placa = veiculoRepository.findByPlaca(veiculo.placa());
    if (placa != null) {
      throw new IllegalArgumentException("Veiculo ja cadastrado com a placa: " + veiculo.placa());
    }

    Veiculo veiculo_criado = veiculoRepository.create(veiculo);
    return veiculoRepository.findById(veiculo_criado.id());
  }

  @Transactional
  public Veiculo update(Veiculo.Update update) {
    veiculoRepository.findById(update.id());
    Veiculo veiculo_atualizado = veiculoRepository.update(update);
    return veiculoRepository.findById(veiculo_atualizado.id());
  }

  @Transactional
  public void setVinculoAtivo(Veiculo.Vincular vinculo) {
    pessoaRepository.findById(vinculo.pessoa().id());
    veiculoRepository.findById(vinculo.veiculo().id());
    veiculoRepository.setVinculo(vinculo);
  }
}
