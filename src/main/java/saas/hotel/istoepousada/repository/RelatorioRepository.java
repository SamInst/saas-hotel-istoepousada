package saas.hotel.istoepousada.repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import saas.hotel.istoepousada.dto.Funcionario;
import saas.hotel.istoepousada.dto.Pagamento;
import saas.hotel.istoepousada.dto.Quarto;
import saas.hotel.istoepousada.dto.Relatorio;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;

@Repository
public class RelatorioRepository {
  private final JdbcTemplate jdbcTemplate;
  private final PagamentoRepository pagamentoRepository;
  private final PessoaRepository pessoaRepository;

  public RelatorioRepository(
      JdbcTemplate jdbcTemplate,
      PagamentoRepository pagamentoRepository,
      PessoaRepository pessoaRepository) {
    this.jdbcTemplate = jdbcTemplate;
    this.pagamentoRepository = pagamentoRepository;
    this.pessoaRepository = pessoaRepository;
  }

  private static float toFloat(Double value) {
    return value == null ? 0f : value.floatValue();
  }

  public Relatorio.Extrato buscar(
      Long id,
      LocalDate dataInicio,
      LocalDate dataFim,
      Long funcionarioId,
      Long quartoId,
      Long tipoPagamentoId,
      Relatorio.Registro registro,
      Boolean despesaPessoal,
      Pageable pageable) {

    String baseFrom =
        """
                        FROM relatorio r
                        LEFT JOIN pagamento pg ON pg.id = r.fk_pagamento
                        LEFT JOIN tipo_pagamento tp ON tp.id = pg.fk_tipo_pagamento
                        LEFT JOIN quarto q ON q.id = r.quarto_id
                        LEFT JOIN pessoa f ON f.id = r.fk_funcionario
                        """;

    String baseSelect =
        """
                        SELECT
                            r.id                            AS relatorio_id,
                            r.data_hora                     AS relatorio_data_hora,
                            r.relatorio                     AS relatorio_descricao,
                            r.valor_historico_dinheiro      AS relatorio_valor_historico_dinheiro,
                            r.despesa_pessoal               AS relatorio_despesa_pessoal,

                            q.id                            AS quarto_id,
                            q.descricao                     AS quarto_descricao,

                            fr.id                           AS funcionario_id,
                            pfr.nome                        AS funcionario_nome,

                            pg.id                           AS pagamento_id,
                            pg.data_hora_registro           AS pagamento_data_hora_registro,
                            pg.nome_pagador                 AS pagamento_nome_pagador,
                            pg.descricao                    AS pagamento_descricao,
                            pg.valor                        AS pagamento_valor,
                            pg.cancelado                    AS pagamento_cancelado,

                            tp.id                           AS tipo_pagamento_id,
                            tp.descricao                    AS tipo_pagamento_descricao,

                            pd.id                           AS pagamento_desconto_id,
                            pd.porcentagem                  AS pagamento_desconto_porcentagem,
                            pd.valor                        AS pagamento_desconto_valor,
                            pd.data_hora_registro           AS pagamento_desconto_data_hora_registro,

                            fpd.id                          AS pagamento_desconto_funcionario_id,
                            pfd.nome                        AS pagamento_desconto_funcionario_nome,

                            pg.path_arquivo                 AS pagamento_path_arquivo,

                        FROM relatorio r
                        LEFT JOIN pagamento pg ON pg.id = r.fk_pagamento
                        LEFT JOIN tipo_pagamento tp ON tp.id = pg.fk_tipo_pagamento
                        LEFT JOIN quarto q ON q.id = r.quarto_id

                        LEFT JOIN funcionario fr ON fr.id = r.fk_funcionario
                        JOIN pessoa pfr ON pfr.id = r.fk_funcionario

                        LEFT JOIN pagamento_desconto pd ON pd.fk_pagamento = pg.id
                        LEFT JOIN funcionario fpd ON fpd.id = pd.fk_funcionario
                        JOIN pessoa pfd ON pfd.id = pd.fk_funcionario
                        """;

    StringBuilder where = new StringBuilder(" WHERE 1 = 1 ");
    List<Object> params = new ArrayList<>();

    if (id != null) {
      where.append(" AND r.id = ? ");
      params.add(id);
    }
    if (dataInicio != null) {
      where.append(" AND r.data_hora >= ? ");
      params.add(Timestamp.valueOf(dataInicio.atStartOfDay()));
    }
    if (dataFim != null) {
      where.append(" AND r.data_hora < ? ");
      params.add(Timestamp.valueOf(dataFim.plusDays(1).atStartOfDay()));
    }
    if (funcionarioId != null) {
      where.append(" AND r.fk_funcionario = ? ");
      params.add(funcionarioId);
    }
    if (quartoId != null) {
      where.append(" AND r.quarto_id = ? ");
      params.add(quartoId);
    }
    if (tipoPagamentoId != null) {
      where.append(" AND tp.id = ? ");
      params.add(tipoPagamentoId);
    }
    if (despesaPessoal != null) {
      where.append(" AND r.despesa_pessoal = ? ");
      params.add(despesaPessoal);
    }
    if (registro != null) {
      if (registro == Relatorio.Registro.ENTRADA) {
        where.append(" AND pg.valor > 0 ");
      } else if (registro == Relatorio.Registro.SAIDA) {
        where.append(" AND pg.valor < 0 ");
      }
    }

    Map<String, Relatorio.Extrato.Resumo> pagamentos =
        buscarTotaisPorTipoPagamento(where.toString(), params);

    long totalDias;
    try {
      totalDias =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(DISTINCT DATE(r.data_hora)) " + baseFrom + where,
              Long.class,
              params.toArray());
    } catch (EmptyResultDataAccessException ex) {
      totalDias = 0L;
    }

