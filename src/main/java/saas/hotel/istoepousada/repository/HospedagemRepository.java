package saas.hotel.istoepousada.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import saas.hotel.istoepousada.dto.*;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;

@Slf4j
@Repository
public class HospedagemRepository {
  private final JdbcTemplate jdbcTemplate;

  public HospedagemRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  private static final String SELECT_HOSPEDAGEM =
      """
                    SELECT
                        hospedagem.id                      AS hospedagem_id,
                        hospedagem.status                  AS hospedagem_status,
                        hospedagem.data_hora_registro      AS hospedagem_data_hora_registro,
                        hospedagem.data_hora_checkin       AS hospedagem_data_hora_checkin,
                        hospedagem.data_hora_checkout      AS hospedagem_data_hora_checout,
                        hospedagem.valor_total             AS hospedagem_valor_total,
                        hospedagem.observacao              AS hospedagem_observacao,
                        pessoa_funcionario.id              AS hospedagem_funcionario_id,
                        pessoa_funcionario_hospedagem.nome AS hospedagem_funcionario_nome,
                        (SELECT fk_grupo FROM public.hospedagem_grupo WHERE fk_hospedagem = hospedagem.id ORDER BY data_hora_registro LIMIT 1) AS hospedagem_grupo_id
                    FROM public.hospedagem
                    LEFT JOIN public.funcionario pessoa_funcionario ON pessoa_funcionario.id = hospedagem.fk_funcionario
                    LEFT JOIN public.pessoa pessoa_funcionario_hospedagem ON pessoa_funcionario_hospedagem.id = pessoa_funcionario.fk_pessoa
                    """;

  public List<Hospedagem> buscar(
      List<Hospedagem.Status> statuses,
      LocalDate data,
      Integer mes,
      Integer ano,
      String nomeTitular) {
    StringBuilder sql = new StringBuilder(SELECT_HOSPEDAGEM);
    List<Object> params = new ArrayList<>();
    List<String> conditions = new ArrayList<>();

    if (nomeTitular != null && !nomeTitular.isBlank()) {
      sql.append(
          """
                            LEFT JOIN public.hospedagem_pessoa hp_titular ON hp_titular.hospedagem_id = hospedagem.id AND hp_titular.representante = true
                            LEFT JOIN public.pessoa pessoa_titular ON pessoa_titular.id = hp_titular.pessoa_id
                            """);
      conditions.add("LOWER(pessoa_titular.nome) LIKE LOWER(?)");
      params.add("%" + nomeTitular + "%");
    }

    if (statuses != null && !statuses.isEmpty()) {
      String placeholders =
          statuses.stream().map(s -> "?::hospedagem_status").collect(Collectors.joining(", "));
      conditions.add("hospedagem.status::hospedagem_status IN (" + placeholders + ")");
      statuses.forEach(s -> params.add(s.name()));
    }

    if (data != null) {
      conditions.add(
          "hospedagem.data_hora_checkin::date <= ? AND hospedagem.data_hora_checkout::date >= ?");
      params.add(data);
      params.add(data);
    }

    if (mes != null) {
      conditions.add("EXTRACT(MONTH FROM hospedagem.data_hora_checkin) = ?");
      params.add(mes);
    }

    if (ano != null) {
      conditions.add("EXTRACT(YEAR FROM hospedagem.data_hora_checkin) = ?");
      params.add(ano);
    }

    if (!conditions.isEmpty()) {
      sql.append(" WHERE ").append(String.join(" AND ", conditions));
    }

    return jdbcTemplate.query(sql.toString(), Hospedagem.MAPPER, params.toArray());
  }

  // ── Reservas por quarto (paginado) ───────────────────────────────────────────
  // periodo: "anteriores" (checkout já passou) | "proximas" (checkout futuro) | null (filtra por
  // mês/ano)

  private void appendQuartoFiltros(
      StringBuilder sql,
      List<Object> params,
      Long quartoId,
      Integer mes,
      Integer ano,
      String periodo) {
    sql.append(" WHERE hospedagem.fk_quarto = ?");
    params.add(quartoId);
    if ("anteriores".equalsIgnoreCase(periodo)) {
      sql.append(" AND hospedagem.data_hora_checkout < now()");
    } else if ("proximas".equalsIgnoreCase(periodo)) {
      sql.append(" AND hospedagem.data_hora_checkout >= now()");
    } else {
      if (mes != null) {
        sql.append(" AND EXTRACT(MONTH FROM hospedagem.data_hora_checkin) = ?");
        params.add(mes);
      }
      if (ano != null) {
        sql.append(" AND EXTRACT(YEAR FROM hospedagem.data_hora_checkin) = ?");
        params.add(ano);
      }
    }
  }

  public long contarPorQuarto(Long quartoId, Integer mes, Integer ano, String periodo) {
    StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM public.hospedagem");
    List<Object> params = new ArrayList<>();
    appendQuartoFiltros(sql, params, quartoId, mes, ano, periodo);
    Long total = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
    return total == null ? 0 : total;
  }

  public List<Hospedagem> buscarPorQuarto(
      Long quartoId, Integer mes, Integer ano, String periodo, int page, int size) {
    StringBuilder sql = new StringBuilder(SELECT_HOSPEDAGEM);
    List<Object> params = new ArrayList<>();
    appendQuartoFiltros(sql, params, quartoId, mes, ano, periodo);
    // próximas: mais cedo primeiro; demais: mais recente primeiro
    String ordem = "proximas".equalsIgnoreCase(periodo) ? "ASC" : "DESC";
    sql.append(" ORDER BY hospedagem.data_hora_checkin ").append(ordem).append(" LIMIT ? OFFSET ?");
    params.add(size);
    params.add(page * size);
    return jdbcTemplate.query(sql.toString(), Hospedagem.MAPPER, params.toArray());
  }

  public Hospedagem buscarPorId(Long id) {
    try {
      return jdbcTemplate.queryForObject(
          SELECT_HOSPEDAGEM + " WHERE hospedagem.id = ? ", Hospedagem.MAPPER, id);
    } catch (EmptyResultDataAccessException e) {
      throw new NotFoundException("Hospedagem nao encontrada para o id: " + id);
    }
  }

  public Long adicionarOrcamento(Orcamento.Request request, Long funcionarioId) {
    return jdbcTemplate.queryForObject(
        """
                        INSERT INTO orcamento (nome_solicitante, fk_funcionario, data_hora_registro)
                        VALUES (?, ?, now())
                        returning id;
                        """,
        Long.class,
        request.nome_solicitante(),
        funcionarioId);
  }

  public void editarOrcamento(Orcamento.Request request) {
    jdbcTemplate.update(
        """
                        UPDATE orcamento
                        SET nome_solicitante = ?
                        WHERE id = ?
                        """,
        request.nome_solicitante(),
        request.id());
  }

