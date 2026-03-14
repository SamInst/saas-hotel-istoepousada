package saas.hotel.istoepousada.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.Funcionario;
import saas.hotel.istoepousada.dto.Pagamento;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;

@Repository
public class HistoricoRecebidosFuncionarioRepository {

  private final JdbcTemplate jdbcTemplate;

  public HistoricoRecebidosFuncionarioRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  private static final RowMapper<Funcionario.Historico.Recebido> RECEBIDOS_ROW_MAPPER =
      (rs, rowNum) ->
          new Funcionario.Historico.Recebido(
              rs.getLong("id"),
              new Funcionario.Nome(
                  rs.getObject("funcionario_id", Long.class), rs.getString("funcionario_nome")),
              rs.getObject("data_hora_inicio", LocalDateTime.class),
              rs.getObject("data_hora_fim", LocalDateTime.class),
              rs.getObject("data_hora_pagamento", LocalDateTime.class),
              new Pagamento(
                  rs.getObject("pagamento_id", UUID.class),
                  new Pagamento.TipoPagamento(
                      rs.getObject("tipo_pagamento_id", Long.class),
                      rs.getString("tipo_pagamento_descricao")),
                  rs.getObject("pagamento_funcionario_id", Long.class) == null
                      ? null
                      : new Funcionario.Nome(
                          rs.getObject("pagamento_funcionario_id", Long.class),
                          rs.getString("pagamento_funcionario_nome")),
                  rs.getObject("pagamento_data_hora_registro", LocalDateTime.class),
                  rs.getString("pagamento_nome_pagador"),
                  rs.getString("pagamento_descricao"),
                  rs.getObject("pagamento_valor", Double.class),
                  rs.getBoolean("pagamento_cancelado"),
                  new Pagamento.Desconto(
                      rs.getObject("pagamento_desconto_id", UUID.class),
                      new Funcionario.Nome(
                          rs.getLong("pagamento_desconto_funcionario_id"),
                          rs.getString("pagamento_desconto_funcionario_nome")),
                      rs.getInt("pagamento_desconto_porcentagem"),
                      rs.getDouble("pagamento_desconto_valor"),
                      rs.getTimestamp("pagamento_desconto_data_hora_registro").toLocalDateTime()),
                  rs.getString("")),
              rs.getString("path_arquivo"));

  public List<Funcionario.Historico.Recebido> buscar(Long historicoFuncionarioId) {
    String sql =
        """
                        SELECT
                          hrf.id AS id,
                          hrf.data_hora_inicio AS data_hora_inicio,
                          hrf.data_hora_fim AS data_hora_fim,
                          hrf.data_hora_pagamento AS data_hora_pagamento,
                          hrf.path_arquivo AS path_arquivo,

                          pgt.id AS pagamento_id,
                          pgt.data_hora_registro AS pagamento_data_hora_registro,
                          pgt.nome_pagador AS pagamento_nome_pagador,
                          pgt.descricao AS pagamento_descricao,
                          pgt.valor AS pagamento_valor,
                          pgt.fk_funcionario AS pagamento_funcionario_id,

                          fp.nome AS pagamento_funcionario_nome,

                          tp.id AS tipo_pagamento_id,
                          tp.descricao AS tipo_pagamento_descricao,

                          f.id AS funcionario_id,
                          pf.nome AS funcionario_nome
                        FROM historico_recebidos_funcionario hrf
                        JOIN pagamento pgt ON pgt.id = hrf.fk_pagamento
                        JOIN tipo_pagamento tp ON tp.id = pgt.fk_tipo_pagamento
                        LEFT JOIN funcionario fpg ON fpg.id = pgt.fk_funcionario
                        LEFT JOIN pessoa fp ON fp.id = fpg.fk_pessoa
                        JOIN historico_funcionario hf ON hf.id = hrf.fk_historico_funcionario
                        JOIN funcionario f ON f.id = hf.funcionario_id
                        JOIN pessoa pf ON pf.id = f.fk_pessoa
                        WHERE hrf.fk_historico_funcionario = ?
                        ORDER BY hrf.data_hora_inicio DESC, hrf.id DESC
                        """;

    return jdbcTemplate.query(sql, RECEBIDOS_ROW_MAPPER, historicoFuncionarioId);
  }

