package saas.hotel.istoepousada.repository;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.Relatorio;
import saas.hotel.istoepousada.dto.RelatorioDia;
import saas.hotel.istoepousada.dto.RelatorioExtratoResponse;
import saas.hotel.istoepousada.dto.enums.Valores;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;

@Repository
public class RelatorioRepository {

  private final JdbcTemplate jdbcTemplate;

  public RelatorioRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  private final ResultSetExtractor<List<Relatorio>> RELATORIO_EXTRACTOR =
      rs -> {
        List<Relatorio> list = new ArrayList<>();
        while (rs.next()) list.add(Relatorio.mapRelatorio(rs));
        return list;
      };

  private static Float toFloat(Double v) {
    return v == null ? 0f : v.floatValue();
  }

  private record Totais(Double totalEntradas, Double totalSaidas) {}

  private record TotaisDinheiro(Double totalDinheiro, Double totalDinheiroSaida) {}

  public RelatorioExtratoResponse buscar(
      Long id,
      LocalDate dataInicio,
      LocalDate dataFim,
      Long funcionarioId,
      Long quartoId,
      Long tipoPagamentoId,
      Valores valores,
      Pageable pageable) {

    // -------- BASES ----------
    String baseFrom =
        """
            FROM relatorio r
            INNER JOIN pessoa pbase ON pbase.id = r.fk_funcionario
            """;

    String baseSelect =
        """
            SELECT
                r.id                 AS relatorio_id,
                r.data_hora          AS data_hora,
                r.relatorio          AS relatorio,
                r.valor              AS valor,

                tp.id                AS tipo_pagamento_id,
                tp.descricao         AS tipo_pagamento_descricao,

                q.id                 AS quarto_id,
                q.descricao          AS quarto_descricao,

                p.id                 AS funcionario_id,
                p.data_hora_cadastro AS funcionario_data_hora_cadastro,
                p.nome               AS funcionario_nome,
                p.data_nascimento    AS funcionario_data_nascimento,
                p.cpf                AS funcionario_cpf,
                p.rg                 AS funcionario_rg,
                p.email              AS funcionario_email,
                p.telefone           AS funcionario_telefone,
                p.pais               AS funcionario_pais,
                p.estado             AS funcionario_estado,
                p.municipio          AS funcionario_municipio,
                p.endereco           AS funcionario_endereco,
                p.complemento        AS funcionario_complemento,
                p.vezes_hospedado    AS funcionario_vezes_hospedado,
                p.cep                AS funcionario_cep,
                p.idade              AS funcionario_idade,
                p.bairro             AS funcionario_bairro,
                p.sexo               AS funcionario_sexo,
                p.numero             AS funcionario_numero,
                p.status             AS funcionario_status,
                p.fk_funcionario     AS funcionario_fk_funcionario,
                p.fk_titular         AS funcionario_fk_titular,
                func.nome            AS funcionario_funcionario_nome,
                titular.nome         AS funcionario_titular_nome
            FROM relatorio r
            INNER JOIN pessoa p ON p.id = r.fk_funcionario
            LEFT JOIN pessoa func ON func.id = p.fk_funcionario
            LEFT JOIN pessoa titular ON titular.id = p.fk_titular
            LEFT JOIN tipo_pagamento tp ON tp.id = r.fk_tipo_pagamento
            LEFT JOIN quarto q ON q.id = r.quarto_id
            """;

    // -------- WHERE BASE (sem tipoPagamento, igual seu padrão) ----------
    StringBuilder whereBase = new StringBuilder(" WHERE 1=1 ");
    List<Object> paramsBase = new ArrayList<>();

    if (id != null) {
      whereBase.append(" AND r.id = ? ");
      paramsBase.add(id);
    }
    if (dataInicio != null) {
      whereBase.append(" AND r.data_hora >= ? ");
      paramsBase.add(Timestamp.valueOf(dataInicio.atStartOfDay()));
    }
    if (dataFim != null) {
      // exclusivo do dia seguinte (range correto)
      whereBase.append(" AND r.data_hora < ? ");
      paramsBase.add(Timestamp.valueOf(dataFim.plusDays(1).atStartOfDay()));
    }
    if (funcionarioId != null) {
      whereBase.append(" AND r.fk_funcionario = ? ");
      paramsBase.add(funcionarioId);
    }
    if (quartoId != null) {
      whereBase.append(" AND r.quarto_id = ? ");
      paramsBase.add(quartoId);
    }
    if (valores != null) {
      if (valores == Valores.ENTRADA) whereBase.append(" AND r.valor > 0 ");
      else if (valores == Valores.SAIDA) whereBase.append(" AND r.valor < 0 ");
    }

    // -------- WHERE LIST (com tipoPagamento) ----------
    StringBuilder whereList = new StringBuilder(whereBase);
    List<Object> paramsList = new ArrayList<>(paramsBase);

    if (tipoPagamentoId != null) {
      whereList.append(" AND r.fk_tipo_pagamento = ? ");
      paramsList.add(tipoPagamentoId);
    }

    // -------- TOTAIS (mantém sua regra atual) ----------
    Totais totaisGerais = buscarTotaisGerais(baseFrom, whereList.toString(), paramsList);

    TotaisDinheiro totaisDinheiro =
        buscarTotaisDinheiro(baseFrom, whereBase + " AND r.fk_tipo_pagamento = 1 ", paramsBase);

    Float totalEntradas = toFloat(totaisGerais.totalEntradas());
    Float totalSaidas = toFloat(totaisGerais.totalSaidas());
    Float balancoGeral = totalEntradas + totalSaidas;

    Float totalDinheiro = toFloat(totaisDinheiro.totalDinheiro());
    Float totalDinheiroSaida = toFloat(totaisDinheiro.totalDinheiroSaida());
    Float balancoDinheiro = totalDinheiro + totalDinheiroSaida;

    // -------- PAGINAÇÃO POR DIA ----------
    Long totalDias;
    try {
      totalDias =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(DISTINCT DATE(r.data_hora)) " + baseFrom + whereList,
              Long.class,
              paramsList.toArray());
    } catch (EmptyResultDataAccessException ex) {
      totalDias = 0L;
    }

