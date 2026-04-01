package saas.hotel.istoepousada.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.Categoria;
import saas.hotel.istoepousada.dto.Funcionario;
import saas.hotel.istoepousada.dto.Sazonalidade;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;

@Repository
public class SazonabilidadeRepository {

  private final JdbcTemplate jdbcTemplate;
  private final PessoaRepository pessoaRepository;

  public SazonabilidadeRepository(JdbcTemplate jdbcTemplate, PessoaRepository pessoaRepository) {
    this.jdbcTemplate = jdbcTemplate;
    this.pessoaRepository = pessoaRepository;
  }

  // ── Busca ─────────────────────────────────────────────────────────────────

  public Page<Sazonalidade> buscar(Long id, String termo, Pageable pageable) {
    String baseFrom = " FROM public.sazonalidade s ";
    StringBuilder where = new StringBuilder(" WHERE 1 = 1 ");
    List<Object> params = new ArrayList<>();

    if (id != null) {
      where.append(" AND s.id = ? ");
      params.add(id);
    }
    if (termo != null && !termo.isBlank()) {
      String t = termo.trim();
      boolean isNumeric = t.matches("\\d+");
      where.append(" AND (s.descricao ILIKE ? ");
      params.add("%" + t + "%");
      if (isNumeric) {
        where.append(" OR s.id = ? ");
        params.add(Long.parseLong(t));
      }
      where.append(") ");
    }

    Long total;
    try {
      total = jdbcTemplate.queryForObject(
          "SELECT COUNT(*)" + baseFrom + where, Long.class, params.toArray());
    } catch (EmptyResultDataAccessException ex) {
      total = 0L;
    }

    if (total == null || total == 0) return new PageImpl<>(List.of(), pageable, 0);

    String idsSql =
        "SELECT s.id AS id" + baseFrom + where
            + " ORDER BY s.descricao ASC NULLS LAST, s.id ASC LIMIT ? OFFSET ?";

    List<Object> idsParams = new ArrayList<>(params);
    idsParams.add(pageable.getPageSize());
    idsParams.add(pageable.getOffset());

    List<Long> ids =
        jdbcTemplate.query(idsSql, (rs, rowNum) -> rs.getLong("id"), idsParams.toArray());

    if (ids.isEmpty()) return new PageImpl<>(List.of(), pageable, total);

    String in = String.join(",", Collections.nCopies(ids.size(), "?"));
    List<Sazonalidade> bases = jdbcTemplate.query(buildBaseSql(in), Sazonalidade.ROW_MAPPER, ids.toArray());
    List<Sazonalidade> content = enriquecerLista(bases, ids);

    return new PageImpl<>(Objects.requireNonNull(content), pageable, total);
  }

  public Sazonalidade findByIdOrThrow(Long id) {
    Sazonalidade base;
    try {
      base = jdbcTemplate.queryForObject(buildBaseSql("?"), Sazonalidade.ROW_MAPPER, id);
    } catch (EmptyResultDataAccessException ex) {
      throw new NotFoundException("Sazonalidade não encontrada para o id: " + id);
    }
    return enriquecerUm(Objects.requireNonNull(base));
  }

  // ── Insert ────────────────────────────────────────────────────────────────

  @Transactional
  public Sazonalidade insert(Sazonalidade.Request request) {
    java.sql.Array semanalArr = toSqlArray(request.semanal());
    java.sql.Array mensalArr  = toSqlArray(request.mensal());
    java.sql.Array anualArr   = toSqlArray(request.anual());

    Long id = jdbcTemplate.queryForObject(
        """
        INSERT INTO public.sazonalidade
          (descricao, data_inicio, data_fim,
           diario_hora_inicio_ciclo, diario_hora_fim_ciclo,
           semanal, mensal, anual, hora_checkin, hora_checkout)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        RETURNING id
        """,
        Long.class,
        request.descricao().trim(),
        request.data_inicio(), request.data_fim(),
        request.diario_hora_inicio_ciclo(), request.diario_hora_fim_ciclo(),
        semanalArr, mensalArr, anualArr,
        request.hora_checkin(), request.hora_checkout());

    if (request.fk_categorias() != null && !request.fk_categorias().isEmpty()) {
      vincularCategorias(id, request.fk_categorias());
    }

    return findByIdOrThrow(id);
  }

  // ── Update ────────────────────────────────────────────────────────────────

