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

@Service
public class PessoaService {

  private static final Logger log = LoggerFactory.getLogger(PessoaService.class);

  private final PessoaRepository pessoaRepository;
  private final EmpresaService empresaService;
  private final VeiculoService veiculoService;

  public PessoaService(
      PessoaRepository pessoaRepository,
      EmpresaService empresaService,
      VeiculoService veiculoService) {
    this.pessoaRepository = pessoaRepository;
    this.empresaService = empresaService;
    this.veiculoService = veiculoService;
  }

  public Page<Pessoa> buscar(
      Long id,
      String termo,
      String placa,
      Pessoa.Status status,
      Pessoa.Ordenacao ordenacao,
      Pessoa.Direcao direcao,
      Pageable pageable) {
    String termoNormalizado = StringUtils.hasText(termo) ? termo.trim() : null;
    String placaNormalizada = StringUtils.hasText(placa) ? placa.trim() : null;
    return pessoaRepository.buscar(
        id, termoNormalizado, placaNormalizada, status, ordenacao, direcao, pageable);
  }

  public Pessoa findById(Long id) {
    if (id == null) {
      throw new IllegalArgumentException("Id é obrigatório.");
    }
    return pessoaRepository.findById(id);
  }

  @Transactional
  public Pessoa atualizarPessoa(Pessoa.Update pessoa) {
    if (pessoa.veiculos_vinculados() != null) {
      pessoa
          .veiculos_vinculados()
          .forEach(
              veiculo -> {
                if (veiculo.id() != null) {
                  veiculoService.update(veiculo);
                } else {
                  var new_veiculo =
                      veiculoService.create(
                          new Veiculo.Request(
                              veiculo.modelo(),
                              veiculo.marca(),
                              veiculo.ano(),
                              veiculo.placa(),
                              veiculo.cor()));
                  veiculoService.setVinculoAtivo(
                      new Veiculo.Vincular(
                          new Veiculo.Id(new_veiculo.id()), new Pessoa.Id(pessoa.id()), true));
                }
              });
    }

    if (pessoa.empresas() != null) {
      pessoa
          .empresas()
          .forEach(
              empresa ->
                  empresaService.vincularPessoa(
                      new Empresa.Vincular(empresa, new Pessoa.Id(pessoa.id()), true)));
    }
    return pessoaRepository.update(pessoa);
  }

  @Transactional
  public List<Pessoa> salvarListaPessoas(Pessoa.BatchRequest request) {
    if (request.pessoas() == null || request.pessoas().isEmpty()) {
      throw new IllegalArgumentException("Lista de pessoas é obrigatória.");
    }

    Pessoa.Request titular_requisicao = request.pessoas().getFirst();
    Pessoa titular_salvo = salvarPessoa(titular_requisicao, null);

    List<Pessoa> acompanhantes_salvos = new ArrayList<>();

    for (Pessoa.Request pessoa : request.pessoas()) {
      if (pessoa == titular_requisicao) {
        continue;
      }

      Pessoa acompanhante_salvo = salvarPessoa(pessoa, titular_salvo.id());
      acompanhantes_salvos.add(acompanhante_salvo);
    }

    if (request.empresas() != null && !request.empresas().isEmpty()) {
      List<Pessoa> todos = new ArrayList<>();
      todos.add(titular_salvo);
      todos.addAll(acompanhantes_salvos);

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
            titular_salvo.id(),
            titular_salvo.data_hora_registro(),
            titular_salvo.data_nascimento(),
            titular_salvo.nome(),
            titular_salvo.cpf(),
            titular_salvo.rg(),
            titular_salvo.email(),
            titular_salvo.telefone(),
            titular_salvo.pais(),
            titular_salvo.estado(),
            titular_salvo.municipio(),
            titular_salvo.endereco(),
            titular_salvo.complemento(),
            titular_salvo.vezes_hospedado(),
            titular_salvo.cep(),
            titular_salvo.idade(),
            titular_salvo.bairro(),
            titular_salvo.sexo(),
            titular_salvo.numero(),
            titular_salvo.status(),
            titular_salvo.empresas_vinculadas(),
            titular_salvo.veiculos_vinculados(),
            titular_salvo.funcionario(),
            titular_salvo.titular(),
            acompanhantes_salvos,
            titular_salvo.profissao());

    List<Pessoa> retorno = new ArrayList<>();
    retorno.add(titularComAcompanhantes);
    retorno.addAll(acompanhantes_salvos);
    return retorno;
  }

  private Pessoa salvarPessoa(Pessoa.Request pessoa, Long titular_id) {
    validarCampos(pessoa);

    Pessoa new_pessoa = pessoaRepository.insert(pessoa);

    pessoaRepository.vincularTitular(
        new Pessoa.VinculoTitular(new Pessoa.Id(titular_id), new Pessoa.Id(new_pessoa.id()), true));
    return salvarOuAtualizarVeiculos(new_pessoa, pessoa.veiculos());
  }

