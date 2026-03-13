package saas.hotel.istoepousada.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.Pessoa;
import saas.hotel.istoepousada.dto.Veiculo;
import saas.hotel.istoepousada.repository.PessoaRepository;
import saas.hotel.istoepousada.repository.VeiculoRepository;

@Service
public class VeiculoService {
  private final VeiculoRepository veiculo_repository;
  private final PessoaRepository pessoa_repository;

  public VeiculoService(VeiculoRepository veiculo_repository, PessoaRepository pessoa_repository) {
    this.veiculo_repository = veiculo_repository;
    this.pessoa_repository = pessoa_repository;
  }

  @Transactional(readOnly = true)
  public Veiculo findById(Long id) {
    return veiculo_repository.findById(id);
  }

  @Transactional(readOnly = true)
  public List<Veiculo> findAll() {
    return veiculo_repository.findAll();
  }

  @Transactional(readOnly = true)
  public List<Veiculo> findAllByPessoaId(Long pessoa_id) {
    pessoa_repository.findById(pessoa_id);
    return veiculo_repository.findAllByPessoaId(pessoa_id);
  }

  @Transactional
  public Veiculo create(Veiculo.Request request, Pessoa.Id pessoa) {
    pessoa_repository.findById(pessoa.id());

    Veiculo veiculo_criado = veiculo_repository.create(request);

    veiculo_repository.vincularPessoa(
        new Veiculo.Vincular(new Veiculo.Id(veiculo_criado.id()), pessoa, true));

    return veiculo_repository.findById(veiculo_criado.id());
  }

  @Transactional
  public Veiculo update(Veiculo.Update update) {
    pessoa_repository.findById(update.pessoa().id());
    veiculo_repository.findById(update.id());

    Veiculo veiculo_atualizado = veiculo_repository.update(update);

    veiculo_repository.vincularPessoa(
        new Veiculo.Vincular(new Veiculo.Id(veiculo_atualizado.id()), update.pessoa(), true));

    return veiculo_repository.findById(veiculo_atualizado.id());
  }

  @Transactional
  public void vincularPessoa(Veiculo.Vincular vinculo) {
    pessoa_repository.findById(vinculo.pessoa().id());
    veiculo_repository.findById(vinculo.veiculo().id());
    veiculo_repository.vincularPessoa(vinculo);
  }

  @Transactional
  public void setVinculoAtivo(Veiculo.Vincular vinculo) {
    pessoa_repository.findById(vinculo.pessoa().id());
    veiculo_repository.findById(vinculo.veiculo().id());
    veiculo_repository.setVinculoAtivo(vinculo);
  }

  @Transactional
  public void deleteById(Long id) {
    veiculo_repository.findById(id);
    veiculo_repository.deleteById(id);
  }
}
