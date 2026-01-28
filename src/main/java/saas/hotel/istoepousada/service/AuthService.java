package saas.hotel.istoepousada.service;

import org.springframework.stereotype.Service;
import saas.hotel.istoepousada.dto.Funcionario;
import saas.hotel.istoepousada.dto.FuncionarioAuth;
import saas.hotel.istoepousada.dto.Login;
import saas.hotel.istoepousada.dto.LoginResponse;
import saas.hotel.istoepousada.handler.exceptions.InvalidTokenException;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;
import saas.hotel.istoepousada.handler.exceptions.UnauthorizedException;
import saas.hotel.istoepousada.repository.FuncionarioRepository;
import saas.hotel.istoepousada.repository.PermissaoRepository;
import saas.hotel.istoepousada.repository.UsuarioRepository;
import saas.hotel.istoepousada.security.JwtUtil;

@Service
public class AuthService {
  private final UsuarioRepository usuarioRepository;
  private final FuncionarioRepository funcionarioRepository;
  private final JwtUtil jwtUtil;
  private final PermissaoRepository permissaoRepository;

  public AuthService(
      UsuarioRepository usuarioRepository,
      FuncionarioRepository funcionarioRepository,
      JwtUtil jwtUtil,
      PermissaoRepository permissaoRepository) {
    this.usuarioRepository = usuarioRepository;
    this.funcionarioRepository = funcionarioRepository;
    this.jwtUtil = jwtUtil;
    this.permissaoRepository = permissaoRepository;
  }

  public LoginResponse login(Login request) {
    boolean autenticado = usuarioRepository.autenticar(request.username(), request.senha());
    if (!autenticado) throw new UnauthorizedException("Credenciais inválidas");
    var usuario = usuarioRepository.findByUsername(request.username());
    Funcionario funcionario = funcionarioRepository.findByUsuarioId(usuario.id());
    if (funcionario == null)
      throw new NotFoundException("Usuário não possui funcionário vinculado");

    var telas = permissaoRepository.buscarTelasComPermissoesPorPessoaId(funcionario.pessoa().id());
    var cargoComTelas = funcionario.cargo() == null ? null : funcionario.cargo().withTelas(telas);

    Funcionario funcionarioComTelas =
        new Funcionario(
            funcionario.id(),
            funcionario.pessoa(),
            funcionario.dataAdmissao(),
            cargoComTelas,
            funcionario.usuario());

    FuncionarioAuth funcionarioAuth = FuncionarioAuth.from(funcionarioComTelas);

    String token = jwtUtil.generateToken(funcionarioAuth);
    return new LoginResponse(token);
  }

  public FuncionarioAuth validarToken(String token) {
    if (!jwtUtil.validateToken(token)) throw new InvalidTokenException("Token inválido");
    return jwtUtil.getFuncionarioFromToken(token);
  }
}