  public void adicionarPessoasHospedagemOrcamento(
      Long hospedagemId, List<Hospedagem.PessoaHospedagemOrcamento.Request> pessoas) {
    jdbcTemplate.batchUpdate(
        """
        INSERT INTO orcamento_hospedagem_pessoa (fk_hospedagem, nome_pessoa, data_nascimento)
        VALUES (?, ?, ?)
        """,
        pessoas,
        pessoas.size(),
        (ps, pessoa) -> {
          ps.setLong(1, hospedagemId);
          ps.setString(2, pessoa.nome());
          ps.setObject(3, pessoa.data_nascimento());
        });
  }

  public void removerPessoasOrcamento(List<Long> pessoasOrcamentoIds) {
    jdbcTemplate.batchUpdate(
        """
            DELETE FROM orcamento_hospedagem_pessoa
            WHERE id = ?
            """,
        pessoasOrcamentoIds,
        pessoasOrcamentoIds.size(),
        (ps, pessoaId) -> {
          ps.setLong(1, pessoaId);
        });
  }

  public List<Orcamento> buscarOrcamento(Long orcamentoId, String nomeSolicitante) {
    String baseSql =
        """
                SELECT
                    orcamento.id                                       AS orcamento_id,
                    orcamento.nome_solicitante                         AS orcamento_nome_solicitante,
                    orcamento.data_hora_registro                       AS orcamento_data_hora_registro,
                    orcamento_funcionario.id                           AS orcamento_funcionario_id,
                    orcamento_pessoa_funcionario.nome                  AS orcamento_funcionario_nome,
                    hospedagem.id                                      AS hospedagem_id,
                    hospedagem.status                                  AS hospedagem_status,
                    hospedagem.data_hora_registro                      AS hospedagem_data_hora_registro,
                    hospedagem.data_hora_checkin                       AS hospedagem_data_hora_checkin,
                    hospedagem.data_hora_checkout                      AS hospedagem_data_hora_checout,
                    hospedagem.valor_total                             AS hospedagem_valor_total,
                    hospedagem.observacao                              AS hospedagem_observacao,
                    hospedagem_funcionario.id                          AS hospedagem_funcionario_id,
                    hospedagem_pessoa_funcionario.nome                 AS hospedagem_funcionario_nome,
                    (SELECT fk_grupo FROM public.hospedagem_grupo WHERE fk_hospedagem = hospedagem.id ORDER BY data_hora_registro LIMIT 1) AS hospedagem_grupo_id
                FROM public.orcamento
                JOIN public.funcionario orcamento_funcionario ON orcamento_funcionario.id = orcamento.fk_funcionario
                JOIN public.pessoa orcamento_pessoa_funcionario ON orcamento_pessoa_funcionario.id = orcamento_funcionario.fk_pessoa
                LEFT JOIN public.hospedagem_orcamento ON hospedagem_orcamento.fk_orcamento = orcamento.id
                LEFT JOIN public.hospedagem ON hospedagem.id = hospedagem_orcamento.fk_hospedagem
                LEFT JOIN public.funcionario hospedagem_funcionario ON hospedagem_funcionario.id = hospedagem.fk_funcionario
                LEFT JOIN public.pessoa hospedagem_pessoa_funcionario ON hospedagem_pessoa_funcionario.id = hospedagem_funcionario.fk_pessoa

                """;

    List<String> conditions = new ArrayList<>();
    List<Object> params = new ArrayList<>();

    if (orcamentoId != null) {
      conditions.add("orcamento.id = ?");
      params.add(orcamentoId);
    }
    if (nomeSolicitante != null && !nomeSolicitante.isBlank()) {
      conditions.add("LOWER(orcamento.nome_solicitante) LIKE LOWER(?)");
      params.add("%" + nomeSolicitante + "%");
    }

    String sql =
        baseSql + (conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions));

    ResultSetExtractor<List<Orcamento>> extractor =
        rs -> {
          Map<Long, Orcamento> orcamentoMap = new LinkedHashMap<>();
          Map<Long, List<Hospedagem>> hospedagensMap = new HashMap<>();

          while (rs.next()) {
            long oId = rs.getLong("orcamento_id");
            if (!orcamentoMap.containsKey(oId)) {
              orcamentoMap.put(oId, Orcamento.MAPPER.mapRow(rs, 0));
            }
            Long hId = rs.getObject("hospedagem_id", Long.class);
            if (hId != null) {
              hospedagensMap
                  .computeIfAbsent(oId, id -> new ArrayList<>())
                  .add(Hospedagem.MAPPER.mapRow(rs, 0));
            }
          }

          return orcamentoMap.entrySet().stream()
              .map(
                  entry -> {
                    Orcamento o = entry.getValue();
                    List<Hospedagem> hospedagens =
                        hospedagensMap.getOrDefault(entry.getKey(), List.of()).stream()
                            .map(
                                h ->
                                    new Hospedagem(
                                        h.id(),
                                        h.funcionario(),
                                        null,
                                        h.data_hora_registro(),
                                        h.data_hora_checkin(),
                                        h.data_hora_checkout(),
                                        h.status(),
                                        h.valor_total(),
                                        h.quantidade_diarias(),
                                        h.numero_diaria_atual(),
                                        h.observacao(),
                                        null,
                                        null,
                                        null,
                                        null,
                                        buscarPessoasHospedagemOrcamento(h.id()),
                                        buscarMotivoCancelamento(h.id()),
                                        h.grupo_id(),
                                        null))
                            .toList();
                    return new Orcamento(
                        o.id(),
                        o.nome_solicitante(),
                        o.funcionario(),
                        o.data_hora_registro(),
                        hospedagens);
                  })
              .toList();
        };