    if (totalDias == null || totalDias == 0) {
      return new RelatorioExtratoResponse(
          balancoGeral,
          totalEntradas,
          totalSaidas,
          totalDinheiro,
          totalDinheiroSaida,
          balancoDinheiro,
          new PageImpl<>(List.of(), pageable, 0));
    }

    String diasSql =
        """
            SELECT DISTINCT DATE(r.data_hora) AS dia
            """
            + baseFrom
            + whereList
            + """
        ORDER BY dia DESC
        LIMIT ? OFFSET ?
        """;

    List<Object> diasParams = new ArrayList<>(paramsList);
    diasParams.add(pageable.getPageSize());
    diasParams.add((int) pageable.getOffset());

    List<LocalDate> dias =
        jdbcTemplate.query(
            diasSql, (rs, rowNum) -> rs.getObject("dia", LocalDate.class), diasParams.toArray());

    if (dias.isEmpty()) {
      return new RelatorioExtratoResponse(
          balancoGeral,
          totalEntradas,
          totalSaidas,
          totalDinheiro,
          totalDinheiroSaida,
          balancoDinheiro,
          new PageImpl<>(List.of(), pageable, totalDias));
    }

    // -------- BUSCAR RELATÓRIOS DOS DIAS DA PÁGINA ----------
    // Em vez de IN (ids), vamos filtrar por range diário para cada dia.
    // Para performance: monta OR ranges: (data_hora>=d0 and <d0+1) OR (data_hora>=d1 and <d1+1)...
    StringBuilder dayRanges = new StringBuilder(" WHERE 1=1 ");