    if (totalDias == 0) {
      return new Relatorio.Extrato(pagamentos, new PageImpl<>(List.of(), pageable, 0));
    }

    String diasSql =
        """
                        SELECT DISTINCT DATE(r.data_hora) AS dia
                        """
            + baseFrom
            + where
            + """
                        ORDER BY dia DESC
                        LIMIT ? OFFSET ?
                        """;

    List<Object> diasParams = new ArrayList<>(params);
    diasParams.add(pageable.getPageSize());
    diasParams.add(pageable.getOffset());

    List<LocalDate> dias =
        jdbcTemplate.query(
            diasSql, (rs, rowNum) -> rs.getObject("dia", LocalDate.class), diasParams.toArray());

    if (dias.isEmpty()) {
      return new Relatorio.Extrato(pagamentos, new PageImpl<>(List.of(), pageable, totalDias));
    }

    StringBuilder ranges = new StringBuilder();
    List<Object> rangeParams = new ArrayList<>();

    for (int i = 0; i < dias.size(); i++) {
      if (i > 0) {
        ranges.append(" OR ");
      }
      ranges.append("(r.data_hora >= ? AND r.data_hora < ?)");
      rangeParams.add(Timestamp.valueOf(dias.get(i).atStartOfDay()));
      rangeParams.add(Timestamp.valueOf(dias.get(i).plusDays(1).atStartOfDay()));
    }

    String pageSql =
        baseSelect
            + where
            + " AND ("
            + ranges
            + ") "
            + " ORDER BY DATE(r.data_hora) DESC, r.data_hora DESC NULLS LAST, r.id DESC";

    List<Object> pageParams = new ArrayList<>(params);
    pageParams.addAll(rangeParams);

