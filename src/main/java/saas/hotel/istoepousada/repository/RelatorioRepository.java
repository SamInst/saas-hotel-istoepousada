package saas.hotel.istoepousada.repository;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import saas.hotel.istoepousada.dto.Pagamento;
import saas.hotel.istoepousada.dto.Relatorio;
import saas.hotel.istoepousada.handler.exceptions.BusinessException;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;

@Repository
public class RelatorioRepository {
  private final JdbcTemplate jdbcTemplate;

  public RelatorioRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public static String BASE_SQL =
      """
                    SELECT
                        relatorio.id                               AS relatorio_id,
                        relatorio.data_hora                        AS relatorio_data_hora,
                        relatorio.relatorio                        AS relatorio_descricao,
                        relatorio.valor_historico_dinheiro         AS relatorio_valor_historico_dinheiro,
                        relatorio.despesa_pessoal                  AS relatorio_despesa_pessoal,

                        q.id                                       AS quarto_id,
                        q.descricao                                AS quarto_descricao,

                        funcionario_relatorio.id                   AS relatorio_funcionario_id,
                        pessoa_funcionario_relatorio.nome          AS relatorio_funcionario_nome,

                        pagamento.id                               AS pagamento_id,
                        pagamento.data_hora_registro               AS pagamento_data_hora_registro,
                        pagamento.nome_pagador                     AS pagamento_nome_pagador,
                        pagamento.descricao                        AS pagamento_descricao,
                        pagamento.valor                            AS pagamento_valor,
                        pagamento.cancelado                        AS pagamento_cancelado,
                        pagamento.path_arquivo                     AS pagamento_path_arquivo,

                        funcionario_pagamento.id                   AS pagamento_funcionario_id,
                        pessoa_funcionario_pagamento.nome          AS pagamento_funcionario_nome,

                        tipo_pagamento.id                          AS tipo_pagamento_id,
                        tipo_pagamento.descricao                   AS tipo_pagamento_descricao,

                        mc.id                                      AS pagamento_motivo_id,
                        mc.motivo_cancelamento                     AS pagamento_motivo_cancelamento,
                        mc.data_hora_registro                      AS pagamento_motivo_data_hora_registro,
                        funcionario_motivo.id                      AS pagamento_motivo_funcionario_id,
                        pessoa_funcionario_motivo.nome             AS pagamento_motivo_funcionario_nome
                    FROM relatorio
                    LEFT JOIN pagamento ON pagamento.id = relatorio.fk_pagamento
                    LEFT JOIN tipo_pagamento ON tipo_pagamento.id = pagamento.fk_tipo_pagamento
                    LEFT JOIN quarto q  ON q.id = relatorio.quarto_id

                    LEFT JOIN funcionario funcionario_relatorio ON funcionario_relatorio.id = relatorio.fk_funcionario
                    LEFT JOIN pessoa pessoa_funcionario_relatorio ON pessoa_funcionario_relatorio.id = funcionario_relatorio.fk_pessoa

                    LEFT JOIN funcionario funcionario_pagamento ON funcionario_pagamento.id = pagamento.fk_funcionario
                    LEFT JOIN pessoa pessoa_funcionario_pagamento ON pessoa_funcionario_pagamento.id = funcionario_pagamento.fk_pessoa

                    LEFT JOIN LATERAL (
                        SELECT * FROM public.pagamento_motivo_cancelamento
                        WHERE fk_pagamento = pagamento.id
                        ORDER BY data_hora_registro DESC LIMIT 1
                    ) mc ON true
                    LEFT JOIN funcionario funcionario_motivo ON funcionario_motivo.id = mc.fk_funcionario
                    LEFT JOIN pessoa pessoa_funcionario_motivo ON pessoa_funcionario_motivo.id = funcionario_motivo.fk_pessoa
                    """;

