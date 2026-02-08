package saas.hotel.istoepousada.repository;

import static saas.hotel.istoepousada.dto.Cargo.mapCargo;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;
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
import saas.hotel.istoepousada.dto.Permissao;
import saas.hotel.istoepousada.dto.Tela;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;

@Repository
public class CargoRepository {

  private final JdbcTemplate jdbcTemplate;

  public CargoRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  private static final String SELECT_WITH_TELAS_PERMISSOES =
      """
                SELECT
                  c.id          AS cargo_id,
                  c.cargo       AS cargo_cargo,
                  t.id          AS tela_id,
                  t.nome        AS tela_nome,
                  t.descricao   AS tela_descricao,
                  p.id          AS permissao_id,
                  p.permissao   AS permissao_permissao,
                  p.descricao   AS permissao_descricao,
                  p.fk_tela     AS permissao_fk_tela
                FROM cargo c
                LEFT JOIN cargo_tela ct ON ct.cargo_id = c.id
                LEFT JOIN tela t ON t.id = ct.tela_id
                LEFT JOIN cargo_permissao cp ON cp.fk_cargo = c.id
                LEFT JOIN permissao p ON p.id = cp.fk_permissao AND p.fk_tela = t.id
                """;

  private final ResultSetExtractor<List<Cargo>> CARGO_WITH_TELAS_PERMISSOES_EXTRACTOR =
      rs -> {
        Map<Long, Cargo> cargoMap = new LinkedHashMap<>();
        Map<Long, Map<Long, Tela>> telasPorCargo = new HashMap<>();
        Map<Long, Set<Long>> permissoesPorTela = new HashMap<>();

        while (rs.next()) {
          Long cargoId = rs.getLong("cargo_id");

          if (!cargoMap.containsKey(cargoId)) {
            Cargo cargo = mapCargo(rs, "cargo_");
            cargoMap.put(cargoId, cargo);
            telasPorCargo.put(cargoId, new LinkedHashMap<>());
          }

          Long telaId = rs.getLong("tela_id");
          if (!rs.wasNull() && telaId != null && telaId > 0) {
            Map<Long, Tela> telasMap = telasPorCargo.get(cargoId);

            if (!telasMap.containsKey(telaId)) {
              Tela tela = Tela.mapTela(rs, "tela_");
              telasMap.put(telaId, tela);
              permissoesPorTela.put(telaId, new HashSet<>());
            }

            Long permissaoId = rs.getLong("permissao_id");
            if (!rs.wasNull() && permissaoId != null && permissaoId > 0) {
              Long permissaoTelaId = rs.getLong("permissao_fk_tela");
              if (permissaoTelaId.equals(telaId)) {
                permissoesPorTela.get(telaId).add(permissaoId);
              }
            }
          }
        }

        List<Cargo> result = new ArrayList<>();
        for (Map.Entry<Long, Cargo> entry : cargoMap.entrySet()) {
          Long cargoId = entry.getKey();
          Cargo cargo = entry.getValue();
          Map<Long, Tela> telasMap = telasPorCargo.get(cargoId);

          List<Tela> telas = new ArrayList<>();
          for (Map.Entry<Long, Tela> telaEntry : telasMap.entrySet()) {
            Long telaId = telaEntry.getKey();
            Tela tela = telaEntry.getValue();

            List<Permissao> permissoes = buscarPermissoesPorCargoTela(cargoId, telaId);
            telas.add(tela.withPermissoes(permissoes));
          }

          result.add(cargo.withTelas(telas));
        }

        return result;
      };