    return jdbcTemplate.query(sql, extractor, params.toArray());
  }

  public void vincularHospedagensOrcamento(List<Long> hospedagemIds, Long orcamentoId) {
    jdbcTemplate.batchUpdate(
        "insert into hospedagem_orcamento (fk_hospedagem, fk_orcamento) values (?, ?) on conflict do nothing; ",
        hospedagemIds,
        hospedagemIds.size(),
        (ps, hospedagemId) -> {
          ps.setLong(1, hospedagemId);
          ps.setLong(2, orcamentoId);
        });
  }

  public Long criarGrupoReserva(Long funcionarioId) {
    return jdbcTemplate.queryForObject(
        """
        INSERT INTO grupo_reserva (fk_funcionario, data_hora_registro)
        VALUES (?, now())
        RETURNING id;
        """,
        Long.class,
        funcionarioId);
  }

  public void vincularHospedagensGrupo(List<Long> hospedagemIds, Long grupoId) {
    jdbcTemplate.batchUpdate(
        "INSERT INTO hospedagem_grupo (fk_hospedagem, fk_grupo) VALUES (?, ?) ON CONFLICT DO NOTHING",
        hospedagemIds,
        hospedagemIds.size(),
        (ps, hospedagemId) -> {
          ps.setLong(1, hospedagemId);
          ps.setLong(2, grupoId);
        });
  }

  public Hospedagem insertHospedagem(Hospedagem.Request request, Long funcionarioId) {
    return buscarPorId(insertHospedagemId(request, funcionarioId));
  }

  /**
   * Insere hospedagem e retorna apenas o ID gerado — evita o SELECT extra quando não necessário.
   */
  public Long insertHospedagemId(Hospedagem.Request request, Long funcionarioId) {
    return jdbcTemplate.queryForObject(
        """
                    insert into hospedagem (
                        fk_funcionario,
                        fk_quarto,
                        data_hora_registro,
                        data_hora_checkin,
                        data_hora_checkout,
                        valor_total,
                        status,
                        observacao)
                    values (?, ?, now(), ?, ?, ?, ?::hospedagem_status, ?)
                    returning id;
                """,
        Long.class,
        funcionarioId,
        request.quarto_id(),
        request.data_hora_checkin(),
        request.data_hora_checkout(),
        request.valor_total(),
        request.status().name(),
        request.observacao());
  }

  public Hospedagem editarHospedagem(Hospedagem.Request request) {
    jdbcTemplate.update(
        """
                        UPDATE hospedagem
                        SET data_hora_checkin  = COALESCE(?, data_hora_checkin),
                            data_hora_checkout = COALESCE(?, data_hora_checkout),
                            observacao         = COALESCE(?, observacao),
                            valor_total        = COALESCE(?, valor_total)
                        WHERE id = ?
                        """,
        request.data_hora_checkin(),
        request.data_hora_checkout(),
        request.observacao(),
        request.valor_total(),
        request.hospedagem_id());
    return buscarPorId(request.hospedagem_id());
  }

  /** Edição de reserva — inclui fk_quarto além dos campos de editarHospedagem. */
  public Hospedagem editarReserva(
      Long hospedagemId, Hospedagem.Request request, Long funcionarioId) {
    jdbcTemplate.update(
        """
                        UPDATE hospedagem
                        SET fk_quarto          = COALESCE(?, fk_quarto),
                            data_hora_checkin  = COALESCE(?, data_hora_checkin),
                            data_hora_checkout = COALESCE(?, data_hora_checkout),
                            valor_total        = COALESCE(?, valor_total),
                            observacao         = COALESCE(?, observacao),
                            fk_funcionario = ?
                        WHERE id = ?
                        """,
        request.quarto_id(),
        request.data_hora_checkin(),
        request.data_hora_checkout(),
        request.valor_total(),
        request.observacao(),
        funcionarioId,
        hospedagemId);
    return buscarPorId(hospedagemId);
  }

  public Long buscarQuartoId(Long hospedagemId) {
    return jdbcTemplate.queryForObject(
        "SELECT fk_quarto FROM hospedagem WHERE id = ?", Long.class, hospedagemId);
  }

  public List<Long> buscarPessoasIds(Long hospedagemId) {
    return jdbcTemplate.queryForList(
        "SELECT pessoa_id FROM hospedagem_pessoa WHERE hospedagem_id = ?",
        Long.class,
        hospedagemId);
  }

  public void deletarDiarias(Long hospedagemId) {
    jdbcTemplate.update("DELETE FROM diaria WHERE fk_hospedagem = ?", hospedagemId);
  }

  public void alterarStatus(Long hospedagemId, Hospedagem.Status status, Long funcionarioId) {
    jdbcTemplate.update(
        """
            update hospedagem set status = ?::hospedagem_status, fk_funcionario = ? where id = ?
            """,
        status.name(),
        funcionarioId,
        hospedagemId);
  }

  public void adicionarHospedagemPagamento(Long hospedagemId, List<UUID> pagamentosUUID) {
    adicionarHospedagemPagamento(hospedagemId, pagamentosUUID, null);
  }

  /** Quando grupoId != null, o vínculo guarda o grupo ao qual o pagamento pertence. */
  public void adicionarHospedagemPagamento(
      Long hospedagemId, List<UUID> pagamentosUUID, Long grupoId) {
    jdbcTemplate.batchUpdate(
        """
                        INSERT INTO hospedagem_pagamento (fk_hospedagem, fk_pagamento, fk_grupo)
                        VALUES (?, ?, ?)
                        """,
        pagamentosUUID.stream()
            .map(pagamentoUUID -> new Object[] {hospedagemId, pagamentoUUID, grupoId})
            .toList());
  }

  public List<Long> buscarHospedagemIdsPorGrupo(Long grupoId) {
    return jdbcTemplate.queryForList(
        "SELECT fk_hospedagem FROM hospedagem_grupo WHERE fk_grupo = ?", Long.class, grupoId);
  }

  public List<Long> filtrarPessoasDuplicadas(Long hospedagemId, List<Long> pessoasIds) {
    List<Long> existentes =
        jdbcTemplate.queryForList(
            """
             SELECT pessoa_id FROM hospedagem_pessoa
             WHERE hospedagem_id = ?
               AND pessoa_id IN (%s)
             """
                .formatted(
                    pessoasIds.stream().map(String::valueOf).collect(Collectors.joining(","))),
            Long.class,
            hospedagemId);

    return pessoasIds.stream().filter(id -> !existentes.contains(id)).toList();
  }

  public void adicionarPessoas(Long hospedagemId, List<Long> pessoasIds) {
    AtomicInteger index = new AtomicInteger(0);
    jdbcTemplate.batchUpdate(
        """
                        INSERT INTO hospedagem_pessoa(hospedagem_id, pessoa_id, representante)
                        VALUES (?, ?, ?)
                        """,
        pessoasIds,
        pessoasIds.size(),
        (ps, pessoaId) -> {
          ps.setLong(1, hospedagemId);
          ps.setLong(2, pessoaId);
          ps.setBoolean(3, index.getAndIncrement() == 0);
        });
  }

  public void removerPessoas(Long hospedagemId, List<Long> pessoasIds) {
    jdbcTemplate.batchUpdate(
        """
                        DELETE FROM hospedagem_pessoa
                        WHERE hospedagem_id = ?
                          AND pessoa_id = ?
                        """,
        pessoasIds,
        pessoasIds.size(),
        (ps, pessoaId) -> {
          ps.setLong(1, hospedagemId);
          ps.setLong(2, pessoaId);
        });
  }

  /** Define a pessoa informada como titular (representante) da hospedagem; as demais deixam de ser. */
  public int definirTitular(Long hospedagemId, Long pessoaId) {
    return jdbcTemplate.update(
        "UPDATE hospedagem_pessoa SET representante = (pessoa_id = ?) WHERE hospedagem_id = ?",
        pessoaId,
        hospedagemId);
  }

  public List<Long> filtrarPessoasExistentes(Long hospedagemId, List<Long> pessoasIds) {
    return jdbcTemplate.queryForList(
        """
                        SELECT pessoa_id FROM hospedagem_pessoa
                        WHERE hospedagem_id = ?
                          AND pessoa_id IN (%s)
                        """
            .formatted(pessoasIds.stream().map(String::valueOf).collect(Collectors.joining(","))),
        Long.class,
        hospedagemId);
  }

  public void adicionarConsumo(
      Long hospedagemId, Item.Consumo.Request request, UUID finalPagamentoId, Long funcionarioId) {
    KeyHolder keyHolder = new GeneratedKeyHolder();

    jdbcTemplate.update(
        con -> {
          PreparedStatement ps =
              con.prepareStatement(
                  """
                                            INSERT INTO consumo (fk_funcionario, fk_pagamento, fk_item, quantidade, despesa_pessoal, fk_quarto)
                                            VALUES (?, ?, ?, ?, ?, ?)
                                            """,
                  Statement.RETURN_GENERATED_KEYS);
          ps.setLong(1, funcionarioId);
          ps.setObject(2, finalPagamentoId);
          ps.setLong(3, request.item().id());
          ps.setFloat(4, request.quantidade());
          ps.setBoolean(5, Boolean.TRUE.equals(request.despesa_pessoal()));
          ps.setObject(6, request.quarto() != null ? request.quarto().id() : null);
          return ps;
        },
        keyHolder);

    Long consumoId = Objects.requireNonNull(keyHolder.getKey()).longValue();

    jdbcTemplate.update(
        """
                        INSERT INTO hospedagem_consumo (fk_hospedagem, fk_consumo)
                        VALUES (?, ?)
                        """,
        hospedagemId,
        consumoId);
  }

  public void editarConsumo(Item.Consumo.Request request) {
    jdbcTemplate.update(
        """
                        UPDATE consumo
                        SET quantidade = ?
                        WHERE id = ?
                        """,
        request.quantidade(),
        request.id());
  }

  public List<Item.Consumo> buscarConsumosPorHospedagem(Long hospedagemId) {
    try {
      return jdbcTemplate.query(
          """

                                        SELECT
                                hospedagem_consumo.fk_hospedagem                              AS consumo_fk_hospedagem,
                                consumo.id                                                    AS consumo_id,
                                consumo.data_hora_registro                                    AS consumo_data_hora_registro,
                                consumo.quantidade                                            AS consumo_quantidade,
                                consumo.despesa_pessoal                                       AS consumo_despesa_pessoal,
                                consumo.cancelado                                             AS consumo_cancelado,
                                item.id                                                       AS consumo_item_id,
                                item.descricao                                                AS consumo_item_descricao,
                                estoque.valor_venda_unidade                                   AS consumo_item_valor_venda_unidade,
                                consumo_funcionario.id                                        AS consumo_funcionario_id,
                                consumo_pessoa_funcionario.nome                               AS consumo_funcionario_nome,
                                consumo_quarto.id                                             AS consumo_quarto_id,
                                consumo_quarto.descricao                                      AS consumo_quarto_descricao,
                                consumo_pagamento.id                                          AS consumo_pagamento_id,
                                consumo_pagamento.data_hora_registro                          AS consumo_pagamento_data_hora_registro,
                                consumo_pagamento.nome_pagador                                AS consumo_pagamento_nome_pagador,
                                consumo_pagamento.descricao                                   AS consumo_pagamento_descricao,
                                consumo_pagamento.valor                                       AS consumo_pagamento_valor,
                                consumo_pagamento.cancelado                                   AS consumo_pagamento_cancelado,
                                consumo_pagamento.path_arquivo                                AS consumo_pagamento_path_arquivo,
                                consumo_pagamento_tipo_pagamento.id                           AS consumo_tipo_pagamento_id,
                                consumo_pagamento_tipo_pagamento.descricao                    AS consumo_tipo_pagamento_descricao,

                                consumo_funcionario_consumo_pagamento.id                      AS consumo_pagamento_funcionario_id,
                                consumo_pessoa_funcionario_consumo_pagamento.nome             AS consumo_pagamento_funcionario_nome,
                                consumo_pagamento_motivo_cancelamento.id                      AS consumo_pagamento_motivo_id,
                                consumo_pagamento_motivo_cancelamento.motivo_cancelamento     AS consumo_pagamento_motivo_cancelamento,
                                consumo_pagamento_motivo_cancelamento_funcionario.id          AS consumo_pagamento_motivo_funcionario_id,
                                consumo_pagamento_motivo_cancelamento_pessoa_funcionario.nome AS consumo_pagamento_motivo_funcionario_nome,
                                consumo_pagamento_motivo_cancelamento.data_hora_registro      AS consumo_pagamento_motivo_data_hora_registro
                            FROM public.hospedagem_consumo
                                     JOIN public.consumo                                                  ON consumo.id = hospedagem_consumo.fk_consumo
                                     JOIN public.item                                                     ON item.id = consumo.fk_item
                                     LEFT JOIN public.funcionario consumo_funcionario                     ON consumo_funcionario.id = consumo.fk_funcionario
                                     LEFT JOIN public.pessoa consumo_pessoa_funcionario                   ON consumo_pessoa_funcionario.id = consumo_funcionario.fk_pessoa
                                     LEFT JOIN public.quarto consumo_quarto                                        ON consumo_quarto.id = consumo.fk_quarto
                                     LEFT JOIN public.pagamento consumo_pagamento                       ON consumo_pagamento.id = consumo.fk_pagamento
                                     LEFT JOIN public.tipo_pagamento consumo_pagamento_tipo_pagamento     ON consumo_pagamento_tipo_pagamento.id = consumo_pagamento.fk_tipo_pagamento
                                     LEFT JOIN public.funcionario consumo_funcionario_consumo_pagamento            ON consumo_funcionario_consumo_pagamento.id = consumo_pagamento.fk_funcionario
                                     LEFT JOIN public.pessoa consumo_pessoa_funcionario_consumo_pagamento ON consumo_pessoa_funcionario_consumo_pagamento.id = consumo_funcionario_consumo_pagamento.fk_pessoa
                                     LEFT JOIN LATERAL (
                                        SELECT * FROM public.pagamento_motivo_cancelamento mc
                                        WHERE mc.fk_pagamento = consumo_pagamento.id
                                        ORDER BY mc.data_hora_registro DESC LIMIT 1
                                        ) consumo_pagamento_motivo_cancelamento ON true
                                     LEFT JOIN public.funcionario consumo_pagamento_motivo_cancelamento_funcionario ON consumo_pagamento_motivo_cancelamento_funcionario.id = consumo_pagamento_motivo_cancelamento.fk_funcionario
                                     LEFT JOIN public.pessoa consumo_pagamento_motivo_cancelamento_pessoa_funcionario ON consumo_pagamento_motivo_cancelamento_pessoa_funcionario.id = consumo_pagamento_motivo_cancelamento_funcionario.fk_pessoa
                                     LEFT JOIN estoque ON estoque.fk_item = item.id
                                     WHERE hospedagem_consumo.fk_hospedagem = ?;
                            """,
          Item.Consumo.ROW_MAPPER,
          hospedagemId);
    } catch (EmptyResultDataAccessException e) {
      return null;
    }
  }

  public void adicionarDiarias(
      Long hospedagemId, List<Hospedagem.Diaria.Request> diarias, List<Double> valores) {
    AtomicInteger index = new AtomicInteger(0);
    jdbcTemplate.batchUpdate(
        """
                        INSERT INTO diaria (fk_hospedagem, fk_quarto, numero, checkin, checkout, valor, meia_diaria)
                        VALUES (?, ?, ?, ?, ?, ?, ?)
                        """,
        diarias,
        diarias.size(),
        (ps, diaria) -> {
          int i = index.getAndIncrement();
          ps.setLong(1, hospedagemId);
          ps.setLong(2, diaria.quarto_id());
          ps.setInt(3, i + 1);
          ps.setObject(4, diaria.checkin());
          ps.setObject(5, diaria.checkout());
          ps.setDouble(6, valores.get(i));
          ps.setBoolean(7, Boolean.TRUE.equals(diaria.meia_diaria()));
        });
  }

  // ── Novo preço (ajuste manual "Gerenciar Preços") ───────────────────────────

  private static final String SELECT_NOVO_PRECO =
      """
      SELECT
          novo_preco.id                  AS novo_preco_id,
          novo_preco.fk_hospedagem       AS novo_preco_fk_hospedagem,
          novo_preco.quantidade_diarias  AS novo_preco_quantidade_diarias,
          novo_preco.quantidade_pessoas  AS novo_preco_quantidade_pessoas,
          novo_preco.valor_diaria        AS novo_preco_valor_diaria,
          novo_preco.porcentagem         AS novo_preco_porcentagem,
          novo_preco.valor_desconto      AS novo_preco_valor_desconto,
          funcionario.id                 AS novo_preco_funcionario_id,
          pessoa_funcionario.nome        AS novo_preco_funcionario_nome
      FROM public.hospedagem_novo_preco novo_preco
      LEFT JOIN public.funcionario funcionario ON funcionario.id = novo_preco.fk_funcionario
      LEFT JOIN public.pessoa pessoa_funcionario ON pessoa_funcionario.id = funcionario.fk_pessoa
      """;

  public HospedagemNovoPreco buscarNovoPreco(Long hospedagemId) {
    List<HospedagemNovoPreco> result =
        jdbcTemplate.query(
            SELECT_NOVO_PRECO
                + " WHERE novo_preco.fk_hospedagem = ? ORDER BY novo_preco.data_hora_registro DESC, novo_preco.id DESC LIMIT 1",
            HospedagemNovoPreco.MAPPER,
            hospedagemId);
    return result.isEmpty() ? null : result.getFirst();
  }

  public Map<Long, HospedagemNovoPreco> buscarNovoPrecoBatch(List<Long> ids) {
    if (ids.isEmpty()) return Map.of();
    String sql =
        SELECT_NOVO_PRECO
            + " WHERE novo_preco.fk_hospedagem IN ("
            + inClause(ids.size())
            + ") ORDER BY novo_preco.fk_hospedagem, novo_preco.data_hora_registro DESC, novo_preco.id DESC";
    Map<Long, HospedagemNovoPreco> result = new LinkedHashMap<>();
    jdbcTemplate.query(
        sql,
        rs -> {
          Long hId = rs.getLong("novo_preco_fk_hospedagem");
          // ORDER BY garante que o primeiro de cada hospedagem é o mais recente.
          if (!result.containsKey(hId)) {
            result.put(hId, HospedagemNovoPreco.MAPPER.mapRow(rs, 0));
          }
        },
        ids.toArray());
    return result;
  }

  /** Substitui o ajuste vigente da hospedagem (um ativo por hospedagem). */
  public void salvarNovoPreco(
      Long hospedagemId, HospedagemNovoPreco.Request request, Long funcionarioId) {
    jdbcTemplate.update(
        "DELETE FROM hospedagem_novo_preco WHERE fk_hospedagem = ?", hospedagemId);
    jdbcTemplate.update(
        """
        INSERT INTO hospedagem_novo_preco
            (fk_hospedagem, quantidade_diarias, quantidade_pessoas, valor_diaria,
             porcentagem, valor_desconto, fk_funcionario, data_hora_registro)
        VALUES (?, ?, ?, ?, ?, ?, ?, now())
        """,
        hospedagemId,
        request.quantidade_diarias(),
        request.quantidade_pessoas(),
        request.valor_diaria(),
        request.porcentagem(),
        request.valor_desconto(),
        funcionarioId);
  }

  /** Remove o ajuste manual de preço (desconto) da hospedagem, se houver. */
  public void deletarNovoPreco(Long hospedagemId) {
    jdbcTemplate.update(
        "DELETE FROM hospedagem_novo_preco WHERE fk_hospedagem = ?", hospedagemId);
  }

  public void atualizarValorTotal(Long hospedagemId, Double valorTotal) {
    jdbcTemplate.update(
        "UPDATE hospedagem SET valor_total = ? WHERE id = ?", valorTotal, hospedagemId);
  }

  /** Atualiza o período (checkin/checkout) da hospedagem — usado ao recalcular as diárias. */
  public void atualizarPeriodo(
      Long hospedagemId, LocalDateTime checkin, LocalDateTime checkout) {
    jdbcTemplate.update(
        "UPDATE hospedagem SET data_hora_checkin = ?, data_hora_checkout = ? WHERE id = ?",
        checkin,
        checkout,
        hospedagemId);
  }

  /** Modo "valor por diária" na criação: sobrescreve o valor de todas as diárias da hospedagem. */
  public void sobrescreverValorDiarias(Long hospedagemId, Double valor) {
    if (valor == null) return;
    jdbcTemplate.update(
        "UPDATE diaria SET valor = ? WHERE fk_hospedagem = ?", valor, hospedagemId);
  }

  /** Modo "valor por diária": sobrescreve o valor das diárias existentes (qtd inalterada). */
  public void atualizarValoresDiarias(List<HospedagemNovoPreco.DiariaValor> diarias) {
    if (diarias == null || diarias.isEmpty()) return;
    jdbcTemplate.batchUpdate(
        "UPDATE diaria SET valor = ? WHERE id = ?",
        diarias,
        diarias.size(),
        (ps, d) -> {
          ps.setObject(1, d.valor());
          ps.setLong(2, d.id());
        });
  }

  /**
   * Retorna chaves "quartoId_checkin_checkout" de todas as diárias já cadastradas para a hospedagem
   * — permite verificação em lote no serviço, sem N queries individuais.
   */
  public Set<String> buscarChavesDiariasExistentes(Long hospedagemId) {
    List<String> chaves =
        jdbcTemplate.query(
            "SELECT fk_quarto, checkin, checkout FROM diaria WHERE fk_hospedagem = ?",
            (rs, rowNum) ->
                rs.getLong("fk_quarto")
                    + "_"
                    + rs.getObject("checkin", LocalDateTime.class)
                    + "_"
                    + rs.getObject("checkout", LocalDateTime.class),
            hospedagemId);
    return new HashSet<>(chaves);
  }

  public Boolean isDiariaJaExiste(Long hospedagemId, Hospedagem.Diaria.Request diaria) {
    return jdbcTemplate.queryForObject(
        """
                        SELECT EXISTS (
                                               SELECT 1
                                               FROM diaria
                                               WHERE fk_hospedagem = ?
                                                 AND fk_quarto = ?
                                                 AND checkin = ?
                                                 AND checkout = ?
                                           )
                        """,
        Boolean.class,
        hospedagemId,
        diaria.quarto_id(),
        diaria.checkin(),
        diaria.checkout());
  }

  // ── Batch helpers (replace N+1 in buscar) ────────────────────────────────────

  private String inClause(int size) {
    return java.util.stream.IntStream.range(0, size)
        .mapToObj(i -> "?")
        .collect(Collectors.joining(", "));
  }

  public Map<Long, List<Hospedagem.Diaria>> listarDiariasBatch(List<Long> ids) {
    if (ids.isEmpty()) return Map.of();
    String sql =
        """
        SELECT
            diaria.fk_hospedagem              AS diaria_fk_hospedagem,
            diaria.id                         AS diaria_id,
            diaria.numero                     AS diaria_numero,
            diaria.valor                      AS diaria_valor,
            diaria.meia_diaria                AS diaria_meia_diaria,
            diaria.checkin                    AS diaria_checkin,
            diaria.checkout                   AS diaria_checkout,
            quarto.id                         AS diaria_quarto_id,
            quarto.descricao                  AS diaria_quarto_descricao
        FROM public.diaria
        LEFT JOIN public.quarto quarto ON quarto.id = diaria.fk_quarto
        WHERE diaria.fk_hospedagem IN (%s)
        ORDER BY diaria.fk_hospedagem, diaria.numero
        """
            .formatted(inClause(ids.size()));
    Map<Long, List<Hospedagem.Diaria>> result = new LinkedHashMap<>();
    jdbcTemplate.query(
        sql,
        rs -> {
          Long hId = rs.getLong("diaria_fk_hospedagem");
          Long quartoId = rs.getObject("diaria_quarto_id", Long.class);
          result
              .computeIfAbsent(hId, k -> new ArrayList<>())
              .add(
                  new Hospedagem.Diaria(
                      rs.getLong("diaria_id"),
                      rs.getInt("diaria_numero"),
                      quartoId == null
                          ? null
                          : new Quarto.Descricao(quartoId, rs.getString("diaria_quarto_descricao")),
                      rs.getObject("diaria_checkin", LocalDateTime.class),
                      rs.getObject("diaria_checkout", LocalDateTime.class),
                      rs.getFloat("diaria_valor"),
                      rs.getObject("diaria_meia_diaria", Boolean.class),
                      null));
        },
        ids.toArray());
    return result;
  }

  public Map<Long, List<Item.Consumo>> buscarConsumosBatch(List<Long> ids) {
    if (ids.isEmpty()) return Map.of();
    String sql =
        """
                    SELECT
            hospedagem_consumo.fk_hospedagem                              AS consumo_fk_hospedagem,
            consumo.id                                                    AS consumo_id,
            consumo.data_hora_registro                                    AS consumo_data_hora_registro,
            consumo.quantidade                                            AS consumo_quantidade,
            consumo.despesa_pessoal                                       AS consumo_despesa_pessoal,
            consumo.cancelado                                             AS consumo_cancelado,
            item.id                                                       AS consumo_item_id,
            item.descricao                                                AS consumo_item_descricao,
            estoque.valor_venda_unidade                                   AS consumo_item_valor_venda_unidade,
            consumo_funcionario.id                                        AS consumo_funcionario_id,
            consumo_pessoa_funcionario.nome                               AS consumo_funcionario_nome,
            consumo_quarto.id                                             AS consumo_quarto_id,
            consumo_quarto.descricao                                      AS consumo_quarto_descricao,
            consumo_pagamento.id                                          AS consumo_pagamento_id,
            consumo_pagamento.data_hora_registro                          AS consumo_pagamento_data_hora_registro,
            consumo_pagamento.nome_pagador                                AS consumo_pagamento_nome_pagador,
            consumo_pagamento.descricao                                   AS consumo_pagamento_descricao,
            consumo_pagamento.valor                                       AS consumo_pagamento_valor,
            consumo_pagamento.cancelado                                   AS consumo_pagamento_cancelado,
            consumo_pagamento.path_arquivo                                AS consumo_pagamento_path_arquivo,
            consumo_pagamento_tipo_pagamento.id                           AS consumo_tipo_pagamento_id,
            consumo_pagamento_tipo_pagamento.descricao                    AS consumo_tipo_pagamento_descricao,
            consumo_funcionario_consumo_pagamento.id                      AS consumo_pagamento_funcionario_id,
            consumo_pessoa_funcionario_consumo_pagamento.nome             AS consumo_pagamento_funcionario_nome,
            consumo_pagamento_motivo_cancelamento.id                      AS consumo_pagamento_motivo_id,
            consumo_pagamento_motivo_cancelamento.motivo_cancelamento     AS consumo_pagamento_motivo_cancelamento,
            consumo_pagamento_motivo_cancelamento_funcionario.id          AS consumo_pagamento_motivo_funcionario_id,
            consumo_pagamento_motivo_cancelamento_pessoa_funcionario.nome AS consumo_pagamento_motivo_funcionario_nome,
            consumo_pagamento_motivo_cancelamento.data_hora_registro      AS consumo_pagamento_motivo_data_hora_registro
        FROM public.hospedagem_consumo
            JOIN public.consumo ON consumo.id = hospedagem_consumo.fk_consumo
            JOIN public.item ON item.id = consumo.fk_item
            LEFT JOIN public.funcionario consumo_funcionario ON consumo_funcionario.id = consumo.fk_funcionario
            LEFT JOIN public.pessoa consumo_pessoa_funcionario ON consumo_pessoa_funcionario.id = consumo_funcionario.fk_pessoa
            LEFT JOIN public.quarto consumo_quarto ON consumo_quarto.id = consumo.fk_quarto
            LEFT JOIN public.pagamento consumo_pagamento ON consumo_pagamento.id = consumo.fk_pagamento
            LEFT JOIN public.tipo_pagamento consumo_pagamento_tipo_pagamento ON consumo_pagamento_tipo_pagamento.id = consumo_pagamento.fk_tipo_pagamento
            LEFT JOIN public.funcionario consumo_funcionario_consumo_pagamento ON consumo_funcionario_consumo_pagamento.id = consumo_pagamento.fk_funcionario
            LEFT JOIN public.pessoa consumo_pessoa_funcionario_consumo_pagamento ON consumo_pessoa_funcionario_consumo_pagamento.id = consumo_funcionario_consumo_pagamento.fk_pessoa
            LEFT JOIN LATERAL (
                SELECT * FROM public.pagamento_motivo_cancelamento mc
                WHERE mc.fk_pagamento = consumo_pagamento.id
                ORDER BY mc.data_hora_registro DESC LIMIT 1
            ) consumo_pagamento_motivo_cancelamento ON true
            LEFT JOIN public.funcionario consumo_pagamento_motivo_cancelamento_funcionario ON consumo_pagamento_motivo_cancelamento_funcionario.id = consumo_pagamento_motivo_cancelamento.fk_funcionario
            LEFT JOIN public.pessoa consumo_pagamento_motivo_cancelamento_pessoa_funcionario ON consumo_pagamento_motivo_cancelamento_pessoa_funcionario.id = consumo_pagamento_motivo_cancelamento_funcionario.fk_pessoa
            LEFT JOIN estoque ON estoque.fk_item = item.id
        WHERE hospedagem_consumo.fk_hospedagem IN (%s)
        """
            .formatted(inClause(ids.size()));
    Map<Long, List<Item.Consumo>> result = new LinkedHashMap<>();
    jdbcTemplate.query(
        sql,
        rs -> {
          Long hId = rs.getLong("consumo_fk_hospedagem");
          Item.Consumo consumo = Item.Consumo.ROW_MAPPER.mapRow(rs, 0);
          if (consumo != null) result.computeIfAbsent(hId, k -> new ArrayList<>()).add(consumo);
        },
        ids.toArray());
    return result;
  }

  public Map<Long, List<Hospedagem.PessoaHospedagemOrcamento>> buscarPessoasOrcamentoBatch(
      List<Long> ids) {
    if (ids.isEmpty()) return Map.of();
    String sql =
        "SELECT * FROM orcamento_hospedagem_pessoa WHERE fk_hospedagem IN ("
            + inClause(ids.size())
            + ")";
    Map<Long, List<Hospedagem.PessoaHospedagemOrcamento>> result = new LinkedHashMap<>();
    jdbcTemplate.query(
        sql,
        rs -> {
          Long hId = rs.getLong("fk_hospedagem");
          result
              .computeIfAbsent(hId, k -> new ArrayList<>())
              .add(Hospedagem.PessoaHospedagemOrcamento.MAPPER.mapRow(rs, 0));
        },
        ids.toArray());
    return result;
  }

  public Map<Long, MotivoCancelamentoHospedagem> buscarMotivoCancelamentoBatch(List<Long> ids) {
    if (ids.isEmpty()) return Map.of();
    String sql =
        """
        SELECT DISTINCT ON (hmc.fk_hospedagem)
            hmc.fk_hospedagem,
            hmc.id,
            hmc.motivo_cancelamento,
            hmc.data_hora_registro,
            f.id   AS funcionario_id,
            p.nome AS funcionario_nome
        FROM hospedagem_motivo_cancelamento hmc
        JOIN funcionario f ON f.id = hmc.fk_funcionario
        JOIN public.pessoa p ON p.id = f.fk_pessoa
        WHERE hmc.fk_hospedagem IN (%s)
        ORDER BY hmc.fk_hospedagem, hmc.id DESC
        """
            .formatted(inClause(ids.size()));
    Map<Long, MotivoCancelamentoHospedagem> result = new HashMap<>();
    jdbcTemplate.query(
        sql,
        rs -> {
          result.put(
              rs.getLong("fk_hospedagem"),
              new MotivoCancelamentoHospedagem(
                  rs.getLong("id"),
                  rs.getString("motivo_cancelamento"),
                  new Funcionario.Nome(
                      rs.getLong("funcionario_id"), rs.getString("funcionario_nome")),
                  rs.getTimestamp("data_hora_registro").toLocalDateTime()));
        },
        ids.toArray());
    return result;
  }

  public List<Hospedagem.Diaria> listarDiarias(Long hospedagemId) {
    return jdbcTemplate.query(
        """
                        SELECT
                             diaria.id                          AS diaria_id,
                             diaria.numero                      AS diaria_numero,
                             diaria.valor                       AS diaria_valor,
                             diaria.meia_diaria                 AS diaria_meia_diaria,
                             diaria.checkin                     AS diaria_checkin,
                             diaria.checkout                    AS diaria_checkout,
                             quarto.id                          AS diaria_quarto_id,
                             quarto.descricao                   AS diaria_quarto_descricao
                         FROM public.diaria
                         LEFT JOIN public.quarto quarto ON quarto.id = diaria.fk_quarto
                         WHERE fk_hospedagem = ?;
                        """,
        (rs, x) -> {
          Long quartoId = rs.getObject("diaria_quarto_id", Long.class);
          return new Hospedagem.Diaria(
              rs.getLong("diaria_id"),
              rs.getInt("diaria_numero"),
              quartoId == null
                  ? null
                  : new Quarto.Descricao(quartoId, rs.getString("diaria_quarto_descricao")),
              rs.getObject("diaria_checkin", LocalDateTime.class),
              rs.getObject("diaria_checkout", LocalDateTime.class),
              rs.getFloat("diaria_valor"),
              rs.getObject("diaria_meia_diaria", Boolean.class),
              null);
        },
        hospedagemId);
  }

  public Quarto.Status statusQuarto(Long quartoId) {
    return jdbcTemplate.queryForObject(
        "SELECT status FROM public.quarto WHERE id = ?", Quarto.Status.class, quartoId);
  }

  public Boolean isQuartoDisponivel(
      Long quartoId, LocalDateTime checkin, LocalDateTime checkout, Long hospedagemIdExcluido) {

    String sql =
        """
      SELECT COUNT(*) > 0
      FROM hospedagem
      WHERE hospedagem.fk_quarto = ?
        AND (
          (
            hospedagem.status = 'ORCAMENTO'
            AND hospedagem.data_hora_checkin < ?
            AND hospedagem.data_hora_checkout > ?
          )
          OR
          (
            hospedagem.status IN (
              'RESERVA_ATIVA',
              'PERNOITE_ATIVO',
              'DAY_USE_SOLICITADO',
              'DAY_USE_ATIVO'
            )
            AND EXISTS (
              SELECT 1
              FROM diaria
              WHERE diaria.fk_hospedagem = hospedagem.id
                AND diaria.fk_quarto = ?
                AND diaria.checkin < ?
                AND diaria.checkout > ?
            )
          )
        )
      """
            + (hospedagemIdExcluido != null ? " AND hospedagem.id != ?" : "");

    boolean conflito;
    if (hospedagemIdExcluido != null) {
      conflito =
          jdbcTemplate.queryForObject(
              sql,
              Boolean.class,
              quartoId, // fk_quarto
              checkout,
              checkin, // bloco ORCAMENTO
              quartoId,
              checkout,
              checkin, // bloco EXISTS
              hospedagemIdExcluido // exclusão
              );
    } else {
      conflito =
          jdbcTemplate.queryForObject(
              sql,
              Boolean.class,
              quartoId, // fk_quarto
              checkout,
              checkin, // bloco ORCAMENTO
              quartoId,
              checkout,
              checkin // bloco EXISTS
              );
    }

    log.info("Conflito encontrado: {}", conflito);
    return conflito;
  }

  public record DisponibilidadeQuartoRow(
      Long quartoId, String descricao, Quarto.Status statusFisico, boolean conflito) {}

  public List<DisponibilidadeQuartoRow> verificarDisponibilidadeLote(
      LocalDateTime checkin, LocalDateTime checkout) {
    String sql =
        """
        SELECT
          q.id        AS quarto_id,
          q.descricao AS quarto_descricao,
          q.status    AS quarto_status,
          EXISTS (
            SELECT 1
            FROM hospedagem h
            WHERE h.fk_quarto = q.id
              AND (
                (
                  h.status = 'ORCAMENTO'
                  AND h.data_hora_checkin::date  < ?::date
                  AND h.data_hora_checkout::date > ?::date
                )
                OR
                (
                  h.status IN (
                    'RESERVA_ATIVA',
                    'PERNOITE_ATIVO',
                    'DAY_USE_ATIVO'
                  )
                  AND EXISTS (
                    SELECT 1
                    FROM diaria d
                    WHERE d.fk_hospedagem = h.id
                      AND d.fk_quarto = q.id
                      AND d.checkin::date  < ?::date
                      AND d.checkout::date > ?::date
                  )
                )
              )
          ) AS conflito
        FROM public.quarto q
        ORDER BY q.id
        """;
    return jdbcTemplate.query(
        sql,
        (rs, rowNum) ->
            new DisponibilidadeQuartoRow(
                rs.getLong("quarto_id"),
                rs.getString("quarto_descricao"),
                Quarto.Status.valueOf(rs.getString("quarto_status")),
                rs.getBoolean("conflito")),
        checkout,
        checkin, // bloco ORCAMENTO (checkin < checkout, checkout > checkin)
        checkout,
        checkin); // bloco diaria
  }

  public void adicionarMotivoCancelamento(
      MotivoCancelamentoHospedagem.Request request, Long funcionarioId) {
    jdbcTemplate.update(
        """
                        INSERT INTO hospedagem_motivo_cancelamento (motivo_cancelamento, fk_funcionario, data_hora_registro, fk_hospedagem)
                        VALUES (?, ?, now(), ?)
                        """,
        request.motivo_cancelamento(),
        funcionarioId,
        request.fk_hospedagem());
  }

  public void editarMotivoCancelamento(MotivoCancelamentoHospedagem.Request request) {
    jdbcTemplate.update(
        """
                        UPDATE hospedagem_motivo_cancelamento
                        SET motivo_cancelamento = ?
                        WHERE id = ?
                        """,
        request.motivo_cancelamento(),
        request.id());
  }

  public MotivoCancelamentoHospedagem buscarMotivoCancelamento(Long hospedagemId) {
    try {
      return jdbcTemplate.queryForObject(
          """
                                  SELECT hospedagem_motivo_cancelamento.id,
                                         hospedagem_motivo_cancelamento.motivo_cancelamento,
                                         hospedagem_motivo_cancelamento.data_hora_registro,
                                         funcionario.id   AS funcionario_id,
                                         pessoa.nome      AS funcionario_nome
                                  FROM hospedagem_motivo_cancelamento
                                  JOIN funcionario ON funcionario.id = hospedagem_motivo_cancelamento.fk_funcionario
                                  join public.pessoa on pessoa.id = funcionario.fk_pessoa
                                  WHERE hospedagem_motivo_cancelamento.fk_hospedagem = ?
                                  """,
          (rs, x) ->
              new MotivoCancelamentoHospedagem(
                  rs.getLong("id"),
                  rs.getString("motivo_cancelamento"),
                  new Funcionario.Nome(
                      rs.getLong("funcionario_id"), rs.getString("funcionario_nome")),
                  rs.getTimestamp("data_hora_registro").toLocalDateTime()),
          hospedagemId);
    } catch (EmptyResultDataAccessException e) {
      return null;
    }
  }

  public Map<Long, Hospedagem> buscarAtivasPorQuartoNaData(LocalDate data) {
    Map<Long, Long> quartoParaHospedagem = new LinkedHashMap<>();
    jdbcTemplate.query(
        """
                        SELECT DISTINCT ON (d.fk_quarto)
                            d.fk_quarto AS quarto_id,
                            h.id        AS hospedagem_id
                        FROM public.diaria d
                        JOIN public.hospedagem h ON h.id = d.fk_hospedagem
                        WHERE h.status::hospedagem_status IN (
                            'PERNOITE_ATIVO',
                            'RESERVA_ATIVA', 'RESERVA_SOLICITADA',
                            'DAY_USE_ATIVO', 'DAY_USE_SOLICITADO'
                        )
                          AND (
                                -- (1) Pernoite ativo: sempre visível, independente da data de checkout,
                                --     até mudar para um status final.
                                h.status::hospedagem_status = 'PERNOITE_ATIVO'
                                -- estadia cujo período contém a data de referência (hoje)
                             OR (d.checkin::date <= ? AND d.checkout::date > ?)
                                -- (2) Reserva ativa já iniciada (mesmo após o checkout): hóspede ainda
                                --     não hospedado — permite hospedar mais tarde.
                             OR (h.status::hospedagem_status = 'RESERVA_ATIVA' AND d.checkin::date <= ?)
                              )
                        ORDER BY
                            d.fk_quarto,
                            -- Pernoite ativo é o principal do quarto.
                            CASE WHEN h.status::hospedagem_status = 'PERNOITE_ATIVO' THEN 0 ELSE 1 END,
                            -- Reservas: respeita a fila — mostra a próxima/mais antiga relevante
                            -- (a que está acontecendo hoje ou ficou para trás), nunca a última.
                            d.checkin::date,
                            h.data_hora_checkin
                        """,
        rs -> {
          quartoParaHospedagem.put(rs.getLong("quarto_id"), rs.getLong("hospedagem_id"));
        },
        data,
        data,
        data);

    if (quartoParaHospedagem.isEmpty()) return Map.of();

    List<Long> hospedagemIds = new ArrayList<>(quartoParaHospedagem.values());
    String in = hospedagemIds.stream().map(id -> "?").collect(Collectors.joining(", "));

    Map<Long, Hospedagem> hospedagemPorId =
        jdbcTemplate
            .query(
                SELECT_HOSPEDAGEM + " WHERE hospedagem.id IN (" + in + ")",
                Hospedagem.MAPPER,
                hospedagemIds.toArray())
            .stream()
            .collect(Collectors.toMap(Hospedagem::id, h -> h));

    Map<Long, Hospedagem> result = new HashMap<>();
    quartoParaHospedagem.forEach(
        (quartoId, hospedagemId) -> {
          Hospedagem h = hospedagemPorId.get(hospedagemId);
          if (h != null) result.put(quartoId, h);
        });
    return result;
  }

  public boolean temReservaAtivaParaQuartoHoje(Long quartoId) {
    Integer count =
        jdbcTemplate.queryForObject(
            """
                                SELECT COUNT(DISTINCT h.id)
                                FROM public.diaria d
                                JOIN public.hospedagem h ON h.id = d.fk_hospedagem
                                WHERE d.fk_quarto = ?
                                  AND h.status::hospedagem_status IN ('RESERVA_ATIVA', 'RESERVA_SOLICITADA')
                                  AND d.checkin::date = CURRENT_DATE
                                """,
            Integer.class,
            quartoId);
    return count > 0;
  }

  public List<Hospedagem.PessoaHospedagemOrcamento> buscarPessoasHospedagemOrcamento(
      Long hospedagemId) {
    try {
      return jdbcTemplate.query(
          """
                                select * from orcamento_hospedagem_pessoa where fk_hospedagem = ?;
                        """,
          Hospedagem.PessoaHospedagemOrcamento.MAPPER,
          hospedagemId);
    } catch (EmptyResultDataAccessException e) {
      return List.of();
    }
  }

  public void atualizarPrecoHospedagem(Long hospedagemId, Double novoValor) {
    jdbcTemplate.update(
        "update hospedagem set valor_total = ? where id = ?", novoValor, hospedagemId);
  }
}
