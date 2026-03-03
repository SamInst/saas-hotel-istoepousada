package saas.hotel.istoepousada.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import saas.hotel.istoepousada.dto.Funcionario;
import saas.hotel.istoepousada.dto.TipoPagamento;
import saas.hotel.istoepousada.dto.pagamento.Pagamento;

@Repository
public class PagamentoRepository {

  private final JdbcTemplate jdbcTemplate;

  public PagamentoRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  private static final RowMapper<Pagamento> PAGAMENTO_ROW_MAPPER =
          (rs, rowNum) -> {
            Long tipoPagamentoId = rs.getObject("tipo_pagamento_id", Long.class);
            String tipoPagamentoDescricao = rs.getString("tipo_pagamento_descricao");

            Long funcionarioId = rs.getObject("funcionario_id", Long.class);
            String funcionarioNome = rs.getString("funcionario_nome");

            UUID descontoId = rs.getObject("desconto_id", UUID.class);
            Long descontoFuncionarioId = rs.getObject("desconto_funcionario_id", Long.class);
            String descontoFuncionarioNome = rs.getString("desconto_funcionario_nome");

            Pagamento.Desconto desconto =
                    descontoId == null
                            ? null
                            : new Pagamento.Desconto(
                            descontoId,
                            descontoFuncionarioId == null ? null : new Funcionario.Nome(descontoFuncionarioId, descontoFuncionarioNome),
                            rs.getObject("desconto_porcentagem", Integer.class),
                            rs.getObject("desconto_valor") == null ? null : rs.getFloat("desconto_valor"),
                            rs.getObject("desconto_data_hora_registro", LocalDateTime.class)
                    );

            return new Pagamento(
                    rs.getObject("id", UUID.class),
                    tipoPagamentoId == null ? null : new TipoPagamento(tipoPagamentoId, tipoPagamentoDescricao),
                    funcionarioId == null ? null : new Funcionario.Nome(funcionarioId, funcionarioNome),
                    rs.getObject("data_hora_registro", LocalDateTime.class),
                    rs.getString("nome_pagador"),
                    rs.getString("descricao"),
                    rs.getObject("valor") == null ? null : rs.getFloat("valor"),
                    desconto
            );
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
    var sql = """
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
                    request.tipo_pagamento_id(),
                    request.funcionario_id(),
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

  public Pagamento update(UUID id, Pagamento.Request request) {
    var sql = """
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
                    request.tipo_pagamento_id(),
                    request.funcionario_id(),
                    request.nome_pagador(),
                    request.descricao(),
                    request.valor(),
                    id);

    return findById(updatedId);
  }

  public void cancelarPagamento(UUID id) {
    jdbcTemplate.update("update pagamento set cancelado = true WHERE id = ?", id);
  }
}