  @Transactional
  public Sazonalidade update(Sazonalidade.Update request) {
    findByIdOrThrow(request.id());

    java.sql.Array semanalArr = toSqlArray(request.semanal());
    java.sql.Array mensalArr  = toSqlArray(request.mensal());
    java.sql.Array anualArr   = toSqlArray(request.anual());

    int rows = jdbcTemplate.update(
        """
        UPDATE public.sazonalidade SET
          descricao                = ?,
          data_inicio              = ?,
          data_fim                 = ?,
          diario_hora_inicio_ciclo = ?,
          diario_hora_fim_ciclo    = ?,
          semanal                  = ?,
          mensal                   = ?,
          anual                    = ?,
          hora_checkin             = ?,
          hora_checkout            = ?
        WHERE id = ?
        """,
        request.descricao().trim(),
        request.data_inicio(), request.data_fim(),
        request.diario_hora_inicio_ciclo(), request.diario_hora_fim_ciclo(),
        semanalArr, mensalArr, anualArr,
        request.hora_checkin(), request.hora_checkout(),
        request.id());

    if (rows == 0) throw new NotFoundException("Sazonalidade não encontrada para o id: " + request.id());

    // fk_categorias null = manter vínculos existentes; lista vazia = remover todos
    if (request.fk_categorias() != null) {
      jdbcTemplate.update(
          "DELETE FROM public.categoria_sazonalidade WHERE fk_sazonalidade = ?", request.id());
      if (!request.fk_categorias().isEmpty()) {
        vincularCategorias(request.id(), request.fk_categorias());
      }
    }

    return findByIdOrThrow(request.id());
  }

  // ── Vínculo com categoria ─────────────────────────────────────────────────

  @Transactional
  public void vincular(Long fkSazonalidade, Long fkCategoria) {
    boolean jaExiste = Boolean.TRUE.equals(jdbcTemplate.queryForObject(
        "SELECT EXISTS (SELECT 1 FROM public.categoria_sazonalidade WHERE fk_sazonalidade = ? AND fk_categoria = ?)",
        Boolean.class, fkSazonalidade, fkCategoria));
    if (jaExiste) throw new IllegalArgumentException(
        "Esta sazonalidade já está vinculada à categoria informada.");

    jdbcTemplate.update(
        """
        INSERT INTO public.categoria_sazonalidade
          (fk_sazonalidade, fk_categoria, fk_funcionario, data_hora_cadastro, ativo)
        VALUES (?, ?, ?, now(), true)
        """,
        fkSazonalidade, fkCategoria, getFuncionarioId());
  }

  public void toggleAtivo(Long vinculoId, Boolean ativo) {
    int rows = jdbcTemplate.update(
        "UPDATE public.categoria_sazonalidade SET ativo = ? WHERE id = ?", ativo, vinculoId);
    if (rows == 0) throw new NotFoundException("Vínculo não encontrado para o id: " + vinculoId);
  }

  @Transactional
  public void removerVinculo(Long vinculoId) {
    int rows = jdbcTemplate.update(
        "DELETE FROM public.categoria_sazonalidade WHERE id = ?", vinculoId);
    if (rows == 0) throw new NotFoundException("Vínculo não encontrado para o id: " + vinculoId);
  }

  // ── Consulta para verificação de conflito (service) ───────────────────────

  public List<Sazonalidade> buscarAtivosParaCategoria(Long fkCategoria, Long excludeSazonId) {
    String base =
        """
        SELECT
          s.id                       AS sazonalidade_id,
          s.descricao                AS sazonalidade_descricao,
          s.data_inicio              AS sazonalidade_data_inicio,
          s.data_fim                 AS sazonalidade_data_fim,
          s.diario_hora_inicio_ciclo AS sazonalidade_diario_hora_inicio,
          s.diario_hora_fim_ciclo    AS sazonalidade_diario_hora_fim,
          s.semanal                  AS sazonalidade_semanal,
          s.mensal                   AS sazonalidade_mensal,
          s.anual                    AS sazonalidade_anual,
          s.hora_checkin             AS sazonalidade_hora_checkin,
          s.hora_checkout            AS sazonalidade_hora_checkout
        FROM public.categoria_sazonalidade cs
        JOIN public.sazonalidade s ON s.id = cs.fk_sazonalidade
        WHERE cs.fk_categoria = ? AND cs.ativo = true
        """;

    if (excludeSazonId != null) {
      return jdbcTemplate.query(
          base + " AND s.id != ?", Sazonalidade.ROW_MAPPER, fkCategoria, excludeSazonId);
    }
    return jdbcTemplate.query(base, Sazonalidade.ROW_MAPPER, fkCategoria);
  }

  // ── Helpers de enriquecimento ─────────────────────────────────────────────

