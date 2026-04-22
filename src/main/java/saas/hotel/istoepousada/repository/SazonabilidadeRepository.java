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
      total =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*)" + baseFrom + where, Long.class, params.toArray());
    } catch (EmptyResultDataAccessException ex) {
      total = 0L;
    }

    if (total == null || total == 0) return new PageImpl<>(List.of(), pageable, 0);

    String idsSql =
        "SELECT s.id AS id"
            + baseFrom
            + where
            + " ORDER BY s.descricao ASC NULLS LAST, s.id ASC LIMIT ? OFFSET ?";

    List<Object> idsParams = new ArrayList<>(params);
    idsParams.add(pageable.getPageSize());
    idsParams.add(pageable.getOffset());

    List<Long> ids =
        jdbcTemplate.query(idsSql, (rs, rowNum) -> rs.getLong("id"), idsParams.toArray());

    if (ids.isEmpty()) return new PageImpl<>(List.of(), pageable, total);

    String in = String.join(",", Collections.nCopies(ids.size(), "?"));
    List<Sazonalidade> bases =
        jdbcTemplate.query(buildBaseSql(in), Sazonalidade.ROW_MAPPER, ids.toArray());
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
    java.sql.Array mensalArr = toSqlArray(request.mensal());
    java.sql.Array anualArr = toSqlArray(request.anual());

    Long id =
        jdbcTemplate.queryForObject(
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
            request.data_inicio(),
            request.data_fim(),
            request.diario_hora_inicio_ciclo(),
            request.diario_hora_fim_ciclo(),
            semanalArr,
            mensalArr,
            anualArr,
            request.hora_checkin(),
            request.hora_checkout());

    if (request.fk_categorias() != null && !request.fk_categorias().isEmpty()) {
      vincularCategorias(id, request.fk_categorias());
    }

    if (request.modelos_ocupacao() != null && !request.modelos_ocupacao().isEmpty()) {
      salvarModelosOcupacao(id, request.modelos_ocupacao());
    }

    if (request.modelos_fixo() != null && !request.modelos_fixo().isEmpty()) {
      salvarModelosFixo(id, request.modelos_fixo());
    }

    if (request.day_use() != null && !request.day_use().isEmpty()) {
      salvarDayUse(id, request.day_use());
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
    java.sql.Array mensalArr = toSqlArray(request.mensal());
    java.sql.Array anualArr = toSqlArray(request.anual());

    int rows =
        jdbcTemplate.update(
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
            request.data_inicio(),
            request.data_fim(),
            request.diario_hora_inicio_ciclo(),
            request.diario_hora_fim_ciclo(),
            semanalArr,
            mensalArr,
            anualArr,
            request.hora_checkin(),
            request.hora_checkout(),
            request.id());

    if (rows == 0)
      throw new NotFoundException("Sazonalidade não encontrada para o id: " + request.id());

    // fk_categorias null = manter vínculos existentes; lista vazia = remover todos
    if (request.fk_categorias() != null) {
      jdbcTemplate.update(
          "DELETE FROM public.categoria_sazonalidade WHERE fk_sazonalidade = ?", request.id());
      if (!request.fk_categorias().isEmpty()) {
        vincularCategorias(request.id(), request.fk_categorias());
      }
    }

    // modelos/day_use null = manter existentes; lista vazia = remover todos
    if (request.modelos_ocupacao() != null
        || request.modelos_fixo() != null
        || request.day_use() != null) {
      deletarModelosEDayUse(request.id());
      if (request.modelos_ocupacao() != null && !request.modelos_ocupacao().isEmpty()) {
        salvarModelosOcupacao(request.id(), request.modelos_ocupacao());
      }
      if (request.modelos_fixo() != null && !request.modelos_fixo().isEmpty()) {
        salvarModelosFixo(request.id(), request.modelos_fixo());
      }
      if (request.day_use() != null && !request.day_use().isEmpty()) {
        salvarDayUse(request.id(), request.day_use());
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
    boolean jaExiste =
        Boolean.TRUE.equals(
            jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM public.categoria_sazonalidade WHERE fk_sazonalidade = ? AND fk_categoria = ?)",
                Boolean.class,
                fkSazonalidade,
                fkCategoria));
    if (jaExiste)
      throw new IllegalArgumentException(
          "Esta sazonalidade já está vinculada à categoria informada.");

    jdbcTemplate.update(
        """
        INSERT INTO public.categoria_sazonalidade
          (fk_sazonalidade, fk_categoria, fk_funcionario, data_hora_cadastro, ativo)
        VALUES (?, ?, ?, now(), true)
        """,
        fkSazonalidade,
        fkCategoria,
        getFuncionarioId());
  }

  public void toggleAtivo(Long vinculoId, Boolean ativo) {
    int rows =
        jdbcTemplate.update(
            "UPDATE public.categoria_sazonalidade SET ativo = ? WHERE id = ?", ativo, vinculoId);
    if (rows == 0) throw new NotFoundException("Vínculo não encontrado para o id: " + vinculoId);
  }

  @Transactional
  public void removerVinculo(Long vinculoId) {
    int rows =
        jdbcTemplate.update("DELETE FROM public.categoria_sazonalidade WHERE id = ?", vinculoId);
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
        """
        .formatted(in);
  }

  private List<Sazonalidade> enriquecerLista(List<Sazonalidade> bases, List<Long> ids) {
    if (bases == null || bases.isEmpty()) return List.of();
    String in = String.join(",", Collections.nCopies(ids.size(), "?"));
    Object[] idsArr = ids.toArray();
    Map<Long, List<Sazonalidade.CategoriaVinculo>> vinculoMap = carregarVinculos(in, idsArr);
    Map<Long, List<Categoria.ModeloOcupacao>> ocupacaoMap = carregarModelosOcupacao(in, idsArr);
    Map<Long, List<Categoria.ModeloFixo>> fixoMap = carregarModelosFixo(in, idsArr);
    Map<Long, List<Categoria.DayUseOperacao>> dayUseMap = carregarDayUse(in, idsArr);
    Map<Long, List<Categoria.MenorIdade>> menoresMap = carregarMenoresIdade(in, idsArr);
    List<Sazonalidade> result = new ArrayList<>();
    for (Sazonalidade base : bases) {
      result.add(
          comVinculos(
              base,
              vinculoMap.getOrDefault(base.id(), List.of()),
              ocupacaoMap.getOrDefault(base.id(), List.of()),
              fixoMap.getOrDefault(base.id(), List.of()),
              dayUseMap.getOrDefault(base.id(), List.of()),
              menoresMap.getOrDefault(base.id(), List.of())));
    }
    return result;
  }

  private Sazonalidade enriquecerUm(Sazonalidade base) {
    Object[] idArr = new Object[] {base.id()};
    Map<Long, List<Sazonalidade.CategoriaVinculo>> vinculoMap = carregarVinculos("?", idArr);
    Map<Long, List<Categoria.ModeloOcupacao>> ocupacaoMap = carregarModelosOcupacao("?", idArr);
    Map<Long, List<Categoria.ModeloFixo>> fixoMap = carregarModelosFixo("?", idArr);
    Map<Long, List<Categoria.DayUseOperacao>> dayUseMap = carregarDayUse("?", idArr);
    Map<Long, List<Categoria.MenorIdade>> menoresMap = carregarMenoresIdade("?", idArr);
    return comVinculos(
        base,
        vinculoMap.getOrDefault(base.id(), List.of()),
        ocupacaoMap.getOrDefault(base.id(), List.of()),
        fixoMap.getOrDefault(base.id(), List.of()),
        dayUseMap.getOrDefault(base.id(), List.of()),
        menoresMap.getOrDefault(base.id(), List.of()));
  }

  private Sazonalidade comVinculos(
      Sazonalidade base,
      List<Sazonalidade.CategoriaVinculo> vinculos,
      List<Categoria.ModeloOcupacao> modelosOcupacao,
      List<Categoria.ModeloFixo> modelosFixo,
      List<Categoria.DayUseOperacao> dayUse,
      List<Categoria.MenorIdade> menoresIdade) {
    return new Sazonalidade(
        base.id(),
        base.descricao(),
        base.data_inicio(),
        base.data_fim(),
        base.diario_hora_inicio_ciclo(),
        base.diario_hora_fim_ciclo(),
        base.semanal(),
        base.mensal(),
        base.anual(),
        base.hora_checkin(),
        base.hora_checkout(),
        vinculos,
        modelosOcupacao,
        modelosFixo,
        dayUse,
        menoresIdade);
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
        """
            .formatted(in);

    Map<Long, List<Sazonalidade.CategoriaVinculo>> map = new LinkedHashMap<>();
    jdbcTemplate.query(
        sql,
        rs -> {
          Long sazonId = rs.getLong("vinculo_fk_sazonalidade");
          Long funcId = rs.getObject("funcionario_id", Long.class);
          Funcionario.Nome funcionario =
              funcId == null
                  ? null
                  : new Funcionario.Nome(funcId, rs.getString("funcionario_nome"));
          map.computeIfAbsent(sazonId, k -> new ArrayList<>())
              .add(
                  new Sazonalidade.CategoriaVinculo(
                      rs.getLong("vinculo_id"),
                      new Categoria.Nome(
                          rs.getLong("categoria_id"), rs.getString("categoria_nome")),
                      rs.getBoolean("vinculo_ativo"),
                      funcionario,
                      rs.getObject("vinculo_data_hora_cadastro", LocalDateTime.class)));
        },
        ids);
    return map;
  }

  // ── Modelos de Ocupação / Fixo / Day Use ─────────────────────────────────

  private void salvarModelosOcupacao(Long sazonId, List<Categoria.ModeloOcupacao.Input> modelos) {
    Long fkFunc = getFuncionarioId();
    for (var mo : modelos) {
      jdbcTemplate.update(
          """
          INSERT INTO public.modelo_ocupacao (fk_categoria, fk_sazonalidade, fk_funcionario, data_hora_cadastro, quantidade, valor)
          VALUES (null, ?, ?, now(), ?, ?)
          """,
          sazonId,
          fkFunc,
          mo.quantidade(),
          mo.valor());
    }
  }

  private void salvarModelosFixo(Long sazonId, List<Categoria.ModeloFixo.Input> modelos) {
    Long fkFunc = getFuncionarioId();
    for (var mf : modelos) {
      jdbcTemplate.update(
          """
          INSERT INTO public.modelo_fixo (fk_categoria, fk_sazonalidade, fk_funcionario, data_hora_cadastro, valor)
          VALUES (null, ?, ?, now(), ?)
          """,
          sazonId,
          fkFunc,
          mf.valor());
    }
  }

  private void salvarDayUse(Long sazonId, List<Categoria.DayUseOperacao.Input> dayUse) {
    Long fkFunc = getFuncionarioId();
    for (var duo : dayUse) {
      Long operacaoId =
          jdbcTemplate.queryForObject(
              """
              INSERT INTO public.day_use_modelo_operacao (fk_categoria, fk_sazonalidade, fk_funcionario, data_hora_cadastro, ativo)
              VALUES (null, ?, ?, now(), ?)
              RETURNING id
              """,
              Long.class,
              sazonId,
              fkFunc,
              duo.ativo());

      if (duo.padrao() != null) {
        var p = duo.padrao();
        jdbcTemplate.update(
            """
            INSERT INTO public.day_use_modelo_padrao (fk_day_use_modelo_operacao, fk_sazonalidade, fk_funcionario, data_hora_cadastro, preco_base, hora_preco_base, valor_hora_adicional)
            VALUES (?, null, ?, now(), ?, ?, ?)
            """,
            operacaoId,
            fkFunc,
            p.preco_base(),
            p.hora_preco_base(),
            p.valor_hora_adicional());
      }

      if (duo.ocupacoes() != null) {
        for (var oc : duo.ocupacoes()) {
          Long ocupacaoId =
              jdbcTemplate.queryForObject(
                  """
                  INSERT INTO public.day_use_modelo_ocupacao (fk_day_use_modelo_operacao, fk_sazonalidade, fk_funcionario, data_hora_cadastro, quantidade_pessoa)
                  VALUES (?, null, ?, now(), ?)
                  RETURNING id
                  """,
                  Long.class,
                  operacaoId,
                  fkFunc,
                  oc.quantidade_pessoa());

          if (oc.quantidades() != null) {
            for (var q : oc.quantidades()) {
              jdbcTemplate.update(
                  """
                  INSERT INTO public.day_use_modelo_ocupacao_quantidade_pessoa (fk_day_use_modelo_ocupacao, fk_funcionario, data_hora_cadastro, quantidade, valor, valor_hora_adicional_por_pessoa)
                  VALUES (?, ?, now(), ?, ?, ?)
                  """,
                  ocupacaoId,
                  fkFunc,
                  q.quantidade(),
                  q.valor(),
                  q.valor_hora_adicional_por_pessoa());
            }
          }
        }
      }
    }
  }

  private void deletarModelosEDayUse(Long sazonId) {
    List<Long> operacaoIds =
        jdbcTemplate.query(
            "SELECT id FROM public.day_use_modelo_operacao WHERE fk_sazonalidade = ? AND fk_categoria IS NULL",
            (rs, rowNum) -> rs.getLong("id"),
            sazonId);

    if (!operacaoIds.isEmpty()) {
      String inOp = String.join(",", Collections.nCopies(operacaoIds.size(), "?"));
      Object[] opArr = operacaoIds.toArray();
      jdbcTemplate.update(
          "DELETE FROM public.day_use_modelo_padrao WHERE fk_day_use_modelo_operacao IN ("
              + inOp
              + ")",
          opArr);

      List<Long> ocupacaoIds =
          jdbcTemplate.query(
              "SELECT id FROM public.day_use_modelo_ocupacao WHERE fk_day_use_modelo_operacao IN ("
                  + inOp
                  + ")",
              (rs, rowNum) -> rs.getLong("id"),
              opArr);

      if (!ocupacaoIds.isEmpty()) {
        String inOc = String.join(",", Collections.nCopies(ocupacaoIds.size(), "?"));
        jdbcTemplate.update(
            "DELETE FROM public.day_use_modelo_ocupacao_quantidade_pessoa WHERE fk_day_use_modelo_ocupacao IN ("
                + inOc
                + ")",
            ocupacaoIds.toArray());
      }
      jdbcTemplate.update(
          "DELETE FROM public.day_use_modelo_ocupacao WHERE fk_day_use_modelo_operacao IN ("
              + inOp
              + ")",
          opArr);
    }
    jdbcTemplate.update(
        "DELETE FROM public.day_use_modelo_operacao WHERE fk_sazonalidade = ? AND fk_categoria IS NULL",
        sazonId);

    jdbcTemplate.update(
        "DELETE FROM public.modelo_ocupacao WHERE fk_sazonalidade = ? AND fk_categoria IS NULL",
        sazonId);
    jdbcTemplate.update(
        "DELETE FROM public.modelo_fixo WHERE fk_sazonalidade = ? AND fk_categoria IS NULL",
        sazonId);
  }

  private Map<Long, List<Categoria.ModeloOcupacao>> carregarModelosOcupacao(
      String in, Object[] ids) {
    String sql =
        """
        SELECT
          mo.id               AS mo_id,
          mo.fk_sazonalidade  AS mo_fk_sazonalidade,
          mo.quantidade       AS mo_quantidade,
          mo.valor            AS mo_valor
        FROM public.modelo_ocupacao mo
        WHERE mo.fk_sazonalidade IN (%s) AND mo.fk_categoria IS NULL
        ORDER BY mo.fk_sazonalidade, mo.id
        """
            .formatted(in);

    Map<Long, List<Categoria.ModeloOcupacao>> map = new LinkedHashMap<>();
    jdbcTemplate.query(
        sql,
        rs -> {
          Long sazonId = rs.getLong("mo_fk_sazonalidade");
          map.computeIfAbsent(sazonId, k -> new ArrayList<>())
              .add(
                  new Categoria.ModeloOcupacao(
                      rs.getLong("mo_id"),
                      null,
                      rs.getInt("mo_quantidade"),
                      rs.getDouble("mo_valor")));
        },
        ids);
    return map;
  }

  private Map<Long, List<Categoria.ModeloFixo>> carregarModelosFixo(String in, Object[] ids) {
    String sql =
        """
        SELECT
          mf.id               AS mf_id,
          mf.fk_sazonalidade  AS mf_fk_sazonalidade,
          mf.valor            AS mf_valor
        FROM public.modelo_fixo mf
        WHERE mf.fk_sazonalidade IN (%s) AND mf.fk_categoria IS NULL
        ORDER BY mf.fk_sazonalidade, mf.id
        """
            .formatted(in);

    Map<Long, List<Categoria.ModeloFixo>> map = new LinkedHashMap<>();
    jdbcTemplate.query(
        sql,
        rs -> {
          Long sazonId = rs.getLong("mf_fk_sazonalidade");
          map.computeIfAbsent(sazonId, k -> new ArrayList<>())
              .add(new Categoria.ModeloFixo(rs.getLong("mf_id"), null, rs.getDouble("mf_valor")));
        },
        ids);
    return map;
  }

  private Map<Long, List<Categoria.DayUseOperacao>> carregarDayUse(String in, Object[] ids) {
    String sql =
        """
        SELECT
          duo.id               AS duo_id,
          duo.fk_sazonalidade  AS duo_fk_sazonalidade,
          duo.ativo            AS duo_ativo
        FROM public.day_use_modelo_operacao duo
        WHERE duo.fk_sazonalidade IN (%s) AND duo.fk_categoria IS NULL
        ORDER BY duo.fk_sazonalidade, duo.id
        """
            .formatted(in);

    Map<Long, List<Categoria.DayUseOperacao>> operacoesMap = new LinkedHashMap<>();
    Map<Long, Long> operacaoSazonMap = new LinkedHashMap<>();

    jdbcTemplate.query(
        sql,
        rs -> {
          Long sazonId = rs.getLong("duo_fk_sazonalidade");
          Long operacaoId = rs.getLong("duo_id");
          operacaoSazonMap.put(operacaoId, sazonId);
          operacoesMap
              .computeIfAbsent(sazonId, k -> new ArrayList<>())
              .add(
                  new Categoria.DayUseOperacao(
                      operacaoId, null, rs.getBoolean("duo_ativo"), null, new ArrayList<>()));
        },
        ids);

    if (operacaoSazonMap.isEmpty()) return operacoesMap;

    String opIn = String.join(",", Collections.nCopies(operacaoSazonMap.size(), "?"));
    Object[] opIds = operacaoSazonMap.keySet().toArray();

    Map<Long, Categoria.DayUsePadrao> padraoMap = carregarDayUsePadroes(opIn, opIds);
    Map<Long, List<Categoria.DayUseOcupacao>> ocupacaoMap = carregarDayUseOcupacoesMap(opIn, opIds);

    for (List<Categoria.DayUseOperacao> lista : operacoesMap.values()) {
      lista.replaceAll(
          op ->
              new Categoria.DayUseOperacao(
                  op.id(),
                  op.sazonalidade(),
                  op.ativo(),
                  padraoMap.get(op.id()),
                  ocupacaoMap.getOrDefault(op.id(), List.of())));
    }

    return operacoesMap;
  }

  private Map<Long, Categoria.DayUsePadrao> carregarDayUsePadroes(String in, Object[] ids) {
    String sql =
        """
        SELECT
          dup.id                         AS dup_id,
          dup.fk_day_use_modelo_operacao AS dup_fk_operacao,
          dup.preco_base                 AS dup_preco_base,
          dup.hora_preco_base            AS dup_hora_preco_base,
          dup.valor_hora_adicional       AS dup_valor_hora_adicional
        FROM public.day_use_modelo_padrao dup
        WHERE dup.fk_day_use_modelo_operacao IN (%s)
        """
            .formatted(in);

    Map<Long, Categoria.DayUsePadrao> map = new LinkedHashMap<>();
    jdbcTemplate.query(
        sql,
        rs -> {
          Long fkOp = rs.getLong("dup_fk_operacao");
          map.put(fkOp, Categoria.DayUsePadrao.ROW_MAPPER.mapRow(rs, 0));
        },
        ids);
    return map;
  }

  private Map<Long, List<Categoria.DayUseOcupacao>> carregarDayUseOcupacoesMap(
      String in, Object[] ids) {
    String sql =
        """
        SELECT
          duo.id                         AS duo_id,
          duo.fk_day_use_modelo_operacao AS duo_fk_operacao,
          duo.quantidade_pessoa          AS duo_quantidade_pessoa,
          duop.id                        AS duop_id,
          duop.quantidade                AS duop_quantidade,
          duop.valor                     AS duop_valor,
          duop.valor_hora_adicional_por_pessoa AS duop_valor_hora_adicional_por_pessoa
        FROM public.day_use_modelo_ocupacao duo
        LEFT JOIN public.day_use_modelo_ocupacao_quantidade_pessoa duop ON duop.fk_day_use_modelo_ocupacao = duo.id
        WHERE duo.fk_day_use_modelo_operacao IN (%s)
        ORDER BY duo.id, duop.quantidade ASC
        """
            .formatted(in);

    Map<Long, List<Categoria.DayUseOcupacao>> byOperacao = new LinkedHashMap<>();
    Map<Long, Categoria.DayUseOcupacao> ocupacaoById = new LinkedHashMap<>();

    jdbcTemplate.query(
        sql,
        rs -> {
          Long fkOp = rs.getLong("duo_fk_operacao");
          Long ocupId = rs.getLong("duo_id");

          if (!ocupacaoById.containsKey(ocupId)) {
            Categoria.DayUseOcupacao oc =
                new Categoria.DayUseOcupacao(
                    ocupId, rs.getInt("duo_quantidade_pessoa"), new ArrayList<>());
            ocupacaoById.put(ocupId, oc);
            byOperacao.computeIfAbsent(fkOp, k -> new ArrayList<>()).add(oc);
          }

          Long pessoaId = rs.getObject("duop_id", Long.class);
          if (pessoaId != null) {
            ocupacaoById
                .get(ocupId)
                .quantidades()
                .add(
                    new Categoria.DayUseOcupacaoPessoa(
                        pessoaId,
                        rs.getInt("duop_quantidade"),
                        rs.getInt("duop_valor"),
                        rs.getObject("duop_valor_hora_adicional_por_pessoa", Integer.class)));
          }
        },
        ids);

    return byOperacao;
  }

  // ── Menor Idade ───────────────────────────────────────────────────────────

  private void salvarMenoresIdade(Long sazonId, List<Categoria.MenorIdade.Input> menoresIdade) {
    Long fkFunc = getFuncionarioId();
    for (var mi : menoresIdade) {
      Long menorId =
          jdbcTemplate.queryForObject(
              """
          INSERT INTO public.menor_idade (fk_funcionario, data_hora_cadastro, fk_categoria, fk_sazonalidade, idade_gratuidade)
          VALUES (?, now(), null, ?, ?)
          RETURNING id
          """,
              Long.class,
              fkFunc,
              sazonId,
              mi.idade_gratuidade());
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
                menorId,
                fkFunc,
                tf.idade_maxima(),
                tf.valor_por_crianca());
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
                menorId,
                fkFunc,
                tq.quantidade_crianca(),
                tq.valor());
          }
        }
      }
      case TAXA_POR_FAIXA_ETARIA -> {
        if (mi.faixas_etarias() != null) {
          for (var fe : mi.faixas_etarias()) {
            java.sql.Array faixaArray =
                jdbcTemplate.execute(
                    (java.sql.Connection con) ->
                        con.createArrayOf("int", fe.faixa_etaria().toArray()));
            jdbcTemplate.update(
                """
                INSERT INTO public.menor_idade_modelo_faixa_etaria (fk_menor_idade, fk_funcionario, data_hora_cadastro, faixa_etaria, valor)
                VALUES (?, ?, now(), ?, ?)
                """,
                menorId,
                fkFunc,
                faixaArray,
                fe.valor());
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
                menorId,
                fkFunc,
                pq.quantidade(),
                pq.porcentagem());
          }
        }
      }
    }
  }

  private void deletarMenoresIdade(Long sazonId) {
    List<Long> menorIds =
        jdbcTemplate.query(
            "SELECT id FROM public.menor_idade WHERE fk_sazonalidade = ?",
            (rs, rowNum) -> rs.getLong("id"),
            sazonId);
    if (!menorIds.isEmpty()) {
      String inMenor = String.join(",", Collections.nCopies(menorIds.size(), "?"));
      Object[] menorArr = menorIds.toArray();
      jdbcTemplate.update(
          "DELETE FROM public.menor_idade_modelo_taxa_adicional_fixa WHERE fk_menor_idade IN ("
              + inMenor
              + ")",
          menorArr);
      jdbcTemplate.update(
          "DELETE FROM public.menor_idade_modelo_taxa_adicional_por_quantidade WHERE fk_menor_idade IN ("
              + inMenor
              + ")",
          menorArr);
      jdbcTemplate.update(
          "DELETE FROM public.menor_idade_modelo_faixa_etaria WHERE fk_menor_idade IN ("
              + inMenor
              + ")",
          menorArr);
      jdbcTemplate.update(
          "DELETE FROM public.menor_idade_modelo_porcentagem_por_quantidade WHERE fk_menor_idade IN ("
              + inMenor
              + ")",
          menorArr);
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
        """
            .formatted(in);

    Map<Long, List<Categoria.MenorIdade>> map = new LinkedHashMap<>();
    Map<Long, Long> menorSazonMap = new LinkedHashMap<>();

    jdbcTemplate.query(
        sql,
        rs -> {
          Long sazonId = rs.getLong("mi_fk_sazonalidade");
          Long menorId = rs.getLong("mi_id");
          menorSazonMap.put(menorId, sazonId);
          map.computeIfAbsent(sazonId, k -> new ArrayList<>())
              .add(
                  new Categoria.MenorIdade(
                      menorId,
                      null,
                      rs.getObject("mi_idade_gratuidade", Integer.class),
                      null,
                      List.of(),
                      List.of(),
                      List.of(),
                      List.of()));
        },
        ids);

    if (menorSazonMap.isEmpty()) return map;

    String miIn = String.join(",", Collections.nCopies(menorSazonMap.size(), "?"));
    Object[] miIds = menorSazonMap.keySet().toArray();

    Map<Long, List<Categoria.MenorTaxaFixa>> taxasFixas = carregarMenoresTaxasFixas(miIn, miIds);
    Map<Long, List<Categoria.MenorTaxaPorQuantidade>> taxasQtd =
        carregarMenoresTaxasPorQuantidade(miIn, miIds);
    Map<Long, List<Categoria.MenorFaixaEtaria>> faixas = carregarMenoresFaixasEtarias(miIn, miIds);
    Map<Long, List<Categoria.MenorPorcentagemPorQuantidade>> porcentagens =
        carregarMenoresPorcentagens(miIn, miIds);

    for (List<Categoria.MenorIdade> lista : map.values()) {
      lista.replaceAll(
          mi -> {
            List<Categoria.MenorTaxaFixa> tf = taxasFixas.getOrDefault(mi.id(), List.of());
            List<Categoria.MenorTaxaPorQuantidade> tq = taxasQtd.getOrDefault(mi.id(), List.of());
            List<Categoria.MenorFaixaEtaria> fe = faixas.getOrDefault(mi.id(), List.of());
            List<Categoria.MenorPorcentagemPorQuantidade> pq =
                porcentagens.getOrDefault(mi.id(), List.of());
            return new Categoria.MenorIdade(
                mi.id(),
                null,
                mi.idade_gratuidade(),
                inferirModeloMenorIdade(tf, tq, fe, pq),
                tf,
                tq,
                fe,
                pq);
          });
    }

    return map;
  }

  private Map<Long, List<Categoria.MenorTaxaFixa>> carregarMenoresTaxasFixas(
      String in, Object[] ids) {
    String sql =
        """
        SELECT id AS mtf_id, fk_menor_idade AS mtf_fk, idade_maxima AS mtf_idade_maxima, valor_por_crianca AS mtf_valor_por_crianca
        FROM public.menor_idade_modelo_taxa_adicional_fixa WHERE fk_menor_idade IN (%s)
        ORDER BY fk_menor_idade, idade_maxima
        """
            .formatted(in);
    Map<Long, List<Categoria.MenorTaxaFixa>> map = new LinkedHashMap<>();
    jdbcTemplate.query(
        sql,
        rs -> {
          map.computeIfAbsent(rs.getLong("mtf_fk"), k -> new ArrayList<>())
              .add(Categoria.MenorTaxaFixa.ROW_MAPPER.mapRow(rs, 0));
        },
        ids);
    return map;
  }

  private Map<Long, List<Categoria.MenorTaxaPorQuantidade>> carregarMenoresTaxasPorQuantidade(
      String in, Object[] ids) {
    String sql =
        """
        SELECT id AS mtq_id, fk_menor_idade AS mtq_fk, quantidade_crianca AS mtq_quantidade_crianca, valor AS mtq_valor
        FROM public.menor_idade_modelo_taxa_adicional_por_quantidade WHERE fk_menor_idade IN (%s)
        ORDER BY fk_menor_idade, quantidade_crianca
        """
            .formatted(in);
    Map<Long, List<Categoria.MenorTaxaPorQuantidade>> map = new LinkedHashMap<>();
    jdbcTemplate.query(
        sql,
        rs -> {
          map.computeIfAbsent(rs.getLong("mtq_fk"), k -> new ArrayList<>())
              .add(Categoria.MenorTaxaPorQuantidade.ROW_MAPPER.mapRow(rs, 0));
        },
        ids);
    return map;
  }

  private Map<Long, List<Categoria.MenorFaixaEtaria>> carregarMenoresFaixasEtarias(
      String in, Object[] ids) {
    String sql =
        """
        SELECT id AS mfe_id, fk_menor_idade AS mfe_fk, faixa_etaria AS mfe_faixa_etaria, valor AS mfe_valor
        FROM public.menor_idade_modelo_faixa_etaria WHERE fk_menor_idade IN (%s)
        ORDER BY fk_menor_idade, id
        """
            .formatted(in);
    Map<Long, List<Categoria.MenorFaixaEtaria>> map = new LinkedHashMap<>();
    jdbcTemplate.query(
        sql,
        rs -> {
          map.computeIfAbsent(rs.getLong("mfe_fk"), k -> new ArrayList<>())
              .add(Categoria.MenorFaixaEtaria.ROW_MAPPER.mapRow(rs, 0));
        },
        ids);
    return map;
  }

  private Map<Long, List<Categoria.MenorPorcentagemPorQuantidade>> carregarMenoresPorcentagens(
      String in, Object[] ids) {
    String sql =
        """
        SELECT id AS mpq_id, fk_menor_idade AS mpq_fk, quantidade AS mpq_quantidade, porcentagem AS mpq_porcentagem
        FROM public.menor_idade_modelo_porcentagem_por_quantidade WHERE fk_menor_idade IN (%s)
        ORDER BY fk_menor_idade, quantidade
        """
            .formatted(in);
    Map<Long, List<Categoria.MenorPorcentagemPorQuantidade>> map = new LinkedHashMap<>();
    jdbcTemplate.query(
        sql,
        rs -> {
          map.computeIfAbsent(rs.getLong("mpq_fk"), k -> new ArrayList<>())
              .add(Categoria.MenorPorcentagemPorQuantidade.ROW_MAPPER.mapRow(rs, 0));
        },
        ids);
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
          sazonId,
          catId,
          fkFunc);
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
