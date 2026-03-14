package saas.hotel.istoepousada.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
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
              c.uuid          AS cargo_id,
              c.descricao       AS cargo_cargo,
              t.uuid          AS tela_id,
              t.nome        AS tela_nome,
              t.descricao   AS tela_descricao,
              p.uuid          AS permissao_id,
              p.permissao   AS permissao_permissao,
              p.descricao   AS permissao_descricao
          FROM descricao c
          LEFT JOIN cargo_tela ct ON ct.cargo_id = c.uuid
          LEFT JOIN tela t ON t.uuid = ct.tela_id
          LEFT JOIN cargo_permissao cp ON cp.fk_cargo = c.uuid
          LEFT JOIN permissao p ON p.uuid = cp.fk_permissao AND p.fk_tela = t.uuid
          """;

  private static final RowMapper<Cargo> CARGO_ROW_MAPPER =
      (rs, rowNum) -> new Cargo(rs.getLong("cargo_id"), rs.getString("cargo_cargo"), List.of());

  private static final RowMapper<Tela> TELA_ROW_MAPPER =
      (rs, rowNum) -> {
        Long telaId = rs.getObject("tela_id", Long.class);
        if (telaId == null) return null;

        return new Tela(
            telaId, rs.getString("tela_nome"), rs.getString("tela_descricao"), List.of());
      };

  private static final RowMapper<Permissao> PERMISSAO_ROW_MAPPER =
      (rs, rowNum) -> {
        Long permissaoId = rs.getObject("permissao_id", Long.class);
        if (permissaoId == null) return null;

        return new Permissao(
            permissaoId, rs.getString("permissao_permissao"), rs.getString("permissao_descricao"));
      };

  private static final ResultSetExtractor<List<Cargo>> CARGO_WITH_TELAS_PERMISSOES_EXTRACTOR =
      rs -> {
        Map<Long, Cargo> cargoMap = new LinkedHashMap<>();
        Map<Long, LinkedHashMap<Long, Tela>> telasPorCargo = new HashMap<>();
        Map<Long, Map<Long, List<Permissao>>> permissoesPorCargoTela = new HashMap<>();
        Map<Long, Map<Long, Set<Long>>> permissaoIdsPorCargoTela = new HashMap<>();

        int rowNum = 0;

        while (rs.next()) {
          Long cargoId = rs.getLong("cargo_id");

          if (!cargoMap.containsKey(cargoId)) {
            Cargo cargo = CARGO_ROW_MAPPER.mapRow(rs, rowNum);
            cargoMap.put(cargoId, cargo);
            telasPorCargo.put(cargoId, new LinkedHashMap<>());
            permissoesPorCargoTela.put(cargoId, new HashMap<>());
            permissaoIdsPorCargoTela.put(cargoId, new HashMap<>());
          }

          Tela tela = TELA_ROW_MAPPER.mapRow(rs, rowNum);
          if (tela != null) {
            telasPorCargo.get(cargoId).putIfAbsent(tela.id(), tela);

            Permissao permissao = PERMISSAO_ROW_MAPPER.mapRow(rs, rowNum);
            if (permissao != null) {
              permissoesPorCargoTela
                  .get(cargoId)
                  .computeIfAbsent(tela.id(), k -> new ArrayList<>());

              permissaoIdsPorCargoTela
                  .get(cargoId)
                  .computeIfAbsent(tela.id(), k -> new HashSet<>());

              if (permissaoIdsPorCargoTela.get(cargoId).get(tela.id()).add(permissao.id())) {
                permissoesPorCargoTela.get(cargoId).get(tela.id()).add(permissao);
              }
            }
          }

          rowNum++;
        }

        List<Cargo> result = new ArrayList<>();

        for (Map.Entry<Long, Cargo> entry : cargoMap.entrySet()) {
          Long cargoId = entry.getKey();
          Cargo cargoBase = entry.getValue();

          List<Tela> telas =
              telasPorCargo.getOrDefault(cargoId, new LinkedHashMap<>()).values().stream()
                  .map(
                      tela ->
                          new Tela(
                              tela.id(),
                              tela.nome(),
                              tela.descricao(),
                              permissoesPorCargoTela
                                  .getOrDefault(cargoId, Map.of())
                                  .getOrDefault(tela.id(), List.of())))
                  .toList();

          result.add(new Cargo(cargoBase.id(), cargoBase.descricao(), telas));
        }

        return result;
      };

  public Page<Cargo> buscarCargoPorIdOuNome(
      Long id, String termo, Long pessoaId, Pageable pageable) {
    boolean hasId = id != null;
    boolean hasPessoaId = pessoaId != null;
    boolean hasTermo = termo != null && !termo.trim().isEmpty();

    String termoTrim = hasTermo ? termo.trim() : null;
    String search = hasTermo ? "%" + termoTrim + "%" : null;

    StringBuilder where = new StringBuilder(" WHERE 1 = 1 ");
    List<Object> params = new ArrayList<>();

    if (hasId) {
      where.append(" AND c.uuid = ? ");
      params.add(id);
    }

    if (hasTermo) {
      where.append(" AND c.descricao ILIKE ? ");
      params.add(search);
    }

    if (hasPessoaId) {
      where.append(
          " AND EXISTS (SELECT 1 FROM funcionario f WHERE f.fk_pessoa = ? AND f.fk_cargo = c.uuid) ");
      params.add(pessoaId);
    }

    Long total;
    try {
      String countSql = "SELECT COUNT(*) FROM cargo c" + where;
      total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
    } catch (EmptyResultDataAccessException ex) {
      total = 0L;
    }

    if (total == null || total == 0) {
      return new PageImpl<>(List.of(), pageable, 0);
    }

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
    idsParams.add(pageable.getOffset());

    List<Long> ids =
        jdbcTemplate.query(idsSql, (rs, rowNum) -> rs.getLong("id"), idsParams.toArray());

    if (ids.isEmpty()) {
      return new PageImpl<>(List.of(), pageable, total);
    }

    String inPlaceholders = String.join(",", Collections.nCopies(ids.size(), "?"));

    String pageSql =
        (SELECT_WITH_TELAS_PERMISSOES
                + """
        WHERE c.uuid IN (%s)
        ORDER BY c.descricao, t.nome, p.permissao
        """)
            .formatted(inPlaceholders);

    List<Cargo> content =
        jdbcTemplate.query(pageSql, CARGO_WITH_TELAS_PERMISSOES_EXTRACTOR, ids.toArray());

    return new PageImpl<>(Objects.requireNonNull(content), pageable, total);
  }

  public Cargo findByIdOrThrow(Long id) {
    Page<Cargo> page = buscarCargoPorIdOuNome(id, null, null, Pageable.ofSize(1));
    if (page.isEmpty()) {
      throw new NotFoundException("Cargo não cadastrado para o uuid: " + id);
    }
    return page.getContent().getFirst();
  }

  public boolean existsById(Long id) {
    try {
      Integer value =
          jdbcTemplate.queryForObject(
              "SELECT 1 FROM cargo WHERE id = ? LIMIT 1", Integer.class, id);
      return value != null;
    } catch (EmptyResultDataAccessException ex) {
      return false;
    }
  }

  @Transactional
  public Cargo insert(Cargo.Request request) {
    KeyHolder keyHolder = new GeneratedKeyHolder();

    jdbcTemplate.update(
        connection -> {
          PreparedStatement ps =
              connection.prepareStatement(
                  "INSERT INTO cargo (cargo) VALUES (?)", Statement.RETURN_GENERATED_KEYS);
          ps.setString(1, request.descricao().trim());
          return ps;
        },
        keyHolder);

    Number generated = keyHolder.getKey();
    if (generated == null) {
      throw new IllegalStateException("Não foi possível obter o uuid do descricao inserido.");
    }

    Long generatedId = generated.longValue();

    if (request.telas() != null && !request.telas().isEmpty()) {
      vincularCargoTelas(generatedId, request.telas().stream().map(Tela.Id::id).toList(), true);
    }

    if (request.permissoes() != null && !request.permissoes().isEmpty()) {
      vincularPermissoesCargo(
          generatedId, request.permissoes().stream().map(Permissao.Id::id).toList(), true);
    }

    return findByIdOrThrow(generatedId);
  }

  @Transactional
  public Cargo update(Cargo.Update request) {
    int rows =
        jdbcTemplate.update(
            "UPDATE cargo SET cargo = ? WHERE id = ?", request.descricao().trim(), request.id());

    if (rows == 0) {
      throw new NotFoundException("Cargo não cadastrado para o uuid: " + request.id());
    }

    if (request.telas() != null) {
      jdbcTemplate.update("DELETE FROM cargo_tela WHERE cargo_id = ?", request.id());

      if (!request.telas().isEmpty()) {
        vincularCargoTelas(request.id(), request.telas().stream().map(Tela.Id::id).toList(), true);
      }
    }

    if (request.permissoes() != null) {
      jdbcTemplate.update("DELETE FROM cargo_permissao WHERE fk_cargo = ?", request.id());

      if (!request.permissoes().isEmpty()) {
        vincularPermissoesCargo(
            request.id(), request.permissoes().stream().map(Permissao.Id::id).toList(), true);
      }
    }

    return findByIdOrThrow(request.id());
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