  private String buildBaseSql(String in) {
    return """
        SELECT
          s.id                       AS sazonalidade_id,
          s.descricao                AS sazonalidade_descricao,
          s.data_inicio              AS sazonalidade_data_inicio,
          s.data_fim                 AS sazonalidade_data_fim,
          s.diario_hora_inicio_ciclo AS sazonalidade_diario_hora_inicio,
          s.diario_hora_fim_ciclo    AS sazonalidade_diario_hora_fim,
          s.semanal                  AS sazonalidade_semanal,
          s.mensal                   AS sazonalidade_mensal,
          s.anual                    AS sazonalidade_anual,
          s.hora_checkin             AS sazonalidade_hora_checkin,
          s.hora_checkout            AS sazonalidade_hora_checkout
        FROM public.sazonalidade s
        WHERE s.id IN (%s)
        ORDER BY s.descricao ASC NULLS LAST, s.id ASC
        """.formatted(in);
  }

  private List<Sazonalidade> enriquecerLista(List<Sazonalidade> bases, List<Long> ids) {
    if (bases == null || bases.isEmpty()) return List.of();
    String in = String.join(",", Collections.nCopies(ids.size(), "?"));
    Map<Long, List<Sazonalidade.CategoriaVinculo>> vinculoMap = carregarVinculos(in, ids.toArray());
    List<Sazonalidade> result = new ArrayList<>();
    for (Sazonalidade base : bases) {
      result.add(comVinculos(base, vinculoMap.getOrDefault(base.id(), List.of())));
    }
    return result;
  }

  private Sazonalidade enriquecerUm(Sazonalidade base) {
    Map<Long, List<Sazonalidade.CategoriaVinculo>> vinculoMap =
        carregarVinculos("?", new Object[]{base.id()});
    return comVinculos(base, vinculoMap.getOrDefault(base.id(), List.of()));
  }

  private Sazonalidade comVinculos(Sazonalidade base, List<Sazonalidade.CategoriaVinculo> vinculos) {
    return new Sazonalidade(
        base.id(), base.descricao(),
        base.data_inicio(), base.data_fim(),
        base.diario_hora_inicio_ciclo(), base.diario_hora_fim_ciclo(),
        base.semanal(), base.mensal(), base.anual(),
        base.hora_checkin(), base.hora_checkout(),
        vinculos);
  }

  private Map<Long, List<Sazonalidade.CategoriaVinculo>> carregarVinculos(String in, Object[] ids) {
    String sql =
        """
        SELECT
          cs.id                  AS vinculo_id,
          cs.fk_sazonalidade     AS vinculo_fk_sazonalidade,
          cs.ativo               AS vinculo_ativo,
          cs.data_hora_cadastro  AS vinculo_data_hora_cadastro,
          c.id                   AS categoria_id,
          c.nome                 AS categoria_nome,
          f.id                   AS funcionario_id,
          p.nome                 AS funcionario_nome
        FROM public.categoria_sazonalidade cs
        JOIN public.categoria c ON c.id = cs.fk_categoria
        LEFT JOIN public.funcionario f ON f.id = cs.fk_funcionario
        LEFT JOIN public.pessoa p ON p.id = f.fk_pessoa
        WHERE cs.fk_sazonalidade IN (%s)
        ORDER BY cs.fk_sazonalidade, c.nome
        """.formatted(in);

    Map<Long, List<Sazonalidade.CategoriaVinculo>> map = new LinkedHashMap<>();
    jdbcTemplate.query(sql, rs -> {
      Long sazonId = rs.getLong("vinculo_fk_sazonalidade");
      Long funcId = rs.getObject("funcionario_id", Long.class);
      Funcionario.Nome funcionario = funcId == null
          ? null : new Funcionario.Nome(funcId, rs.getString("funcionario_nome"));
      map.computeIfAbsent(sazonId, k -> new ArrayList<>()).add(
          new Sazonalidade.CategoriaVinculo(
              rs.getLong("vinculo_id"),
              new Categoria.Nome(rs.getLong("categoria_id"), rs.getString("categoria_nome")),
              rs.getBoolean("vinculo_ativo"),
              funcionario,
              rs.getObject("vinculo_data_hora_cadastro", LocalDateTime.class)));
    }, ids);
    return map;
  }

  private void vincularCategorias(Long sazonId, List<Long> categoriaIds) {
    Long fkFunc = getFuncionarioId();
    for (Long catId : categoriaIds) {
      jdbcTemplate.update(
          """
          INSERT INTO public.categoria_sazonalidade
            (fk_sazonalidade, fk_categoria, fk_funcionario, data_hora_cadastro, ativo)
          VALUES (?, ?, ?, now(), true)
          """,
          sazonId, catId, fkFunc);
    }
  }

  private java.sql.Array toSqlArray(List<Integer> list) {
    if (list == null || list.isEmpty()) return null;
    return jdbcTemplate.execute(
        (java.sql.Connection con) -> con.createArrayOf("integer", list.toArray()));
  }

  private Long getFuncionarioId() {
    return pessoaRepository.getFuncionarioIdFromRequest();
  }
}
