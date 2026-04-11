package saas.hotel.istoepousada.repository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.Reserva;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;

@Repository
public class ReservaRepository {

  private final JdbcTemplate jdbcTemplate;
  private final PessoaRepository pessoaRepository;

  public ReservaRepository(JdbcTemplate jdbcTemplate, PessoaRepository pessoaRepository) {
    this.jdbcTemplate = jdbcTemplate;
    this.pessoaRepository = pessoaRepository;
  }

  // ── Categoria/checkin info por quarto ──────────────────────────────────────

  public record CategoriaCheckin(
      Long id, String nome, LocalTime hora_checkin, LocalTime hora_checkout) {}

  public CategoriaCheckin findCategoriaCheckinByQuartoId(Long quartoId) {
    try {
      return jdbcTemplate.queryForObject(
          """
          SELECT c.id, c.nome, c.hora_checkin, c.hora_checkout
          FROM public.quarto_categoria qc
          JOIN public.categoria c ON c.id = qc.fk_categoria
          WHERE qc.fk_quarto = ?
          """,
          (rs, rowNum) ->
              new CategoriaCheckin(
                  rs.getLong("id"),
                  rs.getString("nome"),
                  rs.getObject("hora_checkin", LocalTime.class),
                  rs.getObject("hora_checkout", LocalTime.class)),
          quartoId);
    } catch (EmptyResultDataAccessException e) {
      return null;
    }
  }

  // ── Sazonalidades ativas para uma categoria ────────────────────────────────

  public record SazonInfo(
      Long id,
      String descricao,
      LocalDate dataInicio,
      LocalDate dataFim,
      LocalTime horaCheckin,
      LocalTime horaCheckout,
      List<Integer> semanal,
      List<Integer> mensal,
      List<Integer> anual) {}

  public List<SazonInfo> findSazonalidades(Long categoriaId) {
    return jdbcTemplate.query(
        """
        SELECT s.id, s.descricao, s.data_inicio, s.data_fim,
               s.hora_checkin, s.hora_checkout, s.semanal, s.mensal, s.anual
        FROM public.categoria_sazonalidade cs
        JOIN public.sazonalidade s ON s.id = cs.fk_sazonalidade
        WHERE cs.fk_categoria = ? AND cs.ativo = true
        ORDER BY s.id
        """,
        (rs, rowNum) ->
            new SazonInfo(
                rs.getLong("id"),
                rs.getString("descricao"),
                rs.getObject("data_inicio", LocalDate.class),
                rs.getObject("data_fim", LocalDate.class),
                rs.getObject("hora_checkin", LocalTime.class),
                rs.getObject("hora_checkout", LocalTime.class),
                parseIntArray(rs.getArray("semanal")),
                parseIntArray(rs.getArray("mensal")),
                parseIntArray(rs.getArray("anual"))),
        categoriaId);
  }

  private static List<Integer> parseIntArray(java.sql.Array arr) {
    if (arr == null) return null;
    try {
      Integer[] boxed = (Integer[]) arr.getArray();
      return (boxed == null || boxed.length == 0) ? null : List.of(boxed);
    } catch (SQLException e) {
      return null;
    }
  }

  // ── SELECT base ────────────────────────────────────────────────────────────

  private static final String SELECT_RESERVA_BASE =
      """
      SELECT
        r.id                    AS reserva_id,
        r.ativa                 AS reserva_ativa,
        r.hospedado             AS reserva_hospedado,
        r.cancelado             AS reserva_cancelado,
        r.data_hora_entrada     AS reserva_data_hora_entrada,
        r.data_hora_saida       AS reserva_data_hora_saida,
        r.data_hora_registro    AS reserva_data_hora_registro,
        q.id                    AS reserva_quarto_id,
        q.descricao             AS reserva_quarto_descricao,
        c.id                    AS reserva_categoria_id,
        c.nome                  AS reserva_categoria_nome,
        f.id                    AS reserva_funcionario_id,
        pf.nome                 AS reserva_funcionario_nome
      FROM public.reserva r
      JOIN public.quarto q ON q.id = r.fk_quarto
      LEFT JOIN public.quarto_categoria qc ON qc.fk_quarto = r.fk_quarto
      LEFT JOIN public.categoria c ON c.id = qc.fk_categoria
      LEFT JOIN public.funcionario f ON f.id = r.fk_funcionario
      LEFT JOIN public.pessoa pf ON pf.id = f.fk_pessoa
      """;

  // ── Queries ────────────────────────────────────────────────────────────────

