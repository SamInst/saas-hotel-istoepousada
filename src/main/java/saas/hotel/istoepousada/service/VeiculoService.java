package saas.hotel.istoepousada.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.Veiculo;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;
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
    if (!pessoaRepository.existsById(pessoa_id)) {
      throw new NotFoundException("Pessoa não encontrada para o id: " + pessoa_id);
    }
    return veiculoRepository.findAllByPessoaId(pessoa_id);
  }

  @Transactional
  public Veiculo create(Veiculo.Request veiculo) {
    var placa = veiculoRepository.findByPlaca(veiculo.placa());
    if (placa != null) {
      throw new IllegalArgumentException("Veiculo ja cadastrado com a placa: " + veiculo.placa());
    }

    // create() já retorna o registro relido do banco.
    return veiculoRepository.create(veiculo);
  }

  @Transactional
  public Veiculo update(Veiculo.Update update) {
    // update() já retorna o registro relido do banco.
    return veiculoRepository.update(update);
  }

  @Transactional
  public void setVinculoAtivo(Veiculo.Vincular vinculo) {
    if (!pessoaRepository.existsById(vinculo.pessoa().id())) {
      throw new NotFoundException("Pessoa não encontrada para o id: " + vinculo.pessoa().id());
    }
    veiculoRepository.findById(vinculo.veiculo().id());
    veiculoRepository.setVinculo(vinculo);
  }
}
