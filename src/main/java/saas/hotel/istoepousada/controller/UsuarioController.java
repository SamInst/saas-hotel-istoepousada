package saas.hotel.istoepousada.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import saas.hotel.istoepousada.dto.Usuario;
import saas.hotel.istoepousada.security.RequireTela;
import saas.hotel.istoepousada.service.UsuarioService;

@RestController
@RequestMapping("/usuario")
@RequireTela("ADMIN")
public class UsuarioController {

  private final UsuarioService usuarioService;

  public UsuarioController(UsuarioService usuarioService) {
    this.usuarioService = usuarioService;
  }

  public record AutenticacaoRequest(String username, String senha) {}

  public record AutenticacaoResponse(Boolean autenticado, String mensagem) {}

  @PatchMapping("/credenciais")
  public Usuario alterarUsernameESenha(@Valid @RequestBody Usuario.Update update) {
    return usuarioService.alterarUsernameESenha(update);
  }

  @PatchMapping("/{id}/bloqueio")
  public Usuario alterarStatusBloqueio(
      @PathVariable Long id,
      @RequestParam Boolean bloqueado) {
    return usuarioService.bloquear(id, bloqueado);
  }

  @PostMapping("/autenticar")
  public AutenticacaoResponse autenticar(@RequestBody AutenticacaoRequest request) {
    boolean autenticado = usuarioService.autenticar(request.username(), request.senha());
    return autenticado ?
            new AutenticacaoResponse(true, "Login bem-sucedido")
            : new AutenticacaoResponse(false, "Credenciais inválidas");
  }
}
