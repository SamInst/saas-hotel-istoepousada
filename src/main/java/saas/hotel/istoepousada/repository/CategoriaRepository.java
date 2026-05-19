package saas.hotel.istoepousada.repository;

import java.sql.Array;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;
import saas.hotel.istoepousada.dto.Categoria;
import saas.hotel.istoepousada.dto.CategoriaCheckin;
import saas.hotel.istoepousada.dto.Quarto;
import saas.hotel.istoepousada.dto.Sazonalidade;
import saas.hotel.istoepousada.dto.enums.ModeloMenorIdade;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;

@Slf4j
@Repository
public class CategoriaRepository {
  private final JdbcTemplate jdbcTemplate;

  public CategoriaRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  // ── Busca ─────────────────────────────────────────────────────────────────

  public Page<Categoria> buscar(Long id, String termo, Pageable pageable) {
    String baseFrom = " FROM public.categoria c ";

    StringBuilder where = new StringBuilder(" WHERE 1 = 1 ");
    List<Object> params = new ArrayList<>();

    if (id != null) {
      where.append(" AND c.id = ? ");
      params.add(id);
    }

    if (termo != null && !termo.isBlank()) {
      String t = termo.trim();
      boolean isNumeric = t.matches("\\d+");
      where.append(" AND (c.nome ILIKE ? ");
      params.add("%" + t + "%");
      if (isNumeric) {
        where.append(" OR c.id = ? ");
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

    if (total == 0) {
      return new PageImpl<>(List.of(), pageable, 0);
    }

    String idsSql =
        "SELECT c.id AS id"
            + baseFrom
            + where
            + " ORDER BY c.nome ASC NULLS LAST, c.id ASC LIMIT ? OFFSET ?";

    List<Object> idsParams = new ArrayList<>(params);
    idsParams.add(pageable.getPageSize());
    idsParams.add(pageable.getOffset());

    List<Long> ids =
        jdbcTemplate.query(idsSql, (rs, rowNum) -> rs.getLong("id"), idsParams.toArray());

    if (ids.isEmpty()) {
      return new PageImpl<>(List.of(), pageable, total);
    }

    String in = String.join(",", Collections.nCopies(ids.size(), "?"));
    String baseSql =
        """
                        SELECT
                          c.id                  AS categoria_id,
                          c.nome                AS categoria_nome,
                          c.descricao           AS categoria_descricao,
                          c.hora_checkin        AS categoria_hora_checkin,
                          c.hora_checkout       AS categoria_hora_checkout,
                          c.data_hora_cadastro  AS categoria_data_hora_cadastro,
                          f.id                  AS funcionario_id,
                          p.nome                AS funcionario_nome
                        FROM public.categoria c
                        LEFT JOIN public.funcionario f ON f.id = c.fk_funcionario
                        LEFT JOIN public.pessoa p ON p.id = f.fk_pessoa
                        WHERE c.id IN (%s)
                        ORDER BY c.nome ASC NULLS LAST, c.id ASC
                        """
            .formatted(in);

    List<Categoria> bases = jdbcTemplate.query(baseSql, Categoria.ROW_MAPPER, ids.toArray());
    List<Categoria> content = enriquecerLista(bases, ids);

    return new PageImpl<>(Objects.requireNonNull(content), pageable, total);
  }

  public Categoria findByIdOrThrow(Long id) {
    if (id == null) throw new IllegalArgumentException("Id é obrigatório.");

    String sql =
        """
                        SELECT
                          c.id                  AS categoria_id,
                          c.nome                AS categoria_nome,
                          c.descricao           AS categoria_descricao,
                          c.hora_checkin        AS categoria_hora_checkin,
                          c.hora_checkout       AS categoria_hora_checkout,
                          c.data_hora_cadastro  AS categoria_data_hora_cadastro,
                          f.id                  AS funcionario_id,
                          p.nome                AS funcionario_nome
                        FROM public.categoria c
                        LEFT JOIN public.funcionario f ON f.id = c.fk_funcionario
                        LEFT JOIN public.pessoa p ON p.id = f.fk_pessoa
                        WHERE c.id = ?
                        """;

    Categoria base;
    try {
      base = jdbcTemplate.queryForObject(sql, Categoria.ROW_MAPPER, id);
    } catch (EmptyResultDataAccessException ex) {
      throw new NotFoundException("Categoria não encontrada para o id: " + id);
    }

    return enriquecerCategoria(Objects.requireNonNull(base));
  }

  public Map<Long, Categoria> findCategoriasParaCalculo(List<Long> categoriaIds) {
    if (categoriaIds == null || categoriaIds.isEmpty()) return Map.of();
    String in = String.join(",", Collections.nCopies(categoriaIds.size(), "?"));
    Object[] idsArr = categoriaIds.toArray();

    Map<Long, List<Categoria.ModeloOcupacao>> ocupacaoMap = carregarModelosOcupacao(in, idsArr);
    Map<Long, List<Categoria.ModeloFixo>> fixoMap = carregarModelosFixo(in, idsArr);
    Map<Long, List<Categoria.DayUseOperacao>> dayUseMap = carregarDayUseOperacoes(in, idsArr);
    Map<Long, List<Categoria.MenorIdade>> menoresMap = carregarMenoresIdade(in, idsArr);

    Map<Long, Categoria> result = new LinkedHashMap<>();
    for (Long id : categoriaIds) {
      result.put(
          id,
          new Categoria(
              id,
              null,
              null,
              null,
              null,
              null,
              null,
              ocupacaoMap.getOrDefault(id, List.of()),
              fixoMap.getOrDefault(id, List.of()),
              dayUseMap.getOrDefault(id, List.of()),
              List.of(),
              List.of(),
              menoresMap.getOrDefault(id, List.of())));
    }
    return result;
  }

  public Categoria findByQuartoId(Long quartoId) {
    try {
      Long categoriaId =
          jdbcTemplate.queryForObject(
              "SELECT fk_categoria FROM public.quarto_categoria WHERE fk_quarto = ?",
              Long.class,
              quartoId);
      if (categoriaId == null) return null;
      return findByIdOrThrow(categoriaId);
    } catch (EmptyResultDataAccessException e) {
      return null;
    }
  }

  // ── Insert completo ───────────────────────────────────────────────────────

  public Categoria insert(Categoria.Request request, Long funcionarioId) {
    Long categoriaId =
        jdbcTemplate.queryForObject(
            """
                                INSERT INTO public.categoria (fk_funcionario, data_hora_cadastro, nome, descricao, hora_checkin, hora_checkout)
                                VALUES (?, now(), ?, ?, ?, ?)
                                RETURNING id
                                """,
            Long.class,
            funcionarioId,
            request.nome().trim(),
            request.descricao(),
            request.hora_checkin(),
            request.hora_checkout());

    // null = nova categoria, qualquer vínculo de quarto existente é conflito
    salvarSubDados(
        categoriaId,
        null,
        request.modelos_ocupacao(),
        request.modelos_fixo(),
        request.day_use(),
        request.fk_quartos(),
        request.fk_sazonalidades(),
        request.menores_idade(),
        funcionarioId);

    return findByIdOrThrow(categoriaId);
  }

  // ── Update completo ───────────────────────────────────────────────────────

  public Categoria update(Categoria.Update request, Long funcionarioId) {
    findByIdOrThrow(request.id());

    int rows =
        jdbcTemplate.update(
            """
                                UPDATE public.categoria SET
                                  nome          = ?,
                                  descricao     = ?,
                                  hora_checkin  = ?,
                                  hora_checkout = ?
                                WHERE id = ?
                                """,
            request.nome().trim(),
            request.descricao(),
            request.hora_checkin(),
            request.hora_checkout(),
            request.id());

    if (rows == 0)
      throw new NotFoundException("Categoria não encontrada para o id: " + request.id());

    deletarSubDados(request.id());

    // request.id() = categoria atual, quartos de outra categoria são conflito
    salvarSubDados(
        request.id(),
        request.id(),
        request.modelos_ocupacao(),
        request.modelos_fixo(),
        request.day_use(),
        request.fk_quartos(),
        request.fk_sazonalidades(),
        request.menores_idade(),
        funcionarioId);

    return findByIdOrThrow(request.id());
  }

  // ── Helpers de persistência ───────────────────────────────────────────────

  private void salvarSubDados(
      Long categoriaId,
      Long categoriaIdParaValidacao,
      List<Categoria.ModeloOcupacao.Input> modelosOcupacao,
      List<Categoria.ModeloFixo.Input> modelosFixo,
      List<Categoria.DayUseOperacao.Input> dayUse,
      List<Long> fkQuartos,
      List<Long> fkSazonalidades,
      List<Categoria.MenorIdade.Input> menoresIdade,
      Long funcionarioId) {

    Long fkFunc = funcionarioId;

    if (modelosOcupacao != null) {
      for (var mo : modelosOcupacao) {
        jdbcTemplate.update(
            """
                                INSERT INTO public.modelo_ocupacao (fk_categoria, fk_sazonalidade, fk_funcionario, data_hora_cadastro, quantidade, valor)
                                VALUES (?, null, ?, now(), ?, ?)
                                """,
            categoriaId,
            fkFunc,
            mo.quantidade(),
            mo.valor());
      }
    }

    if (modelosFixo != null) {
      for (var mf : modelosFixo) {
        jdbcTemplate.update(
            """
                                INSERT INTO public.modelo_fixo (fk_categoria, fk_sazonalidade, fk_funcionario, data_hora_cadastro, valor)
                                VALUES (?, null, ?, now(), ?)
                                """,
            categoriaId,
            fkFunc,
            mf.valor());
      }
    }

    if (dayUse != null) {
      for (var duo : dayUse) {
        Long operacaoId =
            jdbcTemplate.queryForObject(
                """
                                        INSERT INTO public.day_use_modelo_operacao (fk_categoria, fk_sazonalidade, fk_funcionario, data_hora_cadastro, ativo)
                                        VALUES (?, null, ?, now(), ?)
                                        RETURNING id
                                        """,
                Long.class,
                categoriaId,
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

    if (fkQuartos != null && !fkQuartos.isEmpty()) {
      validarQuartosDisponiveis(fkQuartos, categoriaIdParaValidacao);
      for (Long fkQuarto : fkQuartos) {
        jdbcTemplate.update(
            """
                                INSERT INTO public.quarto_categoria (fk_quarto, fk_categoria, fk_funcionario, data_hora_cadastro)
                                VALUES (?, ?, ?, now())
                                """,
            fkQuarto,
            categoriaId,
            fkFunc);
      }
    }

    if (fkSazonalidades != null) {
      for (Long fkSazon : fkSazonalidades) {
        jdbcTemplate.update(
            """
                                INSERT INTO public.categoria_sazonalidade (fk_categoria, fk_sazonalidade, fk_funcionario, data_hora_cadastro, ativo)
                                VALUES (?, ?, ?, now(), true)
                                """,
            categoriaId,
            fkSazon,
            fkFunc);
      }
    }

    if (menoresIdade != null) {
      for (var mi : menoresIdade) {
        Long menorId =
            jdbcTemplate.queryForObject(
                """
                                        INSERT INTO public.menor_idade (fk_funcionario, data_hora_cadastro, fk_categoria, fk_sazonalidade, idade_gratuidade)
                                        VALUES (?, now(), ?, null, ?)
                                        RETURNING id
                                        """,
                Long.class,
                fkFunc,
                categoriaId,
                mi.idade_gratuidade());

        salvarSubModeloMenorIdade(menorId, fkFunc, mi);
      }
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

  private void deletarSubDados(Long categoriaId) {
    // Menor idade e seus sub-modelos
    List<Long> menorIds =
        jdbcTemplate.query(
            "SELECT id FROM public.menor_idade WHERE fk_categoria = ?",
            (rs, rowNum) -> rs.getLong("id"),
            categoriaId);

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
    jdbcTemplate.update("DELETE FROM public.menor_idade WHERE fk_categoria = ?", categoriaId);

    // Day use e seus sub-modelos
    List<Long> operacaoIds =
        jdbcTemplate.query(
            "SELECT id FROM public.day_use_modelo_operacao WHERE fk_categoria = ?",
            (rs, rowNum) -> rs.getLong("id"),
            categoriaId);

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
        "DELETE FROM public.day_use_modelo_operacao WHERE fk_categoria = ?", categoriaId);

    jdbcTemplate.update("DELETE FROM public.modelo_ocupacao WHERE fk_categoria = ?", categoriaId);
    jdbcTemplate.update("DELETE FROM public.modelo_fixo WHERE fk_categoria = ?", categoriaId);
    jdbcTemplate.update("DELETE FROM public.quarto_categoria WHERE fk_categoria = ?", categoriaId);
    jdbcTemplate.update(
        "DELETE FROM public.categoria_sazonalidade WHERE fk_categoria = ?", categoriaId);
  }

  // ── Helpers de enriquecimento (leitura) ───────────────────────────────────

  private List<Categoria> enriquecerLista(List<Categoria> bases, List<Long> ids) {
    if (bases == null || bases.isEmpty()) return List.of();

    String in = String.join(",", Collections.nCopies(ids.size(), "?"));
    Object[] idsArr = ids.toArray();

    Map<Long, List<Categoria.ModeloOcupacao>> ocupacaoMap = carregarModelosOcupacao(in, idsArr);
    Map<Long, List<Categoria.ModeloFixo>> fixoMap = carregarModelosFixo(in, idsArr);
    Map<Long, List<Categoria.DayUseOperacao>> dayUseMap = carregarDayUseOperacoes(in, idsArr);
    Map<Long, List<Quarto.Descricao>> quartosMap = carregarQuartos(in, idsArr);
    Map<Long, List<Sazonalidade.Nome>> sazonMap = carregarSazonalidades(in, idsArr);
    Map<Long, List<Categoria.MenorIdade>> menoresMap = carregarMenoresIdade(in, idsArr);

    List<Categoria> result = new ArrayList<>();
    for (Categoria base : bases) {
      result.add(
          new Categoria(
              base.id(),
              base.nome(),
              base.descricao(),
              base.hora_checkin(),
              base.hora_checkout(),
              base.funcionario(),
              base.data_hora_cadastro(),
              ocupacaoMap.getOrDefault(base.id(), List.of()),
              fixoMap.getOrDefault(base.id(), List.of()),
              dayUseMap.getOrDefault(base.id(), List.of()),
              quartosMap.getOrDefault(base.id(), List.of()),
              sazonMap.getOrDefault(base.id(), List.of()),
              menoresMap.getOrDefault(base.id(), List.of())));
    }
    return result;
  }

  private Categoria enriquecerCategoria(Categoria base) {
    String in = "?";
    Object[] idArr = {base.id()};

    return new Categoria(
        base.id(),
        base.nome(),
        base.descricao(),
        base.hora_checkin(),
        base.hora_checkout(),
        base.funcionario(),
        base.data_hora_cadastro(),
        carregarModelosOcupacao(in, idArr).getOrDefault(base.id(), List.of()),
        carregarModelosFixo(in, idArr).getOrDefault(base.id(), List.of()),
        carregarDayUseOperacoes(in, idArr).getOrDefault(base.id(), List.of()),
        carregarQuartos(in, idArr).getOrDefault(base.id(), List.of()),
        carregarSazonalidades(in, idArr).getOrDefault(base.id(), List.of()),
        carregarMenoresIdade(in, idArr).getOrDefault(base.id(), List.of()));
  }

  private Map<Long, List<Categoria.ModeloOcupacao>> carregarModelosOcupacao(
      String in, Object[] ids) {
    String sql =
        """
                        SELECT
                          mo.id           AS mo_id,
                          mo.fk_categoria AS mo_fk_categoria,
                          mo.quantidade   AS mo_quantidade,
                          mo.valor        AS mo_valor,
                          s.id            AS mo_sazonalidade_id,
                          s.descricao     AS mo_sazonalidade_descricao
                        FROM public.modelo_ocupacao mo
                        LEFT JOIN public.sazonalidade s ON s.id = mo.fk_sazonalidade
                        WHERE mo.fk_categoria IN (%s)
                        ORDER BY mo.fk_categoria, mo.id
                        """
            .formatted(in);

    Map<Long, List<Categoria.ModeloOcupacao>> map = new LinkedHashMap<>();
    jdbcTemplate.query(
        sql,
        rs -> {
          Long catId = rs.getLong("mo_fk_categoria");
          map.computeIfAbsent(catId, k -> new ArrayList<>())
              .add(Categoria.ModeloOcupacao.ROW_MAPPER.mapRow(rs, 0));
        },
        ids);
    return map;
  }

  private Map<Long, List<Categoria.ModeloFixo>> carregarModelosFixo(String in, Object[] ids) {
    String sql =
        """
                        SELECT
                          mf.id           AS mf_id,
                          mf.fk_categoria AS mf_fk_categoria,
                          mf.valor        AS mf_valor,
                          s.id            AS mf_sazonalidade_id,
                          s.descricao     AS mf_sazonalidade_descricao
                        FROM public.modelo_fixo mf
                        LEFT JOIN public.sazonalidade s ON s.id = mf.fk_sazonalidade
                        WHERE mf.fk_categoria IN (%s)
                        ORDER BY mf.fk_categoria, mf.id
                        """
            .formatted(in);

    Map<Long, List<Categoria.ModeloFixo>> map = new LinkedHashMap<>();
    jdbcTemplate.query(
        sql,
        rs -> {
          Long catId = rs.getLong("mf_fk_categoria");
          map.computeIfAbsent(catId, k -> new ArrayList<>())
              .add(Categoria.ModeloFixo.ROW_MAPPER.mapRow(rs, 0));
        },
        ids);
    return map;
  }

  private Map<Long, List<Categoria.DayUseOperacao>> carregarDayUseOperacoes(
      String in, Object[] ids) {
    String sql =
        """
                        SELECT
                          duo.id           AS duo_id,
                          duo.fk_categoria AS duo_fk_categoria,
                          duo.ativo        AS duo_ativo,
                          s.id             AS duo_sazonalidade_id,
                          s.descricao      AS duo_sazonalidade_descricao
                        FROM public.day_use_modelo_operacao duo
                        LEFT JOIN public.sazonalidade s ON s.id = duo.fk_sazonalidade
                        WHERE duo.fk_categoria IN (%s)
                        ORDER BY duo.fk_categoria, duo.id
                        """
            .formatted(in);

    Map<Long, List<Categoria.DayUseOperacao>> operacoesMap = new LinkedHashMap<>();
    Map<Long, Long> operacaoCatMap = new LinkedHashMap<>();

    jdbcTemplate.query(
        sql,
        rs -> {
          Long catId = rs.getLong("duo_fk_categoria");
          Long operacaoId = rs.getLong("duo_id");
          operacaoCatMap.put(operacaoId, catId);
          Long sazonId = rs.getObject("duo_sazonalidade_id", Long.class);
          Sazonalidade.Nome sazon =
              sazonId == null
                  ? null
                  : new Sazonalidade.Nome(sazonId, rs.getString("duo_sazonalidade_descricao"));
          operacoesMap
              .computeIfAbsent(catId, k -> new ArrayList<>())
              .add(
                  new Categoria.DayUseOperacao(
                      operacaoId, sazon, rs.getBoolean("duo_ativo"), null, new ArrayList<>()));
        },
        ids);

    if (operacaoCatMap.isEmpty()) return operacoesMap;

    String opIn = String.join(",", Collections.nCopies(operacaoCatMap.size(), "?"));
    Object[] opIds = operacaoCatMap.keySet().toArray();

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

  private Map<Long, List<Quarto.Descricao>> carregarQuartos(String in, Object[] ids) {
    String sql =
        """
                        SELECT
                          qc.fk_categoria AS qc_fk_categoria,
                          q.id            AS quarto_id,
                          q.descricao     AS quarto_descricao
                        FROM public.quarto_categoria qc
                        JOIN public.quarto q ON q.id = qc.fk_quarto
                        WHERE qc.fk_categoria IN (%s)
                        ORDER BY qc.fk_categoria, q.id ASC
                        """
            .formatted(in);

    Map<Long, List<Quarto.Descricao>> map = new LinkedHashMap<>();
    jdbcTemplate.query(
        sql,
        rs -> {
          Long catId = rs.getLong("qc_fk_categoria");
          map.computeIfAbsent(catId, k -> new ArrayList<>())
              .add(new Quarto.Descricao(rs.getLong("quarto_id"), rs.getString("quarto_descricao")));
        },
        ids);
    return map;
  }

  private Map<Long, List<Sazonalidade.Nome>> carregarSazonalidades(String in, Object[] ids) {
    String sql =
        """
                        SELECT
                          cs.fk_categoria AS cs_fk_categoria,
                          s.id            AS sazonalidade_id,
                          s.descricao     AS sazonalidade_descricao
                        FROM public.categoria_sazonalidade cs
                        JOIN public.sazonalidade s ON s.id = cs.fk_sazonalidade
                        WHERE cs.fk_categoria IN (%s)
                        ORDER BY cs.fk_categoria, s.descricao
                        """
            .formatted(in);

    Map<Long, List<Sazonalidade.Nome>> map = new LinkedHashMap<>();
    jdbcTemplate.query(
        sql,
        rs -> {
          Long catId = rs.getLong("cs_fk_categoria");
          map.computeIfAbsent(catId, k -> new ArrayList<>())
              .add(
                  new Sazonalidade.Nome(
                      rs.getLong("sazonalidade_id"), rs.getString("sazonalidade_descricao")));
        },
        ids);
    return map;
  }

  private Map<Long, List<Categoria.MenorIdade>> carregarMenoresIdade(String in, Object[] ids) {
    String sql =
        """
                        SELECT
                          mi.id               AS mi_id,
                          mi.fk_categoria     AS mi_fk_categoria,
                          mi.idade_gratuidade AS mi_idade_gratuidade,
                          s.id                AS sazonalidade_id,
                          s.descricao         AS sazonalidade_descricao
                        FROM public.menor_idade mi
                        LEFT JOIN public.sazonalidade s ON s.id = mi.fk_sazonalidade
                        WHERE mi.fk_categoria IN (%s)
                        ORDER BY mi.fk_categoria, mi.id
                        """
            .formatted(in);

    Map<Long, List<Categoria.MenorIdade>> map = new LinkedHashMap<>();
    Map<Long, Long> menorCatMap = new LinkedHashMap<>();

    jdbcTemplate.query(
        sql,
        rs -> {
          Long catId = rs.getLong("mi_fk_categoria");
          Long menorId = rs.getLong("mi_id");
          menorCatMap.put(menorId, catId);
          Long sazonId = rs.getObject("sazonalidade_id", Long.class);
          Sazonalidade.Nome sazon =
              sazonId == null
                  ? null
                  : new Sazonalidade.Nome(sazonId, rs.getString("sazonalidade_descricao"));
          map.computeIfAbsent(catId, k -> new ArrayList<>())
              .add(
                  new Categoria.MenorIdade(
                      menorId,
                      sazon,
                      rs.getObject("mi_idade_gratuidade", Integer.class),
                      null,
                      List.of(),
                      List.of(),
                      List.of(),
                      List.of()));
        },
        ids);

    if (menorCatMap.isEmpty()) return map;

    String miIn = String.join(",", Collections.nCopies(menorCatMap.size(), "?"));
    Object[] miIds = menorCatMap.keySet().toArray();

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
                mi.sazonalidade(),
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

  public Map<Long, List<Categoria.MenorIdade>> findSazonMenoresIdade(List<Long> sazonIds) {
    if (sazonIds == null || sazonIds.isEmpty()) return Map.of();

    String in = String.join(",", Collections.nCopies(sazonIds.size(), "?"));
    Object[] idsArr = sazonIds.toArray();

    Map<Long, List<Categoria.MenorIdade>> map = new LinkedHashMap<>();
    Map<Long, Long> menorSazonMap = new LinkedHashMap<>();

    jdbcTemplate.query(
        """
                        SELECT mi.id               AS mi_id,
                               mi.fk_sazonalidade  AS mi_fk_sazonalidade,
                               mi.idade_gratuidade AS mi_idade_gratuidade,
                               s.descricao         AS sazonalidade_descricao
                        FROM public.menor_idade mi
                        JOIN public.sazonalidade s ON s.id = mi.fk_sazonalidade
                        WHERE mi.fk_sazonalidade IN (%s) AND mi.fk_categoria IS NULL
                        ORDER BY mi.fk_sazonalidade, mi.id
                        """
            .formatted(in),
        rs -> {
          Long sazonId = rs.getLong("mi_fk_sazonalidade");
          Long menorId = rs.getLong("mi_id");
          menorSazonMap.put(menorId, sazonId);
          Sazonalidade.Nome sazon =
              new Sazonalidade.Nome(sazonId, rs.getString("sazonalidade_descricao"));
          map.computeIfAbsent(sazonId, k -> new ArrayList<>())
              .add(
                  new Categoria.MenorIdade(
                      menorId,
                      sazon,
                      rs.getObject("mi_idade_gratuidade", Integer.class),
                      null,
                      List.of(),
                      List.of(),
                      List.of(),
                      List.of()));
        },
        idsArr);

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
                mi.sazonalidade(),
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

  private void validarQuartosDisponiveis(List<Long> fkQuartos, Long categoriaId) {
    if (fkQuartos == null || fkQuartos.isEmpty()) return;

    String in = String.join(",", Collections.nCopies(fkQuartos.size(), "?"));

    String sql;
    List<Object> params = new ArrayList<>(fkQuartos);

    if (categoriaId != null) {
      sql =
          """
                            SELECT q.descricao
                            FROM public.quarto_categoria qc
                            JOIN public.quarto q ON q.id = qc.fk_quarto
                            WHERE qc.fk_quarto IN (%s)
                              AND qc.fk_categoria != ?
                            """
              .formatted(in);
      params.add(categoriaId);
    } else {
      sql =
          """
                            SELECT q.descricao
                            FROM public.quarto_categoria qc
                            JOIN public.quarto q ON q.id = qc.fk_quarto
                            WHERE qc.fk_quarto IN (%s)
                            """
              .formatted(in);
    }

    List<String> ocupados =
        jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("descricao"), params.toArray());

    if (!ocupados.isEmpty()) {
      throw new IllegalArgumentException(
          "Os seguintes quartos já pertencem a outra categoria: " + String.join(", ", ocupados));
    }
  }

  public Map<Long, List<Categoria.ModeloOcupacao>> buscaModeloPrecoPorOcupacaoSazonalidade(
      List<Long> sazonIds) {
    if (sazonIds == null || sazonIds.isEmpty()) return Map.of();
    String in = String.join(",", Collections.nCopies(sazonIds.size(), "?"));
    Map<Long, List<Categoria.ModeloOcupacao>> map = new HashMap<>();
    jdbcTemplate.query(
        ("""
                        SELECT mo.id AS mo_id, mo.fk_sazonalidade AS mo_fk_sazonalidade,
                               mo.quantidade AS mo_quantidade, mo.valor AS mo_valor
                        FROM public.modelo_ocupacao mo
                        WHERE mo.fk_sazonalidade IN (%s) AND mo.fk_categoria IS NULL
                        ORDER BY mo.fk_sazonalidade, mo.id
                        """)
            .formatted(in),
        rs -> {
          Long sid = rs.getLong("mo_fk_sazonalidade");
          map.computeIfAbsent(sid, k -> new ArrayList<>())
              .add(
                  new Categoria.ModeloOcupacao(
                      rs.getLong("mo_id"),
                      null,
                      rs.getInt("mo_quantidade"),
                      rs.getDouble("mo_valor")));
        },
        sazonIds.toArray());
    return map;
  }

  public Map<Long, List<Categoria.ModeloFixo>> buscaModeloPrecoFixoSazonalidade(
      List<Long> sazonIds) {
    if (sazonIds == null || sazonIds.isEmpty()) return Map.of();
    String in = String.join(",", Collections.nCopies(sazonIds.size(), "?"));
    Map<Long, List<Categoria.ModeloFixo>> map = new HashMap<>();
    jdbcTemplate.query(
        ("""
                        SELECT mf.id AS mf_id, mf.fk_sazonalidade AS mf_fk_sazonalidade, mf.valor AS mf_valor
                        FROM public.modelo_fixo mf
                        WHERE mf.fk_sazonalidade IN (%s) AND mf.fk_categoria IS NULL
                        ORDER BY mf.fk_sazonalidade, mf.id
                        """)
            .formatted(in),
        rs -> {
          Long sid = rs.getLong("mf_fk_sazonalidade");
          map.computeIfAbsent(sid, k -> new ArrayList<>())
              .add(new Categoria.ModeloFixo(rs.getLong("mf_id"), null, rs.getDouble("mf_valor")));
        },
        sazonIds.toArray());
    return map;
  }

  public Map<Long, Categoria.DayUseOperacao> buscaDayUseSazonalidade(List<Long> sazonIds) {
    if (sazonIds == null || sazonIds.isEmpty()) return Map.of();
    String in = String.join(",", Collections.nCopies(sazonIds.size(), "?"));

    Map<Long, Long> operacaoSazonMap = new LinkedHashMap<>();
    Map<Long, Boolean> operacaoAtivoMap = new LinkedHashMap<>();
    jdbcTemplate.query(
        ("""
                SELECT
                 duo.id AS duo_id,
                 duo.fk_sazonalidade AS duo_fk_sazonalidade,
                 duo.ativo AS duo_ativo
                FROM public.day_use_modelo_operacao duo
                WHERE duo.fk_sazonalidade IN (%s) AND duo.fk_categoria IS NULL
                ORDER BY duo.fk_sazonalidade, duo.id
                """)
            .formatted(in),
        rs -> {
          operacaoSazonMap.put(rs.getLong("duo_id"), rs.getLong("duo_fk_sazonalidade"));
          operacaoAtivoMap.put(rs.getLong("duo_id"), rs.getBoolean("duo_ativo"));
        },
        sazonIds.toArray());

    if (operacaoSazonMap.isEmpty()) return Map.of();

    Object[] opIds = operacaoSazonMap.keySet().toArray();
    String inOp = String.join(",", Collections.nCopies(opIds.length, "?"));

    // Step 2: padrao
    Map<Long, Categoria.DayUsePadrao> padraoMap = new LinkedHashMap<>();
    jdbcTemplate.query(
        ("""
                        SELECT dup.fk_day_use_modelo_operacao AS dup_fk_operacao,
                               dup.id AS dup_id, dup.preco_base AS dup_preco_base,
                               dup.hora_preco_base AS dup_hora_preco_base,
                               dup.valor_hora_adicional AS dup_valor_hora_adicional
                        FROM public.day_use_modelo_padrao dup
                        WHERE dup.fk_day_use_modelo_operacao IN (%s)
                        """)
            .formatted(inOp),
        rs -> {
          padraoMap.put(
              rs.getLong("dup_fk_operacao"), Categoria.DayUsePadrao.ROW_MAPPER.mapRow(rs, 0));
        },
        opIds);

    // Step 3: ocupacoes + pessoas (single query with LEFT JOIN)
    Map<Long, Categoria.DayUseOcupacao> ocupacaoById = new LinkedHashMap<>();
    Map<Long, Long> ocupacaoOperacaoMap = new LinkedHashMap<>();
    jdbcTemplate.query(
        ("""
                        SELECT duo.id AS duo_id, duo.fk_day_use_modelo_operacao AS duo_fk_operacao,
                               duo.quantidade_pessoa AS duo_quantidade_pessoa,
                               duop.id AS duop_id, duop.quantidade AS duop_quantidade,
                               duop.valor AS duop_valor,
                               duop.valor_hora_adicional_por_pessoa AS duop_valor_hora_adicional_por_pessoa
                        FROM public.day_use_modelo_ocupacao duo
                        LEFT JOIN public.day_use_modelo_ocupacao_quantidade_pessoa duop
                               ON duop.fk_day_use_modelo_ocupacao = duo.id
                        WHERE duo.fk_day_use_modelo_operacao IN (%s)
                        ORDER BY duo.id, duop.quantidade ASC
                        """)
            .formatted(inOp),
        rs -> {
          Long ocId = rs.getLong("duo_id");
          if (!ocupacaoById.containsKey(ocId)) {
            ocupacaoById.put(
                ocId,
                new Categoria.DayUseOcupacao(
                    ocId, rs.getInt("duo_quantidade_pessoa"), new ArrayList<>()));
            ocupacaoOperacaoMap.put(ocId, rs.getLong("duo_fk_operacao"));
          }
          Long pessoaId = rs.getObject("duop_id", Long.class);
          if (pessoaId != null) {
            ocupacaoById
                .get(ocId)
                .quantidades()
                .add(
                    new Categoria.DayUseOcupacaoPessoa(
                        pessoaId,
                        rs.getInt("duop_quantidade"),
                        rs.getInt("duop_valor"),
                        rs.getObject("duop_valor_hora_adicional_por_pessoa", Integer.class)));
          }
        },
        opIds);

    // Grupo de ocupacoes por operacao
    Map<Long, List<Categoria.DayUseOcupacao>> ocupacoesPorOperacao = new LinkedHashMap<>();
    ocupacaoOperacaoMap.forEach(
        (ocId, opId) ->
            ocupacoesPorOperacao
                .computeIfAbsent(opId, k -> new ArrayList<>())
                .add(ocupacaoById.get(ocId)));

    // Monta resultado: sazonId -> DayUseOperacao
    Map<Long, Categoria.DayUseOperacao> result = new LinkedHashMap<>();
    operacaoSazonMap.forEach(
        (opId, sazonId) ->
            result.putIfAbsent(
                sazonId,
                new Categoria.DayUseOperacao(
                    opId,
                    null,
                    operacaoAtivoMap.get(opId),
                    padraoMap.get(opId),
                    ocupacoesPorOperacao.getOrDefault(opId, List.of()))));

    return result;
  }

  public double calcularPrecoDiaria(
      LocalDate noite,
      Categoria categoria,
      List<Sazonalidade> sazonalidades,
      Map<Long, List<Categoria.ModeloOcupacao>> sazonalidadeModelosPrecoPorOcupacao,
      Map<Long, List<Categoria.ModeloFixo>> sazonalidadeModelosPrecoFixo,
      int qtdPessoas) {
    Long activeSazonId = findActiveSazonalidade(sazonalidades, noite);

    List<Categoria.ModeloOcupacao> modelosOcupacao =
        activeSazonId != null
            ? sazonalidadeModelosPrecoPorOcupacao.getOrDefault(activeSazonId, List.of())
            : List.of();
    List<Categoria.ModeloFixo> modelosFixo =
        activeSazonId != null
            ? sazonalidadeModelosPrecoFixo.getOrDefault(activeSazonId, List.of())
            : List.of();

    if (modelosOcupacao.isEmpty() && modelosFixo.isEmpty() && activeSazonId != null) {
      modelosOcupacao = filtrarOcupacaoPorSazon(categoria.modelos_ocupacao(), activeSazonId);
      modelosFixo = filtrarFixoPorSazon(categoria.modelos_fixo(), activeSazonId);
    }

    if (modelosOcupacao.isEmpty() && modelosFixo.isEmpty()) {
      modelosOcupacao = filtrarOcupacaoPorSazon(categoria.modelos_ocupacao(), null);
      modelosFixo = filtrarFixoPorSazon(categoria.modelos_fixo(), null);
    }

    return resolverPrecoAdultos(modelosOcupacao, modelosFixo, qtdPessoas);
  }

  private Long findActiveSazonalidade(List<Sazonalidade> sazonalidades, LocalDate date) {
    for (Sazonalidade s : sazonalidades) {
      boolean isPeriodo = s.data_inicio() != null || s.data_fim() != null;
      if (!isPeriodo) continue;
      boolean inRange =
          (s.data_inicio() == null || !date.isBefore(s.data_inicio()))
              && (s.data_fim() == null || !date.isAfter(s.data_fim()));
      if (inRange) return s.id();
    }
    for (Sazonalidade s : sazonalidades) {
      boolean semanal =
          s.semanal() != null
              && !s.semanal().isEmpty()
              && s.semanal().contains(date.getDayOfWeek().getValue());
      boolean mensal =
          s.mensal() != null && !s.mensal().isEmpty() && s.mensal().contains(date.getDayOfMonth());
      boolean anual =
          s.anual() != null && !s.anual().isEmpty() && s.anual().contains(date.getMonthValue());
      if (semanal || mensal || anual) return s.id();
    }
    return null;
  }

  private List<Categoria.ModeloOcupacao> filtrarOcupacaoPorSazon(
      List<Categoria.ModeloOcupacao> modelos, Long sazonId) {
    if (modelos == null) return List.of();
    return modelos.stream().filter(m -> sazonIdMatch(m.sazonalidade(), sazonId)).toList();
  }

  private List<Categoria.ModeloFixo> filtrarFixoPorSazon(
      List<Categoria.ModeloFixo> modelos, Long sazonId) {
    if (modelos == null) return List.of();
    return modelos.stream().filter(m -> sazonIdMatch(m.sazonalidade(), sazonId)).toList();
  }

  private boolean sazonIdMatch(Sazonalidade.Nome sazon, Long activeSazonId) {
    if (activeSazonId == null) return sazon == null;
    return sazon != null && sazon.id().equals(activeSazonId);
  }

  private double resolverPrecoAdultos(
      List<Categoria.ModeloOcupacao> modelosOcupacao,
      List<Categoria.ModeloFixo> modelosFixo,
      int quantidade) {
    if (!modelosOcupacao.isEmpty()) {
      var modelo = modelosOcupacao.stream().filter(m -> m.quantidade() == quantidade).findFirst();
      if (modelo.isEmpty()) {
        modelo =
            modelosOcupacao.stream()
                .filter(m -> m.quantidade() <= quantidade)
                .max(Comparator.comparingInt(Categoria.ModeloOcupacao::quantidade));
      }
      if (modelo.isPresent()) return modelo.get().valor();
    } else if (!modelosFixo.isEmpty()) {
      return modelosFixo.getFirst().valor();
    }
    return 0.0;
  }

  public List<Sazonalidade> buscarSazonalidadesAtivas(Long categoriaId) {
    var categoria = findCategoriasParaCalculo(List.of(categoriaId)).getOrDefault(categoriaId, null);
    if (categoria == null) return null;
    return jdbcTemplate.query(
        """
                    SELECT
                        categoria_sazonalidade.fk_categoria,
                        sazonalidade.id,
                        sazonalidade.descricao,
                        sazonalidade.data_inicio,
                        sazonalidade.data_fim,
                        sazonalidade.hora_checkin,
                        sazonalidade.hora_checkout,
                        sazonalidade.semanal,
                        sazonalidade.mensal,
                        sazonalidade.anual
                    FROM public.categoria_sazonalidade
                    JOIN public.sazonalidade ON sazonalidade.id = categoria_sazonalidade.fk_sazonalidade
                    WHERE categoria_sazonalidade.fk_categoria = ? AND categoria_sazonalidade.ativo = true
                    ORDER BY categoria_sazonalidade.fk_categoria, sazonalidade.id
                """,
        (rs, x) ->
            new Sazonalidade(
                rs.getLong("id"),
                rs.getString("descricao"),
                rs.getObject("data_inicio", LocalDate.class),
                rs.getObject("data_fim", LocalDate.class),
                rs.getObject("diario_hora_inicio_ciclo", LocalTime.class),
                rs.getObject("diario_hora_fim_ciclo", LocalTime.class),
                parseIntArray(rs.getArray("semanal")),
                parseIntArray(rs.getArray("mensal")),
                parseIntArray(rs.getArray("anual")),
                rs.getObject("hora_checkin", LocalTime.class),
                rs.getObject("hora_checkout", LocalTime.class),
                null,
                null,
                null,
                null,
                null));
  }

  private static List<Integer> parseIntArray(Array arr) {
    if (arr == null) return null;
    try {
      Integer[] boxed = (Integer[]) arr.getArray();
      return (boxed == null || boxed.length == 0) ? null : List.of(boxed);
    } catch (SQLException e) {
      return null;
    }
  }

  //    public float calcularValorTotal(
  //            Long categoriaId,
  //            LocalDate dataEntrada,
  //            int diarias,
  //            int quantidadePessoas) {
  //        long inicio = System.currentTimeMillis();
  //
  //        List<Sazonalidade> sazonalidades = buscarSazonalidadesAtivas(categoriaId);
  //        List<Long> sazonIds = sazonalidades.stream().map(Sazonalidade::id).toList();
  //        Map<Long, List<Categoria.ModeloOcupacao>> modelosOcupacao =
  //                sazonIds.isEmpty() ? Map.of() :
  // buscaModeloPrecoPorOcupacaoSazonalidade(sazonIds);
  //        Map<Long, List<Categoria.ModeloFixo>> modelosFixo = sazonIds.isEmpty() ? Map.of() :
  // buscaModeloPrecoFixoSazonalidade(sazonIds);
  //        Categoria categoria = findCategoriasParaCalculo(List.of(categoriaId))
  //                .getOrDefault(categoriaId, null);
  //        if (categoria == null) return 0f;
  //
  //        float total = 0f;
  //        for (int i = 0; i < diarias; i++) {
  //            total +=
  //                    (float)
  //                            calcularPrecoDiaria(
  //                                    dataEntrada.plusDays(i),
  //                                    categoria,
  //                                    sazonalidades,
  //                                    modelosOcupacao,
  //                                    modelosFixo,
  //                                    quantidadePessoas);
  //        }
  //
  //        log.info("calcularValorTotal — categoria={} diarias={} pessoas={} total={} tempo={}ms",
  //                categoriaId, diarias, quantidadePessoas, total, System.currentTimeMillis() -
  // inicio);
  //
  //        return total;
  //    }

  public Map<Long, List<Sazonalidade>> findSazonalidades(List<Long> categoriaIds) {
    if (categoriaIds == null || categoriaIds.isEmpty()) return Map.of();
    String in = String.join(",", Collections.nCopies(categoriaIds.size(), "?"));
    Map<Long, List<Sazonalidade>> map = new LinkedHashMap<>();
    jdbcTemplate.query(
        ("""
        SELECT
        cs.fk_categoria,
        s.id,
        s.descricao,
        s.data_inicio,
        s.data_fim,
        s.hora_checkin,
        s.hora_checkout,
        s.semanal,
        s.mensal,
        s.anual,
        s.diario_hora_inicio_ciclo,
        s.diario_hora_fim_ciclo
        FROM public.categoria_sazonalidade cs
        JOIN public.sazonalidade s ON s.id = cs.fk_sazonalidade
        WHERE cs.fk_categoria IN (%s) AND cs.ativo = true
        ORDER BY cs.fk_categoria, s.id
        """)
            .formatted(in),
        (RowCallbackHandler)
            rs ->
                map.computeIfAbsent(rs.getLong("fk_categoria"), k -> new ArrayList<>())
                    .add(
                        new Sazonalidade(
                            rs.getLong("id"),
                            rs.getString("descricao"),
                            rs.getObject("data_inicio", LocalDate.class),
                            rs.getObject("data_fim", LocalDate.class),
                            rs.getObject("diario_hora_inicio_ciclo", LocalTime.class),
                            rs.getObject("diario_hora_fim_ciclo", LocalTime.class),
                            parseIntArray(rs.getArray("semanal")),
                            parseIntArray(rs.getArray("mensal")),
                            parseIntArray(rs.getArray("anual")),
                            rs.getObject("hora_checkin", LocalTime.class),
                            rs.getObject("hora_checkout", LocalTime.class),
                            null,
                            null,
                            null,
                            null,
                            null)),
        categoriaIds.toArray());
    return map;
  }

  public Map<Long, CategoriaCheckin> findCategoriasCheckinByQuartoIds(List<Long> quartoIds) {
    String in = String.join(",", Collections.nCopies(quartoIds.size(), "?"));
    Map<Long, CategoriaCheckin> map = new HashMap<>();
    jdbcTemplate.query(
        ("""
        SELECT qc.fk_quarto, c.id, c.nome, c.hora_checkin, c.hora_checkout
        FROM public.quarto_categoria qc
        JOIN public.categoria c ON c.id = qc.fk_categoria
        WHERE qc.fk_quarto IN (%s)
        """)
            .formatted(in),
        (RowCallbackHandler)
            rs ->
                map.put(
                    rs.getLong("fk_quarto"),
                    new CategoriaCheckin(
                        rs.getLong("id"),
                        rs.getString("nome"),
                        rs.getObject("hora_checkin", LocalTime.class),
                        rs.getObject("hora_checkout", LocalTime.class))),
        quartoIds.toArray());
    return map;
  }
}
