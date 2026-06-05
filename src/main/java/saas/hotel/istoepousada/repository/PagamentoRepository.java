package saas.hotel.istoepousada.repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import saas.hotel.istoepousada.dto.Pagamento;

@Repository
public class PagamentoRepository {

  private final JdbcTemplate jdbcTemplate;

  public PagamentoRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  private static final String SELECT_PAGAMENTO_BASE =
      """
                    SELECT
                      p.id                 as pagamento_id,
                      p.data_hora_registro as pagamento_data_hora_registro,
                      p.nome_pagador       as pagamento_nome_pagador,
                      p.descricao          as pagamento_descricao,
                      p.valor              as pagamento_valor,
                      p.cancelado          as pagamento_cancelado,
                      p.path_arquivo       AS pagamento_path_arquivo,

                      tp.id                AS tipo_pagamento_id,
                      tp.descricao         AS tipo_pagamento_descricao,

                      f.id                 AS pagamento_funcionario_id,
                      pe.nome              AS pagamento_funcionario_nome,

                      d.id                 AS pagamento_desconto_id,
                      d.fk_funcionario     AS pagamento_desconto_funcionario_id,
                      pde.nome             AS pagamento_desconto_funcionario_nome,
                      d.porcentagem        AS pagamento_desconto_porcentagem,
                      d.valor              AS pagamento_desconto_valor,
                      d.data_hora_registro AS pagamento_desconto_data_hora_registro,

                      mc.id                AS pagamento_motivo_id,
                      mc.motivo_cancelamento AS pagamento_motivo_cancelamento,
                      mcf.id               AS pagamento_motivo_funcionario_id,
                      mcp.nome             AS pagamento_motivo_funcionario_nome,
                      mc.data_hora_registro AS pagamento_motivo_data_hora_registro
                    FROM pagamento p
                    JOIN tipo_pagamento tp ON tp.id = p.fk_tipo_pagamento
                    LEFT JOIN funcionario f ON f.id = p.fk_funcionario
                    LEFT JOIN pessoa pe ON pe.id = f.fk_pessoa
                    LEFT JOIN LATERAL (
                      SELECT *
                      FROM pagamento_desconto d
                      WHERE d.fk_pagamento = p.id
                      ORDER BY d.data_hora_registro DESC
                      LIMIT 1
                    ) d ON true
                    LEFT JOIN funcionario df ON df.id = d.fk_funcionario
                    LEFT JOIN pessoa pde ON pde.id = df.fk_pessoa
                    LEFT JOIN LATERAL (
                      SELECT *
                      FROM pagamento_motivo_cancelamento mc
                      WHERE mc.fk_pagamento = p.id
                      ORDER BY mc.data_hora_registro DESC
                      LIMIT 1
                    ) mc ON true
                    LEFT JOIN funcionario mcf ON mcf.id = mc.fk_funcionario
                    LEFT JOIN pessoa mcp ON mcp.id = mcf.fk_pessoa
                    """;

  public Pagamento create(Pagamento.Request pagamento, Long funcionarioId) {
    UUID uuid =
        jdbcTemplate.queryForObject(
            """
            INSERT INTO pagamento (
              fk_tipo_pagamento,
              fk_funcionario,
              nome_pagador,
              descricao,
              valor)
            VALUES (?, ?, ?, ?, ?)
            RETURNING id
            """,
            (rs, rowNum) -> rs.getObject("id", UUID.class),
            pagamento.tipo_pagamento().id(),
            funcionarioId,
            pagamento.nome_pagador(),
            pagamento.descricao(),
            pagamento.valor() == null ? 0f : pagamento.valor());

    return findById(uuid);
  }

  public Pagamento findById(UUID uuid) {
    var sql = SELECT_PAGAMENTO_BASE + " WHERE p.id = ? ";
    return jdbcTemplate.queryForObject(sql, Pagamento.ROW_MAPPER, uuid);
  }

  public List<Pagamento> findByHospedagemId(Long id) {
    var sql =
        SELECT_PAGAMENTO_BASE
            + " JOIN hospedagem_pagamento ON hospedagem_pagamento.fk_pagamento = p.id WHERE hospedagem_pagamento.fk_hospedagem = ? ORDER BY p.data_hora_registro DESC";
    return jdbcTemplate.query(sql, Pagamento.ROW_MAPPER, id);
  }