  public List<Reserva> buscarPorMesAno(int mes, int ano, Long idFiltro, String nome) {
    StringBuilder where =
        new StringBuilder(
            """
            WHERE r.cancelado = false
              AND EXTRACT(MONTH FROM r.data_hora_entrada) = ?
              AND EXTRACT(YEAR FROM r.data_hora_entrada) = ?
            """);
    List<Object> params = new ArrayList<>();
    params.add(mes);
    params.add(ano);

    if (idFiltro != null) {
      where.append(" AND r.id = ? ");
      params.add(idFiltro);
    }
    if (nome != null && !nome.isBlank()) {
      where.append(
          """
          AND EXISTS (
            SELECT 1 FROM public.reserva_pessoa rp2
            JOIN public.pessoa p2 ON p2.id = rp2.fk_pessoa
            WHERE rp2.fk_reserva = r.id AND p2.nome ILIKE ?
          )
          """);
      params.add("%" + nome.trim() + "%");
    }

    String sql = SELECT_RESERVA_BASE + where + " ORDER BY r.data_hora_entrada ASC ";
    List<Reserva> bases = jdbcTemplate.query(sql, Reserva.ROW_MAPPER, params.toArray());
    return enriquecer(bases);
  }

  public List<Reserva> buscarPorData(LocalDate data, Long idFiltro, String nome) {
    StringBuilder where =
        new StringBuilder(
            """
            WHERE r.cancelado = false
              AND r.data_hora_entrada::date = ?
            """);
    List<Object> params = new ArrayList<>();
    params.add(data);

    if (idFiltro != null) {
      where.append(" AND r.id = ? ");
      params.add(idFiltro);
    }
    if (nome != null && !nome.isBlank()) {
      where.append(
          """
          AND EXISTS (
            SELECT 1 FROM public.reserva_pessoa rp2
            JOIN public.pessoa p2 ON p2.id = rp2.fk_pessoa
            WHERE rp2.fk_reserva = r.id AND p2.nome ILIKE ?
          )
          """);
      params.add("%" + nome.trim() + "%");
    }

    String sql = SELECT_RESERVA_BASE + where + " ORDER BY r.data_hora_entrada ASC ";
    List<Reserva> bases = jdbcTemplate.query(sql, Reserva.ROW_MAPPER, params.toArray());
    return enriquecer(bases);
  }

  public List<Reserva> buscarPorQuartoMes(Long quartoId, int mes, int ano) {
    String sql =
        SELECT_RESERVA_BASE
            + """
            WHERE r.fk_quarto = ?
              AND r.cancelado = false
              AND EXTRACT(MONTH FROM r.data_hora_entrada) = ?
              AND EXTRACT(YEAR FROM r.data_hora_entrada) = ?
            ORDER BY r.data_hora_entrada ASC
            """;
    List<Reserva> bases = jdbcTemplate.query(sql, Reserva.ROW_MAPPER, quartoId, mes, ano);
    return enriquecer(bases);
  }

  public Reserva findById(Long id) {
    String sql = SELECT_RESERVA_BASE + " WHERE r.id = ? ";
    Reserva base;
    try {
      base = jdbcTemplate.queryForObject(sql, Reserva.ROW_MAPPER, id);
    } catch (EmptyResultDataAccessException e) {
      throw new NotFoundException("Reserva não encontrada: " + id);
    }
    return enriquecer(List.of(base)).get(0);
  }

  // ── Enriquecimento ─────────────────────────────────────────────────────────

  private List<Reserva> enriquecer(List<Reserva> bases) {
    if (bases.isEmpty()) return bases;

    List<Long> ids = bases.stream().map(Reserva::id).toList();
    Map<Long, List<Reserva.ReservaPessoa>> pessoasMap = buscarPessoasPorReservas(ids);
    Map<Long, List<Reserva.ReservaPagamento>> pagamentosMap = buscarPagamentosPorReservas(ids);

    return bases.stream()
        .map(
            r ->
                new Reserva(
                    r.id(),
                    r.quarto(),
                    r.categoria(),
                    r.funcionario(),
                    r.data_hora_entrada(),
                    r.data_hora_saida(),
                    r.data_hora_registro(),
                    r.ativa(),
                    r.hospedado(),
                    r.cancelado(),
                    pessoasMap.getOrDefault(r.id(), List.of()),
                    pagamentosMap.getOrDefault(r.id(), List.of())))
        .toList();
  }

