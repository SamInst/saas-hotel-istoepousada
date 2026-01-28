package saas.hotel.istoepousada.repository;

import java.sql.*;
import java.util.*;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.Quarto;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;

@Repository
public class QuartoRepository {

  private final JdbcTemplate jdbcTemplate;

  public QuartoRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  private final ResultSetExtractor<List<Quarto>> QUARTO_EXTRACTOR =
      rs -> {
        List<Quarto> list = new ArrayList<>();
        while (rs.next()) list.add(Quarto.mapQuarto(rs, "quarto_"));
        return list;
      };

  public Page<Quarto> buscar(Long id, String termo, Quarto.StatusQuarto status, Pageable pageable) {
    String baseFrom = "FROM public.quarto quarto ";

    String baseSelect =
        """
                SELECT
                  quarto.id              AS quarto_id,
                  quarto.descricao       AS quarto_descricao,
                  quarto.qtd_pessoas     AS quarto_qtd_pessoas,
                  quarto.status          AS quarto_status,
                  quarto.qtd_cama_casal  AS quarto_qtd_cama_casal,
                  quarto.qtd_cama_solteiro AS quarto_qtd_cama_solteiro,
                  quarto.qtd_rede        AS quarto_qtd_rede,
                  quarto.qtd_beliche     AS quarto_qtd_beliche
                FROM public.quarto quarto
                """;

    StringBuilder where = new StringBuilder(" WHERE 1=1 ");
    List<Object> params = new ArrayList<>();

    if (id != null) {
      where.append(" AND quarto.id = ? ");
      params.add(id);
    }

    if (termo != null && !termo.isBlank()) {
      String t = termo.trim();
      boolean isNumeric = t.matches("\\d+");

      where.append(" AND (quarto.descricao ILIKE ? ");
      params.add("%" + t + "%");

      if (isNumeric) {
        where.append(" OR quarto.id = ? ");
        params.add(Long.parseLong(t));
      }
      where.append(") ");
    }

    if (status != null) {
      where.append(" AND quarto.status = CAST(? AS public.quarto_status) ");
      params.add(status.name());
    }

    Long total;
    try {
      total =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) " + baseFrom + where, Long.class, params.toArray());
    } catch (EmptyResultDataAccessException ex) {
      total = 0L;
    }

    if (total == null || total == 0) return new PageImpl<>(List.of(), pageable, 0);

    String idsSql =
        "SELECT quarto.id AS id "
            + baseFrom
            + where
            + """
            ORDER BY quarto.descricao ASC NULLS LAST, quarto.id ASC
            LIMIT ? OFFSET ?
            """;

    List<Object> idsParams = new ArrayList<>(params);
    idsParams.add(pageable.getPageSize());
    idsParams.add((int) pageable.getOffset());

    List<Long> ids =
        jdbcTemplate.query(idsSql, (rs, rowNum) -> rs.getLong("id"), idsParams.toArray());

    if (ids.isEmpty()) return new PageImpl<>(List.of(), pageable, total);

    String in = String.join(",", Collections.nCopies(ids.size(), "?"));

    String pageSql =
        baseSelect
            + " WHERE quarto.id IN ("
            + in
            + ") ORDER BY quarto.descricao ASC NULLS LAST, quarto.id ASC";