    List<Relatorio> relatorios =
        jdbcTemplate.query(
            pageSql,
            (rs, rowNum) -> {
              Long funcionarioRowId = rs.getObject("funcionario_id", Long.class);
              Long quartoRowId = rs.getObject("quarto_id", Long.class);
              Long tipoPagamentoRowId = rs.getObject("tipo_pagamento_id", Long.class);
              UUID pagamentoId = rs.getObject("pagamento_id", UUID.class);

              Funcionario.Nome funcionario =
                  funcionarioRowId == null
                      ? null
                      : new Funcionario.Nome(funcionarioRowId, rs.getString("funcionario_nome"));

              Quarto.Descricao quarto =
                  quartoRowId == null
                      ? null
                      : new Quarto.Descricao(quartoRowId, rs.getString("quarto_descricao"));

              Pagamento pagamento =
                  pagamentoId == null
                      ? null
                      : new Pagamento(
                          pagamentoId,
                          tipoPagamentoRowId == null
                              ? null
                              : new Pagamento.TipoPagamento(
                                  tipoPagamentoRowId, rs.getString("tipo_pagamento_descricao")),
                          funcionario,
                          rs.getObject("pagamento_data_hora_registro", LocalDateTime.class),
                          rs.getString("pagamento_nome_pagador"),
                          rs.getString("pagamento_descricao"),
                          rs.getDouble("pagamento_valor"),
                          rs.getBoolean("pagamento_cancelado"),
                          new Pagamento.Desconto(
                              rs.getObject("pagamento_desconto_id", UUID.class),
                              new Funcionario.Nome(
                                  rs.getLong("pagamento_desconto_funcionario_id"),
                                  rs.getString("pagamento_desconto_funcionario_nome")),
                              rs.getInt("pagamento_desconto_porcentagem"),
                              rs.getDouble("pagamento_desconto_valor"),
                              rs.getTimestamp("pagamento_desconto_data_hora_registro")
                                  .toLocalDateTime()),
                          rs.getString("pagamento_path_arquivo"));

              Double valorPagamento =
                  rs.getObject("pagamento_valor", Double.class) != null
                      ? rs.getObject("pagamento_valor", Double.class)
                      : 0d;

              return new Relatorio(
                  rs.getLong("relatorio_id"),
                  rs.getObject("relatorio_data_hora", LocalDateTime.class),
                  rs.getString("relatorio_descricao"),
                  valorPagamento,
                  funcionario,
                  pagamento,
                  quarto,
                  rs.getObject("relatorio_valor_historico_dinheiro", Double.class),
                  rs.getObject("relatorio_despesa_pessoal", Boolean.class));
            },
            pageParams.toArray());

    Map<LocalDate, Float> totalEntradaDiaMap =
        buscarTotaisEntradaPorDia(where.toString(), params, dias);

    Map<LocalDate, Float> totalSaidaDiaMap =
        buscarTotaisSaidaPorDia(where.toString(), params, dias);

    Map<LocalDate, List<Relatorio>> porDia = new LinkedHashMap<>();
    for (LocalDate dia : dias) {
      porDia.put(dia, new ArrayList<>());
    }

    for (Relatorio relatorio : relatorios) {
      LocalDate dia =
          relatorio.data_hora_registro() != null
              ? relatorio.data_hora_registro().toLocalDate()
              : null;
      if (dia != null && porDia.containsKey(dia)) {
        porDia.get(dia).add(relatorio);
      }
    }

    List<Relatorio.Extrato.Diaria> diarias =
        dias.stream()
            .map(
                dia -> {
                  float entradas = totalEntradaDiaMap.getOrDefault(dia, 0f);
                  float saidas = totalSaidaDiaMap.getOrDefault(dia, 0f);
                  return new Relatorio.Extrato.Diaria(
                      dia,
                      entradas,
                      saidas,
                      entradas - saidas,
                      porDia.getOrDefault(dia, List.of()));
                })
            .toList();

