package saas.hotel.istoepousada.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import saas.hotel.istoepousada.dto.Empresa;
import saas.hotel.istoepousada.dto.Pessoa;
import saas.hotel.istoepousada.dto.Veiculo;
import saas.hotel.istoepousada.repository.PessoaRepository;
import saas.hotel.istoepousada.repository.VeiculoRepository;

@Service
public class PessoaService {

  private static final Logger log = LoggerFactory.getLogger(PessoaService.class);

  private final PessoaRepository pessoaRepository;
  private final VeiculoRepository veiculoRepository;
  private final EmpresaService empresaService;

  public PessoaService(
      PessoaRepository pessoaRepository,
      VeiculoRepository veiculoRepository,
      EmpresaService empresaService) {
    this.pessoaRepository = pessoaRepository;
    this.veiculoRepository = veiculoRepository;
    this.empresaService = empresaService;
  }

  public Page<Pessoa> buscar(
      Long id, String termo, String placaVeiculo, Pessoa.Status status, Pageable pageable) {
    String termoNormalizado = StringUtils.hasText(termo) ? termo.trim() : null;
    String placaNormalizada = StringUtils.hasText(placaVeiculo) ? placaVeiculo.trim() : null;
    return pessoaRepository.buscar(id, termoNormalizado, placaNormalizada, status, pageable);
  }

  public Pessoa findById(Long id) {
    if (id == null) {
      throw new IllegalArgumentException("Id é obrigatório.");
    }
    return pessoaRepository.findById(id);
  }

  @Transactional
  public Pessoa atualizarPessoa(Pessoa.Update pessoa) {
    return pessoaRepository.update(pessoa);
  }

  @Transactional
  public List<Pessoa> salvarListaPessoas(Pessoa.BatchRequest request) {
    if (request.pessoas() == null || request.pessoas().isEmpty()) {
      throw new IllegalArgumentException("Lista de pessoas é obrigatória.");
    }

    Pessoa.Request titularReq = request.pessoas().getFirst();
    Pessoa titularSalvo = salvarInternoSemVinculoEmpresa(titularReq, null);

    List<Pessoa> acompanhantesSalvos = new ArrayList<>();

    for (Pessoa.Request pessoa : request.pessoas()) {
      if (pessoa == titularReq) {
        continue;
      }

      Pessoa acompanhanteSalvo = salvarInternoSemVinculoEmpresa(pessoa, titularSalvo.id());
      acompanhantesSalvos.add(acompanhanteSalvo);
    }

    if (request.empresas() != null && !request.empresas().isEmpty()) {
      List<Pessoa> todos = new ArrayList<>();
      todos.add(titularSalvo);
      todos.addAll(acompanhantesSalvos);

      for (Empresa.Id empresa :
          request.empresas().stream().filter(Objects::nonNull).distinct().toList()) {
        for (Pessoa pessoa : todos) {
          empresaService.vincularPessoa(
              new Empresa.Vincular(new Empresa.Id(empresa.id()), new Pessoa.Id(pessoa.id()), true));
        }
      }
    }

    Pessoa titularComAcompanhantes =
        new Pessoa(
            titularSalvo.id(),
            titularSalvo.data_hora_registro(),
            titularSalvo.data_nascimento(),
            titularSalvo.nome(),
            titularSalvo.cpf(),
            titularSalvo.rg(),
            titularSalvo.email(),
            titularSalvo.telefone(),
            titularSalvo.pais(),
            titularSalvo.estado(),
            titularSalvo.municipio(),
            titularSalvo.endereco(),
            titularSalvo.complemento(),
            titularSalvo.vezes_hospedado(),
            titularSalvo.cep(),
            titularSalvo.idade(),
            titularSalvo.bairro(),
            titularSalvo.sexo(),
            titularSalvo.numero(),
            titularSalvo.status(),
            titularSalvo.empresas_vinculadas(),
            titularSalvo.veiculos_vinculados(),
            titularSalvo.funcionario(),
            titularSalvo.titular(),
            acompanhantesSalvos);

    List<Pessoa> retorno = new ArrayList<>();
    retorno.add(titularComAcompanhantes);
    retorno.addAll(acompanhantesSalvos);
    return retorno;
  }

