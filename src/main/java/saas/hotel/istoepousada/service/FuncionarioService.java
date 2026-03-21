package saas.hotel.istoepousada.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.Cargo;
import saas.hotel.istoepousada.dto.Funcionario;
import saas.hotel.istoepousada.dto.Pessoa;
import saas.hotel.istoepousada.repository.FuncionarioRepository;

@Service
public class FuncionarioService {
  private final FuncionarioRepository funcionarioRepository;
  private final PessoaService pessoaService;

  public FuncionarioService(
      FuncionarioRepository funcionarioRepository, PessoaService pessoaService) {
    this.funcionarioRepository = funcionarioRepository;
    this.pessoaService = pessoaService;
  }

  @Transactional
  public Funcionario create(Funcionario.Request request) {
    validarFuncionarioRequest(request);
    Pessoa pessoa = pessoaService.findById(request.pessoa().id());

    pessoaService.alterarStatus(request.pessoa().id(), Pessoa.Status.CONTRATADO);

    return funcionarioRepository.insert(
        new Funcionario.Request(
            new Pessoa.Id(pessoa.id()),
            request.data_admissao(),
            new Cargo.Id(request.cargo().id()),
            request.usuario(),
            request.salario()));
  }

  private void validarFuncionarioRequest(Funcionario.Request funcionario) {
    if (funcionario.pessoa() == null) {
      throw new IllegalArgumentException("Pessoa não pode ser nula");
    }
    if (funcionario.pessoa().id() == null) {
      throw new IllegalArgumentException("ID da pessoa não pode ser nulo");
    }
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

  public Funcionario update(Funcionario.Update funcionario) {
    return funcionarioRepository.update(funcionario);
  }

  public Page<Funcionario> search(
      Long id, String termo, Long cargoId, Long pessoaId, Long usuarioId, Pageable pageable) {
    return funcionarioRepository.buscar(id, termo, cargoId, pessoaId, usuarioId, pageable);
  }
}