    return new Relatorio.Extrato(pagamentos, new PageImpl<>(diarias, pageable, totalDias));
  }

  public Relatorio insert(Relatorio.Request request) {
    Double valorHistoricoDinheiro = calcularNovoHistoricoDinheiro(request.pagamento().valor());
    boolean despesaPessoal = Boolean.TRUE.equals(request.despesa_pessoal());
    var pagamento = pagamentoRepository.create(request.pagamento());

    return jdbcTemplate.queryForObject(
        """
                INSERT INTO relatorio (
                    data_hora,
                    relatorio,
                    fk_funcionario,
                    quarto_id,
                    valor_historico_dinheiro,
                    despesa_pessoal,
                    fk_pagamento
                ) VALUES (now(), ?, ?, ?, ?, ?, ?) returning id
                """,
        Relatorio.class,
        request.relatorio(),
        getFuncionarioId(),
        request.quarto() != null ? request.quarto().id() : null,
        valorHistoricoDinheiro,
        despesaPessoal,
        pagamento.id());
  }

  public Relatorio update(Relatorio.Update relatorio) {
    Relatorio relatorioAntigo = getByIdOrThrow(relatorio.id());
    Double valorHistoricoDinheiro = recalcularHistoricoDinheiro(relatorioAntigo, relatorio.valor());
    boolean despesaPessoal = Boolean.TRUE.equals(relatorio.despesa_pessoal());

    String sql =
        """
                        UPDATE relatorio SET
                            relatorio = ?,
                            quarto_id = ?,
                            valor_historico_dinheiro = ?,
                            despesa_pessoal = ?
                        WHERE id = ?
                        """;

    Long quartoId = relatorio.quarto() != null ? relatorio.quarto().id() : null;

    int rows =
        jdbcTemplate.update(
            sql,
            relatorio.descricao(),
            quartoId,
            valorHistoricoDinheiro,
            despesaPessoal,
            relatorio.id());

    if (rows == 0) {
      throw new NotFoundException("Relatório não encontrado para o id: " + relatorio.id());
    }

    recalcularHistoricoPosteriores(relatorio.id(), valorHistoricoDinheiro);

    return getByIdOrThrow(relatorio.id());
  }

  private Double calcularNovoHistoricoDinheiro(Double valorPagamento) {
    Double ultimoHistorico = buscarUltimoHistoricoDinheiro();
    return ultimoHistorico + (valorPagamento != null ? valorPagamento : 0d);
  }

  private Double recalcularHistoricoDinheiro(Relatorio relatorioAntigo, Double novoValorPagamento) {
    Double historicoAnterior = buscarHistoricoAnteriorAoRegistro(relatorioAntigo.id());
    Double valorAntigo = relatorioAntigo.valor() != null ? relatorioAntigo.valor() : 0d;
    return historicoAnterior - valorAntigo + (novoValorPagamento != null ? novoValorPagamento : 0d);
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
      return valor != null ? valor : 0d;
    } catch (EmptyResultDataAccessException ex) {
      return 0d;
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
      return jdbcTemplate.queryForObject(sql, Double.class, relatorioId, relatorioId, relatorioId);
    } catch (EmptyResultDataAccessException ex) {
      return 0d;
    }
  }

  private void recalcularHistoricoPosteriores(Long relatorioId, Double novoHistorico) {
    String sqlBuscar =
        """
                        SELECT r.id, COALESCE(pg.valor, 0) AS valor
                        FROM relatorio r
                        LEFT JOIN pagamento pg ON pg.id = r.fk_pagamento
                        WHERE r.data_hora > (SELECT data_hora FROM relatorio WHERE id = ?)
                           OR (r.data_hora = (SELECT data_hora FROM relatorio WHERE id = ?) AND r.id > ?)
                        ORDER BY r.data_hora, r.id
                        """;

    List<Map<String, Object>> posteriores =
        jdbcTemplate.queryForList(sqlBuscar, relatorioId, relatorioId, relatorioId);

    double historicoAcumulado = novoHistorico != null ? novoHistorico : 0d;

    for (Map<String, Object> registro : posteriores) {
      Long id = ((Number) registro.get("id")).longValue();
      Number valor = (Number) registro.get("valor");
      historicoAcumulado += valor != null ? valor.doubleValue() : 0d;

      jdbcTemplate.update(
          "UPDATE relatorio SET valor_historico_dinheiro = ? WHERE id = ?", historicoAcumulado, id);
    }
  }

  private Relatorio getByIdOrThrow(Long id) {
    if (id == null) {
      throw new IllegalStateException("Registro salvo sem ID.");
    }

    Relatorio.Extrato resp =
        buscar(id, null, null, null, null, null, null, null, Pageable.ofSize(1));

    if (resp == null || resp.page() == null || resp.page().isEmpty()) {
      throw new NotFoundException("Relatório não encontrado para o id: " + id);
    }

    Relatorio.Extrato.Diaria diaria = resp.page().getContent().getFirst();
    if (diaria.relatorios() == null || diaria.relatorios().isEmpty()) {
      throw new NotFoundException("Relatório não encontrado para o id: " + id);
    }

    return diaria.relatorios().getFirst();
  }

  private Map<LocalDate, Float> buscarTotaisEntradaPorDia(
      String where, List<Object> paramsBase, List<LocalDate> dias) {
    return buscarTotaisPorDia(where, paramsBase, dias, true);
  }

  private Map<LocalDate, Float> buscarTotaisSaidaPorDia(
      String where, List<Object> paramsBase, List<LocalDate> dias) {
    return buscarTotaisPorDia(where, paramsBase, dias, false);
  }

  private Map<LocalDate, Float> buscarTotaisPorDia(
      String where, List<Object> paramsBase, List<LocalDate> dias, boolean entradas) {
    if (dias == null || dias.isEmpty()) {
      return Map.of();
    }

    String in = String.join(",", Collections.nCopies(dias.size(), "?"));

    String soma =
        entradas
            ? "COALESCE(SUM(CASE WHEN pg.valor > 0 THEN pg.valor ELSE 0 END), 0)"
            : "COALESCE(SUM(CASE WHEN pg.valor < 0 THEN ABS(pg.valor) ELSE 0 END), 0)";

    String sql =
        """
                        SELECT
                          DATE(r.data_hora) AS dia,
                        """
            + soma
            + """
                         AS total_dia
                        FROM relatorio r
                        LEFT JOIN pagamento pg ON pg.id = r.fk_pagamento
                        LEFT JOIN tipo_pagamento tp ON tp.id = pg.fk_tipo_pagamento
                        WHERE 1 = 1
                        """
            + where.replaceFirst(" WHERE 1 = 1 ", "")
            + " AND DATE(r.data_hora) IN ("
            + in
            + ") GROUP BY DATE(r.data_hora) ";

    List<Object> params = new ArrayList<>(paramsBase);
    params.addAll(dias);

    return jdbcTemplate.query(
        sql,
        rs -> {
          Map<LocalDate, Float> map = new HashMap<>();
          while (rs.next()) {
            map.put(
                rs.getObject("dia", LocalDate.class),
                toFloat(rs.getObject("total_dia", Double.class)));
          }
          return map;
        },
        params.toArray());
  }

  private Map<String, Relatorio.Extrato.Resumo> buscarTotaisPorTipoPagamento(
      String where, List<Object> params) {

    String sqlAgregado =
        """
                        SELECT
                          tp.id AS tipo_id,
                          tp.descricao AS descricao,
                          COALESCE(SUM(CASE WHEN pg.valor > 0 THEN pg.valor ELSE 0 END), 0) AS receitas,
                          COALESCE(SUM(CASE WHEN pg.valor < 0 THEN ABS(pg.valor) ELSE 0 END), 0) AS despesas
                        FROM relatorio r
                        LEFT JOIN pagamento pg ON pg.id = r.fk_pagamento
                        LEFT JOIN tipo_pagamento tp ON tp.id = pg.fk_tipo_pagamento
                        """
            + where
            + """
                        GROUP BY tp.id, tp.descricao
                        """;

    Map<Long, Relatorio.Extrato.Resumo> agregadosPorId = new HashMap<>();

    jdbcTemplate.query(
        sqlAgregado,
        rs -> {
          while (rs.next()) {
            Long tipoId = rs.getObject("tipo_id", Long.class);
            Double receitas = rs.getObject("receitas", Double.class);
            Double despesas = rs.getObject("despesas", Double.class);
            agregadosPorId.put(tipoId, Relatorio.Extrato.Resumo.of(receitas, despesas));
          }
        },
        params.toArray());

    String sqlTipos =
        """
                        SELECT id, descricao
                        FROM tipo_pagamento
                        ORDER BY descricao
                        """;

    Map<String, Relatorio.Extrato.Resumo> mapa = new LinkedHashMap<>();

    jdbcTemplate.query(
        sqlTipos,
        rs -> {
          while (rs.next()) {
            Long tipoId = rs.getObject("id", Long.class);
            String descricao = rs.getString("descricao");
            mapa.put(
                descricao,
                agregadosPorId.getOrDefault(tipoId, Relatorio.Extrato.Resumo.of(0d, 0d)));
          }
        });

    double totalReceitas = 0d;
    double totalDespesas = 0d;
    for (Relatorio.Extrato.Resumo resumo : mapa.values()) {
      totalReceitas += resumo.receitas();
      totalDespesas += resumo.despesas();
    }

    mapa.put("TOTAL", Relatorio.Extrato.Resumo.of(totalReceitas, totalDespesas));
    return mapa;
  }

  public Long getFuncionarioId() {
    return pessoaRepository.getFuncionarioIdFromRequest();
  }
}
