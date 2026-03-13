package saas.hotel.istoepousada.service;

import org.springframework.stereotype.Service;
import saas.hotel.istoepousada.repository.FuncionarioRepository;
import saas.hotel.istoepousada.repository.UsuarioRepository;
import saas.hotel.istoepousada.security.JwtUtil;

@Service
public class AuthService {
  private final UsuarioRepository usuarioRepository;
  private final FuncionarioRepository funcionarioRepository;
  private final JwtUtil jwtUtil;

  public AuthService(
      UsuarioRepository usuarioRepository,
      FuncionarioRepository funcionarioRepository,
      JwtUtil jwtUtil) {
    this.usuarioRepository = usuarioRepository;
    this.funcionarioRepository = funcionarioRepository;
    this.jwtUtil = jwtUtil;
  }
}
