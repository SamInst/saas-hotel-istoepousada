package saas.hotel.istoepousada.repository;

import static saas.hotel.istoepousada.dto.Usuario.mapUsuario;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.Usuario;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;
import saas.hotel.istoepousada.handler.exceptions.UnauthorizedException;

@Repository
public class UsuarioRepository {
  Logger log = LoggerFactory.getLogger(UsuarioRepository.class);
  private final JdbcTemplate jdbcTemplate;

  public UsuarioRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  private final ResultSetExtractor<List<Usuario>> USUARIO_EXTRACTOR =
      rs -> {
        List<Usuario> usuarios = new ArrayList<>();
        while (rs.next()) {
          usuarios.add(mapUsuario(rs));
        }
        return usuarios;
      };

  public Page<Usuario> buscar(Long id, String username, Boolean bloqueado, Pageable pageable) {
    boolean hasId = id != null;
    boolean hasUsername = username != null && !username.trim().isEmpty();
    boolean hasBloqueado = bloqueado != null;

    String usernameTrim = hasUsername ? username.trim() : null;
    String search = hasUsername ? "%" + usernameTrim + "%" : null;

    String baseSelect =
        """
                SELECT
                    u.id          AS id,
                    u.username    AS username,
                    u.senha       AS senha,
                    u.bloqueado   AS bloqueado
                FROM usuario u
                """;

    StringBuilder where = new StringBuilder(" WHERE 1=1 ");
    List<Object> params = new ArrayList<>();

    if (hasId) {
      where.append(" AND u.id = ? ");
      params.add(id);
    }

    if (hasUsername) {
      where.append(" AND u.username ILIKE ? ");
      params.add(search);
    }

    if (hasBloqueado) {
      where.append(" AND u.bloqueado = ? ");
      params.add(bloqueado);
    }

    Long total;
    try {
      String countSql = "SELECT COUNT(*) FROM usuario u" + where;
      total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
    } catch (EmptyResultDataAccessException ex) {
      total = 0L;
    }

    if (total == null || total == 0) return new PageImpl<>(List.of(), pageable, 0);

    String idsSql =
        """
                SELECT u.id
                FROM usuario u
                """
            + where
            + """
        ORDER BY u.username ASC
        LIMIT ? OFFSET ?
        """;

    List<Object> idsParams = new ArrayList<>(params);
    idsParams.add(pageable.getPageSize());
    idsParams.add((int) pageable.getOffset());

    List<Long> ids =
        jdbcTemplate.query(idsSql, (rs, rowNum) -> rs.getLong("id"), idsParams.toArray());

    if (ids.isEmpty()) {
      return new PageImpl<>(List.of(), pageable, total);
    }

    String inPlaceholders = String.join(",", Collections.nCopies(ids.size(), "?"));
    String pageSql =
        baseSelect + " WHERE u.id IN (" + inPlaceholders + ") " + " ORDER BY u.username";

    List<Usuario> content = jdbcTemplate.query(pageSql, USUARIO_EXTRACTOR, ids.toArray());
    return new PageImpl<>(Objects.requireNonNull(content), pageable, total);
  }

  public Usuario findById(Long id) {
    Page<Usuario> page = buscar(id, null, null, Pageable.ofSize(1));
    if (page.isEmpty()) {
      throw new NotFoundException("Usuário não encontrado para o id: " + id);
    }
    return page.getContent().getFirst();
  }

  public boolean existsByUsername(String username) {
    String sql = "SELECT COUNT(*) FROM usuario WHERE username = ?";
    Long count = jdbcTemplate.queryForObject(sql, Long.class, username);
    return count != null && count > 0;
  }

  @Transactional
  public Usuario save(Usuario usuario) {
    if (usuario.id() == null) return insert(usuario);
    else {
      update(usuario);
      return findById(usuario.id());
    }
  }

  private Usuario insert(Usuario usuario) {
    if (existsByUsername(usuario.username()))
      throw new IllegalArgumentException("Username já existe: " + usuario.username());

    String sql =
        """
                INSERT INTO usuario (username, senha, bloqueado)
                VALUES (?, ?, ?)
                """;

    KeyHolder keyHolder = new GeneratedKeyHolder();
    String senhaMd5 = gerarMD5(usuario.senha());
    jdbcTemplate.update(
        connection -> {
          PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
          ps.setString(1, usuario.username());
          ps.setString(2, senhaMd5);
          ps.setBoolean(3, usuario.bloqueado());
          return ps;
        },
        keyHolder);

    Long generatedId =
        keyHolder.getKeys() != null && keyHolder.getKeys().containsKey("id")
            ? ((Number) keyHolder.getKeys().get("id")).longValue()
            : null;

    log.info("Usuário criado: id={}, username={}", generatedId, usuario.username());
    return usuario.withId(generatedId).withSenha(senhaMd5);
  }

  @Transactional
  public void update(Usuario usuario) {
    var user = findById(usuario.id());
    if (user.bloqueado()) throw new UnauthorizedException("Usuario bloqueado");

    String sql = """
        UPDATE usuario SET username = ? WHERE id = ?
        """;

    jdbcTemplate.update(sql, usuario.username(), usuario.id());
    log.info("Usuário atualizado: id={}, username={}", usuario.id(), usuario.username());
  }

  @Transactional
  public void alterarSenha(Long id, String novaSenha) {
    var usuario = findById(id);
    if (usuario.bloqueado()) throw new UnauthorizedException("Usuario bloqueado");

    String sql = "UPDATE usuario SET senha = ? WHERE id = ?";
    String senhaMd5 = gerarMD5(novaSenha);

    jdbcTemplate.update(sql, senhaMd5, id);
    log.info("Senha alterada para o usuário id={}", id);
  }

  @Transactional
  public void bloquear(Long id, Boolean bloqueado) {
    findById(id);
    String sql = "UPDATE usuario SET bloqueado = ? WHERE id = ?";
    jdbcTemplate.update(sql, bloqueado, id);

    if (bloqueado) log.info("Usuário bloqueado: id={}", id);
    else log.info("Usuário desbloqueado: id={}", id);
  }

  public boolean autenticar(String username, String senha) {
    String sql =
        """
                SELECT COUNT(*) FROM usuario
                WHERE username = ? AND senha = MD5(?) AND bloqueado = false
                """;

    Long count = jdbcTemplate.queryForObject(sql, Long.class, username, senha);
    boolean autenticado = count != null && count > 0;

    if (autenticado) {
      log.info("Autenticação bem-sucedida para username={}", username);
    } else {
      log.warn("Tentativa de autenticação falhou para username={}", username);
    }

    return autenticado;
  }

  private String gerarMD5(String texto) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] messageDigest = md.digest(texto.getBytes());

      StringBuilder hexString = new StringBuilder();
      for (byte b : messageDigest) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }

      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Erro ao gerar MD5", e);
    }
  }

  public Usuario findByUsername(String username) {
    String sql =
        """
            SELECT
                id,
                username,
                senha,
                bloqueado
            FROM usuario
            WHERE username = ?
            """;

    try {
      return jdbcTemplate.queryForObject(
          sql,
          (rs, rowNum) ->
              new Usuario(
                  rs.getLong("id"),
                  rs.getString("username"),
                  rs.getString("senha"),
                  rs.getBoolean("bloqueado")),
          username);
    } catch (EmptyResultDataAccessException ex) {
      throw new NotFoundException("Usuário não encontrado com username: " + username);
    }
  }
}
