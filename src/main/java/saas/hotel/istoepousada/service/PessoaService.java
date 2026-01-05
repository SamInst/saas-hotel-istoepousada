package saas.hotel.istoepousada.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

  /**
   * Busca unificada paginada: - id != null => busca por id - termo preenchido => busca por nome
   * (ILIKE) ou CPF exato - hospedados == true => filtra hospedados - se todos nulos/vazios => lista
   * todos paginado (ordenado por nome asc no repo)
   */
  public Page<Pessoa> buscar(Long id, String termo, Boolean hospedados, Pageable pageable) {
    String termoNormalizado = StringUtils.hasText(termo) ? termo.trim() : null;
    return pessoaRepository.buscarPorIdNomeCpfOuHospedados(
        id, termoNormalizado, hospedados, pageable);
  }

  @Transactional
  public Pessoa salvar(Pessoa pessoa) {
    validarPessoa(pessoa);
    Pessoa salva = pessoaRepository.save(pessoa);

    if (pessoa.empresasVinculadas() != null && !pessoa.empresasVinculadas().isEmpty()) {
      Pessoa finalSalva = salva;
      pessoa.empresasVinculadas().stream()
          .map(Empresa::id)
          .filter(Objects::nonNull)
          .distinct()
          .forEach(empresaId -> empresaRepository.vincularPessoa(empresaId, finalSalva.id(), true));
    }

    List<Veiculo> veiculos = pessoa.veiculos() == null ? List.of() : pessoa.veiculos();
    if (!veiculos.isEmpty()) {
      List<Veiculo> veiculosSalvos = new ArrayList<>(veiculos.size());

      for (Veiculo v : veiculos) {
        Veiculo veiculoSalvo = veiculoRepository.save(v);

        if (veiculoSalvo.id() == null) {
          throw new IllegalStateException("Veículo salvo sem ID (verifique o RETURNING id).");
        }

        veiculoRepository.setVinculoAtivo(salva.id(), veiculoSalvo.id(), true);
        veiculosSalvos.add(veiculoSalvo);
      }

      salva = salva.withVeiculos(veiculosSalvos);
    }

    notificacaoService.criar(9L, "SAM HELSON", "ATUALIZOU OS DADOS DO CLIENTE: " + pessoa.nome());

    return salva;
  }

  public void alterarStatus(Long id, Pessoa.Status status) {
    if (id == null) {
      throw new IllegalArgumentException("id é obrigatório.");
    }
    if (status == null) {
      throw new IllegalArgumentException("status é obrigatório.");
    }
    pessoaRepository.alterarStatus(id, status);
  }

  public void incrementarHospedagem(Long id) {
    if (id == null) {
      throw new IllegalArgumentException("id é obrigatório.");
    }
    pessoaRepository.incrementarHospedagem(id);
  }

  private void validarPessoa(Pessoa pessoa) {
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
