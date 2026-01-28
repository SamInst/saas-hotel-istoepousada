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
import saas.hotel.istoepousada.repository.EmpresaRepository;
import saas.hotel.istoepousada.repository.PessoaRepository;
import saas.hotel.istoepousada.repository.VeiculoRepository;

@Service
public class PessoaService {
  Logger log = LoggerFactory.getLogger(PessoaService.class);
  private final PessoaRepository pessoaRepository;
  private final VeiculoRepository veiculoRepository;
  private final EmpresaRepository empresaRepository;
  private final NotificacaoService notificacaoService;

  public PessoaService(
      PessoaRepository pessoaRepository,
      VeiculoRepository veiculoRepository,
      EmpresaRepository empresaRepository,
      NotificacaoService notificacaoService) {
    this.pessoaRepository = pessoaRepository;
    this.veiculoRepository = veiculoRepository;
    this.empresaRepository = empresaRepository;
    this.notificacaoService = notificacaoService;
  }

  public Page<Pessoa> buscar(
      Long id, String termo, String placaVeiculo, Pessoa.Status status, Pageable pageable) {
    String termoNormalizado = StringUtils.hasText(termo) ? termo.trim() : null;
    return pessoaRepository.buscar(id, termoNormalizado, placaVeiculo, status, pageable);
  }

  @Transactional
  public Pessoa salvar(Pessoa pessoa) {
    validarPessoa(pessoa);
    Long funcionarioIdLogado = pessoaRepository.getFuncionarioPessoaIdFromRequest();
    var funcionario = pessoaRepository.findById(funcionarioIdLogado);

    Pessoa salva = pessoaRepository.save(pessoa, funcionarioIdLogado);

    if (pessoa.empresasVinculadas() != null && !pessoa.empresasVinculadas().isEmpty()) {
      Pessoa finalSalva = salva;
      pessoa.empresasVinculadas().stream()
          .map(Empresa::id)
          .filter(Objects::nonNull)
          .distinct()
          .forEach(empresaId -> empresaRepository.vincularPessoa(empresaId, finalSalva.id(), true));
    }

    var veiculos = veiculoRepository.findAllByPessoaId(pessoa.id());

    if (veiculos.isEmpty()) {
      if (pessoa.veiculos() != null) {
        List<Veiculo> veiculosSalvos = new ArrayList<>(pessoa.veiculos().size());
        for (Veiculo veiculo : pessoa.veiculos()) {
          Veiculo veiculoSalvo = veiculoRepository.save(pessoa.id(), veiculo);
          if (veiculoSalvo.id() == null)
            throw new IllegalStateException("Veículo salvo sem ID (verifique o RETURNING id).");
          veiculoRepository.setVinculoAtivo(salva.id(), veiculoSalvo.id(), true);
          veiculosSalvos.add(veiculoSalvo);
        }
        salva = salva.withVeiculos(veiculosSalvos);
      }

    } else {
      if (pessoa.veiculos() != null) {
        if (!pessoa.veiculos().isEmpty()) {
          var oldVeiculo = veiculos.getFirst();
          var newVeiculo = pessoa.veiculos().getFirst();
          Veiculo veiculo =
              new Veiculo(
                  oldVeiculo.id(),
                  newVeiculo.modelo(),
                  newVeiculo.marca(),
                  newVeiculo.ano(),
                  newVeiculo.placa(),
                  newVeiculo.cor());
          veiculoRepository.save(pessoa.id(), veiculo);
        }
      }
    }
    notificacaoService.criar(funcionario, "ATUALIZOU OS DADOS DO CLIENTE: " + pessoa.nome());
    log.info(
        "Funcionário [{} - {}] cadastrou/atualizou o cliente [{}]",
        funcionario.id(),
        funcionario.nome(),
        pessoa.nome());
    return salva;
  }

  public void alterarStatus(Long id, Pessoa.Status status) {
    if (id == null) throw new IllegalArgumentException("id é obrigatório.");
    if (status == null) throw new IllegalArgumentException("status é obrigatório.");
    pessoaRepository.alterarStatus(id, status);
  }

  public void incrementarHospedagem(Long id) {
    if (id == null) throw new IllegalArgumentException("id é obrigatório.");
    pessoaRepository.incrementarHospedagem(id);
  }

  private void validarPessoa(Pessoa pessoa) {
    if (pessoa == null) throw new IllegalArgumentException("Pessoa é obrigatória.");
    if (!StringUtils.hasText(pessoa.nome()))
      throw new IllegalArgumentException("Nome é obrigatório.");
    if (!StringUtils.hasText(pessoa.cpf()))
      throw new IllegalArgumentException("CPF é obrigatório.");
  }
}