  public Funcionario.Historico.Recebido findById(Long id) {
    String sql =
        """
                        SELECT
                          hrf.id AS id,
                          hrf.data_hora_inicio AS data_hora_inicio,
                          hrf.data_hora_fim AS data_hora_fim,
                          hrf.data_hora_pagamento AS data_hora_pagamento,
                          hrf.path_arquivo AS path_arquivo,

                          pgt.id AS pagamento_id,
                          pgt.data_hora_registro AS pagamento_data_hora_registro,
                          pgt.nome_pagador AS pagamento_nome_pagador,
                          pgt.descricao AS pagamento_descricao,
                          pgt.valor AS pagamento_valor,
                          pgt.fk_funcionario AS pagamento_funcionario_id,

                          fp.nome AS pagamento_funcionario_nome,

                          tp.id AS tipo_pagamento_id,
                          tp.descricao AS tipo_pagamento_descricao,

                          f.id AS funcionario_id,
                          pf.nome AS funcionario_nome
                        FROM historico_recebidos_funcionario hrf
                        JOIN pagamento pgt ON pgt.id = hrf.fk_pagamento
                        JOIN tipo_pagamento tp ON tp.id = pgt.fk_tipo_pagamento
                        LEFT JOIN funcionario fpg ON fpg.id = pgt.fk_funcionario
                        LEFT JOIN pessoa fp ON fp.id = fpg.fk_pessoa
                        JOIN historico_funcionario hf ON hf.id = hrf.fk_historico_funcionario
                        JOIN funcionario f ON f.id = hf.funcionario_id
                        JOIN pessoa pf ON pf.id = f.fk_pessoa
                        WHERE hrf.id = ?
                        """;

    try {
      return jdbcTemplate.queryForObject(sql, RECEBIDOS_ROW_MAPPER, id);
    } catch (EmptyResultDataAccessException ex) {
      throw new NotFoundException("Recebido não encontrado para o uuid: " + id);
    }
  }

  @Transactional
  public Funcionario.Historico.Recebido insert(Funcionario.Historico.Recebido.Request recebido) {
    UUID pagamentoId =
        jdbcTemplate.queryForObject(
            """
                        INSERT INTO pagamento (
                          fk_tipo_pagamento,
                          fk_funcionario,
                          data_hora_registro,
                          nome_pagador,
                          descricao,
                          valor
                        ) VALUES (?, ?, now(), ?, ?, ?)
                        RETURNING id
                        """,
            UUID.class,
            recebido.pagamento().tipo_pagamento().id(),
            recebido.pagamento().funcionario().id(),
            recebido.pagamento().nome_pagador(),
            recebido.pagamento().descricao(),
            recebido.pagamento().valor());

    var recebido_id =
        jdbcTemplate.queryForObject(
            """
                        INSERT INTO historico_recebidos_funcionario (
                          fk_historico_funcionario,
                          fk_pagamento,
                          data_hora_inicio,
                          data_hora_fim,
                          data_hora_pagamento,
                          path_arquivo
                        ) VALUES (?, ?, ?, ?, ?, ?)
                        RETURNING id
                        """,
            Long.class,
            recebido.historico().id(),
            pagamentoId,
            recebido.data_hora_inicio(),
            recebido.data_hora_fim(),
            recebido.data_hora_pagamento(),
            recebido.path_arquivo());

    return findById(recebido_id);
  }

  @Transactional
  public Funcionario.Historico.Recebido update(Funcionario.Historico.Recebido.Update request) {
    findById(request.id());

    jdbcTemplate.queryForObject(
        """
                        UPDATE historico_recebidos_funcionario
                        SET
                          data_hora_inicio = ?,
                          data_hora_fim = ?,
                          data_hora_pagamento = ?,
                          path_arquivo = ?
                        WHERE id = ?
                        """,
        Long.class,
        request.data_hora_inicio(),
        request.data_hora_fim(),
        request.data_hora_pagamento(),
        request.arquivo(),
        request.id());

    return findById(request.id());
  }
}