  private Map<Long, List<Reserva.ReservaPessoa>> buscarPessoasPorReservas(List<Long> reservaIds) {
    String in = String.join(",", Collections.nCopies(reservaIds.size(), "?"));
    String sql =
        ("""
        SELECT
          rp.id                   AS rp_id,
          rp.fk_reserva           AS rp_fk_reserva,
          rp.representante        AS rp_representante,
          rp.data_hora_registro   AS rp_data_hora_registro,
          p.id                    AS pessoa_id,
          p.nome                  AS pessoa_nome,
          p.data_nascimento       AS pessoa_data_nascimento,
          p.cpf                   AS pessoa_cpf,
          p.email                 AS pessoa_email,
          p.telefone              AS pessoa_telefone,
          p.status                AS pessoa_status,
          rp.representante        AS pessoa_titular,
          rf.id                   AS rp_funcionario_id,
          prf.nome                AS rp_funcionario_nome
        FROM public.reserva_pessoa rp
        JOIN public.pessoa p ON p.id = rp.fk_pessoa
        LEFT JOIN public.funcionario rf ON rf.id = rp.fk_funcionario
        LEFT JOIN public.pessoa prf ON prf.id = rf.fk_pessoa
        WHERE rp.fk_reserva IN (%s)
        ORDER BY rp.fk_reserva, rp.representante DESC, p.nome ASC
        """)
            .formatted(in);

    Map<Long, List<Reserva.ReservaPessoa>> map = new HashMap<>();
    jdbcTemplate.query(
        sql,
        rs -> {
          Long reservaId = rs.getLong("rp_fk_reserva");
          map.computeIfAbsent(reservaId, k -> new ArrayList<>())
              .add(Reserva.ReservaPessoa.ROW_MAPPER.mapRow(rs, 0));
        },
        reservaIds.toArray());
    return map;
  }

  private Map<Long, List<Reserva.ReservaPagamento>> buscarPagamentosPorReservas(
      List<Long> reservaIds) {
    String in = String.join(",", Collections.nCopies(reservaIds.size(), "?"));
    String sql =
        ("""
        SELECT
          rpag.id                    AS rpag_id,
          rpag.fk_reserva            AS rpag_fk_reserva,
          rpag.data_hora_registro    AS rpag_data_hora_registro,
          rf.id                      AS rpag_funcionario_id,
          prf.nome                   AS rpag_funcionario_nome,
          pag.id                     AS pagamento_id,
          pag.data_hora_registro     AS pagamento_data_hora_registro,
          pag.nome_pagador           AS pagamento_nome_pagador,
          pag.descricao              AS pagamento_descricao,
          pag.valor                  AS pagamento_valor,
          pag.cancelado              AS pagamento_cancelado,
          pag.path_arquivo           AS pagamento_path_arquivo,
          tp.id                      AS tipo_pagamento_id,
          tp.descricao               AS tipo_pagamento_descricao,
          pagf.id                    AS pagamento_funcionario_id,
          pagp.nome                  AS pagamento_funcionario_nome,
          d.id                       AS pagamento_desconto_id,
          d.fk_funcionario           AS pagamento_desconto_funcionario_id,
          desp.nome                  AS pagamento_desconto_funcionario_nome,
          d.porcentagem              AS pagamento_desconto_porcentagem,
          d.valor                    AS pagamento_desconto_valor,
          d.data_hora_registro       AS pagamento_desconto_data_hora_registro,
          mc.id                      AS pagamento_motivo_id,
          mc.motivo_cancelamento     AS pagamento_motivo_cancelamento,
          mcf.id                     AS pagamento_motivo_funcionario_id,
          mcp.nome                   AS pagamento_motivo_funcionario_nome,
          mc.data_hora_registro      AS pagamento_motivo_data_hora_registro
        FROM public.reserva_pagamento rpag
        JOIN public.pagamento pag ON pag.id = rpag.fk_pagamento
        JOIN public.tipo_pagamento tp ON tp.id = pag.fk_tipo_pagamento
        LEFT JOIN public.funcionario rf ON rf.id = rpag.fk_funcionario
        LEFT JOIN public.pessoa prf ON prf.id = rf.fk_pessoa
        LEFT JOIN public.funcionario pagf ON pagf.id = pag.fk_funcionario
        LEFT JOIN public.pessoa pagp ON pagp.id = pagf.fk_pessoa
        LEFT JOIN LATERAL (
          SELECT * FROM public.pagamento_desconto d
          WHERE d.fk_pagamento = pag.id
          ORDER BY d.data_hora_registro DESC LIMIT 1
        ) d ON true
        LEFT JOIN public.funcionario desf ON desf.id = d.fk_funcionario
        LEFT JOIN public.pessoa desp ON desp.id = desf.fk_pessoa
        LEFT JOIN LATERAL (
          SELECT * FROM public.pagamento_motivo_cancelamento mc
          WHERE mc.fk_pagamento = pag.id
          ORDER BY mc.data_hora_registro DESC LIMIT 1
        ) mc ON true
        LEFT JOIN public.funcionario mcf ON mcf.id = mc.fk_funcionario
        LEFT JOIN public.pessoa mcp ON mcp.id = mcf.fk_pessoa
        WHERE rpag.fk_reserva IN (%s)
        ORDER BY rpag.fk_reserva, rpag.data_hora_registro ASC
        """)
            .formatted(in);

    Map<Long, List<Reserva.ReservaPagamento>> map = new HashMap<>();
    jdbcTemplate.query(
        sql,
        rs -> {
          Long reservaId = rs.getLong("rpag_fk_reserva");
          map.computeIfAbsent(reservaId, k -> new ArrayList<>())
              .add(Reserva.ReservaPagamento.ROW_MAPPER.mapRow(rs, 0));
        },
        reservaIds.toArray());
    return map;
  }

