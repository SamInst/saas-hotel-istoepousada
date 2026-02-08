package saas.hotel.istoepousada.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.Funcionario;
import saas.hotel.istoepousada.dto.Pessoa;
import saas.hotel.istoepousada.dto.Usuario;
import saas.hotel.istoepousada.repository.FuncionarioRepository;

@Service
public class FuncionarioService {
  private final FuncionarioRepository funcionarioRepository;
  private final PessoaService pessoaService;
  private final UsuarioService usuarioService;

  public FuncionarioService(
      FuncionarioRepository funcionarioRepository,
      PessoaService pessoaService,
      UsuarioService usuarioService) {
    this.funcionarioRepository = funcionarioRepository;
    this.pessoaService = pessoaService;
    this.usuarioService = usuarioService;
  }

  @Transactional
  public Funcionario criar(Funcionario.FuncionarioRequest request) {
    Pessoa pessoaRequest = request.pessoa().withId(null);
    Pessoa pessoaSalva = pessoaService.salvarPessoaIndividual(pessoaRequest);

    pessoaService.alterarStatus(pessoaSalva.id(), Pessoa.Status.CONTRATADO);

    Long usuarioId = null;
    if (request.usuario() != null && request.usuario().username() != null) {
      Usuario.UsuarioResponse usuario =
          usuarioService.criar(request.usuario().username(), request.usuario().senha());
      usuarioId = usuario.id();
    }

    return funcionarioRepository.insert(pessoaSalva.id(), request, usuarioId);
  }

  public Page<Funcionario> buscar(
      Long id, String termo, Long cargoId, Long pessoaId, Long usuarioId, Pageable pageable) {
    return funcionarioRepository.buscar(id, termo, cargoId, pessoaId, usuarioId, pageable);
  }
}
