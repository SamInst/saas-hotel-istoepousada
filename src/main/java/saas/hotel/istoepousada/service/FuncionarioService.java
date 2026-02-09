package saas.hotel.istoepousada.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
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

  public Funcionario create(Funcionario.FuncionarioRequest request) {
    Pessoa pessoa = pessoaService.findById(request.pessoaId());

    pessoaService.alterarStatus(request.pessoaId(), Pessoa.Status.CONTRATADO);

    Long usuarioId = null;
    if (request.usuario() != null && request.usuario().username() != null) {
      Usuario.UsuarioResponse usuario =
          usuarioService.criar(request.usuario().username(), request.usuario().senha());
      usuarioId = usuario.id();
    }

    return funcionarioRepository.insert(pessoa.id(), request, usuarioId);
  }

  public Funcionario update(Long id, Funcionario.FuncionarioRequest request) {
    return funcionarioRepository.update(id, request);
  }

  public Page<Funcionario> search(
      Long id, String termo, Long cargoId, Long pessoaId, Long usuarioId, Pageable pageable) {
    return funcionarioRepository.buscar(id, termo, cargoId, pessoaId, usuarioId, pageable);
  }
}
