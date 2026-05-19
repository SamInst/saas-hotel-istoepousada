package saas.hotel.istoepousada.service;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import saas.hotel.istoepousada.dto.Empresa;
import saas.hotel.istoepousada.dto.Pessoa;
import saas.hotel.istoepousada.repository.EmpresaRepository;
import saas.hotel.istoepousada.repository.PessoaRepository;

@Service
public class EmpresaService {
  private final EmpresaRepository empresaRepository;
  private final PessoaRepository pessoaRepository;
  private final PessoaService pessoaService;

  public EmpresaService(
      EmpresaRepository empresaRepository,
      PessoaRepository pessoaRepository,
      PessoaService pessoaService) {
    this.empresaRepository = empresaRepository;
    this.pessoaRepository = pessoaRepository;
    this.pessoaService = pessoaService;
  }

  public Page<Empresa> buscarPorIdNomeOuCnpj(Long id, String termo, Pageable pageable) {
    String termoNormalizado = StringUtils.hasText(termo) ? termo.trim() : null;
    Page<Empresa> page = empresaRepository.findByIdNomeOuCnpj(id, termoNormalizado, pageable);
    List<Empresa> enriched =
        page.getContent().stream()
            .map(
                e ->
                    new Empresa(
                        e.id(),
                        e.data_hora_registro(),
                        e.razao_social(),
                        e.nome_fantasia(),
                        e.cnpj(),
                        e.telefone(),
                        e.email(),
                        e.endereco(),
                        e.cep(),
                        e.numero(),
                        e.complemento(),
                        e.pais(),
                        e.estado(),
                        e.municipio(),
                        e.bairro(),
                        e.tipo_empresa(),
                        e.status(),
                        e.funcionario(),
                        pessoaRepository.findPessoasByEmpresaId(e.id())))
            .toList();
    return new PageImpl<>(enriched, pageable, page.getTotalElements());
  }

  public Empresa salvar(Empresa.Request empresa) {
    validarEmpresaRequest(empresa);
    return empresaRepository.create(empresa, getFuncionarioId());
  }

  public Empresa update(Empresa.Update empresa) {
    validarEmpresaUpdate(empresa);
    return empresaRepository.update(empresa, getFuncionarioId());
  }

  public void vincularPessoa(Empresa.Vincular vinculo) {
    if (vinculo.empresa().id() == null)
      throw new IllegalArgumentException("empresaId é obrigatório.");
    if (vinculo.pessoa().id() == null)
      throw new IllegalArgumentException("pessoaIds é obrigatório.");

    empresaRepository.vincularPessoa(vinculo);
  }

  private Long getFuncionarioId() {
    return pessoaService.getFuncionarioIdFromRequest();
  }

  private void validarEmpresaUpdate(Empresa.Update empresa) {
    if (empresa == null) {
      throw new IllegalArgumentException("Empresa é obrigatória.");
    }
    if (!StringUtils.hasText(empresa.razao_social())) {
      throw new IllegalArgumentException("Razão social é obrigatória.");
    }
    if (!StringUtils.hasText(empresa.cnpj())) {
      throw new IllegalArgumentException("CNPJ é obrigatório.");
    }
  }

  private void validarEmpresaRequest(Empresa.Request empresa) {
    if (empresa == null) {
      throw new IllegalArgumentException("Empresa é obrigatória.");
    }
    if (!StringUtils.hasText(empresa.razao_social())) {
      throw new IllegalArgumentException("Razão social é obrigatória.");
    }
    if (!StringUtils.hasText(empresa.cnpj())) {
      throw new IllegalArgumentException("CNPJ é obrigatório.");
    }
  }
}