    List<Quarto> content = jdbcTemplate.query(pageSql, QUARTO_EXTRACTOR, ids.toArray());
    return new PageImpl<>(Objects.requireNonNull(content), pageable, total);
  }

  public Quarto findByIdOrThrow(Long id) {
    if (id == null) throw new IllegalArgumentException("id é obrigatório.");
    try {
      String sql =
          """
                    SELECT
                      quarto.id              AS quarto_id,
                      quarto.descricao       AS quarto_descricao,
                      quarto.qtd_pessoas     AS quarto_qtd_pessoas,
                      quarto.status          AS quarto_status,
                      quarto.qtd_cama_casal  AS quarto_qtd_cama_casal,
                      quarto.qtd_cama_solteiro AS quarto_qtd_cama_solteiro,
                      quarto.qtd_rede        AS quarto_qtd_rede,
                      quarto.qtd_beliche     AS quarto_qtd_beliche
                    FROM public.quarto quarto
                    WHERE quarto.id = ?
                    """;

      return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> Quarto.mapQuarto(rs, "quarto_"), id);
    } catch (EmptyResultDataAccessException ex) {
      throw new NotFoundException("Quarto não encontrado para o id: " + id);
    } catch (Exception ex) {
      throw new RuntimeException("Erro ao buscar quarto por id: " + id, ex);
    }
  }

  @Transactional
  public Quarto insert(Quarto quarto) {
    if (quarto == null) throw new IllegalArgumentException("Quarto é obrigatório.");
    validar(quarto);

    String sql =
        """
                INSERT INTO public.quarto (
                  descricao,
                  qtd_pessoas,
                  status,
                  qtd_cama_casal,
                  qtd_cama_solteiro,
                  qtd_rede,
                  qtd_beliche,
                  fk_categoria
                ) VALUES (?, ?, CAST(? AS public.quarto_status), ?, ?, ?, ?, ?)
                """;

    KeyHolder keyHolder = new GeneratedKeyHolder();

    jdbcTemplate.update(
        con -> {
          PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
          ps.setString(1, quarto.descricao());
          setIntOrNull(ps, 2, quarto.quantidade_pessoas());
          setStringOrNull(
              ps, 3, quarto.status_quarto() == null ? null : quarto.status_quarto().name());
          setIntOrNull(ps, 4, quarto.qtd_cama_casal());
          setIntOrNull(ps, 5, quarto.qtd_cama_solteiro());
          setIntOrNull(ps, 6, quarto.qtd_rede());
          setIntOrNull(ps, 7, quarto.qtd_beliche());
          ps.setNull(8, Types.INTEGER);

          return ps;
        },
        keyHolder);

    Long id =
        keyHolder.getKeys() != null && keyHolder.getKeys().containsKey("id")
            ? ((Number) keyHolder.getKeys().get("id")).longValue()
            : null;

    if (id == null) throw new IllegalStateException("Registro salvo sem ID (verifique RETURNING).");
    return findByIdOrThrow(id);
  }

  @Transactional
  public Quarto update(Long id, Quarto quarto) {
    if (id == null) throw new IllegalArgumentException("id é obrigatório.");
    if (quarto == null) throw new IllegalArgumentException("Quarto é obrigatório.");
    validar(quarto);

    String sql =
        """
                UPDATE public.quarto SET
                  descricao = ?,
                  qtd_pessoas = ?,
                  status = CAST(? AS public.quarto_status),
                  qtd_cama_casal = ?,
                  qtd_cama_solteiro = ?,
                  qtd_rede = ?,
                  qtd_beliche = ?
                WHERE id = ?
                """;

    int rows =
        jdbcTemplate.update(
            sql,
            quarto.descricao(),
            quarto.quantidade_pessoas(),
            quarto.status_quarto() == null ? null : quarto.status_quarto().name(),
            quarto.qtd_cama_casal(),
            quarto.qtd_cama_solteiro(),
            quarto.qtd_rede(),
            quarto.qtd_beliche(),
            id);

    if (rows == 0) throw new NotFoundException("Quarto não encontrado para o id: " + id);
    return findByIdOrThrow(id);
  }

  @Transactional
  public void delete(Long id) {
    if (id == null) throw new IllegalArgumentException("id é obrigatório.");
    int rows = jdbcTemplate.update("DELETE FROM public.quarto WHERE id = ?", id);
    if (rows == 0) throw new NotFoundException("Quarto não encontrado para o id: " + id);
  }

  private void validar(Quarto q) {
    if (q.descricao() == null || q.descricao().isBlank())
      throw new IllegalArgumentException("descricao é obrigatória.");
    if (q.quantidade_pessoas() != null && q.quantidade_pessoas() <= 0)
      throw new IllegalArgumentException("qtd_pessoas deve ser maior que 0.");
  }

  private void setIntOrNull(PreparedStatement ps, int idx, Integer v) throws SQLException {
    if (v == null) ps.setNull(idx, Types.INTEGER);
    else ps.setInt(idx, v);
  }

  private void setStringOrNull(PreparedStatement ps, int idx, String v) throws SQLException {
    if (v == null || v.isBlank()) ps.setNull(idx, Types.VARCHAR);
    else ps.setString(idx, v);
  }
}
