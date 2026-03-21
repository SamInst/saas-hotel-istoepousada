package saas.hotel.istoepousada.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import saas.hotel.istoepousada.dto.Empresa;
import saas.hotel.istoepousada.repository.EmpresaRepository;

@Service
public class EmpresaService {
  private final EmpresaRepository empresaRepository;

  public EmpresaService(EmpresaRepository empresaRepository) {
    this.empresaRepository = empresaRepository;
  }

  public Page<Empresa> buscarPorIdNomeOuCnpj(Long id, String termo, Pageable pageable) {
    String termoNormalizado = StringUtils.hasText(termo) ? termo.trim() : null;
    return empresaRepository.findByIdNomeOuCnpj(id, termoNormalizado, pageable);
  }

  public Empresa salvar(Empresa.Request empresa) {
    validarEmpresaRequest(empresa);
    return empresaRepository.create(empresa);
  }

  public Empresa update(Empresa.Update empresa) {
    validarEmpresaUpdate(empresa);
    return empresaRepository.update(empresa);
  }

  public void vincularPessoa(Empresa.Vincular vinculo) {
    if (vinculo.empresa().id() == null)
      throw new IllegalArgumentException("empresaId é obrigatório.");
    if (vinculo.pessoa().id() == null)
      throw new IllegalArgumentException("pessoaIds é obrigatório.");

    empresaRepository.vincularPessoa(vinculo);
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