    // Reaplica os mesmos filtros (whereList) mas removendo o "WHERE 1=1" inicial duplicado:
    // Melhor: usar whereList direto, e adicionar "AND (" + ranges + ")"
    String whereListStr = whereList.toString();

    StringBuilder ranges = new StringBuilder();
    List<Object> rangeParams = new ArrayList<>();

    for (int i = 0; i < dias.size(); i++) {
      if (i > 0) ranges.append(" OR ");
      ranges.append("(r.data_hora >= ? AND r.data_hora < ?)");
      rangeParams.add(Timestamp.valueOf(dias.get(i).atStartOfDay()));
      rangeParams.add(Timestamp.valueOf(dias.get(i).plusDays(1).atStartOfDay()));
    }

    String pageSql =
        baseSelect
            + whereListStr
            + " AND ("
            + ranges
            + ") "
            + " ORDER BY DATE(r.data_hora) DESC, r.data_hora DESC NULLS LAST, r.id DESC";

    List<Object> pageParams = new ArrayList<>(paramsList);
    pageParams.addAll(rangeParams);

    List<Relatorio> relatorios =
        jdbcTemplate.query(pageSql, RELATORIO_EXTRACTOR, pageParams.toArray());

    // -------- TOTAL DO DIA (somente positivos) + AGRUPAMENTO ----------
    // totalDia respeita os mesmos filtros da listagem (whereList) + dia específico + valor>0
    Map<LocalDate, Float> totalDiaMap =
        buscarTotaisPorDiaPositivos(baseFrom, whereListStr, paramsList, dias);

    Map<LocalDate, List<Relatorio>> porDia = new LinkedHashMap<>();
    for (LocalDate d : dias) porDia.put(d, new ArrayList<>());
    for (Relatorio r : relatorios) {
      LocalDate dia = r.dataHora() != null ? r.dataHora().toLocalDate() : null;
      if (dia != null && porDia.containsKey(dia)) porDia.get(dia).add(r);
    }

    List<RelatorioDia> grupos =
        dias.stream()
            .map(
                d ->
                    new RelatorioDia(
                        d, totalDiaMap.getOrDefault(d, 0f), porDia.getOrDefault(d, List.of())))
            .toList();

    Page<RelatorioDia> pageDias = new PageImpl<>(grupos, pageable, totalDias);

