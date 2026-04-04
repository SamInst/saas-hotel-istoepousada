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
import saas.hotel.istoepousada.dto.enums.ModeloMenorIdade;
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

    if (request.menores_idade() != null && !request.menores_idade().isEmpty()) {
      salvarMenoresIdade(id, request.menores_idade());
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

    // menores_idade null = manter existentes; lista vazia = remover todos
    if (request.menores_idade() != null) {
      deletarMenoresIdade(request.id());
      if (!request.menores_idade().isEmpty()) {
        salvarMenoresIdade(request.id(), request.menores_idade());
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
    Object[] idsArr = ids.toArray();
    Map<Long, List<Sazonalidade.CategoriaVinculo>> vinculoMap = carregarVinculos(in, idsArr);
    Map<Long, List<Categoria.MenorIdade>> menoresMap = carregarMenoresIdade(in, idsArr);
    List<Sazonalidade> result = new ArrayList<>();
    for (Sazonalidade base : bases) {
      result.add(comVinculos(base,
          vinculoMap.getOrDefault(base.id(), List.of()),
          menoresMap.getOrDefault(base.id(), List.of())));
    }
    return result;
  }

  private Sazonalidade enriquecerUm(Sazonalidade base) {
    Object[] idArr = new Object[]{base.id()};
    Map<Long, List<Sazonalidade.CategoriaVinculo>> vinculoMap = carregarVinculos("?", idArr);
    Map<Long, List<Categoria.MenorIdade>> menoresMap = carregarMenoresIdade("?", idArr);
    return comVinculos(base,
        vinculoMap.getOrDefault(base.id(), List.of()),
        menoresMap.getOrDefault(base.id(), List.of()));
  }

  private Sazonalidade comVinculos(Sazonalidade base, List<Sazonalidade.CategoriaVinculo> vinculos,
      List<Categoria.MenorIdade> menoresIdade) {
    return new Sazonalidade(
        base.id(), base.descricao(),
        base.data_inicio(), base.data_fim(),
        base.diario_hora_inicio_ciclo(), base.diario_hora_fim_ciclo(),
        base.semanal(), base.mensal(), base.anual(),
        base.hora_checkin(), base.hora_checkout(),
        vinculos, menoresIdade);
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

  // ── Menor Idade ───────────────────────────────────────────────────────────

  private void salvarMenoresIdade(Long sazonId, List<Categoria.MenorIdade.Input> menoresIdade) {
    Long fkFunc = getFuncionarioId();
    for (var mi : menoresIdade) {
      Long menorId = jdbcTemplate.queryForObject(
          """
          INSERT INTO public.menor_idade (fk_funcionario, data_hora_cadastro, fk_categoria, fk_sazonalidade, idade_gratuidade)
          VALUES (?, now(), null, ?, ?)
          RETURNING id
          """,
          Long.class,
          fkFunc, sazonId, mi.idade_gratuidade());
      salvarSubModeloMenorIdade(menorId, fkFunc, mi);
    }
  }

  private void salvarSubModeloMenorIdade(Long menorId, Long fkFunc, Categoria.MenorIdade.Input mi) {
    switch (mi.modelo()) {
      case TAXA_ADICIONAL_FIXA -> {
        if (mi.taxas_fixas() != null) {
          for (var tf : mi.taxas_fixas()) {
            jdbcTemplate.update(
                """
                INSERT INTO public.menor_idade_modelo_taxa_adicional_fixa (fk_menor_idade, fk_funcionario, data_hora_cadastro, idade_maxima, valor_por_crianca)
                VALUES (?, ?, now(), ?, ?)
                """,
                menorId, fkFunc, tf.idade_maxima(), tf.valor_por_crianca());
          }
        }
      }
      case TAXA_POR_QUANTIDADE -> {
        if (mi.taxas_por_quantidade() != null) {
          for (var tq : mi.taxas_por_quantidade()) {
            jdbcTemplate.update(
                """
                INSERT INTO public.menor_idade_modelo_taxa_adicional_por_quantidade (fk_menor_idade, fk_funcionario, data_hora_cadastro, quantidade_crianca, valor)
                VALUES (?, ?, now(), ?, ?)
                """,
                menorId, fkFunc, tq.quantidade_crianca(), tq.valor());
          }
        }
      }
      case TAXA_POR_FAIXA_ETARIA -> {
        if (mi.faixas_etarias() != null) {
          for (var fe : mi.faixas_etarias()) {
            java.sql.Array faixaArray = jdbcTemplate.execute(
                (java.sql.Connection con) -> con.createArrayOf("int", fe.faixa_etaria().toArray()));
            jdbcTemplate.update(
                """
                INSERT INTO public.menor_idade_modelo_faixa_etaria (fk_menor_idade, fk_funcionario, data_hora_cadastro, faixa_etaria, valor)
                VALUES (?, ?, now(), ?, ?)
                """,
                menorId, fkFunc, faixaArray, fe.valor());
          }
        }
      }
      case PORCENTAGEM_POR_QUANTIDADE -> {
        if (mi.porcentagens_por_quantidade() != null) {
          for (var pq : mi.porcentagens_por_quantidade()) {
            jdbcTemplate.update(
                """
                INSERT INTO public.menor_idade_modelo_porcentagem_por_quantidade (fk_menor_idade, fk_funcionario, data_hora_cadastro, quantidade, porcentagem)
                VALUES (?, ?, now(), ?, ?)
                """,
                menorId, fkFunc, pq.quantidade(), pq.porcentagem());
          }
        }
      }
    }
  }

  private void deletarMenoresIdade(Long sazonId) {
    List<Long> menorIds = jdbcTemplate.query(
        "SELECT id FROM public.menor_idade WHERE fk_sazonalidade = ?",
        (rs, rowNum) -> rs.getLong("id"),
        sazonId);
    if (!menorIds.isEmpty()) {
      String inMenor = String.join(",", Collections.nCopies(menorIds.size(), "?"));
      Object[] menorArr = menorIds.toArray();
      jdbcTemplate.update("DELETE FROM public.menor_idade_modelo_taxa_adicional_fixa WHERE fk_menor_idade IN (" + inMenor + ")", menorArr);
      jdbcTemplate.update("DELETE FROM public.menor_idade_modelo_taxa_adicional_por_quantidade WHERE fk_menor_idade IN (" + inMenor + ")", menorArr);
      jdbcTemplate.update("DELETE FROM public.menor_idade_modelo_faixa_etaria WHERE fk_menor_idade IN (" + inMenor + ")", menorArr);
      jdbcTemplate.update("DELETE FROM public.menor_idade_modelo_porcentagem_por_quantidade WHERE fk_menor_idade IN (" + inMenor + ")", menorArr);
    }
    jdbcTemplate.update("DELETE FROM public.menor_idade WHERE fk_sazonalidade = ?", sazonId);
  }

  private Map<Long, List<Categoria.MenorIdade>> carregarMenoresIdade(String in, Object[] ids) {
    String sql =
        """
        SELECT
          mi.id               AS mi_id,
          mi.fk_sazonalidade  AS mi_fk_sazonalidade,
          mi.idade_gratuidade AS mi_idade_gratuidade
        FROM public.menor_idade mi
        WHERE mi.fk_sazonalidade IN (%s)
        ORDER BY mi.fk_sazonalidade, mi.id
        """.formatted(in);

    Map<Long, List<Categoria.MenorIdade>> map = new LinkedHashMap<>();
    Map<Long, Long> menorSazonMap = new LinkedHashMap<>();

    jdbcTemplate.query(sql, rs -> {
      Long sazonId = rs.getLong("mi_fk_sazonalidade");
      Long menorId = rs.getLong("mi_id");
      menorSazonMap.put(menorId, sazonId);
      map.computeIfAbsent(sazonId, k -> new ArrayList<>())
          .add(new Categoria.MenorIdade(
              menorId, null,
              rs.getObject("mi_idade_gratuidade", Integer.class),
              null, List.of(), List.of(), List.of(), List.of()));
    }, ids);

    if (menorSazonMap.isEmpty()) return map;

    String miIn = String.join(",", Collections.nCopies(menorSazonMap.size(), "?"));
    Object[] miIds = menorSazonMap.keySet().toArray();

    Map<Long, List<Categoria.MenorTaxaFixa>> taxasFixas = carregarMenoresTaxasFixas(miIn, miIds);
    Map<Long, List<Categoria.MenorTaxaPorQuantidade>> taxasQtd = carregarMenoresTaxasPorQuantidade(miIn, miIds);
    Map<Long, List<Categoria.MenorFaixaEtaria>> faixas = carregarMenoresFaixasEtarias(miIn, miIds);
    Map<Long, List<Categoria.MenorPorcentagemPorQuantidade>> porcentagens = carregarMenoresPorcentagens(miIn, miIds);

    for (List<Categoria.MenorIdade> lista : map.values()) {
      lista.replaceAll(mi -> {
        List<Categoria.MenorTaxaFixa> tf = taxasFixas.getOrDefault(mi.id(), List.of());
        List<Categoria.MenorTaxaPorQuantidade> tq = taxasQtd.getOrDefault(mi.id(), List.of());
        List<Categoria.MenorFaixaEtaria> fe = faixas.getOrDefault(mi.id(), List.of());
        List<Categoria.MenorPorcentagemPorQuantidade> pq = porcentagens.getOrDefault(mi.id(), List.of());
        return new Categoria.MenorIdade(
            mi.id(), null, mi.idade_gratuidade(),
            inferirModeloMenorIdade(tf, tq, fe, pq), tf, tq, fe, pq);
      });
    }

    return map;
  }

  private Map<Long, List<Categoria.MenorTaxaFixa>> carregarMenoresTaxasFixas(String in, Object[] ids) {
    String sql =
        """
        SELECT id AS mtf_id, fk_menor_idade AS mtf_fk, idade_maxima AS mtf_idade_maxima, valor_por_crianca AS mtf_valor_por_crianca
        FROM public.menor_idade_modelo_taxa_adicional_fixa WHERE fk_menor_idade IN (%s)
        ORDER BY fk_menor_idade, idade_maxima
        """.formatted(in);
    Map<Long, List<Categoria.MenorTaxaFixa>> map = new LinkedHashMap<>();
    jdbcTemplate.query(sql, rs -> {
      map.computeIfAbsent(rs.getLong("mtf_fk"), k -> new ArrayList<>())
          .add(Categoria.MenorTaxaFixa.ROW_MAPPER.mapRow(rs, 0));
    }, ids);
    return map;
  }

  private Map<Long, List<Categoria.MenorTaxaPorQuantidade>> carregarMenoresTaxasPorQuantidade(String in, Object[] ids) {
    String sql =
        """
        SELECT id AS mtq_id, fk_menor_idade AS mtq_fk, quantidade_crianca AS mtq_quantidade_crianca, valor AS mtq_valor
        FROM public.menor_idade_modelo_taxa_adicional_por_quantidade WHERE fk_menor_idade IN (%s)
        ORDER BY fk_menor_idade, quantidade_crianca
        """.formatted(in);
    Map<Long, List<Categoria.MenorTaxaPorQuantidade>> map = new LinkedHashMap<>();
    jdbcTemplate.query(sql, rs -> {
      map.computeIfAbsent(rs.getLong("mtq_fk"), k -> new ArrayList<>())
          .add(Categoria.MenorTaxaPorQuantidade.ROW_MAPPER.mapRow(rs, 0));
    }, ids);
    return map;
  }

  private Map<Long, List<Categoria.MenorFaixaEtaria>> carregarMenoresFaixasEtarias(String in, Object[] ids) {
    String sql =
        """
        SELECT id AS mfe_id, fk_menor_idade AS mfe_fk, faixa_etaria AS mfe_faixa_etaria, valor AS mfe_valor
        FROM public.menor_idade_modelo_faixa_etaria WHERE fk_menor_idade IN (%s)
        ORDER BY fk_menor_idade, id
        """.formatted(in);
    Map<Long, List<Categoria.MenorFaixaEtaria>> map = new LinkedHashMap<>();
    jdbcTemplate.query(sql, rs -> {
      map.computeIfAbsent(rs.getLong("mfe_fk"), k -> new ArrayList<>())
          .add(Categoria.MenorFaixaEtaria.ROW_MAPPER.mapRow(rs, 0));
    }, ids);
    return map;
  }

  private Map<Long, List<Categoria.MenorPorcentagemPorQuantidade>> carregarMenoresPorcentagens(String in, Object[] ids) {
    String sql =
        """
        SELECT id AS mpq_id, fk_menor_idade AS mpq_fk, quantidade AS mpq_quantidade, porcentagem AS mpq_porcentagem
        FROM public.menor_idade_modelo_porcentagem_por_quantidade WHERE fk_menor_idade IN (%s)
        ORDER BY fk_menor_idade, quantidade
        """.formatted(in);
    Map<Long, List<Categoria.MenorPorcentagemPorQuantidade>> map = new LinkedHashMap<>();
    jdbcTemplate.query(sql, rs -> {
      map.computeIfAbsent(rs.getLong("mpq_fk"), k -> new ArrayList<>())
          .add(Categoria.MenorPorcentagemPorQuantidade.ROW_MAPPER.mapRow(rs, 0));
    }, ids);
    return map;
  }

  private ModeloMenorIdade inferirModeloMenorIdade(
      List<Categoria.MenorTaxaFixa> tf,
      List<Categoria.MenorTaxaPorQuantidade> tq,
      List<Categoria.MenorFaixaEtaria> fe,
      List<Categoria.MenorPorcentagemPorQuantidade> pq) {
    if (!tf.isEmpty()) return ModeloMenorIdade.TAXA_ADICIONAL_FIXA;
    if (!tq.isEmpty()) return ModeloMenorIdade.TAXA_POR_QUANTIDADE;
    if (!fe.isEmpty()) return ModeloMenorIdade.TAXA_POR_FAIXA_ETARIA;
    if (!pq.isEmpty()) return ModeloMenorIdade.PORCENTAGEM_POR_QUANTIDADE;
    return null;
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