  private Pessoa salvarInternoSemVinculoEmpresa(Pessoa.Request pessoa, Long titular_id) {
    validarPessoa(pessoa);

    Pessoa salva = pessoaRepository.insert(pessoa);
    pessoaRepository.vincularTitular(salva.id(), titular_id, true);
    return salvarOuAtualizarVeiculos(salva, pessoa.veiculos());
  }

  private Pessoa sincronizarEmpresas(Pessoa salva, List<Empresa> empresas) {
    if (empresas == null || empresas.isEmpty()) {
      return salva;
    }

    List<Long> idsEmpresas =
        empresas.stream().map(Empresa::id).filter(Objects::nonNull).distinct().toList();

    for (Long empresaId : idsEmpresas) {
      empresaService.vincularPessoa(
          new Empresa.Vincular(new Empresa.Id(empresaId), new Pessoa.Id(salva.id()), true));
    }

    return pessoaRepository.findById(salva.id());
  }

  private Pessoa salvarOuAtualizarVeiculos(Pessoa salva, List<Veiculo> veiculosRequest) {
    if (veiculosRequest == null) {
      return salva;
    }

    List<Veiculo> veiculosExistentes = veiculoRepository.findAllByPessoaId(salva.id());

    if (veiculosExistentes.isEmpty()) {
      if (veiculosRequest.isEmpty()) {
        return salva;
      }

      List<Veiculo> veiculosSalvos = new ArrayList<>(veiculosRequest.size());

      for (Veiculo veiculo : veiculosRequest) {
        Veiculo veiculoSalvo =
            veiculoRepository.update(
                new Veiculo.Update(
                    veiculo.id(),
                    new Pessoa.Id(salva.id()),
                    veiculo.modelo(),
                    veiculo.marca(),
                    veiculo.ano(),
                    veiculo.placa(),
                    veiculo.cor()));
        if (veiculoSalvo.id() == null) {
          throw new IllegalStateException("Veículo salvo sem ID.");
        }
        veiculoRepository.setVinculoAtivo(
            new Veiculo.Vincular(
                new Veiculo.Id(veiculoSalvo.id()), new Pessoa.Id(salva.id()), true));
        veiculosSalvos.add(veiculoSalvo);
      }

      return pessoaRepository.findById(salva.id());
    }

    if (!veiculosRequest.isEmpty()) {
      Veiculo oldVeiculo = veiculosExistentes.getFirst();
      Veiculo newVeiculo = veiculosRequest.getFirst();

      Veiculo veiculoAtualizado =
          new Veiculo(
              oldVeiculo.id(),
              newVeiculo.modelo(),
              newVeiculo.marca(),
              newVeiculo.ano(),
              newVeiculo.placa(),
              newVeiculo.cor());

      veiculoRepository.update(
          new Veiculo.Update(
              veiculoAtualizado.id(),
              new Pessoa.Id(salva.id()),
              veiculoAtualizado.modelo(),
              veiculoAtualizado.marca(),
              veiculoAtualizado.ano(),
              veiculoAtualizado.placa(),
              veiculoAtualizado.cor()));
      veiculoRepository.setVinculoAtivo(
          new Veiculo.Vincular(new Veiculo.Id(oldVeiculo.id()), new Pessoa.Id(salva.id()), true));
    }

    return pessoaRepository.findById(salva.id());
  }

  @Transactional
  public void alterarStatus(Long id, Pessoa.Status status) {
    if (id == null) {
      throw new IllegalArgumentException("Id é obrigatório.");
    }
    if (status == null) {
      throw new IllegalArgumentException("Status é obrigatório.");
    }
    pessoaRepository.alterarStatus(id, status);
  }

  @Transactional
  public void incrementarHospedagem(Long id) {
    if (id == null) {
      throw new IllegalArgumentException("Id é obrigatório.");
    }
    pessoaRepository.incrementarHospedagem(id);
  }

  private void validarPessoa(Pessoa.Request pessoa) {
    if (pessoa == null) {
      throw new IllegalArgumentException("Pessoa é obrigatória.");
    }
    if (!StringUtils.hasText(pessoa.nome())) {
      throw new IllegalArgumentException("Nome é obrigatório.");
    }
    if (!StringUtils.hasText(pessoa.cpf())) {
      throw new IllegalArgumentException("CPF é obrigatório.");
    }
  }

}