  public Relatorio.Extrato buscar(
      Long id,
      LocalDate data_inicio,
      LocalDate data_fim,
      Long funcionario_id,
      Long quarto_id,
      Long tipo_pagamento_id,
      Relatorio.Registro registro,
      Boolean despesa_pessoal,
      int page,
      int size) {
    Pageable pageable = PageRequest.of(page, size);

    String baseFrom =
        """
                        FROM relatorio relatorio
                        LEFT JOIN pagamento ON pagamento.id = relatorio.fk_pagamento
                        LEFT JOIN tipo_pagamento ON tipo_pagamento.id = pagamento.fk_tipo_pagamento
                        LEFT JOIN quarto ON quarto.id = relatorio.quarto_id
                        LEFT JOIN pessoa pessoa_funcionario ON pessoa_funcionario.id = relatorio.fk_funcionario
                        """;

    // whereTotais usa SOMENTE os filtros explicitamente informados pelo caller.
    // Sem o range de data padrão (último dia), para que os totais do dashboard
    // (receita/despesa/lucro/saldo) reflitam todo o banco quando nenhum filtro é
    // aplicado, e passem a refletir a busca quando algum filtro é aplicado.
    StringBuilder whereTotais = new StringBuilder(" WHERE 1 = 1 ");
    List<Object> paramsTotais = new ArrayList<>();

    if (id != null) {
      whereTotais.append(" AND relatorio.id = ? ");
      paramsTotais.add(id);
    }
    if (data_inicio != null) {
      whereTotais.append(" AND relatorio.data_hora >= ? ");
      paramsTotais.add(Timestamp.valueOf(data_inicio.atStartOfDay()));
    }
    if (data_fim != null) {
      whereTotais.append(" AND relatorio.data_hora < ? ");
      paramsTotais.add(Timestamp.valueOf(data_fim.plusDays(1).atStartOfDay()));
    }
    if (funcionario_id != null) {
      whereTotais.append(" AND relatorio.fk_funcionario = ? ");
      paramsTotais.add(funcionario_id);
    }
    if (quarto_id != null) {
      whereTotais.append(" AND relatorio.quarto_id = ? ");
      paramsTotais.add(quarto_id);
    }
    if (tipo_pagamento_id != null) {
      whereTotais.append(" AND tipo_pagamento.id = ? ");
      paramsTotais.add(tipo_pagamento_id);
    }
    if (despesa_pessoal != null) {
      whereTotais.append(" AND relatorio.despesa_pessoal = ? ");
      paramsTotais.add(despesa_pessoal);
    }
    if (registro != null) {
      if (registro == Relatorio.Registro.ENTRADA) {
        whereTotais.append(" AND pagamento.valor > 0 ");
      } else if (registro == Relatorio.Registro.SAIDA) {
        whereTotais.append(" AND pagamento.valor < 0 ");
      }
    }

    // where = filtros explícitos + range de data padrão (último dia) para a
    // listagem/paginação por dia, mantendo o comportamento atual da tabela.
    StringBuilder where = new StringBuilder(whereTotais);
    List<Object> params = new ArrayList<>(paramsTotais);

    if (data_inicio == null && id == null) {
      where.append(" AND relatorio.data_hora >= ? ");
      params.add(Timestamp.valueOf(LocalDate.now().minusDays(1).atStartOfDay()));
    }
    if (data_fim == null && id == null) {
      where.append(" AND relatorio.data_hora < ? ");
      params.add(Timestamp.valueOf(LocalDate.now().plusDays(1).atStartOfDay()));
    }

    Map<String, Relatorio.Extrato.Resumo> pagamentos =
        buscarTotaisPorTipoPagamento(whereTotais.toString(), paramsTotais);

    long totalDias;
    try {
      totalDias =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(DISTINCT DATE(relatorio.data_hora)) " + baseFrom + where,
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
                        SELECT DISTINCT DATE(relatorio.data_hora) AS dia
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
      ranges.append("(relatorio.data_hora >= ? AND relatorio.data_hora < ?)");
      rangeParams.add(Timestamp.valueOf(dias.get(i).atStartOfDay()));
      rangeParams.add(Timestamp.valueOf(dias.get(i).plusDays(1).atStartOfDay()));
    }

    String pageSql =
        BASE_SQL
            + where
            + " AND ("
            + ranges
            + ") "
            + " ORDER BY DATE(relatorio.data_hora) DESC, relatorio.data_hora DESC NULLS LAST, relatorio.id DESC";

    List<Object> pageParams = new ArrayList<>(params);
    pageParams.addAll(rangeParams);

    List<Relatorio> relatorios =
        jdbcTemplate.query(pageSql, Relatorio.Extrato.ROW_MAPPER, pageParams.toArray());

    Map<LocalDate, Float> totalEntradaDiaMap =
        buscarTotaisEntradaPorDia(where.toString(), params, dias);

    Map<LocalDate, Float> totalSaidaDiaMap =
        buscarTotaisSaidaPorDia(where.toString(), params, dias);

    Map<LocalDate, List<Relatorio>> porDia = new LinkedHashMap<>();
    for (LocalDate dia : dias) {
      porDia.put(dia, new ArrayList<>());
    }

    for (Relatorio relatorio2 : relatorios) {
      LocalDate dia =
          relatorio2.data_hora_registro() != null
              ? relatorio2.data_hora_registro().toLocalDate()
              : null;
      if (dia != null && porDia.containsKey(dia)) {
        porDia.get(dia).add(relatorio2);
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

  public void registrarRelatorio(Pagamento pagamento, Long funcionarioId) {
    Long tipoPagamentoId =
        pagamento.tipo_pagamento() != null ? pagamento.tipo_pagamento().id() : null;
    Float valorHistoricoDinheiro =
        calcularNovoHistoricoDinheiro(pagamento.valor(), tipoPagamentoId);

    jdbcTemplate.update(
        """
                        INSERT INTO relatorio (data_hora, relatorio, fk_funcionario, quarto_id, valor_historico_dinheiro, despesa_pessoal, fk_pagamento)
                        VALUES (now(), ?, ?, null, ?, false, ?)
                        """,
        pagamento.descricao(),
        funcionarioId,
        valorHistoricoDinheiro,
        pagamento.uuid());
  }

  public void registrarRelatorio(
      Relatorio.Request request, Long funcionarioId, UUID pagamentoUuid) {
    Long tipoPagamentoId =
        request.pagamento().tipo_pagamento() != null
            ? request.pagamento().tipo_pagamento().id()
            : null;

    Float valorHistoricoDinheiro =
        calcularNovoHistoricoDinheiro(request.pagamento().valor(), tipoPagamentoId);

    jdbcTemplate.update(
        """
                            INSERT INTO relatorio (data_hora, relatorio, fk_funcionario, quarto_id, valor_historico_dinheiro, despesa_pessoal, fk_pagamento)
                            VALUES (now(), ?, ?, null, ?, false, ?)
                            """,
        request.relatorio(),
        funcionarioId,
        valorHistoricoDinheiro,
        pagamentoUuid);
  }

  public Relatorio insert(Relatorio.Request request, Pagamento pagamento, Long funcionarioId) {
    Long tipoPagamentoId =
        pagamento.tipo_pagamento() != null ? pagamento.tipo_pagamento().id() : null;

    Float valorOriginal = pagamento.valor();

    Float valorHistoricoDinheiro = calcularNovoHistoricoDinheiro(valorOriginal, tipoPagamentoId);

    if (tipoPagamentoId != null && tipoPagamentoId == 1L && valorHistoricoDinheiro < 0) {
      Float saldoAtual = buscarUltimoHistoricoDinheiro();
      throw new BusinessException(
          String.format(
              "Saldo insuficiente no caixa. Saldo atual: R$ %.2f, valor a retirar: R$ %.2f.",
              saldoAtual, Math.abs(valorOriginal != null ? valorOriginal : 0F)));
    }

    boolean despesaPessoal = Boolean.TRUE.equals(request.despesa_pessoal());

    Long id =
        jdbcTemplate.queryForObject(
            """
                                INSERT INTO relatorio (
                                    data_hora,
                                    relatorio,
                                    fk_funcionario,
                                    quarto_id,
                                    valor_historico_dinheiro,
                                    despesa_pessoal,
                                    fk_pagamento
                                ) VALUES (now(), ?, ?, ?, ?, ?, ?)
                                RETURNING id
                                """,
            Long.class,
            request.relatorio(),
            funcionarioId,
            request.quarto() != null ? request.quarto().id() : null,
            valorHistoricoDinheiro,
            despesaPessoal,
            pagamento.uuid());

    return getByIdOrThrow(new Relatorio.Id(id));
  }

  public Relatorio update(Relatorio.Update relatorio, Long funcionarioId) {
    Relatorio relatorioAntigo = getByIdOrThrow(new Relatorio.Id(relatorio.id()));

    // Busca o pagamento atualizado com desconto
    Relatorio relatorioAtualizado = getByIdOrThrow(new Relatorio.Id(relatorio.id()));
    Float valorComDesconto = relatorioAtualizado.valor();
    Long novoTipoPagamentoId =
        relatorioAtualizado.pagamento() != null
                && relatorioAtualizado.pagamento().tipo_pagamento() != null
            ? relatorioAtualizado.pagamento().tipo_pagamento().id()
            : null;

    Float valorHistoricoDinheiro =
        recalcularHistoricoDinheiro(relatorioAntigo, valorComDesconto, novoTipoPagamentoId);
    boolean despesaPessoal = Boolean.TRUE.equals(relatorio.despesa_pessoal());

    Long quartoId = relatorio.quarto() != null ? relatorio.quarto().id() : null;

    Relatorio.Id id =
        new Relatorio.Id(
            jdbcTemplate.queryForObject(
                """
                                        UPDATE relatorio SET
                                            relatorio = ?,
                                            quarto_id = ?,
                                            valor_historico_dinheiro = ?,
                                            despesa_pessoal = ?
                                        WHERE id = ? returning id
                                        """,
                Long.class,
                relatorio.descricao(),
                quartoId,
                valorHistoricoDinheiro,
                despesaPessoal,
                relatorio.id()));

    recalcularHistoricoPosteriores(relatorio.id(), valorHistoricoDinheiro);
    return getByIdOrThrow(id);
  }

  private Float calcularValorComDesconto(Float valorOriginal) {
    if (valorOriginal == null) {
      return 0F;
    }
    if (valorOriginal <= 0) {
      return valorOriginal;
    }

    return valorOriginal;
  }

  private Float calcularNovoHistoricoDinheiro(Float valor, Long tipoPagamentoId) {
    Float ultimoHistorico = buscarUltimoHistoricoDinheiro();

    if (tipoPagamentoId == null || tipoPagamentoId != 1) {
      return ultimoHistorico;
    }

    return ultimoHistorico + (valor != null ? valor : 0F);
  }

  private Float recalcularHistoricoDinheiro(
      Relatorio relatorioAntigo, Float novoValorPagamento, Long novoTipoPagamentoId) {
    Float historicoAnterior = buscarHistoricoAnteriorAoRegistro(relatorioAntigo.id());
    if (novoTipoPagamentoId != null && novoTipoPagamentoId == 1L) {
      return historicoAnterior + (novoValorPagamento != null ? novoValorPagamento : 0F);
    }
    return historicoAnterior;
  }

  private Float buscarUltimoHistoricoDinheiro() {
    String sql =
        """
                        SELECT COALESCE(valor_historico_dinheiro, 0)
                        FROM relatorio
                        ORDER BY data_hora DESC, id DESC
                        LIMIT 1
                        """;
    try {
      Float valor = jdbcTemplate.queryForObject(sql, Float.class);
      return valor != null ? valor : 0F;
    } catch (EmptyResultDataAccessException ex) {
      return 0F;
    }
  }

  private Float buscarHistoricoAnteriorAoRegistro(Long relatorioId) {
    String sql =
        """
                        SELECT COALESCE(valor_historico_dinheiro, 0)
                        FROM relatorio
                        WHERE relatorio.data_hora < (SELECT data_hora FROM relatorio WHERE id = ?)
                           OR (relatorio.data_hora = (SELECT data_hora FROM relatorio WHERE id = ?) AND relatorio.id < ?)
                        ORDER BY relatorio.data_hora DESC, relatorio.id DESC
                        LIMIT 1
                        """;

    try {
      return jdbcTemplate.queryForObject(sql, Float.class, relatorioId, relatorioId, relatorioId);
    } catch (EmptyResultDataAccessException ex) {
      return 0F;
    }
  }

  private void recalcularHistoricoPosteriores(Long relatorioId, Float novoHistorico) {
    String sqlBuscar =
        """
                         SELECT
                            relatorio.id,
                            COALESCE(pagamento.valor, 0) AS valor,
                            tipo_pagamento.id AS tipo_id
                        FROM relatorio
                        LEFT JOIN pagamento ON pagamento.id = relatorio.fk_pagamento
                        LEFT JOIN tipo_pagamento ON tipo_pagamento.id = pagamento.fk_tipo_pagamento

                        WHERE relatorio.data_hora > (SELECT data_hora FROM relatorio WHERE id = ?)
                           OR (relatorio.data_hora = (SELECT data_hora FROM relatorio WHERE id = ?) AND relatorio.id > ?)
                        ORDER BY relatorio.data_hora, relatorio.id
                        """;

    List<Map<String, Object>> posteriores =
        jdbcTemplate.queryForList(sqlBuscar, relatorioId, relatorioId, relatorioId);

    float historicoAcumulado = novoHistorico != null ? novoHistorico : 0F;

    for (Map<String, Object> registro : posteriores) {
      Long id = ((Number) registro.get("id")).longValue();
      Number valor = (Number) registro.get("valor");
      Number tipoId = (Number) registro.get("tipo_id");
      Float valorOriginal = valor != null ? valor.floatValue() : 0F;
      Float valorComDesconto = calcularValorComDesconto(valorOriginal);

      if (tipoId != null && tipoId.longValue() == 1) {
        historicoAcumulado += valorComDesconto;
      }

      jdbcTemplate.update(
          "UPDATE relatorio SET valor_historico_dinheiro = ? WHERE id = ?", historicoAcumulado, id);
    }
  }

  private Relatorio getByIdOrThrow(Relatorio.Id relatorio) {
    if (relatorio == null) {
      throw new IllegalStateException("Registro salvo sem ID.");
    }

    Relatorio.Extrato resp = buscar(relatorio.id(), null, null, null, null, null, null, null, 0, 1);

    if (resp == null || resp.page() == null || resp.page().isEmpty()) {
      throw new NotFoundException("Relatório não encontrado para o uuid: " + relatorio.id());
    }

    Relatorio.Extrato.Diaria diaria = resp.page().getContent().getFirst();
    if (diaria.relatorios() == null || diaria.relatorios().isEmpty()) {
      throw new NotFoundException("Relatório não encontrado para o uuid: " + relatorio.id());
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
            ? """
                        COALESCE(SUM(
                          CASE WHEN pagamento.valor > 0 THEN pagamento.valor
                          ELSE 0
                          END
                        ), 0)
                        """
            : """
                        COALESCE(SUM(
                          CASE WHEN pagamento.valor < 0 THEN ABS(pagamento.valor)
                          ELSE 0
                          END
                        ), 0)
                        """;

    String sql =
        """
                        SELECT
                          DATE(relatorio.data_hora) AS dia,
                        """
            + soma
            + """
                         AS total_dia
                        FROM relatorio
                        LEFT JOIN pagamento ON pagamento.id = relatorio.fk_pagamento
                        LEFT JOIN tipo_pagamento ON tipo_pagamento.id = pagamento.fk_tipo_pagamento
                        WHERE 1 = 1
                        """
            + where.replaceFirst(" WHERE 1 = 1 ", "")
            + " AND DATE(relatorio.data_hora) IN ("
            + in
            + ") GROUP BY DATE(relatorio.data_hora) ";

    List<Object> params = new ArrayList<>(paramsBase);
    params.addAll(dias);

    return jdbcTemplate.query(
        sql,
        rs -> {
          Map<LocalDate, Float> map = new HashMap<>();
          while (rs.next()) {
            map.put(rs.getObject("dia", LocalDate.class), rs.getFloat("total_dia"));
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
                          tipo_pagamento.id        AS tipo_id,
                          tipo_pagamento.descricao AS descricao,
                          COALESCE(SUM(
                            CASE WHEN pagamento.valor > 0 THEN pagamento.valor
                            ELSE 0
                            END
                          ), 0) AS receitas,
                          COALESCE(SUM(
                            CASE WHEN pagamento.valor < 0 THEN ABS(pagamento.valor)
                            ELSE 0
                            END
                          ), 0) AS despesas
                        FROM relatorio
                        LEFT JOIN pagamento ON pagamento.id = relatorio.fk_pagamento
                        LEFT JOIN tipo_pagamento ON tipo_pagamento.id = pagamento.fk_tipo_pagamento
                        """
            + where
            + """
                        GROUP BY tipo_pagamento.id, tipo_pagamento.descricao
                        """;

    Map<Long, Relatorio.Extrato.Resumo> agregadosPorId = new HashMap<>();

    jdbcTemplate.query(
        sqlAgregado,
        rs -> {
          Long tipoId = rs.getLong("tipo_id");
          Float receitas = rs.getFloat("receitas");
          Float despesas = rs.getFloat("despesas");
          agregadosPorId.put(tipoId, Relatorio.Extrato.Resumo.of(receitas, despesas));
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
          Long tipoId = rs.getObject("id", Long.class);
          String descricao = rs.getString("descricao");
          mapa.put(
              descricao, agregadosPorId.getOrDefault(tipoId, Relatorio.Extrato.Resumo.of(0F, 0F)));
        });

    Float totalReceitas = 0F;
    Float totalDespesas = 0F;
    for (Relatorio.Extrato.Resumo resumo : mapa.values()) {
      totalReceitas += resumo.receitas();
      totalDespesas += resumo.despesas();
    }

    mapa.put("TOTAL", Relatorio.Extrato.Resumo.of(totalReceitas, totalDespesas));
    return mapa;
  }
}
