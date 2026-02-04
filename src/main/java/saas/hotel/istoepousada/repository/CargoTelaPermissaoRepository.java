package saas.hotel.istoepousada.repository;

import static saas.hotel.istoepousada.dto.Cargo.mapCargo;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
import saas.hotel.istoepousada.dto.Cargo;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;

@Repository
public class CargoTelaPermissaoRepository {

  private final JdbcTemplate jdbcTemplate;

  public CargoTelaPermissaoRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  private static final String BASE_SELECT =
      """
            SELECT
              c.id    AS cargo_id,
              c.cargo AS cargo_cargo
            FROM cargo c
            """;

  private final ResultSetExtractor<List<Cargo>> CARGO_EXTRACTOR =
      rs -> {
        List<Cargo> list = new ArrayList<>();
        while (rs.next()) {
          list.add(mapCargo(rs, "cargo_"));
        }
        return list;
      };

  public Page<Cargo> buscarCargoPorIdOuNome(
      Long id, String termo, Long pessoaId, Pageable pageable) {
    boolean hasId = id != null;
    boolean hasPessoaId = pessoaId != null;
    boolean hasTermo = termo != null && !termo.trim().isEmpty();

    String termoTrim = hasTermo ? termo.trim() : null;
    String search = hasTermo ? "%" + termoTrim + "%" : null;

    StringBuilder where = new StringBuilder(" WHERE 1=1 ");
    List<Object> params = new ArrayList<>();

    if (hasId) {
      where.append(" AND c.id = ? ");
      params.add(id);
    }

    if (hasTermo) {
      where.append(" AND c.cargo ILIKE ? ");
      params.add(search);
    }

    if (hasPessoaId) {
      where.append(
          " AND EXISTS (SELECT 1 FROM pessoa_cargo pc WHERE pc.fk_pessoa = ? AND pc.fk_cargo = c.id) ");
      params.add(pessoaId);
    }

    Long total;
    try {
      String countSql = "SELECT COUNT(*) FROM cargo c" + where;
      total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
    } catch (EmptyResultDataAccessException ex) {
      total = 0L;
    }

    if (total == null || total == 0) return new PageImpl<>(List.of(), pageable, 0);

    String idsSql =
        """
                SELECT c.id
                FROM cargo c
                """
            + where
            + """
      ORDER BY c.cargo
      LIMIT ? OFFSET ?
      """;

    List<Object> idsParams = new ArrayList<>(params);
    idsParams.add(pageable.getPageSize());
    idsParams.add((int) pageable.getOffset());

    List<Long> ids =
        jdbcTemplate.query(idsSql, (rs, rowNum) -> rs.getLong("id"), idsParams.toArray());

    if (ids.isEmpty()) return new PageImpl<>(List.of(), pageable, total);

    String inPlaceholders = String.join(",", Collections.nCopies(ids.size(), "?"));

    String pageSql =
        (BASE_SELECT + """
      WHERE c.id IN (%s)
      ORDER BY c.cargo
      """)
            .formatted(inPlaceholders);

    List<Cargo> content = jdbcTemplate.query(pageSql, CARGO_EXTRACTOR, ids.toArray());
    return new PageImpl<>(Objects.requireNonNull(content), pageable, total);
  }

  public Cargo findByIdOrThrow(Long id) {
    Page<Cargo> page = buscarCargoPorIdOuNome(id, null, null, Pageable.ofSize(1));
    if (page.isEmpty()) throw new NotFoundException("Cargo não cadastrado para o id: " + id);
    return page.getContent().getFirst();
  }

  public boolean existsById(Long id) {
    try {
      Long v =
          jdbcTemplate.queryForObject("SELECT 1 FROM cargo WHERE id = ? LIMIT 1", Long.class, id);
      return v != null;
    } catch (EmptyResultDataAccessException ex) {
      return false;
    }
  }

  @Transactional
  public Cargo insert(Cargo.Request cargo) {
    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          PreparedStatement ps =
              connection.prepareStatement(
                  "INSERT INTO cargo (cargo) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
          ps.setString(1, cargo.descricao().trim());
          return ps;
        },
        keyHolder);

    Long generatedId = Objects.requireNonNull(keyHolder.getKey()).longValue();
    return findByIdOrThrow(generatedId);
  }

  @Transactional
  public Cargo update(Cargo.Request cargo) {
    int rows =
        jdbcTemplate.update(
            "UPDATE cargo SET cargo = ? WHERE id = ?", cargo.descricao().trim(), cargo.id());
    if (rows == 0) throw new NotFoundException("Cargo não cadastrado para o id: " + cargo.id());
    return findByIdOrThrow(cargo.id());
  }

  @Transactional
  public void deleteById(Long id) {
    jdbcTemplate.update("DELETE FROM cargo WHERE id = ?", id);
  }

  @Transactional
  public void vincularCargoTelas(Long cargoId, List<Long> telaIds, Boolean vinculo) {
    if (Boolean.TRUE.equals(vinculo)) {
      String insertSql =
          "INSERT INTO cargo_tela (cargo_id, tela_id) VALUES (?, ?) ON CONFLICT DO NOTHING";

      jdbcTemplate.batchUpdate(
          insertSql,
          telaIds,
          200,
          (ps, telaId) -> {
            ps.setLong(1, cargoId);
            ps.setLong(2, telaId);
          });
      return;
    }

    String deleteSql = "DELETE FROM cargo_tela WHERE cargo_id = ? AND tela_id = ?";
    jdbcTemplate.batchUpdate(
        deleteSql,
        telaIds,
        200,
        (ps, telaId) -> {
          ps.setLong(1, cargoId);
          ps.setLong(2, telaId);
        });
  }

  @Transactional
  public void vincularPermissoesPessoa(Long pessoaId, List<Long> permissaoIds, Boolean vinculo) {
    if (Boolean.TRUE.equals(vinculo)) {
      String insertSql =
          "INSERT INTO pessoa_permissao (fk_pessoa, fk_permissao) VALUES (?, ?) ON CONFLICT DO NOTHING";

      jdbcTemplate.batchUpdate(
          insertSql,
          permissaoIds,
          200,
          (ps, permissaoId) -> {
            ps.setLong(1, pessoaId);
            ps.setLong(2, permissaoId);
          });

      return;
    }

    String deleteSql = "DELETE FROM pessoa_permissao WHERE fk_pessoa = ? AND fk_permissao = ?";

    jdbcTemplate.batchUpdate(
        deleteSql,
        permissaoIds,
        200,
        (ps, permissaoId) -> {
          ps.setLong(1, pessoaId);
          ps.setLong(2, permissaoId);
        });
  }
}
