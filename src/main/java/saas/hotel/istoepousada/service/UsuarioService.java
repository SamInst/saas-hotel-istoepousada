package saas.hotel.istoepousada.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import saas.hotel.istoepousada.dto.Usuario;
import saas.hotel.istoepousada.repository.UsuarioRepository;

@Service
public class UsuarioService {
  private final UsuarioRepository usuarioRepository;

  public UsuarioService(UsuarioRepository usuarioRepository) {
    this.usuarioRepository = usuarioRepository;
  }

  public Usuario.UsuarioResponse criar(String username, String senha) {
    Usuario usuario = new Usuario(username, senha);
    Usuario salvo = usuarioRepository.save(usuario);
    return Usuario.UsuarioResponse.from(salvo);
  }

  public Page<Usuario.UsuarioResponse> buscar(
      Long id, String username, Boolean bloqueado, Pageable pageable) {
    return usuarioRepository
        .buscar(id, username, bloqueado, pageable)
        .map(Usuario.UsuarioResponse::from);
  }

  public void alterarSenha(Long id, String novaSenha) {
    usuarioRepository.alterarSenha(id, novaSenha);
  }

  public void alterarStatusBloqueio(Long id, Boolean bloqueado) {
    if (bloqueado == null)
      throw new IllegalArgumentException("O parâmetro 'bloqueado' não pode ser nulo");
    usuarioRepository.bloquear(id, bloqueado);
  }

  public boolean autenticar(String username, String senha) {
    return usuarioRepository.autenticar(username, senha);
  }
}
