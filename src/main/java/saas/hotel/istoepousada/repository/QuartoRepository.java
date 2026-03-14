package saas.hotel.istoepousada.repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
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

  private static final ResultSetExtractor<List<Quarto>> QUARTO_EXTRACTOR =
      rs -> {
        List<Quarto> list = new ArrayList<>();
        int rowNum = 0;
        while (rs.next()) {
          list.add(Quarto.ROW_MAPPER.mapRow(rs, rowNum++));
        }
        return list;
      };

  public Page<Quarto> buscar(Long id, String termo, Quarto.Status status, Pageable pageable) {
    String baseFrom = " FROM public.quarto quarto ";

    String baseSelect =
        """
            SELECT
              quarto.id                         AS quarto_id,
              quarto.descricao                  AS quarto_descricao,
              quarto.quantidade_pessoa          AS quarto_quantidade_pessoas,
              quarto.status                     AS quarto_status,
              quarto.quantidade_cama_casal      AS quarto_quantidade_cama_casal,
              quarto.quantidade_cama_solteiro   AS quarto_quantidade_cama_solteiro,
              quarto.quantidade_rede            AS quarto_quantidade_rede,
              quarto.quantidade_beliche         AS quarto_quantidade_beliche
            FROM public.quarto quarto
            """;

    StringBuilder where = new StringBuilder(" WHERE 1 = 1 ");
    List<Object> params = new ArrayList<>();

    if (id != null) {
      where.append(" AND quarto.uuid = ? ");
      params.add(id);
    }

    if (termo != null && !termo.isBlank()) {
      String t = termo.trim();
      boolean isNumeric = t.matches("\\d+");

      where.append(" AND (quarto.descricao ILIKE ? ");
      params.add("%" + t + "%");

      if (isNumeric) {
        where.append(" OR quarto.uuid = ? ");
        params.add(Long.parseLong(t));
      }

      where.append(") ");
    }

    if (status != null) {
      where.append(" AND quarto.status = CAST(? AS public.status_quarto_enum) ");
      params.add(status.name());
    }

    Long total;
    try {
      total =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*)" + baseFrom + where, Long.class, params.toArray());
    } catch (EmptyResultDataAccessException ex) {
      total = 0L;
    }

    if (total == null || total == 0) {
      return new PageImpl<>(List.of(), pageable, 0);
    }

    String idsSql =
        "SELECT quarto.id AS id"
            + baseFrom
            + where
            + """
            ORDER BY quarto.descricao ASC NULLS LAST, quarto.id ASC
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
    if (id == null) {
      throw new IllegalArgumentException("Id é obrigatório.");
    }

    try {
      String sql =
          """
              SELECT
                quarto.id                         AS quarto_id,
                quarto.descricao                  AS quarto_descricao,
                quarto.quantidade_pessoa          AS quarto_quantidade_pessoas,
                quarto.status                     AS quarto_status,
                quarto.quantidade_cama_casal      AS quarto_quantidade_cama_casal,
                quarto.quantidade_cama_solteiro   AS quarto_quantidade_cama_solteiro,
                quarto.quantidade_rede            AS quarto_quantidade_rede,
                quarto.quantidade_beliche         AS quarto_quantidade_beliche
              FROM public.quarto quarto
              WHERE quarto.id = ?
              """;

      return jdbcTemplate.queryForObject(sql, Quarto.ROW_MAPPER, id);
    } catch (EmptyResultDataAccessException ex) {
      throw new NotFoundException("Quarto não encontrado para o uuid: " + id);
    }
  }

  @Transactional
  public Quarto insert(Quarto.Request quarto) {
    var quarto_id =
        jdbcTemplate.queryForObject(
            """
            INSERT INTO public.quarto (
              descricao,
              quantidade_pessoa,
              status,
              quantidade_cama_casal,
              quantidade_cama_solteiro,
              quantidade_rede,
              quantidade_beliche
            ) VALUES (?, ?, 'DISPONIVEL'::status_quarto_enum, ?, ?, ?, ?)
            returning id
            """,
            Long.class,
            quarto.descricao(),
            quarto.quantidade_pessoas(),
            quarto.quantidade_cama_casal(),
            quarto.quantidade_cama_solteiro(),
            quarto.quantidade_rede(),
            quarto.quantidade_beliche());

    vincularCategoriaAtiva(quarto_id, quarto.categoria().id());
    return findByIdOrThrow(quarto_id);
  }

  @Transactional
  public Quarto update(Quarto.Update request) {
    findByIdOrThrow(request.id());

    String sql =
        """
            UPDATE public.quarto SET
              descricao = ?,
              quantidade_pessoa = ?,
              status = CAST(? AS public.status_quarto_enum),
              quantidade_cama_casal = ?,
              quantidade_cama_solteiro = ?,
              quantidade_rede = ?,
              quantidade_beliche = ?
            WHERE id = ?
            """;

    int rows =
        jdbcTemplate.update(
            sql,
            request.descricao().trim(),
            request.quantidade_pessoas(),
            request.status().name(),
            request.quantidade_cama_casal(),
            request.quantidade_cama_solteiro(),
            request.quantidade_rede(),
            request.quantidade_beliche(),
            request.id());

    if (rows == 0) {
      throw new NotFoundException("Quarto não encontrado para o uuid: " + request.id());
    }

    atualizarCategoriaAtiva(request.id(), request.categoria().id());

    return findByIdOrThrow(request.id());
  }

  private void vincularCategoriaAtiva(Long quartoId, Long categoriaId) {
    jdbcTemplate.update(
        """
            INSERT INTO public.quarto_categoria (fk_quarto, fk_categoria, ativo)
            VALUES (?, ?, true)
            """,
        quartoId,
        categoriaId);
  }

  private void atualizarCategoriaAtiva(Long quartoId, Long categoriaId) {
    jdbcTemplate.update(
        """
            UPDATE public.quarto_categoria
            SET ativo = false
            WHERE fk_quarto = ?
            """,
        quartoId);

    Integer existenteAtivo =
        jdbcTemplate.queryForObject(
            """
                    SELECT COUNT(*)
                    FROM public.quarto_categoria
                    WHERE fk_quarto = ? AND fk_categoria = ?
                    """,
            Integer.class,
            quartoId,
            categoriaId);

    if (existenteAtivo > 0) {
      jdbcTemplate.update(
          """
              UPDATE public.quarto_categoria
              SET ativo = true
              WHERE fk_quarto = ? AND fk_categoria = ?
              """,
          quartoId,
          categoriaId);
      return;
    }

    jdbcTemplate.update(
        """
            INSERT INTO public.quarto_categoria (fk_quarto, fk_categoria, ativo)
            VALUES (?, ?, true)
            """,
        quartoId,
        categoriaId);
  }

  private void setIntOrNull(PreparedStatement ps, int idx, Integer value) throws SQLException {
    if (value == null) {
      ps.setNull(idx, Types.INTEGER);
    } else {
      ps.setInt(idx, value);
    }
  }
}
