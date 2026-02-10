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
import saas.hotel.istoepousada.dto.Relatorio;
import saas.hotel.istoepousada.dto.RelatorioDia;
import saas.hotel.istoepousada.dto.RelatorioExtratoResponse;
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
      Relatorio.Valores valores,
      Pageable pageable) {

    String baseFrom =
        """
                FROM relatorio r
                INNER JOIN pessoa pbase ON pbase.id = r.fk_funcionario
                """;

    String baseSelect =
        """
                SELECT
                    r.id                       AS relatorio_id,
                    r.data_hora                AS data_hora,
                    r.relatorio                AS relatorio,
                    r.valor                    AS valor,
                    r.valor_historico_dinheiro AS valor_historico_dinheiro,

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
      if (valores == Relatorio.Valores.ENTRADA) whereBase.append(" AND r.valor > 0 ");
      else if (valores == Relatorio.Valores.SAIDA) whereBase.append(" AND r.valor < 0 ");
    }

    StringBuilder whereList = new StringBuilder(whereBase);
    List<Object> paramsList = new ArrayList<>(paramsBase);

    if (tipoPagamentoId != null) {
      whereList.append(" AND r.fk_tipo_pagamento = ? ");
      paramsList.add(tipoPagamentoId);
    }

    Totais totaisGerais = buscarTotaisGerais(baseFrom, whereList.toString(), paramsList);

    TotaisDinheiro totaisDinheiro =
        buscarTotaisDinheiro(baseFrom, whereBase + " AND r.fk_tipo_pagamento = 1 ", paramsBase);

    Float totalEntradas = toFloat(totaisGerais.totalEntradas());
    Float totalSaidas = toFloat(totaisGerais.totalSaidas());
    Float balancoGeral = totalEntradas + totalSaidas;

    Float totalDinheiro = toFloat(totaisDinheiro.totalDinheiro());
    Float totalDinheiroSaida = toFloat(totaisDinheiro.totalDinheiroSaida());
    Float balancoDinheiro = totalDinheiro + totalDinheiroSaida;

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
            + whereList
            + " AND ("
            + ranges
            + ") "
            + " ORDER BY DATE(r.data_hora) DESC, r.data_hora DESC NULLS LAST, r.id DESC";

    List<Object> pageParams = new ArrayList<>(paramsList);
    pageParams.addAll(rangeParams);

    List<Relatorio> relatorios =
        jdbcTemplate.query(pageSql, RELATORIO_EXTRACTOR, pageParams.toArray());

    Map<LocalDate, Float> totalDiaMap =
        buscarTotaisPorDiaPositivos(baseFrom, whereList.toString(), paramsList, dias);

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

  public Relatorio insert(Relatorio.RelatorioRequest request, Long funcionarioPessoaId) {
    Double valorHistoricoDinheiro = calcularNovoHistoricoDinheiro(request);

    String sql =
        """
                INSERT INTO relatorio (
                    data_hora,
                    relatorio,
                    valor,
                    fk_tipo_pagamento,
                    fk_funcionario,
                    quarto_id,
                    valor_historico_dinheiro
                ) VALUES (now(), ?, ?, ?, ?, ?, ?)
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
          if (request.quartoId() != null && request.quartoId() == 0L) ps.setNull(5, Types.BIGINT);
          else ps.setNull(5, Types.BIGINT);

          ps.setDouble(6, valorHistoricoDinheiro);
          return ps;
        },
        keyHolder);

    Long id =
        keyHolder.getKeys() != null && keyHolder.getKeys().containsKey("id")
            ? ((Number) keyHolder.getKeys().get("id")).longValue()
            : null;

    return getByIdOrThrow(id);
  }

  public Relatorio update(Long id, Relatorio.RelatorioRequest request, Long funcionarioPessoaId) {
    if (id == null) throw new IllegalArgumentException("id é obrigatório.");

    Relatorio relatorioAntigo = getByIdOrThrow(id);
    Double valorHistoricoDinheiro = recalcularHistoricoDinheiro(relatorioAntigo, request);

    String sql =
        """
                UPDATE relatorio SET
                    relatorio = ?,
                    valor = ?,
                    fk_tipo_pagamento = ?,
                    fk_funcionario = ?,
                    quarto_id = ?,
                    valor_historico_dinheiro = ?
                WHERE id = ?
                """;

    int rows =
        jdbcTemplate.update(
            sql,
            request.relatorio(),
            request.valor(),
            request.tipoPagamentoId(),
            funcionarioPessoaId,
            request.quartoId(),
            valorHistoricoDinheiro,
            id);

    if (rows == 0) throw new NotFoundException("Relatório não encontrado para o id: " + id);

    recalcularHistoricoPosteriores(id, valorHistoricoDinheiro);

    return getByIdOrThrow(id);
  }

  private Double calcularNovoHistoricoDinheiro(Relatorio.RelatorioRequest request) {
    Double ultimoHistorico = buscarUltimoHistoricoDinheiro();

    if (request.tipoPagamentoId() == 1) {
      return ultimoHistorico + request.valor();
    }

    return ultimoHistorico;
  }

  private Double recalcularHistoricoDinheiro(
      Relatorio relatorioAntigo, Relatorio.RelatorioRequest request) {
    Double historicoAnterior = buscarHistoricoAnteriorAoRegistro(relatorioAntigo.id());

    Long tipoPagamentoAntigoId = relatorioAntigo.tipoPagamentoId();
    Double valorAntigo = relatorioAntigo.valor();

    if (tipoPagamentoAntigoId == 1) {
      historicoAnterior -= valorAntigo;
    }

    if (request.tipoPagamentoId() == 1) {
      return historicoAnterior + request.valor();
    }

    return historicoAnterior;
  }

  private Double buscarUltimoHistoricoDinheiro() {
    String sql =
        """
                SELECT COALESCE(valor_historico_dinheiro, 0)
                FROM relatorio
                ORDER BY data_hora DESC, id DESC
                LIMIT 1
                """;

    try {
      Double valor = jdbcTemplate.queryForObject(sql, Double.class);
      return valor != null ? valor : 0.0;
    } catch (EmptyResultDataAccessException ex) {
      return 0.0;
    }
  }

  private Double buscarHistoricoAnteriorAoRegistro(Long relatorioId) {
    String sql =
        """
                SELECT COALESCE(valor_historico_dinheiro, 0)
                FROM relatorio r1
                WHERE r1.data_hora < (SELECT data_hora FROM relatorio WHERE id = ?)
                   OR (r1.data_hora = (SELECT data_hora FROM relatorio WHERE id = ?) AND r1.id < ?)
                ORDER BY r1.data_hora DESC, r1.id DESC
                LIMIT 1
                """;

    try {
      Double valor =
          jdbcTemplate.queryForObject(sql, Double.class, relatorioId, relatorioId, relatorioId);
      return valor != null ? valor : 0.0;
    } catch (EmptyResultDataAccessException ex) {
      return 0.0;
    }
  }

  private void recalcularHistoricoPosteriores(Long relatorioId, Double novoHistorico) {
    String sqlBuscar =
        """
                SELECT id, fk_tipo_pagamento, valor
                FROM relatorio
                WHERE data_hora > (SELECT data_hora FROM relatorio WHERE id = ?)
                   OR (data_hora = (SELECT data_hora FROM relatorio WHERE id = ?) AND id > ?)
                ORDER BY data_hora ASC, id ASC
                """;

    List<Map<String, Object>> posteriores =
        jdbcTemplate.queryForList(sqlBuscar, relatorioId, relatorioId, relatorioId);

    Double historicoAcumulado = novoHistorico;

    for (Map<String, Object> registro : posteriores) {
      Long id = ((Number) registro.get("id")).longValue();
      Long tipoPagamentoId = ((Number) registro.get("fk_tipo_pagamento")).longValue();
      Double valor = ((Number) registro.get("valor")).doubleValue();

      if (tipoPagamentoId == 1) {
        historicoAcumulado += valor;
      }

      jdbcTemplate.update(
          "UPDATE relatorio SET valor_historico_dinheiro = ? WHERE id = ?", historicoAcumulado, id);
    }
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