    return new RelatorioExtratoResponse(
        balancoGeral,
        totalEntradas,
        totalSaidas,
        totalDinheiro,
        totalDinheiroSaida,
        balancoDinheiro,
        pageDias);
  }

  @Transactional
  public Relatorio insert(Relatorio.RelatorioRequest request, Long funcionarioPessoaId) {
    String sql =
        """
            INSERT INTO relatorio (
                data_hora,
                relatorio,
                valor,
                fk_tipo_pagamento,
                fk_funcionario,
                quarto_id
            ) VALUES (now(), ?, ?, ?, ?, ?)
            """;

    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
          ps.setString(1, request.relatorio());
          ps.setDouble(2, request.valor());
          ps.setLong(3, request.tipoPagamentoId());
          ps.setLong(4, funcionarioPessoaId);

          if (request.quartoId() != null) ps.setLong(5, request.quartoId());
          else ps.setNull(5, Types.BIGINT);
          return ps;
        },
        keyHolder);

    Long id =
        keyHolder.getKeys() != null && keyHolder.getKeys().containsKey("id")
            ? ((Number) keyHolder.getKeys().get("id")).longValue()
            : null;

    return getByIdOrThrow(id);
  }

  @Transactional
  public Relatorio update(Long id, Relatorio.RelatorioRequest request, Long funcionarioPessoaId) {
    if (id == null) throw new IllegalArgumentException("id é obrigatório.");

    String sql =
        """
            UPDATE relatorio SET
                data_hora = ?,
                relatorio = ?,
                valor = ?,
                fk_tipo_pagamento = ?,
                fk_funcionario = ?,
                quarto_id = ?
            WHERE id = ?
            """;

    int rows =
        jdbcTemplate.update(
            sql,
            Timestamp.valueOf(request.dataHora()),
            request.relatorio(),
            request.valor(),
            request.tipoPagamentoId(),
            funcionarioPessoaId,
            request.quartoId(),
            id);

    if (rows == 0) throw new NotFoundException("Relatório não encontrado para o id: " + id);
    return getByIdOrThrow(id);
  }

  private Relatorio getByIdOrThrow(Long id) {
    if (id == null) throw new IllegalStateException("Registro salvo sem ID (verifique RETURNING).");

    RelatorioExtratoResponse resp =
        buscar(id, null, null, null, null, null, null, Pageable.ofSize(1));
    if (resp == null || resp.page() == null || resp.page().isEmpty())
      throw new NotFoundException("Relatório não encontrado para o id: " + id);

    RelatorioDia dia = resp.page().getContent().getFirst();
    if (dia.content() == null || dia.content().isEmpty())
      throw new NotFoundException("Relatório não encontrado para o id: " + id);

    return dia.content().getFirst();
  }

  private Totais buscarTotaisGerais(String baseFromCount, String where, List<Object> params) {
    String sql =
        """
            SELECT
              COALESCE(SUM(CASE WHEN r.valor > 0 THEN r.valor ELSE 0 END), 0) AS total_entradas,
              COALESCE(SUM(CASE WHEN r.valor < 0 THEN r.valor ELSE 0 END), 0) AS total_saidas
            """
            + baseFromCount
            + where;

    try {
      return jdbcTemplate.queryForObject(
          sql,
          (rs, rowNum) ->
              new Totais(
                  rs.getObject("total_entradas", Double.class),
                  rs.getObject("total_saidas", Double.class)),
          params.toArray());
    } catch (EmptyResultDataAccessException ex) {
      return new Totais(0d, 0d);
    }
  }

  private TotaisDinheiro buscarTotaisDinheiro(
      String baseFromCount, String where, List<Object> params) {
    String sql =
        """
            SELECT
              COALESCE(SUM(CASE WHEN r.valor > 0 THEN r.valor ELSE 0 END), 0) AS total_dinheiro,
              COALESCE(SUM(CASE WHEN r.valor < 0 THEN r.valor ELSE 0 END), 0) AS total_dinheiro_saida
            """
            + baseFromCount
            + where;

    try {
      return jdbcTemplate.queryForObject(
          sql,
          (rs, rowNum) ->
              new TotaisDinheiro(
                  rs.getObject("total_dinheiro", Double.class),
                  rs.getObject("total_dinheiro_saida", Double.class)),
          params.toArray());
    } catch (EmptyResultDataAccessException ex) {
      return new TotaisDinheiro(0d, 0d);
    }
  }

  private Map<LocalDate, Float> buscarTotaisPorDiaPositivos(
      String baseFrom, String whereList, List<Object> paramsList, List<LocalDate> dias) {

    if (dias == null || dias.isEmpty()) return Map.of();

    // monta IN em datas
    String in = String.join(",", Collections.nCopies(dias.size(), "?"));

    String sql =
        """
            SELECT
              DATE(r.data_hora) AS dia,
              COALESCE(SUM(CASE WHEN r.valor > 0 THEN r.valor ELSE 0 END), 0) AS total_dia
            """
            + baseFrom
            + whereList
            + " AND DATE(r.data_hora) IN ("
            + in
            + ") "
            + " GROUP BY DATE(r.data_hora) ";

    List<Object> params = new ArrayList<>(paramsList);
    params.addAll(dias);

    return jdbcTemplate.query(
        sql,
        rs -> {
          Map<LocalDate, Float> map = new HashMap<>();
          while (rs.next()) {
            LocalDate dia = rs.getObject("dia", LocalDate.class);
            Double totalDia = rs.getObject("total_dia", Double.class);
            map.put(dia, toFloat(totalDia));
          }
          return map;
        },
        params.toArray());
  }
}
