package saas.hotel.istoepousada.controller;

import org.springframework.web.bind.annotation.*;
import saas.hotel.istoepousada.dto.Funcionario;
import saas.hotel.istoepousada.dto.Login;
import saas.hotel.istoepousada.dto.Usuario;
import saas.hotel.istoepousada.service.UsuarioService;

@RestController
@RequestMapping("/auth")
public class AuthController {
  private final UsuarioService usuarioService;

  public AuthController(UsuarioService usuarioService) {
    this.usuarioService = usuarioService;
  }

  @PostMapping("/login")
  public Login login(@RequestBody Usuario.Request request) {
    return usuarioService.login(request);
  }

  @GetMapping("/validate")
  public Funcionario.Authorization validarToken(@RequestHeader("Authorization") String authHeader) {
    return usuarioService.validarToken(authHeader);
  }
}
