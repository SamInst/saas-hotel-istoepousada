package saas.hotel.istoepousada.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import saas.hotel.istoepousada.dto.Cargo;
import saas.hotel.istoepousada.dto.Funcionario;
import saas.hotel.istoepousada.dto.Pessoa;
import saas.hotel.istoepousada.dto.Usuario;
import saas.hotel.istoepousada.repository.FuncionarioRepository;

@Service
public class FuncionarioService {
  private final FuncionarioRepository funcionarioRepository;
  private final UsuarioService usuarioService;
  private final PessoaService pessoaService;

  public FuncionarioService(
      FuncionarioRepository funcionarioRepository,
      PessoaService pessoaService,
      UsuarioService usuarioService) {
    this.funcionarioRepository = funcionarioRepository;
    this.pessoaService = pessoaService;
    this.usuarioService = usuarioService;
  }

  public Funcionario create(Funcionario.Request request) {
    validarFuncionarioRequest(request);
    Pessoa pessoa = pessoaService.findById(request.pessoa().id());

    pessoaService.alterarStatus(request.pessoa().id(), Pessoa.Status.CONTRATO_ATIVO);

    var usuario =
        usuarioService.create(
            new Usuario.Request(request.usuario().username(), request.usuario().senha()));

    return funcionarioRepository.insert(
        new Funcionario.Request(
            new Pessoa.Id(pessoa.id()),
            request.data_admissao(),
            new Cargo.Id(request.cargo().id()),
            null,
            request.salario()));
  }

  private void validarFuncionarioRequest(Funcionario.Request funcionario) {
    if (funcionario.usuario() == null) {
      throw new IllegalArgumentException("Usuario não pode ser nulo");
    }
    if (funcionario.usuario().senha() == null) {
      throw new IllegalArgumentException("senha não pode ser nula");
    }
    if (funcionario.usuario().senha().isEmpty()) {
      throw new IllegalArgumentException("senha não pode ser vazia");
    }
    if (funcionario.usuario().username() == null) {
      throw new IllegalArgumentException("username não pode ser nulo");
    }
    if (funcionario.usuario().username().isEmpty()) {
      throw new IllegalArgumentException("username não pode ser vazio");
    }
  }

  public Funcionario update(Long id, Funcionario.Request request) {
    return funcionarioRepository.update(id, request);
  }

  public Page<Funcionario> search(
      Long id, String termo, Long cargoId, Long pessoaId, Long usuarioId, Pageable pageable) {
    return funcionarioRepository.buscar(id, termo, cargoId, pessoaId, usuarioId, pageable);
  }
}
