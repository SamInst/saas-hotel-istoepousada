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
                        pessoa_funcionario_hospedagem.nome AS hospedagem_funcionario_nome
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
      String placeholders = statuses.stream().map(s -> "?").collect(Collectors.joining(", "));
      conditions.add("hospedagem.status::text IN (" + placeholders + ")");
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

  public Hospedagem buscarPorId(Long id) {
    try {
      return jdbcTemplate.queryForObject(
          SELECT_HOSPEDAGEM + " WHERE hospedagem.id = ? ", Hospedagem.MAPPER, id);
    } catch (EmptyResultDataAccessException e) {
      throw new NotFoundException("Hospedagem nao encontrada para o id: " + id);
    }
  }

  public List<Long> adicionarOrcamentos(
      Long hospedagemId, List<Orcamento.Request> requests, Long funcionarioId) {
    List<Long> orcamentosIds = new ArrayList<>();

    requests.forEach(
        request -> {
          KeyHolder keyHolder = new GeneratedKeyHolder();

          jdbcTemplate.update(
              con -> {
                PreparedStatement ps =
                    con.prepareStatement(
                        """
                INSERT INTO orcamento (nome_solicitante, fk_funcionario, fk_categoria, observacao, data_checkin, data_checkout)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                        Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, request.nome_solicitante());
                ps.setLong(2, funcionarioId);
                ps.setLong(3, request.categoria().id());
                ps.setString(4, request.observacao());
                ps.setObject(5, request.checkin());
                ps.setObject(6, request.checkout());
                return ps;
              },
              keyHolder);

          orcamentosIds.add(Objects.requireNonNull(keyHolder.getKey()).longValue());
        });

    jdbcTemplate.batchUpdate(
        """
        INSERT INTO hospedagem_orcamento (fk_hospedagem, fk_orcamento)
        VALUES (?, ?)
        """,
        orcamentosIds,
        orcamentosIds.size(),
        (ps, orcamentoId) -> {
          ps.setLong(1, hospedagemId);
          ps.setLong(2, orcamentoId);
        });
    return orcamentosIds;
  }

  public void editarOrcamento(Orcamento.Request request) {
    jdbcTemplate.update(
        """
            UPDATE orcamento
            SET nome_solicitante = ?,
                fk_categoria     = ?,
                observacao       = ?,
                data_checkin     = ?,
                data_checkout    = ?
            WHERE id = ?
            """,
        request.nome_solicitante(),
        request.categoria().id(),
        request.observacao(),
        request.checkin().toLocalDate(),
        request.checkout().toLocalDate(),
        request.id());
  }

  public void adicionarPessoasOrcamento(Long orcamentoId, List<Orcamento.Pessoa.Request> pessoas) {
    jdbcTemplate.batchUpdate(
        """
            INSERT INTO orcamento_pessoa (fk_orcamento, nome_pessoa, data_nascimento)
            VALUES (?, ?, ?)
            """,
        pessoas,
        pessoas.size(),
        (ps, pessoa) -> {
          ps.setLong(1, orcamentoId);
          ps.setString(2, pessoa.nome());
          ps.setObject(3, pessoa.data_nascimento());
        });
  }

  public void removerPessoasOrcamento(Long orcamentoId, List<Long> pessoasIds) {
    jdbcTemplate.batchUpdate(
        """
            DELETE FROM orcamento_pessoa
            WHERE id = ? AND fk_orcamento = ?
            """,
        pessoasIds,
        pessoasIds.size(),
        (ps, pessoaId) -> {
          ps.setLong(1, pessoaId);
          ps.setLong(2, orcamentoId);
        });
  }

  public Orcamento buscarOrcamento(Long hospedagemId) {
    Orcamento orcamento =
        jdbcTemplate.queryForObject(
            """
            SELECT orcamento.id,
                   orcamento.nome_solicitante,
                   funcionario.id    AS funcionario_id,
                   pessoa.nome       AS funcionario_nome,
                   categoria.id      AS categoria_id,
                   categoria.nome    AS categoria_nome,
                   orcamento.observacao,
                   orcamento.data_checkin,
                   orcamento.data_checkout,
                   orcamento.data_hora_registro
            FROM orcamento
            JOIN hospedagem_orcamento ON hospedagem_orcamento.fk_orcamento = orcamento.id
            JOIN funcionario          ON funcionario.id = orcamento.fk_funcionario
            JOIN pessoa               ON pessoa.id = funcionario.fk_pessoa
            JOIN categoria            ON categoria.id = orcamento.fk_categoria
            WHERE hospedagem_orcamento.fk_hospedagem = ?
            """,
            (rs, x) ->
                new Orcamento(
                    rs.getLong("id"),
                    rs.getString("nome_solicitante"),
                    new Funcionario.Nome(
                        rs.getLong("funcionario_id"), rs.getString("funcionario_nome")),
                    new Categoria.Nome(rs.getLong("categoria_id"), rs.getString("categoria_nome")),
                    rs.getString("observacao"),
                    rs.getTimestamp("data_checkin").toLocalDateTime(),
                    rs.getTimestamp("data_checkout").toLocalDateTime(),
                    rs.getTimestamp("data_hora_registro").toLocalDateTime(),
                    List.of()),
            hospedagemId);

    List<Orcamento.Pessoa> pessoas =
        jdbcTemplate.query(
            """
            SELECT id, nome_pessoa, data_nascimento
            FROM orcamento_pessoa
            WHERE fk_orcamento = ?
            """,
            (rs, x) ->
                new Orcamento.Pessoa(
                    rs.getLong("id"),
                    rs.getString("nome_pessoa"),
                    rs.getDate("data_nascimento").toLocalDate().atStartOfDay()),
            orcamento.id());

    return new Orcamento(
        orcamento.id(),
        orcamento.nome_solicitante(),
        orcamento.funcionario(),
        orcamento.categoria(),
        orcamento.observacao(),
        orcamento.checkin(),
        orcamento.checkout(),
        orcamento.data_hora_registro(),
        pessoas);
  }

  public Hospedagem insertHospedagem(Hospedagem.Request request, Long funcionarioId) {
    var hospedagem_id =
        jdbcTemplate.queryForObject(
            """
            insert into hospedagem (
                fk_funcionario,
                data_hora_registro,
                data_hora_checkin,
                data_hora_checkout,
                valor_total,
                status,
                observacao)
            values (?, now(), ?, ?, ?, ?::hospedagem_status, ?)
            returning id;
        """,
            Long.class,
            funcionarioId,
            request.data_hora_checkin(),
            request.data_hora_checkout(),
            request.valor_total(),
            request.status(),
            request.observacao());
    return buscarPorId(hospedagem_id);
  }

  public void alterarStatus(Long hospedagemId, Hospedagem.Status status) {
    jdbcTemplate.update(
        """
                update hospedagem set status = ?::hospedagem_status where id = ?
                """,
        status,
        hospedagemId);
  }

  public void adicionarHospedagemPagamento(Long hospedagemId, List<UUID> pagamentosUUID) {
    jdbcTemplate.batchUpdate(
        """
        INSERT INTO hospedagem_pagamento (fk_hospedagem, fk_pagamento)
        VALUES (?, ?)
        """,
        pagamentosUUID.stream()
            .map(pagamentoUUID -> new Object[] {hospedagemId, pagamentoUUID})
            .toList());
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
                estoque.valor_venda_unidade                                         AS consumo_item_valor_venda_unidade,
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
            """,
        Item.Consumo.ROW_MAPPER,
        hospedagemId);
  }

  public void adicionarDiarias(
      Long hospedagemId, List<Hospedagem.Diaria.Request> diarias, Double valorTotal) {
    jdbcTemplate.batchUpdate(
        """
                        INSERT INTO diaria (fk_hospedagem, fk_quarto, numero, checkin, checkout, valor)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
        diarias,
        diarias.size(),
        (ps, diaria) -> {
          ps.setLong(1, hospedagemId);
          ps.setLong(2, diaria.quarto_id());
          ps.setInt(3, diarias.indexOf(diaria) + 1);
          ps.setObject(4, diaria.checkin());
          ps.setObject(5, diaria.checkout());
          ps.setDouble(6, valorTotal);
        });
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

  public List<Hospedagem.Diaria> listarDiarias(Long hospedagemId) {
    return jdbcTemplate.query(
        """
                        SELECT
                             diaria.id                          AS diaria_id,
                             diaria.numero                      AS diaria_numero,
                             diaria.valor                       AS diaria_valor,
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
    String sqlHospedagem =
        """
                        SELECT COUNT(*) > 0
                        FROM diaria
                        JOIN hospedagem ON hospedagem.id = diaria.fk_hospedagem
                        WHERE diaria.fk_quarto = 1
                          AND hospedagem.status IN (
                          'HOSPEDAGEM_ORCAMENTO',
                          'HOSPEDAGEM_RESERVA_ATIVA',
                          'HOSPEDAGEM_RESERVA_SOLICITADA',
                          'HOSPEDAGEM_PERNOITE_ATIVA',
                          'HOSPEDAGEM_DAY_USE_SOLICITADA',
                          'HOSPEDAGEM_DAY_USE_ATIVA')
                          AND diaria.checkin < '2025-05-13'
                          AND diaria.checkout > '2025-05-13'
                        """
            + (hospedagemIdExcluido != null ? " AND hospedagem.id != ? " : "");

    boolean hospedagemConflito;
    if (hospedagemIdExcluido != null) {
      hospedagemConflito =
          jdbcTemplate.queryForObject(
              sqlHospedagem, Boolean.class, quartoId, checkin, checkout, hospedagemIdExcluido);
    } else {
      hospedagemConflito =
          jdbcTemplate.queryForObject(sqlHospedagem, Boolean.class, quartoId, checkout, checkin);
    }
    log.info("Quarto disponivel: {}", hospedagemConflito);
    return hospedagemConflito;
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
                WHERE h.status::text IN (
                    'PERNOITE_ATIVA', 'PERNOITE_FINALIZADA_PAGAMENTO_PENDENTE',
                    'RESERVA_ATIVA', 'RESERVA_SOLICITADA',
                    'DAY_USE_ATIVA', 'DAY_USE_SOLICITADA'
                )
                  AND d.checkin::date <= ?
                  AND d.checkout::date > ?
                ORDER BY d.fk_quarto, h.data_hora_checkin DESC
                """,
        rs -> {
          quartoParaHospedagem.put(rs.getLong("quarto_id"), rs.getLong("hospedagem_id"));
        },
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
                  AND h.status::text IN ('RESERVA_ATIVA', 'RESERVA_SOLICITADA')
                  AND d.checkin::date = CURRENT_DATE
                """,
            Integer.class,
            quartoId);
    return count != null && count > 0;
  }
}