  private Pessoa salvarOuAtualizarVeiculos(Pessoa salva, List<Veiculo> veiculos) {
    if (veiculos == null) {
      return salva;
    }

    List<Veiculo> veiculosExistentes = veiculoService.findAllByPessoaId(salva.id());

    if (veiculosExistentes.isEmpty()) {
      if (veiculos.isEmpty()) {
        return salva;
      }

      List<Veiculo> veiculosSalvos = new ArrayList<>(veiculos.size());

      for (Veiculo veiculo : veiculos) {
        Veiculo veiculoSalvo;

        if (veiculo.id() == null) {
          veiculoSalvo =
              veiculoService.create(
                  new Veiculo.Request(
                      veiculo.modelo(),
                      veiculo.marca(),
                      veiculo.ano(),
                      veiculo.placa(),
                      veiculo.cor()));
        } else {
          veiculoSalvo =
              veiculoService.update(
                  new Veiculo.Update(
                      veiculo.id(),
                      veiculo.modelo(),
                      veiculo.marca(),
                      veiculo.ano(),
                      veiculo.placa(),
                      veiculo.cor()));
        }
        veiculoService.setVinculoAtivo(
            new Veiculo.Vincular(
                new Veiculo.Id(veiculoSalvo.id()), new Pessoa.Id(salva.id()), true));

        if (veiculoSalvo.id() == null) {
          throw new IllegalStateException("Veículo salvo sem ID.");
        }
        veiculosSalvos.add(veiculoSalvo);
      }

      return pessoaRepository.findById(salva.id());
    }

    if (!veiculos.isEmpty()) {
      Veiculo oldVeiculo = veiculosExistentes.getFirst();
      Veiculo newVeiculo = veiculos.getFirst();

      Veiculo veiculoAtualizado =
          new Veiculo(
              oldVeiculo.id(),
              newVeiculo.modelo(),
              newVeiculo.marca(),
              newVeiculo.ano(),
              newVeiculo.placa(),
              newVeiculo.cor());

      veiculoService.update(
          new Veiculo.Update(
              veiculoAtualizado.id(),
              veiculoAtualizado.modelo(),
              veiculoAtualizado.marca(),
              veiculoAtualizado.ano(),
              veiculoAtualizado.placa(),
              veiculoAtualizado.cor()));
      veiculoService.setVinculoAtivo(
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

  private void validarCampos(Pessoa.Request pessoa) {
    if (pessoa == null) {
      throw new IllegalArgumentException("Pessoa é obrigatória.");
    }
    if (!StringUtils.hasText(pessoa.nome())) {
      throw new IllegalArgumentException("Nome é obrigatório.");
    }
    if (pessoa.data_nascimento() == null) {
      throw new IllegalArgumentException("Data de nascimento é obrigatória.");
    }
    if (pessoa.telefone() == null) {
      throw new IllegalArgumentException("Telefone é obrigatório.");
    }
    if (pessoa.cep() == null) {
      throw new IllegalArgumentException("CEP é obrigatório.");
    }
    if (!StringUtils.hasText(pessoa.cpf())) {
      throw new IllegalArgumentException("CPF é obrigatório.");
    }
    if (pessoaRepository.cpfJaExiste(pessoa.cpf())) {
      throw new IllegalArgumentException("CPF já cadastrado.");
    }
    if (pessoa.email() != null) {
      if (pessoaRepository.emailJaExiste(pessoa.email())) {
        throw new IllegalArgumentException("Email já cadastrado.");
      }
    }

    if (pessoa.veiculos() != null && !pessoa.veiculos().isEmpty()) {
      pessoa
          .veiculos()
          .forEach(
              veiculo -> {
                if (veiculo.modelo() == null) {
                  throw new IllegalArgumentException("Modelo do veículo é obrigatório.");
                }
                if (veiculo.marca() == null) {
                  throw new IllegalArgumentException("Marca do veículo é obrigatória.");
                }
                if (veiculo.placa() == null) {
                  throw new IllegalArgumentException("Placa do veículo é obrigatória.");
                }
                if (veiculo.placa().isEmpty()) {
                  throw new IllegalArgumentException("Placa do veículo não pode ser vazia.");
                }
                if (pessoaRepository.placaJaExiste(veiculo.placa())) {
                  throw new IllegalArgumentException("Placa do veículo já cadastrada.");
                }
              });
    }
  }

  public void vincularTitular(Pessoa.VinculoTitular vinculo) {
    pessoaRepository.vincularTitular(vinculo);
  }
}
