package saas.hotel.istoepousada.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import saas.hotel.istoepousada.dto.Usuario;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;

@Repository
public class UsuarioRepository {

  private final JdbcTemplate jdbcTemplate;

  public UsuarioRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  private static final RowMapper<Usuario> USUARIO_ROW_MAPPER =
      (rs, rowNum) ->
          new Usuario(rs.getLong("id"), rs.getString("username"), rs.getBoolean("bloqueado"));

  private static final String SELECT_BASE =
      """
          SELECT
              u.id,
              u.username,
              u.bloqueado
          FROM usuario u
          """;

  public Page<Usuario> buscar(Long id, String username, Boolean bloqueado, Pageable pageable) {
    boolean hasId = id != null;
    boolean hasUsername = username != null && !username.trim().isEmpty();
    boolean hasBloqueado = bloqueado != null;

    String search = hasUsername ? "%" + username.trim() + "%" : null;

    StringBuilder where = new StringBuilder(" WHERE 1=1 ");
    List<Object> params = new ArrayList<>();

    if (hasId) {
      where.append(" AND u.uuid = ? ");
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

    long total =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM usuario u" + where, Long.class, params.toArray());

    if (total == 0) {
      return new PageImpl<>(List.of(), pageable, 0);
    }

    List<Object> queryParams = new ArrayList<>(params);
    queryParams.add(pageable.getPageSize());
    queryParams.add(pageable.getOffset());

    String sql =
        SELECT_BASE
            + where
            + """
            ORDER BY u.username ASC
            LIMIT ? OFFSET ?
            """;

    List<Usuario> content = jdbcTemplate.query(sql, USUARIO_ROW_MAPPER, queryParams.toArray());

    return new PageImpl<>(content, pageable, total);
  }

  public Usuario findById(Long id) {
    try {
      return jdbcTemplate.queryForObject(SELECT_BASE + " WHERE u.id = ? ", USUARIO_ROW_MAPPER, id);
    } catch (EmptyResultDataAccessException ex) {
      throw new NotFoundException("Usuário não encontrado para o uuid: " + id);
    }
  }

  public Usuario findByUsername(String username) {
    String sql =
        """
            SELECT
                u.id,
                u.username,
                u.bloqueado
            FROM usuario u
            WHERE u.username = ?
            """;

    try {
      return jdbcTemplate.queryForObject(sql, USUARIO_ROW_MAPPER, username);
    } catch (EmptyResultDataAccessException ex) {
      throw new NotFoundException("Usuário não encontrado com username: " + username);
    }
  }

  public boolean existsByUsername(String username) {
    String sql = "SELECT COUNT(*) FROM usuario WHERE username = ?";
    Long count = jdbcTemplate.queryForObject(sql, Long.class, username);
    return count > 0;
  }

  public Usuario create(String username, String senhaMd5) {
    String sql =
        """
            INSERT INTO usuario (username, senha, bloqueado)
            VALUES (?, ?, false)
            """;

    KeyHolder keyHolder = new GeneratedKeyHolder();

    jdbcTemplate.update(
        connection -> {
          PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
          ps.setString(1, username);
          ps.setString(2, senhaMd5);
          return ps;
        },
        keyHolder);

    Long generatedId = keyHolder.getKey() != null ? keyHolder.getKey().longValue() : null;

    if (generatedId == null) {
      throw new IllegalStateException("Não foi possível obter o uuid do usuário criado");
    }

    return findById(generatedId);
  }

  public Usuario updateUsername(Long id, String username) {
    String sql =
        """
            UPDATE usuario
            SET username = ?
            WHERE id = ?
            """;

    jdbcTemplate.update(sql, username, id);
    return findById(id);
  }

  public Usuario updateBloqueado(Long id, Boolean bloqueado) {
    String sql = "UPDATE usuario SET bloqueado = ? WHERE id = ?";
    jdbcTemplate.update(sql, bloqueado, id);
    return findById(id);
  }

  public Usuario updateUsernameESenha(Long id, String username, String senhaMd5) {
    String sql = "UPDATE usuario SET username = ?, senha = ? WHERE id = ?";
    jdbcTemplate.update(sql, username, senhaMd5, id);
    return findById(id);
  }

  public boolean autenticar(String username, String senhaMd5) {
    String sql =
        """
            SELECT COUNT(*)
            FROM usuario
            WHERE username = ?
            AND senha = ?
            AND bloqueado = false
            """;
    Long count = jdbcTemplate.queryForObject(sql, Long.class, username, senhaMd5);
    return count > 0;
  }
}
