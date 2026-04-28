package saas.hotel.istoepousada.repository;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.Categoria;
import saas.hotel.istoepousada.dto.Reserva;
import saas.hotel.istoepousada.handler.exceptions.BusinessException;
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

  public Map<Long, CategoriaCheckin> findCategoriasCheckinByQuartoIds(List<Long> quartoIds) {
    String in = String.join(",", Collections.nCopies(quartoIds.size(), "?"));
    Map<Long, CategoriaCheckin> map = new HashMap<>();
    jdbcTemplate.query(
        ("""
        SELECT qc.fk_quarto, c.id, c.nome, c.hora_checkin, c.hora_checkout
        FROM public.quarto_categoria qc
        JOIN public.categoria c ON c.id = qc.fk_categoria
        WHERE qc.fk_quarto IN (%s)
        """)
            .formatted(in),
        (RowCallbackHandler)
            rs ->
                map.put(
                    rs.getLong("fk_quarto"),
                    new CategoriaCheckin(
                        rs.getLong("id"),
                        rs.getString("nome"),
                        rs.getObject("hora_checkin", LocalTime.class),
                        rs.getObject("hora_checkout", LocalTime.class))),
        quartoIds.toArray());
    return map;
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
    return findSazonalidades(List.of(categoriaId)).getOrDefault(categoriaId, List.of());
  }

  public Map<Long, List<SazonInfo>> findSazonalidades(List<Long> categoriaIds) {
    if (categoriaIds == null || categoriaIds.isEmpty()) return Map.of();
    String in = String.join(",", Collections.nCopies(categoriaIds.size(), "?"));
    Map<Long, List<SazonInfo>> map = new LinkedHashMap<>();
    jdbcTemplate.query(
        ("""
        SELECT cs.fk_categoria, s.id, s.descricao, s.data_inicio, s.data_fim,
               s.hora_checkin, s.hora_checkout, s.semanal, s.mensal, s.anual
        FROM public.categoria_sazonalidade cs
        JOIN public.sazonalidade s ON s.id = cs.fk_sazonalidade
        WHERE cs.fk_categoria IN (%s) AND cs.ativo = true
        ORDER BY cs.fk_categoria, s.id
        """)
            .formatted(in),
        (RowCallbackHandler)
            rs ->
                map.computeIfAbsent(rs.getLong("fk_categoria"), k -> new ArrayList<>())
                    .add(
                        new SazonInfo(
                            rs.getLong("id"),
                            rs.getString("descricao"),
                            rs.getObject("data_inicio", LocalDate.class),
                            rs.getObject("data_fim", LocalDate.class),
                            rs.getObject("hora_checkin", LocalTime.class),
                            rs.getObject("hora_checkout", LocalTime.class),
                            parseIntArray(rs.getArray("semanal")),
                            parseIntArray(rs.getArray("mensal")),
                            parseIntArray(rs.getArray("anual")))),
        categoriaIds.toArray());
    return map;
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
        r.status                AS reserva_status,
        r.data_hora_entrada     AS reserva_data_hora_entrada,
        r.data_hora_saida       AS reserva_data_hora_saida,
        r.data_hora_registro    AS reserva_data_hora_registro,
        r.valor_total           AS reserva_valor_total,
        r.observacao            AS reserva_observacao,
        o.id                    AS orcamento_id,
        o.nome_solicitante      AS orcamento_nome_solicitante,
        o.data_hora_registro    AS orcamento_data_hora_registro,
        q.id                    AS reserva_quarto_id,
        q.descricao             AS reserva_quarto_descricao,
        c.id                    AS reserva_categoria_id,
        c.nome                  AS reserva_categoria_nome,
        f.id                    AS reserva_funcionario_id,
        pf.nome                 AS reserva_funcionario_nome
      FROM public.reserva r
      JOIN public.quarto q ON q.id = r.fk_quarto
      LEFT JOIN public.orcamento_reserva orv ON orv.fk_reserva = r.id
      LEFT JOIN public.orcamento o ON o.id = orv.fk_orcamento
      LEFT JOIN public.quarto_categoria qc ON qc.fk_quarto = r.fk_quarto
      LEFT JOIN public.categoria c ON c.id = qc.fk_categoria
      LEFT JOIN public.funcionario f ON f.id = r.fk_funcionario
      LEFT JOIN public.pessoa pf ON pf.id = f.fk_pessoa
      """;

  // ── Queries ────────────────────────────────────────────────────────────────

  public List<Reserva> buscarPorMesAno(
      int mes, int ano, Long idFiltro, String nome, List<Reserva.Status> status) {
    List<Reserva.Status> statusFiltro =
        (status == null || status.isEmpty())
            ? List.of(Reserva.Status.HOSPEDADO, Reserva.Status.ATIVO, Reserva.Status.FINALIZADO)
            : status;

    String inStatus =
        String.join(",", Collections.nCopies(statusFiltro.size(), "?::public.status_reserva"));
    StringBuilder where =
        new StringBuilder(
            "WHERE r.status IN ("
                + inStatus
                + ")\n"
                + "  AND EXTRACT(MONTH FROM r.data_hora_entrada) = ?\n"
                + "  AND EXTRACT(YEAR FROM r.data_hora_entrada) = ?\n");

    List<Object> params = new ArrayList<>();
    statusFiltro.forEach(s -> params.add(s.name()));
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

    String sql = SELECT_RESERVA_BASE + where + " ORDER BY r.data_hora_entrada DESC ";
    List<Reserva> bases = jdbcTemplate.query(sql, Reserva.ROW_MAPPER, params.toArray());
    return enriquecer(bases);
  }

  public List<Reserva> buscarPorData(LocalDate data, Long idFiltro, String nome) {
    StringBuilder where =
        new StringBuilder(
            """
            WHERE r.status NOT IN ('CANCELADO')
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

  public List<Reserva> buscarPorFiltro(String nome, List<Reserva.Status> status) {
    StringBuilder where = new StringBuilder("WHERE 1=1\n");
    List<Object> params = new ArrayList<>();

    if (status != null && !status.isEmpty()) {
      String inStatus =
          String.join(",", Collections.nCopies(status.size(), "?::public.status_reserva"));
      where.append("  AND r.status IN (").append(inStatus).append(")\n");
      status.forEach(s -> params.add(s.name()));
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

    String sql = SELECT_RESERVA_BASE + where + " ORDER BY r.data_hora_entrada DESC ";
    List<Reserva> bases = jdbcTemplate.query(sql, Reserva.ROW_MAPPER, params.toArray());
    return enriquecer(bases);
  }

  public List<Reserva> buscarPorQuartoMes(Long quartoId, int mes, int ano) {
    String sql =
        SELECT_RESERVA_BASE
            + """
            WHERE r.fk_quarto = ?
              AND r.status NOT IN ('CANCELADO')
              AND EXTRACT(MONTH FROM r.data_hora_entrada) = ?
              AND EXTRACT(YEAR FROM r.data_hora_entrada) = ?
            ORDER BY r.data_hora_entrada
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
    Map<Long, List<Reserva.OrcamentoPessoa>> orcamentoPessoasMap =
        buscarOrcamentoPessoasPorReservas(ids);
    Map<Long, List<Reserva.ReservaPagamento>> pagamentosMap = buscarPagamentosPorReservas(ids);

    List<Long> canceladosIds =
        bases.stream()
            .filter(r -> r.status() == Reserva.Status.CANCELADO)
            .map(Reserva::id)
            .toList();
    Map<Long, Reserva.MotivoCancelamento> motivosMap =
        canceladosIds.isEmpty() ? Map.of() : buscarMotivosCancelamentoPorReservas(canceladosIds);

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
                    r.status(),
                    r.valor_total(),
                    r.observacao(),
                    r.orcamento_info(),
                    pessoasMap.getOrDefault(r.id(), List.of()),
                    orcamentoPessoasMap.getOrDefault(r.id(), List.of()),
                    pagamentosMap.getOrDefault(r.id(), List.of()),
                    motivosMap.get(r.id())))
        .toList();
  }

  private Map<Long, Reserva.MotivoCancelamento> buscarMotivosCancelamentoPorReservas(
      List<Long> reservaIds) {
    String in = String.join(",", Collections.nCopies(reservaIds.size(), "?"));
    Map<Long, Reserva.MotivoCancelamento> map = new HashMap<>();
    jdbcTemplate.query(
        ("""
        SELECT DISTINCT ON (rmc.fk_reserva)
               rmc.id, rmc.fk_reserva, rmc.motivo_cancelamento, rmc.data_hora_registro,
               f.id AS funcionario_id, p.nome AS funcionario_nome
        FROM public.reserva_motivo_cancelamento rmc
        LEFT JOIN public.funcionario f ON f.id = rmc.fk_funcionario
        LEFT JOIN public.pessoa p ON p.id = f.fk_pessoa
        WHERE rmc.fk_reserva IN (%s)
        ORDER BY rmc.fk_reserva, rmc.data_hora_registro DESC
        """)
            .formatted(in),
        rs -> {
          Long funcId = rs.getObject("funcionario_id", Long.class);
          map.put(
              rs.getLong("fk_reserva"),
              new Reserva.MotivoCancelamento(
                  rs.getLong("id"),
                  rs.getString("motivo_cancelamento"),
                  funcId == null
                      ? null
                      : new saas.hotel.istoepousada.dto.Funcionario.Nome(
                          funcId, rs.getString("funcionario_nome")),
                  rs.getObject("data_hora_registro", java.time.LocalDateTime.class)));
        },
        reservaIds.toArray());
    return map;
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

  public Set<Long> findQuartosComConflito(Map<Long, LocalDateTime[]> quartoEntradaSaida) {
    if (quartoEntradaSaida.isEmpty()) return Set.of();

    List<Object> params = new ArrayList<>();
    List<String> valueParts = new ArrayList<>();
    for (Map.Entry<Long, LocalDateTime[]> entry : quartoEntradaSaida.entrySet()) {
      valueParts.add("(?::bigint, ?::timestamp, ?::timestamp)");
      params.add(entry.getKey());
      params.add(entry.getValue()[0]);
      params.add(entry.getValue()[1]);
    }

    String sql =
        ("""
        SELECT DISTINCT r.fk_quarto
        FROM public.reserva r
        JOIN (VALUES %s) AS v(quarto_id, entrada, saida)
          ON r.fk_quarto = v.quarto_id
        WHERE r.status != 'CANCELADO'
          AND r.data_hora_entrada < v.saida
          AND r.data_hora_saida > v.entrada
        """)
            .formatted(String.join(", ", valueParts));

    Set<Long> result = new HashSet<>();
    jdbcTemplate.query(
        sql, (RowCallbackHandler) rs -> result.add(rs.getLong("fk_quarto")), params.toArray());
    return result;
  }

  public boolean hasConflito(
      Long quartoId, LocalDateTime entrada, LocalDateTime saida, Long excludeReservaId) {
    String sql =
        """
        SELECT COUNT(*) > 0
        FROM public.reserva
        WHERE fk_quarto = ?
          AND status != 'CANCELADO'
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

  public Map<Long, String> findQuartosDescricao(List<Long> quartoIds) {
    String in = String.join(",", Collections.nCopies(quartoIds.size(), "?"));
    Map<Long, String> map = new HashMap<>();
    jdbcTemplate.query(
        "SELECT id, descricao FROM public.quarto WHERE id IN (%s)".formatted(in),
        (RowCallbackHandler) rs -> map.put(rs.getLong("id"), rs.getString("descricao")),
        quartoIds.toArray());
    return map;
  }

  // ── Insert ────────────────────────────────────────────────────────────────

  @Transactional
  public Long inserirHospedado(
      Long quartoId, LocalDateTime entrada, LocalDateTime saida, double valorTotal) {
    return jdbcTemplate.queryForObject(
        """
        INSERT INTO public.reserva (fk_quarto, status,
                                    data_hora_entrada, data_hora_saida, fk_funcionario,
                                    valor_total)
        VALUES (?, 'HOSPEDADO'::public.status_reserva, ?, ?, ?, ?)
        RETURNING id
        """,
        Long.class,
        quartoId,
        entrada,
        saida,
        getFuncionarioId(),
        valorTotal);
  }

  @Transactional
  public Long insertAndGetId(
      Long quartoId,
      LocalDateTime entrada,
      LocalDateTime saida,
      double valorTotal,
      boolean orcamento,
      String observacao) {
    String status = orcamento ? "ORCAMENTO" : "ATIVO";
    return jdbcTemplate.queryForObject(
        """
        INSERT INTO public.reserva (fk_quarto, status,
                                    data_hora_entrada, data_hora_saida, fk_funcionario,
                                    valor_total, observacao)
        VALUES (?, ?::public.status_reserva, ?, ?, ?, ?, ?)
        RETURNING id
        """,
        Long.class,
        quartoId,
        status,
        entrada,
        saida,
        getFuncionarioId(),
        valorTotal,
        observacao);
  }

  @Transactional
  public Long insertOrcamento(String nomeSolicitante, Long funcionarioId) {
    return jdbcTemplate.queryForObject(
        """
        INSERT INTO public.orcamento (nome_solicitante, fk_funcionario)
        VALUES (?, ?) RETURNING id
        """,
        Long.class,
        nomeSolicitante,
        funcionarioId);
  }

  @Transactional
  public void insertOrcamentoPessoa(Long reservaId, String nome, LocalDate dataNascimento) {
    jdbcTemplate.update(
        "INSERT INTO public.orcamento_reserva_pessoa (fk_reserva, nome, data_nascimento) VALUES (?, ?, ?)",
        reservaId,
        nome,
        dataNascimento);
  }

  private Map<Long, List<Reserva.OrcamentoPessoa>> buscarOrcamentoPessoasPorReservas(
      List<Long> reservaIds) {
    String in = String.join(",", Collections.nCopies(reservaIds.size(), "?"));
    List<Map<String, Object>> rows =
        jdbcTemplate.queryForList(
            "SELECT id, fk_reserva, nome, data_nascimento FROM public.orcamento_reserva_pessoa WHERE fk_reserva IN ("
                + in
                + ")",
            reservaIds.toArray());
    Map<Long, List<Reserva.OrcamentoPessoa>> map = new HashMap<>();
    for (Map<String, Object> row : rows) {
      Long resId = ((Number) row.get("fk_reserva")).longValue();
      map.computeIfAbsent(resId, k -> new ArrayList<>())
          .add(
              new Reserva.OrcamentoPessoa(
                  ((Number) row.get("id")).longValue(),
                  (String) row.get("nome"),
                  ((java.sql.Date) row.get("data_nascimento")).toLocalDate()));
    }
    return map;
  }

  @Transactional
  public void vincularOrcamentoReserva(Long orcamentoId, Long reservaId) {
    jdbcTemplate.update(
        "INSERT INTO public.orcamento_reserva (fk_orcamento, fk_reserva) VALUES (?, ?)",
        orcamentoId,
        reservaId);
  }

  public List<Reserva> findByOrcamentoId(Long orcamentoId) {
    String sql =
        SELECT_RESERVA_BASE
            + " WHERE orv.fk_orcamento = ? AND r.status = 'ORCAMENTO'::public.status_reserva ORDER BY r.data_hora_entrada ASC ";
    List<Reserva> bases = jdbcTemplate.query(sql, Reserva.ROW_MAPPER, orcamentoId);
    return enriquecer(bases);
  }

  public Reserva.OrcamentoDetalhe findOrcamentoDetalhe(Long orcamentoId) {
    Reserva.OrcamentoDetalhe info;
    try {
      info =
          jdbcTemplate.queryForObject(
              """
          SELECT o.id, o.nome_solicitante, o.data_hora_registro,
                 f.id AS funcionario_id, p.nome AS funcionario_nome
          FROM public.orcamento o
          LEFT JOIN public.funcionario f ON f.id = o.fk_funcionario
          LEFT JOIN public.pessoa p ON p.id = f.fk_pessoa
          WHERE o.id = ?
          """,
              (rs, rowNum) -> {
                Long funcId = rs.getObject("funcionario_id", Long.class);
                return new Reserva.OrcamentoDetalhe(
                    rs.getLong("id"),
                    rs.getString("nome_solicitante"),
                    funcId == null
                        ? null
                        : new saas.hotel.istoepousada.dto.Funcionario.Nome(
                            funcId, rs.getString("funcionario_nome")),
                    rs.getObject("data_hora_registro", java.time.LocalDateTime.class),
                    null);
              },
              orcamentoId);
    } catch (EmptyResultDataAccessException e) {
      throw new NotFoundException("Orçamento não encontrado: " + orcamentoId);
    }
    List<Reserva> reservas = findByOrcamentoId(orcamentoId);
    return new Reserva.OrcamentoDetalhe(
        info.id(),
        info.nome_solicitante(),
        info.funcionario(),
        info.data_hora_registro(),
        reservas);
  }

  @Transactional
  public void atualizarStatus(List<Long> ids, Reserva.Status status) {
    String inClause = ids.stream().map(id -> "?").collect(java.util.stream.Collectors.joining(","));
    List<Object> params = new ArrayList<>();
    params.add(status.name());
    params.addAll(ids);
    jdbcTemplate.update(
        "UPDATE public.reserva SET status = ?::status_reserva WHERE id IN (" + inClause + ")",
        params.toArray());
  }

  @Transactional
  public void desvincularPessoa(Long reservaId, Long pessoaId) {
    jdbcTemplate.update(
        "DELETE FROM public.reserva_pessoa WHERE fk_reserva = ? AND fk_pessoa = ?",
        reservaId,
        pessoaId);
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
      Long reservaId,
      Long novoQuartoId,
      LocalDateTime novaEntrada,
      LocalDateTime novaSaida,
      Double valorTotal,
      String observacao) {
    int rows =
        jdbcTemplate.update(
            """
            UPDATE public.reserva SET
              fk_quarto         = COALESCE(?, fk_quarto),
              data_hora_entrada = COALESCE(?, data_hora_entrada),
              data_hora_saida   = COALESCE(?, data_hora_saida),
              fk_funcionario    = COALESCE(?, fk_funcionario),
              valor_total       = COALESCE(?, valor_total),
              observacao        = COALESCE(?, observacao)
            WHERE id = ?
            """,
            novoQuartoId,
            novaEntrada,
            novaSaida,
            getFuncionarioId(),
            valorTotal,
            observacao,
            reservaId);

    if (rows == 0) throw new NotFoundException("Reserva não encontrada: " + reservaId);
    return findById(reservaId);
  }

  // ── Cancel ────────────────────────────────────────────────────────────────

  @Transactional
  public void cancelar(Long id) {
    int rows =
        jdbcTemplate.update(
            "UPDATE public.reserva SET status = 'CANCELADO'::public.status_reserva WHERE id = ?",
            id);
    if (rows == 0) throw new NotFoundException("Reserva não encontrada: " + id);
  }

  @Transactional
  public void cancelarComMotivo(Long id, String motivo) {
    int rows =
        jdbcTemplate.update(
            "UPDATE public.reserva SET status = 'CANCELADO'::public.status_reserva WHERE id = ?",
            id);
    if (rows == 0) throw new NotFoundException("Reserva não encontrada: " + id);
    jdbcTemplate.update(
        """
        INSERT INTO public.reserva_motivo_cancelamento
          (fk_reserva, fk_funcionario, motivo_cancelamento, data_hora_registro)
        VALUES (?, ?, ?, NOW())
        """,
        id,
        getFuncionarioId(),
        motivo);
  }

  public Reserva.MotivoCancelamento findMotivoCancelamento(Long reservaId) {
    try {
      return jdbcTemplate.queryForObject(
          """
          SELECT rmc.id, rmc.motivo_cancelamento, rmc.data_hora_registro,
                 f.id AS funcionario_id, p.nome AS funcionario_nome
          FROM public.reserva_motivo_cancelamento rmc
          LEFT JOIN public.funcionario f ON f.id = rmc.fk_funcionario
          LEFT JOIN public.pessoa p ON p.id = f.fk_pessoa
          WHERE rmc.fk_reserva = ?
          ORDER BY rmc.data_hora_registro DESC
          LIMIT 1
          """,
          (rs, rowNum) -> {
            Long funcId = rs.getObject("funcionario_id", Long.class);
            return new Reserva.MotivoCancelamento(
                rs.getLong("id"),
                rs.getString("motivo_cancelamento"),
                funcId == null
                    ? null
                    : new saas.hotel.istoepousada.dto.Funcionario.Nome(
                        funcId, rs.getString("funcionario_nome")),
                rs.getObject("data_hora_registro", java.time.LocalDateTime.class));
          },
          reservaId);
    } catch (org.springframework.dao.EmptyResultDataAccessException e) {
      return null;
    }
  }

  @Transactional
  public Reserva ativar(Long id) {
    int rows =
        jdbcTemplate.update(
            "UPDATE public.reserva SET status = 'ATIVO'::public.status_reserva WHERE id = ? AND status = 'ORCAMENTO'::public.status_reserva",
            id);
    if (rows == 0)
      throw new BusinessException(
          "Reserva não encontrada ou não está em status de orçamento: " + id);
    return findById(id);
  }

  // ── Day Use próprio da sazonalidade (fk_categoria IS NULL) ───────────────

  public Map<Long, Categoria.DayUseOperacao> findSazonDayUse(List<Long> sazonIds) {
    if (sazonIds == null || sazonIds.isEmpty()) return Map.of();
    String in = String.join(",", Collections.nCopies(sazonIds.size(), "?"));

    // Step 1: operacoes
    Map<Long, Long> operacaoSazonMap = new LinkedHashMap<>();
    Map<Long, Boolean> operacaoAtivoMap = new LinkedHashMap<>();
    jdbcTemplate.query(
        ("""
        SELECT duo.id AS duo_id, duo.fk_sazonalidade AS duo_fk_sazonalidade, duo.ativo AS duo_ativo
        FROM public.day_use_modelo_operacao duo
        WHERE duo.fk_sazonalidade IN (%s) AND duo.fk_categoria IS NULL
        ORDER BY duo.fk_sazonalidade, duo.id
        """)
            .formatted(in),
        rs -> {
          operacaoSazonMap.put(rs.getLong("duo_id"), rs.getLong("duo_fk_sazonalidade"));
          operacaoAtivoMap.put(rs.getLong("duo_id"), rs.getBoolean("duo_ativo"));
        },
        sazonIds.toArray());

    if (operacaoSazonMap.isEmpty()) return Map.of();

    Object[] opIds = operacaoSazonMap.keySet().toArray();
    String inOp = String.join(",", Collections.nCopies(opIds.length, "?"));

    // Step 2: padrao
    Map<Long, Categoria.DayUsePadrao> padraoMap = new LinkedHashMap<>();
    jdbcTemplate.query(
        ("""
        SELECT dup.fk_day_use_modelo_operacao AS dup_fk_operacao,
               dup.id AS dup_id, dup.preco_base AS dup_preco_base,
               dup.hora_preco_base AS dup_hora_preco_base,
               dup.valor_hora_adicional AS dup_valor_hora_adicional
        FROM public.day_use_modelo_padrao dup
        WHERE dup.fk_day_use_modelo_operacao IN (%s)
        """)
            .formatted(inOp),
        rs -> {
          padraoMap.put(
              rs.getLong("dup_fk_operacao"), Categoria.DayUsePadrao.ROW_MAPPER.mapRow(rs, 0));
        },
        opIds);

    // Step 3: ocupacoes + pessoas (single query with LEFT JOIN)
    Map<Long, Categoria.DayUseOcupacao> ocupacaoById = new LinkedHashMap<>();
    Map<Long, Long> ocupacaoOperacaoMap = new LinkedHashMap<>();
    jdbcTemplate.query(
        ("""
        SELECT duo.id AS duo_id, duo.fk_day_use_modelo_operacao AS duo_fk_operacao,
               duo.quantidade_pessoa AS duo_quantidade_pessoa,
               duop.id AS duop_id, duop.quantidade AS duop_quantidade,
               duop.valor AS duop_valor,
               duop.valor_hora_adicional_por_pessoa AS duop_valor_hora_adicional_por_pessoa
        FROM public.day_use_modelo_ocupacao duo
        LEFT JOIN public.day_use_modelo_ocupacao_quantidade_pessoa duop
               ON duop.fk_day_use_modelo_ocupacao = duo.id
        WHERE duo.fk_day_use_modelo_operacao IN (%s)
        ORDER BY duo.id, duop.quantidade ASC
        """)
            .formatted(inOp),
        rs -> {
          Long ocId = rs.getLong("duo_id");
          if (!ocupacaoById.containsKey(ocId)) {
            ocupacaoById.put(
                ocId,
                new Categoria.DayUseOcupacao(
                    ocId, rs.getInt("duo_quantidade_pessoa"), new ArrayList<>()));
            ocupacaoOperacaoMap.put(ocId, rs.getLong("duo_fk_operacao"));
          }
          Long pessoaId = rs.getObject("duop_id", Long.class);
          if (pessoaId != null) {
            ocupacaoById
                .get(ocId)
                .quantidades()
                .add(
                    new Categoria.DayUseOcupacaoPessoa(
                        pessoaId,
                        rs.getInt("duop_quantidade"),
                        rs.getInt("duop_valor"),
                        rs.getObject("duop_valor_hora_adicional_por_pessoa", Integer.class)));
          }
        },
        opIds);

    // Grupo de ocupacoes por operacao
    Map<Long, List<Categoria.DayUseOcupacao>> ocupacoesPorOperacao = new LinkedHashMap<>();
    ocupacaoOperacaoMap.forEach(
        (ocId, opId) ->
            ocupacoesPorOperacao
                .computeIfAbsent(opId, k -> new ArrayList<>())
                .add(ocupacaoById.get(ocId)));

    // Monta resultado: sazonId -> DayUseOperacao
    Map<Long, Categoria.DayUseOperacao> result = new LinkedHashMap<>();
    operacaoSazonMap.forEach(
        (opId, sazonId) ->
            result.putIfAbsent(
                sazonId,
                new Categoria.DayUseOperacao(
                    opId,
                    null,
                    operacaoAtivoMap.get(opId),
                    padraoMap.get(opId),
                    ocupacoesPorOperacao.getOrDefault(opId, List.of()))));

    return result;
  }

  // ── Modelos de preço próprios da sazonalidade (fk_categoria IS NULL) ──────

  public Map<Long, List<Categoria.ModeloOcupacao>> findSazonModelosOcupacao(List<Long> sazonIds) {
    if (sazonIds == null || sazonIds.isEmpty()) return Map.of();
    String in = String.join(",", Collections.nCopies(sazonIds.size(), "?"));
    Map<Long, List<Categoria.ModeloOcupacao>> map = new HashMap<>();
    jdbcTemplate.query(
        ("""
        SELECT mo.id AS mo_id, mo.fk_sazonalidade AS mo_fk_sazonalidade,
               mo.quantidade AS mo_quantidade, mo.valor AS mo_valor
        FROM public.modelo_ocupacao mo
        WHERE mo.fk_sazonalidade IN (%s) AND mo.fk_categoria IS NULL
        ORDER BY mo.fk_sazonalidade, mo.id
        """)
            .formatted(in),
        rs -> {
          Long sid = rs.getLong("mo_fk_sazonalidade");
          map.computeIfAbsent(sid, k -> new ArrayList<>())
              .add(
                  new Categoria.ModeloOcupacao(
                      rs.getLong("mo_id"),
                      null,
                      rs.getInt("mo_quantidade"),
                      rs.getDouble("mo_valor")));
        },
        sazonIds.toArray());
    return map;
  }

  public Map<Long, List<Categoria.ModeloFixo>> findSazonModelosFixo(List<Long> sazonIds) {
    if (sazonIds == null || sazonIds.isEmpty()) return Map.of();
    String in = String.join(",", Collections.nCopies(sazonIds.size(), "?"));
    Map<Long, List<Categoria.ModeloFixo>> map = new HashMap<>();
    jdbcTemplate.query(
        ("""
        SELECT mf.id AS mf_id, mf.fk_sazonalidade AS mf_fk_sazonalidade, mf.valor AS mf_valor
        FROM public.modelo_fixo mf
        WHERE mf.fk_sazonalidade IN (%s) AND mf.fk_categoria IS NULL
        ORDER BY mf.fk_sazonalidade, mf.id
        """)
            .formatted(in),
        rs -> {
          Long sid = rs.getLong("mf_fk_sazonalidade");
          map.computeIfAbsent(sid, k -> new ArrayList<>())
              .add(new Categoria.ModeloFixo(rs.getLong("mf_id"), null, rs.getDouble("mf_valor")));
        },
        sazonIds.toArray());
    return map;
  }

  // ── Helper ────────────────────────────────────────────────────────────────

  private Long getFuncionarioId() {
    return pessoaRepository.getFuncionarioIdFromRequest();
  }
}
