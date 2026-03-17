package saas.hotel.istoepousada.repository;

import java.util.List;
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
                      d.data_hora_registro AS pagamento_desconto_data_hora_registro
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
                    """;

  public Pagamento create(Pagamento.Request request) {
    var sql =
        """
        INSERT INTO pagamento (
          fk_tipo_pagamento,
          fk_funcionario,
          nome_pagador,
          descricao,
          valor
        )
        VALUES (?, ?, ?, ?, ?)
        RETURNING id
        """;

    UUID id =
        jdbcTemplate.queryForObject(
            sql,
            (rs, rowNum) -> rs.getObject("id", UUID.class),
            request.tipo_pagamento().id(),
            request.funcionario().id(),
            request.nome_pagador(),
            request.descricao(),
            request.valor() == null ? 0f : request.valor());

    return findById(id);
  }

  public Pagamento findById(UUID id) {
    var sql = SELECT_PAGAMENTO_BASE + " WHERE p.id = ? ";
    return jdbcTemplate.queryForObject(sql, Pagamento.ROW_MAPPER, id);
  }

  public List<Pagamento> findAll() {
    var sql = SELECT_PAGAMENTO_BASE + " ORDER BY p.data_hora_registro DESC ";
    return jdbcTemplate.query(sql, Pagamento.ROW_MAPPER);
  }

  public Pagamento update(Pagamento.Update pagamento) {
    var result =
        jdbcTemplate.query(
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
            pagamento.funcionario().id(),
            pagamento.nome_pagador(),
            pagamento.descricao(),
            pagamento.valor(),
            pagamento.uuid());

    if (result.isEmpty()) {
      throw new IllegalArgumentException(
          "Pagamento com id " + pagamento.uuid() + " não encontrado");
    }

    return findById(result.getFirst());
  }

  public void cancelarPagamento(UUID id) {
    jdbcTemplate.update("update pagamento set cancelado = true WHERE id = ?", id);
  }

  //  public QuartoResponse.Pagamento.Desconto registrarDesconto(PagamentoDescontoRequest request) {
  //    var sql =
  //        """
  //                INSERT INTO pagamento_desconto (
  //                  fk_pagamento,
  //                  fk_funcionario,
  //                  porcentagem,
  //                  valor
  //                )
  //                VALUES (?, ?, ?, ?)
  //                RETURNING uuid
  //                """;
  //
  //    Long uuid =
  //        jdbcTemplate.queryForObject(
  //            sql,
  //            (rs, rowNum) -> rs.getObject("uuid", Long.class),
  //            request.pagamento_id(),
  //            request.funcionario_id(),
  //            request.porcentagem() == null ? 0 : request.porcentagem(),
  //            request.valor() == null ? 0f : request.valor());
  //
  //    return findDescontoById(uuid);
  //  }
}