  public Map<Long, List<Pagamento>> findByHospedagemIds(List<Long> ids) {
    if (ids.isEmpty()) return Map.of();
    String in = ids.stream().map(id -> "?").collect(java.util.stream.Collectors.joining(", "));
    String sql =
        """
        SELECT
          hp.fk_hospedagem     AS pagamento_fk_hospedagem,
          p.id                 AS pagamento_id,
          p.data_hora_registro AS pagamento_data_hora_registro,
          p.nome_pagador       AS pagamento_nome_pagador,
          p.descricao          AS pagamento_descricao,
          p.valor              AS pagamento_valor,
          p.cancelado          AS pagamento_cancelado,
          p.path_arquivo       AS pagamento_path_arquivo,
          tp.id                AS tipo_pagamento_id,
          tp.descricao         AS tipo_pagamento_descricao,
          f.id                 AS pagamento_funcionario_id,
          pe.nome              AS pagamento_funcionario_nome,
          d.id                 AS pagamento_desconto_id,
          d.fk_funcionario     AS pagamento_desconto_funcionario_id,
          pde.nome             AS pagamento_desconto_funcionario_nome,
          d.porcentagem        AS pagamento_desconto_porcentagem,
          d.valor              AS pagamento_desconto_valor,
          d.data_hora_registro AS pagamento_desconto_data_hora_registro,
          mc.id                AS pagamento_motivo_id,
          mc.motivo_cancelamento AS pagamento_motivo_cancelamento,
          mcf.id               AS pagamento_motivo_funcionario_id,
          mcp.nome             AS pagamento_motivo_funcionario_nome,
          mc.data_hora_registro AS pagamento_motivo_data_hora_registro
        FROM hospedagem_pagamento hp
        JOIN pagamento p ON p.id = hp.fk_pagamento
        JOIN tipo_pagamento tp ON tp.id = p.fk_tipo_pagamento
        LEFT JOIN funcionario f ON f.id = p.fk_funcionario
        LEFT JOIN pessoa pe ON pe.id = f.fk_pessoa
        LEFT JOIN LATERAL (
          SELECT * FROM pagamento_desconto d
          WHERE d.fk_pagamento = p.id
          ORDER BY d.data_hora_registro DESC LIMIT 1
        ) d ON true
        LEFT JOIN funcionario df ON df.id = d.fk_funcionario
        LEFT JOIN pessoa pde ON pde.id = df.fk_pessoa
        LEFT JOIN LATERAL (
          SELECT * FROM pagamento_motivo_cancelamento mc
          WHERE mc.fk_pagamento = p.id
          ORDER BY mc.data_hora_registro DESC LIMIT 1
        ) mc ON true
        LEFT JOIN funcionario mcf ON mcf.id = mc.fk_funcionario
        LEFT JOIN pessoa mcp ON mcp.id = mcf.fk_pessoa
        WHERE hp.fk_hospedagem IN (%s)
        ORDER BY hp.fk_hospedagem, p.data_hora_registro DESC
        """
            .formatted(in);
    Map<Long, List<Pagamento>> result = new java.util.LinkedHashMap<>();
    jdbcTemplate.query(
        sql,
        rs -> {
          Long hId = rs.getLong("pagamento_fk_hospedagem");
          Pagamento p = Pagamento.ROW_MAPPER.mapRow(rs, 0);
          if (p != null) result.computeIfAbsent(hId, k -> new java.util.ArrayList<>()).add(p);
        },
        ids.toArray());
    return result;
  }

  public List<Pagamento> findAll() {
    var sql = SELECT_PAGAMENTO_BASE + " ORDER BY p.data_hora_registro DESC ";
    return jdbcTemplate.query(sql, Pagamento.ROW_MAPPER);
  }

  public Pagamento update(Pagamento.Update pagamento, Long funcionarioId) {
    var uuid =
        jdbcTemplate.queryForObject(
            """
             UPDATE pagamento
             SET
               fk_tipo_pagamento = COALESCE(?, fk_tipo_pagamento),
               fk_funcionario    = COALESCE(?, fk_funcionario),
               nome_pagador      = COALESCE(?, nome_pagador),
               descricao         = COALESCE(?, descricao),
               valor             = COALESCE(?, valor)
             WHERE id = ?
             RETURNING id
             """,
            (rs, rowNum) -> rs.getObject("id", UUID.class),
            pagamento.tipo_pagamento().id(),
            funcionarioId,
            pagamento.nome_pagador(),
            pagamento.descricao(),
            pagamento.valor(),
            pagamento.uuid());

    if (uuid == null) {
      throw new IllegalArgumentException(
          "Pagamento com uuid " + pagamento.uuid() + " não encontrado");
    }
    return findById(uuid);
  }

  public void cancelarPagamento(UUID id) {
    jdbcTemplate.update("update pagamento set cancelado = true WHERE id = ?", id);
  }

  public void cancelarPagamento(UUID id, String motivo, Long funcionarioId) {
    int rows = jdbcTemplate.update("UPDATE pagamento SET cancelado = true WHERE id = ?", id);
    if (rows == 0) {
      throw new IllegalArgumentException("Pagamento com uuid " + id + " não encontrado");
    }
    jdbcTemplate.update(
        """
            INSERT INTO public.pagamento_motivo_cancelamento
              (fk_pagamento, fk_funcionario, motivo_cancelamento, data_hora_registro)
            VALUES (?, ?, ?, NOW())
            """,
        id,
        funcionarioId,
        motivo);
  }

  public Boolean existsDescontoByPagamentoUuid(UUID uuid) {
    return jdbcTemplate.queryForObject(
        """
                SELECT COUNT(*) > 0
                FROM pagamento_desconto
                WHERE fk_pagamento = ?
                """,
        Boolean.class,
        uuid);
  }
}