  // ── Verificação de conflito ────────────────────────────────────────────────

  public boolean hasConflito(
      Long quartoId, LocalDateTime entrada, LocalDateTime saida, Long excludeReservaId) {
    String sql =
        """
        SELECT COUNT(*) > 0
        FROM public.reserva
        WHERE fk_quarto = ?
          AND cancelado = false
          AND data_hora_entrada < ?
          AND data_hora_saida > ?
        """
            + (excludeReservaId != null ? " AND id != ? " : "");

    if (excludeReservaId != null) {
      return Boolean.TRUE.equals(
          jdbcTemplate.queryForObject(
              sql, Boolean.class, quartoId, saida, entrada, excludeReservaId));
    }
    return Boolean.TRUE.equals(
        jdbcTemplate.queryForObject(sql, Boolean.class, quartoId, saida, entrada));
  }

  // ── Descricao do quarto ────────────────────────────────────────────────────

  public String findQuartoDescricao(Long quartoId) {
    try {
      return jdbcTemplate.queryForObject(
          "SELECT descricao FROM public.quarto WHERE id = ?", String.class, quartoId);
    } catch (EmptyResultDataAccessException e) {
      return null;
    }
  }

  // ── Insert ────────────────────────────────────────────────────────────────

  @Transactional
  public Long insertAndGetId(Long quartoId, LocalDateTime entrada, LocalDateTime saida) {
    return jdbcTemplate.queryForObject(
        """
        INSERT INTO public.reserva (fk_quarto, ativa, hospedado, cancelado,
                                    data_hora_entrada, data_hora_saida, fk_funcionario)
        VALUES (?, true, false, false, ?, ?, ?)
        RETURNING id
        """,
        Long.class,
        quartoId,
        entrada,
        saida,
        getFuncionarioId());
  }

  @Transactional
  public void vincularPessoa(Long reservaId, Long pessoaId, Boolean representante) {
    jdbcTemplate.update(
        """
        INSERT INTO public.reserva_pessoa (fk_reserva, fk_pessoa, fk_funcionario, representante)
        VALUES (?, ?, ?, ?)
        """,
        reservaId,
        pessoaId,
        getFuncionarioId(),
        Boolean.TRUE.equals(representante));
  }

  @Transactional
  public void vincularPagamento(Long reservaId, UUID pagamentoId) {
    jdbcTemplate.update(
        """
        INSERT INTO public.reserva_pagamento (fk_reserva, fk_pagamento, fk_funcionario)
        VALUES (?, ?, ?)
        """,
        reservaId,
        pagamentoId,
        getFuncionarioId());
  }

  // ── Update ────────────────────────────────────────────────────────────────

  @Transactional
  public Reserva update(
      Long reservaId, Long novoQuartoId, LocalDateTime novaEntrada, LocalDateTime novaSaida) {
    int rows =
        jdbcTemplate.update(
            """
            UPDATE public.reserva SET
              fk_quarto         = COALESCE(?, fk_quarto),
              data_hora_entrada = COALESCE(?, data_hora_entrada),
              data_hora_saida   = COALESCE(?, data_hora_saida),
              fk_funcionario    = COALESCE(?, fk_funcionario)
            WHERE id = ?
            """,
            novoQuartoId,
            novaEntrada,
            novaSaida,
            getFuncionarioId(),
            reservaId);

    if (rows == 0) throw new NotFoundException("Reserva não encontrada: " + reservaId);
    return findById(reservaId);
  }

  // ── Cancel ────────────────────────────────────────────────────────────────

  @Transactional
  public void cancelar(Long id) {
    int rows =
        jdbcTemplate.update(
            "UPDATE public.reserva SET cancelado = true, ativa = false WHERE id = ?", id);
    if (rows == 0) throw new NotFoundException("Reserva não encontrada: " + id);
  }

  // ── Helper ────────────────────────────────────────────────────────────────

  private Long getFuncionarioId() {
    return pessoaRepository.getFuncionarioIdFromRequest();
  }
}
