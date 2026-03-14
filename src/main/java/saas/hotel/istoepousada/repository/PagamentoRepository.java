package saas.hotel.istoepousada.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import saas.hotel.istoepousada.dto.Funcionario;
import saas.hotel.istoepousada.dto.Pagamento;

@Repository
public class PagamentoRepository {

  private final JdbcTemplate jdbcTemplate;

  public PagamentoRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  private static final RowMapper<Pagamento> PAGAMENTO_ROW_MAPPER =
      (rs, rowNum) -> {
        Long tipo_pagamento_id = rs.getObject("tipo_pagamento_id", Long.class);
        String tipo_pagamento_descricao = rs.getString("tipo_pagamento_descricao");

        Long funcionario_id = rs.getObject("funcionario_id", Long.class);
        String funcionario_nome = rs.getString("funcionario_nome");

        UUID desconto_id = rs.getObject("desconto_id", UUID.class);
        Long desconto_funcionario_id = rs.getObject("desconto_funcionario_id", Long.class);
        String desconto_funcionario_nome = rs.getString("desconto_funcionario_nome");

        Pagamento.Desconto desconto =
            desconto_id == null
                ? null
                : new Pagamento.Desconto(
                    desconto_id,
                    desconto_funcionario_id == null
                        ? null
                        : new Funcionario.Nome(desconto_funcionario_id, desconto_funcionario_nome),
                    rs.getObject("desconto_porcentagem", Integer.class),
                    rs.getObject("desconto_valor") == null ? null : rs.getDouble("desconto_valor"),
                    rs.getObject("desconto_data_hora_registro", LocalDateTime.class));

        return new Pagamento(
            rs.getObject("id", UUID.class),
            tipo_pagamento_id == null
                ? null
                : new Pagamento.TipoPagamento(tipo_pagamento_id, tipo_pagamento_descricao),
            funcionario_id == null ? null : new Funcionario.Nome(funcionario_id, funcionario_nome),
            rs.getObject("data_hora_registro", LocalDateTime.class),
            rs.getString("nome_pagador"),
            rs.getString("descricao"),
            rs.getObject("valor") == null ? null : rs.getDouble("valor"),
            rs.getBoolean("cancelado"),
            desconto,
            rs.getString("pagamento_path_arquivo"));
      };

  private static final String SELECT_PAGAMENTO_BASE =
      """
                    SELECT
                      p.id,
                      p.data_hora_registro,
                      p.nome_pagador,
                      p.descricao,
                      p.valor,

                      tp.id AS tipo_pagamento_id,
                      tp.descricao AS tipo_pagamento_descricao,

                      f.id AS funcionario_id,
                      pe.nome AS funcionario_nome,

                      d.id AS desconto_id,
                      d.fk_funcionario AS desconto_funcionario_id,
                      pde.nome AS desconto_funcionario_nome,
                      d.porcentagem AS desconto_porcentagem,
                      d.valor AS desconto_valor,
                      d.data_hora_registro AS desconto_data_hora_registro
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
    return jdbcTemplate.queryForObject(sql, PAGAMENTO_ROW_MAPPER, id);
  }

  public List<Pagamento> findAll() {
    var sql = SELECT_PAGAMENTO_BASE + " ORDER BY p.data_hora_registro DESC ";
    return jdbcTemplate.query(sql, PAGAMENTO_ROW_MAPPER);
  }

  public Pagamento update(Pagamento.Update pagamento) {
    var sql =
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
                        """;

    UUID updatedId =
        jdbcTemplate.queryForObject(
            sql,
            (rs, rowNum) -> rs.getObject("id", UUID.class),
            pagamento.tipo_pagamento().id(),
            pagamento.funcionario().id(),
            pagamento.nome_pagador(),
            pagamento.descricao(),
            pagamento.valor(),
            pagamento.uuid());

    return findById(updatedId);
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
