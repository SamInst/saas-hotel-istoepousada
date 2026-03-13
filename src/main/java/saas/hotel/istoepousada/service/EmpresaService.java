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

  public Empresa salvar(Empresa.Update empresa) {
    validarEmpresa(empresa);
    return empresaRepository.create(empresa);
  }

  public void vincularPessoa(Empresa.Vincular vinculo) {
    if (vinculo.empresa().id() == null)
      throw new IllegalArgumentException("empresaId é obrigatório.");
    if (vinculo.pessoa().id() == null)
      throw new IllegalArgumentException("pessoaIds é obrigatório.");

    empresaRepository.vincularPessoa(vinculo);
  }

  private void validarEmpresa(Empresa.Update empresa) {
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
