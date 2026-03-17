package saas.hotel.istoepousada.repository;

import java.util.*;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
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
                c.id        AS cargo_id,
                c.cargo     AS cargo_cargo,
                t.id        AS tela_id,
                t.nome      AS tela_nome,
                t.descricao AS tela_descricao,
                p.id        AS permissao_id,
                p.permissao AS permissao_permissao,
                p.descricao AS permissao_descricao
            FROM cargo c
            LEFT JOIN cargo_tela ct ON ct.cargo_id = c.id
            LEFT JOIN tela t ON t.id = ct.tela_id
            LEFT JOIN cargo_permissao cp ON cp.fk_cargo = c.id
            LEFT JOIN permissao p ON p.id = cp.fk_permissao AND p.fk_tela = t.id
            """;

  private static final RowMapper<Cargo> CARGO_ROW_MAPPER =
      (rs, rowNum) -> new Cargo(rs.getLong("cargo_id"), rs.getString("cargo_cargo"), List.of());

  private static final RowMapper<Tela> TELA_ROW_MAPPER =
      (rs, rowNum) -> {
        Long tela_id = rs.getObject("tela_id", Long.class);
        if (tela_id == null) return null;

        return new Tela(
            tela_id, rs.getString("tela_nome"), rs.getString("tela_descricao"), List.of());
      };

  private static final RowMapper<Permissao> PERMISSAO_ROW_MAPPER =
      (rs, rowNum) -> {
        Long permissao_id = rs.getObject("permissao_id", Long.class);
        if (permissao_id == null) return null;

        return new Permissao(
            permissao_id, rs.getString("permissao_permissao"), rs.getString("permissao_descricao"));
      };

  private static final ResultSetExtractor<List<Cargo>> CARGO_WITH_TELAS_PERMISSOES_EXTRACTOR =
      rs -> {
        Map<Long, Cargo> cargo_map = new LinkedHashMap<>();
        Map<Long, LinkedHashMap<Long, Tela>> telas_por_cargo = new HashMap<>();
        Map<Long, Map<Long, List<Permissao>>> permissoes_por_cargo_tela = new HashMap<>();
        Map<Long, Map<Long, Set<Long>>> permissoes_id_por_cargo_tela = new HashMap<>();

        int rowNum = 0;

        while (rs.next()) {
          Long cargo_id = rs.getLong("cargo_id");

          if (!cargo_map.containsKey(cargo_id)) {
            Cargo cargo = CARGO_ROW_MAPPER.mapRow(rs, rowNum);
            cargo_map.put(cargo_id, cargo);
            telas_por_cargo.put(cargo_id, new LinkedHashMap<>());
            permissoes_por_cargo_tela.put(cargo_id, new HashMap<>());
            permissoes_id_por_cargo_tela.put(cargo_id, new HashMap<>());
          }

          Tela tela = TELA_ROW_MAPPER.mapRow(rs, rowNum);
          if (tela != null) {
            telas_por_cargo.get(cargo_id).putIfAbsent(tela.id(), tela);

            Permissao permissao = PERMISSAO_ROW_MAPPER.mapRow(rs, rowNum);
            if (permissao != null) {
              permissoes_por_cargo_tela
                  .get(cargo_id)
                  .computeIfAbsent(tela.id(), k -> new ArrayList<>());

              permissoes_id_por_cargo_tela
                  .get(cargo_id)
                  .computeIfAbsent(tela.id(), k -> new HashSet<>());

              if (permissoes_id_por_cargo_tela.get(cargo_id).get(tela.id()).add(permissao.id())) {
                permissoes_por_cargo_tela.get(cargo_id).get(tela.id()).add(permissao);
              }
            }
          }

          rowNum++;
        }

        List<Cargo> result = new ArrayList<>();

        for (Map.Entry<Long, Cargo> entry : cargo_map.entrySet()) {
          Long cargo_id = entry.getKey();
          Cargo cargo_base = entry.getValue();

          List<Tela> telas =
              telas_por_cargo.getOrDefault(cargo_id, new LinkedHashMap<>()).values().stream()
                  .map(
                      tela ->
                          new Tela(
                              tela.id(),
                              tela.nome(),
                              tela.descricao(),
                              permissoes_por_cargo_tela
                                  .getOrDefault(cargo_id, Map.of())
                                  .getOrDefault(tela.id(), List.of())))
                  .toList();

          result.add(new Cargo(cargo_base.id(), cargo_base.descricao(), telas));
        }

        return result;
      };

  public Page<Cargo> buscarCargoPorIdOuNome(Cargo.Buscar cargo) {
    Pageable pageable = PageRequest.of(cargo.page(), cargo.size());
    boolean has_id = cargo.id() != null;
    boolean has_pessoa_id = cargo.pessoa() != null && cargo.pessoa().id() != null;
    boolean has_termo = cargo.termo() != null && !cargo.termo().trim().isEmpty();

    String termoTrim = has_termo ? cargo.termo().trim() : null;
    String search = has_termo ? "%" + termoTrim + "%" : null;

    StringBuilder where = new StringBuilder(" WHERE 1 = 1 ");
    List<Object> params = new ArrayList<>();

    if (has_id) {
      where.append(" AND c.id = ? ");
      params.add(cargo.id());
    }

    if (has_termo) {
      where.append(" AND c.cargo ILIKE ? ");
      params.add(search);
    }

    if (has_pessoa_id) {
      where.append(
          " AND EXISTS (SELECT 1 FROM funcionario f WHERE f.fk_pessoa = ? AND f.fk_cargo = c.id) ");
      params.add(cargo.pessoa().id());
    }

    long total;
    try {
      String count = "SELECT COUNT(*) FROM cargo c" + where;
      total = jdbcTemplate.queryForObject(count, Long.class, params.toArray());
    } catch (EmptyResultDataAccessException ex) {
      total = 0L;
    }

    if (total == 0) {
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

    List<Object> ids_params = new ArrayList<>(params);
    ids_params.add(pageable.getPageSize());
    ids_params.add(pageable.getOffset());

    List<Long> ids =
        jdbcTemplate.query(idsSql, (rs, rowNum) -> rs.getLong("id"), ids_params.toArray());

    if (ids.isEmpty()) {
      return new PageImpl<>(List.of(), pageable, total);
    }

    String in_place_holders = String.join(",", Collections.nCopies(ids.size(), "?"));

    String page_sql =
        (SELECT_WITH_TELAS_PERMISSOES
                + """
                        WHERE c.id IN (%s)
                        ORDER BY c.cargo, t.nome, p.permissao
                        """)
            .formatted(in_place_holders);

    List<Cargo> content =
        jdbcTemplate.query(page_sql, CARGO_WITH_TELAS_PERMISSOES_EXTRACTOR, ids.toArray());

    return new PageImpl<>(Objects.requireNonNull(content), pageable, total);
  }

  public Cargo findByIdOrThrow(Cargo.Id cargo) {
    Page<Cargo> page = buscarCargoPorIdOuNome(new Cargo.Buscar(cargo.id(), null, null, 0, 1));
    if (page.isEmpty()) {
      throw new NotFoundException("Cargo não cadastrado para o id: " + cargo.id());
    }
    return page.getContent().getFirst();
  }

  public boolean existsById(Cargo.Id cargo) {
    try {
      Integer value =
          jdbcTemplate.queryForObject(
              "SELECT 1 FROM cargo WHERE id = ? LIMIT 1", Integer.class, cargo.id());
      return value > 0;
    } catch (EmptyResultDataAccessException ex) {
      return false;
    }
  }

  @Transactional
  public Cargo insert(Cargo.Request request) {
    Cargo.Id id =
        new Cargo.Id(
            jdbcTemplate.queryForObject(
                """
                                INSERT INTO cargo (cargo) values (?) RETURNING id
                                """,
                Long.class,
                request.descricao().trim()));

    if (request.telas() != null && !request.telas().isEmpty()) {
      vincularCargoTelas(new Cargo.Vincular(id, request.telas(), true));
    }

    if (request.permissoes() != null && !request.permissoes().isEmpty()) {
      vincularPermissoesCargo(new Permissao.Vincular(id, request.permissoes(), true));
    }

    return findByIdOrThrow(id);
  }

  @Transactional
  public Cargo update(Cargo.Update request) {
    Cargo.Id id =
        new Cargo.Id(
            jdbcTemplate.queryForObject(
                """
                                UPDATE cargo SET cargo = ? WHERE id = ? returning id
                                """,
                Long.class,
                request.descricao().trim(),
                request.id()));

    if (request.telas() != null) {
      deleteCargoTelas(id);
      if (!request.telas().isEmpty()) {
        vincularCargoTelas(new Cargo.Vincular(id, request.telas(), true));
      }
    }

    if (request.permissoes() != null) {
      deleteCargoPermissoes(id);
      if (!request.permissoes().isEmpty()) {
        vincularPermissoesCargo(new Permissao.Vincular(id, request.permissoes(), true));
      }
    }

    return findByIdOrThrow(id);
  }

  @Transactional
  public void deleteById(Cargo.Id cargo) {
    deleteCargoTelas(cargo);
    deleteCargoPermissoes(cargo);
    deleteCargo(cargo);
  }

  @Transactional
  public void vincularCargoTelas(Cargo.Vincular vinculo) {
    if (vinculo.telas() == null || vinculo.telas().isEmpty()) return;

    if (Boolean.TRUE.equals(vinculo.ativo())) {
      String insert =
          "INSERT INTO cargo_tela (cargo_id, tela_id) VALUES (?, ?) ON CONFLICT DO NOTHING";

      jdbcTemplate.batchUpdate(
          insert,
          vinculo.telas(),
          200,
          (ps, tela) -> {
            ps.setLong(1, vinculo.cargo().id());
            ps.setLong(2, tela.id());
          });
      return;
    }

    String delete = "DELETE FROM cargo_tela WHERE cargo_id = ? AND tela_id = ?";

    jdbcTemplate.batchUpdate(
        delete,
        vinculo.telas(),
        200,
        (ps, tela) -> {
          ps.setLong(1, vinculo.cargo().id());
          ps.setLong(2, tela.id());
        });
  }

  @Transactional
  public void vincularPermissoesCargo(Permissao.Vincular vinculo) {
    if (vinculo.permissoes() == null || vinculo.permissoes().isEmpty()) return;

    if (Boolean.TRUE.equals(vinculo.ativo())) {
      String insert =
          "INSERT INTO cargo_permissao (fk_cargo, fk_permissao) VALUES (?, ?) ON CONFLICT DO NOTHING";

      jdbcTemplate.batchUpdate(
          insert,
          vinculo.permissoes(),
          200,
          (ps, permissao) -> {
            ps.setLong(1, vinculo.cargo().id());
            ps.setLong(2, permissao.id());
          });
      return;
    }

    String delete = "DELETE FROM cargo_permissao WHERE fk_cargo = ? AND fk_permissao = ?";

    jdbcTemplate.batchUpdate(
        delete,
        vinculo.permissoes(),
        200,
        (ps, permissao) -> {
          ps.setLong(1, vinculo.cargo().id());
          ps.setLong(2, permissao.id());
        });
  }

  private void deleteCargoTelas(Cargo.Id cargo) {
    jdbcTemplate.update("DELETE FROM cargo_tela WHERE cargo_id = ?", cargo.id());
  }

  private void deleteCargoPermissoes(Cargo.Id cargo) {
    jdbcTemplate.update("DELETE FROM cargo_permissao WHERE fk_cargo = ?", cargo.id());
  }

  private void deleteCargo(Cargo.Id cargo) {
    jdbcTemplate.update("DELETE FROM cargo WHERE id = ?", cargo.id());
  }
}
