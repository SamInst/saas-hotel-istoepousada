package saas.hotel.istoepousada.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import saas.hotel.istoepousada.dto.Pagamento;

@Repository
public class PagamentoRepository {

  private final JdbcTemplate jdbcTemplate;
  private final PessoaRepository pessoaRepository;

  public PagamentoRepository(JdbcTemplate jdbcTemplate, PessoaRepository pessoaRepository) {
    this.jdbcTemplate = jdbcTemplate;
    this.pessoaRepository = pessoaRepository;
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

  public Pagamento create(Pagamento.Request pagamento) {
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
            getFuncionarioIdFromRequest(),
            pagamento.nome_pagador(),
            pagamento.descricao(),
            pagamento.valor() == null ? 0f : pagamento.valor());
    System.out.println(uuid);

    if (pagamento.desconto() != null) {
      var desconto = findDescontoByPagamentoUuid(uuid);
      if (desconto != null) {
        updateDesconto(
            new Pagamento.Desconto.Update(
                desconto.uuid(), pagamento.desconto().porcentagem(), pagamento.desconto().valor()));
      } else {
        registrarDesconto(
            new Pagamento.Desconto.Request(
                new Pagamento.Uuid(uuid),
                pagamento.desconto().porcentagem(),
                pagamento.desconto().valor()));
      }
    }

    return findById(uuid);
  }

  public Pagamento findById(UUID uuid) {
    var sql = SELECT_PAGAMENTO_BASE + " WHERE p.id = ? ";
    return jdbcTemplate.queryForObject(sql, Pagamento.ROW_MAPPER, uuid);
  }

  public List<Pagamento> findAll() {
    var sql = SELECT_PAGAMENTO_BASE + " ORDER BY p.data_hora_registro DESC ";
    return jdbcTemplate.query(sql, Pagamento.ROW_MAPPER);
  }

  public Pagamento update(Pagamento.Update pagamento) {
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
            getFuncionarioIdFromRequest(),
            pagamento.nome_pagador(),
            pagamento.descricao(),
            pagamento.valor(),
            pagamento.uuid());

    if (uuid == null) {
      throw new IllegalArgumentException(
          "Pagamento com uuid " + pagamento.uuid() + " não encontrado");
    }

    if (pagamento.desconto() != null) {
      if (pagamento.desconto().uuid() != null && existsDescontoByPagamentoUuid(uuid)) {
        updateDesconto(
            new Pagamento.Desconto.Update(
                pagamento.desconto().uuid(),
                pagamento.desconto().porcentagem(),
                pagamento.desconto().valor()));
      } else {
        registrarDesconto(
            new Pagamento.Desconto.Request(
                new Pagamento.Uuid(pagamento.uuid()),
                pagamento.desconto().porcentagem(),
                pagamento.desconto().valor()));
      }
    }

    return findById(uuid);
  }

  public void cancelarPagamento(UUID id) {
    jdbcTemplate.update("update pagamento set cancelado = true WHERE id = ?", id);
  }

  public void cancelarPagamento(UUID id, String motivo) {
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
        getFuncionarioIdFromRequest(),
        motivo);
  }

  public Pagamento.Desconto findDescontoById(UUID uuid) {
    return jdbcTemplate.queryForObject(
        """
            SELECT
                pagamento_desconto.id                 AS pagamento_desconto_id,
                pagamento_desconto.fk_funcionario     AS pagamento_desconto_funcionario_id,
                pessoa.nome                           AS pagamento_desconto_funcionario_nome,
                pagamento_desconto.porcentagem        AS pagamento_desconto_porcentagem,
                pagamento_desconto.valor              AS pagamento_desconto_valor,
                pagamento_desconto.data_hora_registro AS pagamento_desconto_data_hora_registro
            FROM pagamento_desconto
            LEFT JOIN funcionario ON funcionario.id = pagamento_desconto.fk_funcionario
            LEFT JOIN pessoa      ON pessoa.id = funcionario.fk_pessoa
            WHERE pagamento_desconto.id = ?
            """,
        Pagamento.Desconto.ROW_MAPPER,
        uuid);
  }

  public Pagamento.Desconto findDescontoByPagamentoUuid(UUID uuid) {
    try {
      return jdbcTemplate.queryForObject(
          """
                  SELECT
                      pagamento_desconto.id                 AS pagamento_desconto_id,
                      pagamento_desconto.fk_funcionario     AS pagamento_desconto_funcionario_id,
                      pessoa.nome                           AS pagamento_desconto_funcionario_nome,
                      pagamento_desconto.porcentagem        AS pagamento_desconto_porcentagem,
                      pagamento_desconto.valor              AS pagamento_desconto_valor,
                      pagamento_desconto.data_hora_registro AS pagamento_desconto_data_hora_registro
                  FROM pagamento_desconto
                  LEFT JOIN funcionario ON funcionario.id = pagamento_desconto.fk_funcionario
                  LEFT JOIN pessoa      ON pessoa.id = funcionario.fk_pessoa
                  Join pagamento ON pagamento.id = pagamento_desconto.fk_pagamento
                  WHERE pagamento.id = ?
                  """,
          Pagamento.Desconto.ROW_MAPPER,
          uuid);
    } catch (EmptyResultDataAccessException e) {
      return null;
    }
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

  public void updateDesconto(Pagamento.Desconto.Update desconto) {
    int rowsAffected =
        jdbcTemplate.update(
            """
            UPDATE pagamento_desconto SET
                porcentagem = ?,
                valor = ?,
                fk_funcionario = ?
            WHERE id = ?
            """,
            desconto.porcentagem(),
            desconto.valor(),
            getFuncionarioIdFromRequest(),
            desconto.uuid());

    if (rowsAffected == 0) {
      throw new IllegalArgumentException(
          "Desconto com uuid " + desconto.uuid() + " não encontrado");
    }

    findDescontoById(desconto.uuid());
  }

  public void registrarDesconto(Pagamento.Desconto.Request request) {
    if (request == null) {
      throw new IllegalArgumentException("DescontoRequest cannot be null");
    }
    var sql =
        """
                  INSERT INTO pagamento_desconto (
                    fk_pagamento,
                    fk_funcionario,
                    porcentagem,
                    valor
                  )
                  VALUES (?, ?, ?, ?)
                  RETURNING id
                  """;

    UUID uuid =
        jdbcTemplate.queryForObject(
            sql,
            (rs, rowNum) -> rs.getObject("id", UUID.class),
            request.pagamento().uuid(),
            getFuncionarioIdFromRequest(),
            request.porcentagem() == null ? 0 : request.porcentagem(),
            request.valor() == null ? 0F : request.valor());

    findDescontoById(uuid);
  }

  public Long getFuncionarioIdFromRequest() {
    return pessoaRepository.getFuncionarioIdFromRequest();
  }
}
