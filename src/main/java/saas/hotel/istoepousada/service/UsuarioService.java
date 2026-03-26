package saas.hotel.istoepousada.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.Funcionario;
import saas.hotel.istoepousada.dto.Login;
import saas.hotel.istoepousada.dto.Usuario;
import saas.hotel.istoepousada.handler.exceptions.InvalidTokenException;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;
import saas.hotel.istoepousada.handler.exceptions.UnauthorizedException;
import saas.hotel.istoepousada.repository.FuncionarioRepository;
import saas.hotel.istoepousada.repository.UsuarioRepository;
import saas.hotel.istoepousada.security.JwtUtil;

@Service
public class UsuarioService {

  private final Logger log = LoggerFactory.getLogger(UsuarioService.class);
  private final UsuarioRepository repository;
  private final FuncionarioRepository funcionarioRepository;
  private final JwtUtil jwtUtil;

  public UsuarioService(
      UsuarioRepository repository, FuncionarioRepository funcionarioRepository, JwtUtil jwtUtil) {
    this.repository = repository;
    this.funcionarioRepository = funcionarioRepository;
    this.jwtUtil = jwtUtil;
  }

  @Transactional(readOnly = true)
  public Page<Usuario> buscar(Long id, String username, Boolean bloqueado, Pageable pageable) {
    return repository.buscar(id, username, bloqueado, pageable);
  }

  @Transactional(readOnly = true)
  public Usuario findById(Long id) {
    return repository.findById(id);
  }

  @Transactional(readOnly = true)
  public Usuario findByUsername(String username) {
    return repository.findByUsername(username);
  }

  @Transactional
  public Usuario create(Usuario.Request usuario) {
    String usernameTrim = usuario.username().trim();

    if (repository.existsByUsername(usernameTrim)) {
      throw new IllegalArgumentException("Username já existe: " + usernameTrim);
    }

    Usuario created = repository.create(usernameTrim, usuario.senha());

    log.info("Usuário criado: uuid={}, username={}", created.id(), created.username());
    return created;
  }

  @Transactional
  public Usuario update(Usuario usuario) {
    Usuario atual = repository.findById(usuario.id());

    if (atual.bloqueado()) {
      throw new UnauthorizedException("Usuario bloqueado");
    }

    Usuario updated = repository.updateUsername(usuario.id(), usuario.username().trim());

    log.info("Usuário atualizado: uuid={}, username={}", updated.id(), updated.username());
    return updated;
  }

  @Transactional
  public Usuario bloquear(Long id, Boolean bloqueado) {
    repository.findById(id);

    Usuario updated = repository.updateBloqueado(id, bloqueado);

    if (Boolean.TRUE.equals(bloqueado)) {
      log.info("Usuário bloqueado: uuid={}", id);
    } else {
      log.info("Usuário desbloqueado: uuid={}", id);
    }

    return updated;
  }

  @Transactional(readOnly = true)
  public boolean autenticar(String username, String senha) {
    boolean autenticado = repository.autenticar(username, senha);

    if (autenticado) log.info("Autenticação bem-sucedida para username={}", username);
    else log.warn("Tentativa de autenticação falhou para username={}", username);
    return autenticado;
  }

  @Transactional
  public Usuario alterarUsernameESenha(Usuario.Update update) {
    Usuario atual = repository.findById(update.id());

    if (atual.bloqueado()) {
      throw new UnauthorizedException("Usuário bloqueado");
    }

    String usernameTrim = update.username().trim();

    if (!usernameTrim.equalsIgnoreCase(atual.username())
        && repository.existsByUsername(usernameTrim)) {
      throw new IllegalArgumentException("Username já existe: " + usernameTrim);
    }

    Usuario updated = repository.updateUsernameESenha(update.id(), usernameTrim, update.senha());

    log.info("Username e senha alterados: uuid={}, username={}", updated.id(), updated.username());
    return updated;
  }

  @Transactional(readOnly = true)
  public Login login(Usuario.Request request) {
    boolean autenticado = autenticar(request.username(), request.senha());
    if (!autenticado) throw new UnauthorizedException("Credenciais inválidas");

    var usuario = findByUsername(request.username());
    System.out.println(usuario);
    Funcionario.Authorization funcionario =
        funcionarioRepository.funcionarioLogin(new Usuario.Id(usuario.id()));
    System.out.println(funcionario);

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
