package saas.hotel.istoepousada.service;

import java.util.List;
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
    return empresaRepository.buscarPorIdNomeOuCnpj(id, termoNormalizado, pageable);
  }

  public Empresa salvar(Empresa empresa) {
    validarEmpresa(empresa);
    return empresaRepository.save(empresa);
  }

  public void vincularPessoas(Long empresaId, List<Long> pessoaIds, Boolean vinculo) {
    if (empresaId == null) {
      throw new IllegalArgumentException("empresaId é obrigatório.");
    }
    if (pessoaIds == null || pessoaIds.isEmpty()) {
      throw new IllegalArgumentException("pessoaIds é obrigatório.");
    }
    empresaRepository.vincularPessoas(empresaId, pessoaIds, vinculo);
  }

  private void validarEmpresa(Empresa empresa) {
    if (empresa == null) {
      throw new IllegalArgumentException("Empresa é obrigatória.");
    }
    if (!StringUtils.hasText(empresa.razaoSocial())) {
      throw new IllegalArgumentException("Razão social é obrigatória.");
    }
    if (!StringUtils.hasText(empresa.cnpj())) {
      throw new IllegalArgumentException("CNPJ é obrigatório.");
    }
  }
}
