package saas.hotel.istoepousada.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import saas.hotel.istoepousada.dto.Empresa;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;
import saas.hotel.istoepousada.repository.EmpresaRepository;
import saas.hotel.istoepousada.repository.PessoaRepository;

@Service
public class EmpresaService {
  private final EmpresaRepository empresaRepository;
  private final NotificacaoService notificacaoService;
  private final PessoaRepository pessoaRepository;

  public EmpresaService(
      EmpresaRepository empresaRepository,
      NotificacaoService notificacaoService,
      PessoaRepository pessoaRepository) {
    this.empresaRepository = empresaRepository;
    this.notificacaoService = notificacaoService;
    this.pessoaRepository = pessoaRepository;
  }

  public Page<Empresa> buscarPorIdNomeOuCnpj(Long id, String termo, Pageable pageable) {
    String termoNormalizado = StringUtils.hasText(termo) ? termo.trim() : null;
    return empresaRepository.buscarPorIdNomeOuCnpj(id, termoNormalizado, pageable);
  }

  public Empresa salvar(Empresa empresa) {
    validarEmpresa(empresa);
    var novaEmpresa = empresaRepository.save(empresa);
    notificacaoService.criar(
        9L, "SAM HELSON", "ATUALIZOU OS DADOS DA EMPRESA: " + novaEmpresa.razaoSocial());
    return novaEmpresa;
  }

  public void vincularPessoas(Long empresaId, Long pessoaId, Boolean vinculo) {
    if (empresaId == null) {
      throw new IllegalArgumentException("empresaId é obrigatório.");
    }
    if (pessoaId == null) {
      throw new IllegalArgumentException("pessoaIds é obrigatório.");
    }

    var pessoa = pessoaRepository.findById(pessoaId);
    var empresa =
        empresaRepository
            .findById(empresaId)
            .orElseThrow(
                () -> new NotFoundException("Empresa não encontrada para o id: " + empresaId));

    empresaRepository.vincularPessoa(empresaId, pessoaId, vinculo);

    notificacaoService.criar(
        9L,
        "SAM HELSON",
        "VINCULOU O HOSPEDE [" + pessoa.nome() + "] À EMPRESA [" + empresa.razaoSocial() + "]");
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
