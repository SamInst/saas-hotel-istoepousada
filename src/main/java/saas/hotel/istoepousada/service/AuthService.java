package saas.hotel.istoepousada.service;

import org.springframework.stereotype.Service;
import saas.hotel.istoepousada.dto.Funcionario;
import saas.hotel.istoepousada.dto.Login;
import saas.hotel.istoepousada.dto.Usuario;
import saas.hotel.istoepousada.handler.exceptions.InvalidTokenException;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;
import saas.hotel.istoepousada.handler.exceptions.UnauthorizedException;
import saas.hotel.istoepousada.repository.CargoRepository;
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

  public Login login(Usuario.Request request) {
    boolean autenticado = usuarioRepository.autenticar(request.username(), request.senha());
    if (!autenticado) throw new UnauthorizedException("Credenciais inválidas");

    var usuario = usuarioRepository.findByUsername(request.username());
    Funcionario funcionario = funcionarioRepository.findByUsuarioId(usuario.id());

    if (funcionario == null)
      throw new NotFoundException("Usuário não possui funcionário vinculado");

    String token = jwtUtil.generateToken(funcionario);
    return new Login(token);
  }

  public Funcionario.Authorization validarToken(String token) {
    if (!jwtUtil.validateToken(token)) throw new InvalidTokenException("Token inválido");
    return jwtUtil.getFuncionarioFromToken(token);
  }
}