  private List<Permissao> buscarPermissoesPorCargoTela(Long cargoId, Long telaId) {
    String sql =
        """
                SELECT
                  p.id          AS permissao_id,
                  p.permissao   AS permissao_permissao,
                  p.descricao   AS permissao_descricao,
                  p.fk_tela     AS permissao_fk_tela
                FROM cargo_permissao cp
                INNER JOIN permissao p ON p.id = cp.fk_permissao
                WHERE cp.fk_cargo = ? AND p.fk_tela = ?
                ORDER BY p.permissao
                """;

    return jdbcTemplate.query(
        sql, (rs, rowNum) -> Permissao.mapPermissao(rs, "permissao_"), cargoId, telaId);
  }

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
          " AND EXISTS (SELECT 1 FROM funcionario f WHERE f.fk_pessoa = ? AND f.fk_cargo = c.id) ");
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
        (SELECT_WITH_TELAS_PERMISSOES
                + """
      WHERE c.id IN (%s)
      ORDER BY c.cargo, t.nome, p.permissao
      """)
            .formatted(inPlaceholders);

    List<Cargo> content =
        jdbcTemplate.query(pageSql, CARGO_WITH_TELAS_PERMISSOES_EXTRACTOR, ids.toArray());
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

    Long generatedId =
        keyHolder.getKeys() != null && keyHolder.getKeys().containsKey("id")
            ? ((Number) keyHolder.getKeys().get("id")).longValue()
            : null;

    if (cargo.telasIds() != null && !cargo.telasIds().isEmpty()) {
      vincularCargoTelas(generatedId, cargo.telasIds(), true);
    }

    if (cargo.permissoesIds() != null && !cargo.permissoesIds().isEmpty()) {
      vincularPermissoesCargo(generatedId, cargo.permissoesIds(), true);
    }

    return findByIdOrThrow(generatedId);
  }

  @Transactional
  public Cargo update(Cargo.Request cargo) {
    int rows =
        jdbcTemplate.update(
            "UPDATE cargo SET cargo = ? WHERE id = ?", cargo.descricao().trim(), cargo.id());
    if (rows == 0) throw new NotFoundException("Cargo não cadastrado para o id: " + cargo.id());

    if (cargo.telasIds() != null) {
      jdbcTemplate.update("DELETE FROM cargo_tela WHERE cargo_id = ?", cargo.id());

      if (!cargo.telasIds().isEmpty()) {
        vincularCargoTelas(cargo.id(), cargo.telasIds(), true);
      }
    }

    if (cargo.permissoesIds() != null) {
      jdbcTemplate.update("DELETE FROM cargo_permissao WHERE fk_cargo = ?", cargo.id());

      if (!cargo.permissoesIds().isEmpty()) {
        vincularPermissoesCargo(cargo.id(), cargo.permissoesIds(), true);
      }
    }

    return findByIdOrThrow(cargo.id());
  }

  @Transactional
  public void deleteById(Long id) {
    jdbcTemplate.update("DELETE FROM cargo_tela WHERE cargo_id = ?", id);
    jdbcTemplate.update("DELETE FROM cargo_permissao WHERE fk_cargo = ?", id);
    jdbcTemplate.update("DELETE FROM cargo WHERE id = ?", id);
  }

  @Transactional
  public void vincularCargoTelas(Long cargoId, List<Long> telaIds, Boolean vinculo) {
    if (telaIds == null || telaIds.isEmpty()) return;

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
  public void vincularPermissoesCargo(Long cargoId, List<Long> permissaoIds, Boolean vinculo) {
    if (permissaoIds == null || permissaoIds.isEmpty()) return;

    if (Boolean.TRUE.equals(vinculo)) {
      String insertSql =
          "INSERT INTO cargo_permissao (fk_cargo, fk_permissao) VALUES (?, ?) ON CONFLICT DO NOTHING";

      jdbcTemplate.batchUpdate(
          insertSql,
          permissaoIds,
          200,
          (ps, permissaoId) -> {
            ps.setLong(1, cargoId);
            ps.setLong(2, permissaoId);
          });

      return;
    }

    String deleteSql = "DELETE FROM cargo_permissao WHERE fk_cargo = ? AND fk_permissao = ?";

    jdbcTemplate.batchUpdate(
        deleteSql,
        permissaoIds,
        200,
        (ps, permissaoId) -> {
          ps.setLong(1, cargoId);
          ps.setLong(2, permissaoId);
        });
  }
}
