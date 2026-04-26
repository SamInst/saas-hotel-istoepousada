package saas.hotel.istoepousada.repository;

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
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.Funcionario;
import saas.hotel.istoepousada.dto.Item;
import saas.hotel.istoepousada.dto.Pagamento;
import saas.hotel.istoepousada.dto.Pernoite;
import saas.hotel.istoepousada.dto.Pessoa;
import saas.hotel.istoepousada.dto.Quarto;
import saas.hotel.istoepousada.handler.exceptions.BusinessException;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;

@Repository
public class PernoiteRepository {

  private final JdbcTemplate jdbcTemplate;
  private final PessoaRepository pessoaRepository;
  private final PagamentoRepository pagamentoRepository;

  public PernoiteRepository(
      JdbcTemplate jdbcTemplate,
      PessoaRepository pessoaRepository,
      PagamentoRepository pagamentoRepository) {
    this.jdbcTemplate = jdbcTemplate;
    this.pessoaRepository = pessoaRepository;
    this.pagamentoRepository = pagamentoRepository;
  }

  // ── SELECT base ─────────────────────────────────────────────────────────────

  private static final String SELECT_PERNOITE_BASE =
      """
      SELECT
        p.id                    AS pernoite_id,
        p.status                AS pernoite_status,
        p.data_hora_registro    AS pernoite_data_hora_registro,
        p.data_hora_entrada     AS pernoite_data_hora_entrada,
        p.data_hora_saida       AS pernoite_data_hora_saida,
        p.hora_chegada          AS pernoite_hora_chegada,
        p.hora_saida            AS pernoite_hora_saida,
        p.valor_total           AS pernoite_valor_total,
        q.id                    AS pernoite_quarto_id,
        q.descricao             AS pernoite_quarto_descricao,
        f.id                    AS pernoite_funcionario_id,
        pf.nome                 AS pernoite_funcionario_nome
      FROM public.pernoite p
      JOIN public.quarto q ON q.id = p.fk_quarto
      LEFT JOIN public.funcionario f ON f.id = p.fk_funcionario
      LEFT JOIN public.pessoa pf ON pf.id = f.fk_pessoa
      """;

  private static final String SELECT_DIARIA_BASE =
      """
      SELECT
        d.id                    AS diaria_id,
        d.numero                AS diaria_numero,
        d.valor                 AS diaria_valor,
        d.status                AS diaria_status,
        d.data_hora_inicio      AS diaria_data_hora_inicio,
        d.data_hora_fim         AS diaria_data_hora_fim,
        d.observacao            AS diaria_observacao,
        d.fk_pernoite           AS diaria_fk_pernoite,
        dq.id                   AS diaria_quarto_id,
        dq.descricao            AS diaria_quarto_descricao
      FROM public.diaria d
      LEFT JOIN public.quarto dq ON dq.id = d.fk_quarto
      """;

  private static final String SELECT_CONSUMO_POR_DIARIA =
      """
      SELECT
        dc.fk_diaria            AS consumo_fk_diaria,
        c.id                    AS consumo_id,
        c.data_hora_registro    AS consumo_data_hora_registro,
        c.quantidade            AS consumo_quantidade,
        c.despesa_pessoal       AS consumo_despesa_pessoal,
        c.cancelado             AS consumo_cancelado,
        i.id                    AS consumo_item_id,
        i.descricao             AS consumo_item_descricao,
        cf.id                   AS consumo_funcionario_id,
        cpf.nome                AS consumo_funcionario_nome,
        cq.id                   AS consumo_quarto_id,
        cq.descricao            AS consumo_quarto_descricao,
        p.id                    AS pagamento_id,
        p.data_hora_registro    AS pagamento_data_hora_registro,
        p.nome_pagador          AS pagamento_nome_pagador,
        p.descricao             AS pagamento_descricao,
        p.valor                 AS pagamento_valor,
        p.cancelado             AS pagamento_cancelado,
        p.path_arquivo          AS pagamento_path_arquivo,
        tp.id                   AS tipo_pagamento_id,
        tp.descricao            AS tipo_pagamento_descricao,
        pagf.id                 AS pagamento_funcionario_id,
        pagp.nome               AS pagamento_funcionario_nome,
        pd.id                   AS pagamento_desconto_id,
        pd.fk_funcionario       AS pagamento_desconto_funcionario_id,
        desp.nome               AS pagamento_desconto_funcionario_nome,
        pd.porcentagem          AS pagamento_desconto_porcentagem,
        pd.valor                AS pagamento_desconto_valor,
        pd.data_hora_registro   AS pagamento_desconto_data_hora_registro,
        mc.id                   AS pagamento_motivo_id,
        mc.motivo_cancelamento  AS pagamento_motivo_cancelamento,
        mcf.id                  AS pagamento_motivo_funcionario_id,
        mcp.nome                AS pagamento_motivo_funcionario_nome,
        mc.data_hora_registro   AS pagamento_motivo_data_hora_registro
      FROM public.diaria_consumo dc
      JOIN public.consumo c ON c.id = dc.fk_consumo
      JOIN public.item i ON i.id = c.fk_item
      LEFT JOIN public.funcionario cf ON cf.id = c.fk_funcionario
      LEFT JOIN public.pessoa cpf ON cpf.id = cf.fk_pessoa
      LEFT JOIN public.quarto cq ON cq.id = c.fk_quarto
      LEFT JOIN public.pagamento p ON p.id = c.fk_pagamento
      LEFT JOIN public.tipo_pagamento tp ON tp.id = p.fk_tipo_pagamento
      LEFT JOIN public.funcionario pagf ON pagf.id = p.fk_funcionario
      LEFT JOIN public.pessoa pagp ON pagp.id = pagf.fk_pessoa
      LEFT JOIN LATERAL (
        SELECT * FROM public.pagamento_desconto pd
        WHERE pd.fk_pagamento = p.id
        ORDER BY pd.data_hora_registro DESC LIMIT 1
      ) pd ON true
      LEFT JOIN public.funcionario desf ON desf.id = pd.fk_funcionario
      LEFT JOIN public.pessoa desp ON desp.id = desf.fk_pessoa
      LEFT JOIN LATERAL (
        SELECT * FROM public.pagamento_motivo_cancelamento mc
        WHERE mc.fk_pagamento = p.id
        ORDER BY mc.data_hora_registro DESC LIMIT 1
      ) mc ON true
      LEFT JOIN public.funcionario mcf ON mcf.id = mc.fk_funcionario
      LEFT JOIN public.pessoa mcp ON mcp.id = mcf.fk_pessoa
      """;

  private static final String SELECT_PAGAMENTO_POR_DIARIA =
      """
      SELECT
        dp.id                   AS dp_id,
        dp.fk_diaria            AS dp_fk_diaria,
        p.id                    AS pagamento_id,
        p.data_hora_registro    AS pagamento_data_hora_registro,
        p.nome_pagador          AS pagamento_nome_pagador,
        p.descricao             AS pagamento_descricao,
        p.valor                 AS pagamento_valor,
        p.cancelado             AS pagamento_cancelado,
        p.path_arquivo          AS pagamento_path_arquivo,
        tp.id                   AS tipo_pagamento_id,
        tp.descricao            AS tipo_pagamento_descricao,
        pagf.id                 AS pagamento_funcionario_id,
        pagp.nome               AS pagamento_funcionario_nome,
        pd.id                   AS pagamento_desconto_id,
        pd.fk_funcionario       AS pagamento_desconto_funcionario_id,
        desp.nome               AS pagamento_desconto_funcionario_nome,
        pd.porcentagem          AS pagamento_desconto_porcentagem,
        pd.valor                AS pagamento_desconto_valor,
        pd.data_hora_registro   AS pagamento_desconto_data_hora_registro,
        mc.id                   AS pagamento_motivo_id,
        mc.motivo_cancelamento  AS pagamento_motivo_cancelamento,
        mcf.id                  AS pagamento_motivo_funcionario_id,
        mcp.nome                AS pagamento_motivo_funcionario_nome,
        mc.data_hora_registro   AS pagamento_motivo_data_hora_registro
      FROM public.diaria_pagamento dp
      JOIN public.pagamento p ON p.id = dp.fk_pagamento
      JOIN public.tipo_pagamento tp ON tp.id = p.fk_tipo_pagamento
      LEFT JOIN public.funcionario pagf ON pagf.id = p.fk_funcionario
      LEFT JOIN public.pessoa pagp ON pagp.id = pagf.fk_pessoa
      LEFT JOIN LATERAL (
        SELECT * FROM public.pagamento_desconto pd
        WHERE pd.fk_pagamento = p.id
        ORDER BY pd.data_hora_registro DESC LIMIT 1
      ) pd ON true
      LEFT JOIN public.funcionario desf ON desf.id = pd.fk_funcionario
      LEFT JOIN public.pessoa desp ON desp.id = desf.fk_pessoa
      LEFT JOIN LATERAL (
        SELECT * FROM public.pagamento_motivo_cancelamento mc
        WHERE mc.fk_pagamento = p.id
        ORDER BY mc.data_hora_registro DESC LIMIT 1
      ) mc ON true
      LEFT JOIN public.funcionario mcf ON mcf.id = mc.fk_funcionario
      LEFT JOIN public.pessoa mcp ON mcp.id = mcf.fk_pessoa
      """;

  // ── findById ────────────────────────────────────────────────────────────────

  public Pernoite findById(Long id) {
    String sql = SELECT_PERNOITE_BASE + " WHERE p.id = ? ";
    Pernoite base;
    try {
      base =
          jdbcTemplate.queryForObject(
              sql,
              (rs, rowNum) -> {
                Long funcId = rs.getObject("pernoite_funcionario_id", Long.class);
                return new Pernoite(
                    rs.getLong("pernoite_id"),
                    funcId == null
                        ? null
                        : new Funcionario.Nome(funcId, rs.getString("pernoite_funcionario_nome")),
                    rs.getObject("pernoite_data_hora_registro", LocalDateTime.class),
                    rs.getObject("pernoite_data_hora_entrada", LocalDateTime.class),
                    rs.getObject("pernoite_data_hora_saida", LocalDateTime.class),
                    rs.getObject("pernoite_hora_chegada", LocalTime.class),
                    rs.getObject("pernoite_hora_saida", LocalTime.class),
                    Pernoite.Status.valueOf(rs.getString("pernoite_status")),
                    rs.getObject("pernoite_valor_total", Float.class),
                    0,
                    0,
                    List.of(),
                    List.of());
              },
              id);
    } catch (EmptyResultDataAccessException e) {
      throw new NotFoundException("Pernoite não encontrado: " + id);
    }
    return enriquecer(List.of(base)).getFirst();
  }

  public Pernoite.Diaria findDiariaById(Long diariaId) {
    String sql = SELECT_DIARIA_BASE + " WHERE d.id = ? ";
    Pernoite.Diaria base;
    try {
      base =
          jdbcTemplate.queryForObject(
              sql,
              (rs, rowNum) -> mapDiariaBase(rs),
              diariaId);
    } catch (EmptyResultDataAccessException e) {
      throw new NotFoundException("Diária não encontrada: " + diariaId);
    }
    List<Pernoite.Diaria> enriched = enriquecerDiarias(List.of(base));
    return enriched.getFirst();
  }

  // ── Enriquecimento ──────────────────────────────────────────────────────────

  private List<Pernoite> enriquecer(List<Pernoite> bases) {
    if (bases.isEmpty()) return bases;
    List<Long> ids = bases.stream().map(Pernoite::id).toList();

    Map<Long, List<Pernoite.Diaria>> diariasMap = buscarDiariasPorPernoites(ids);
    Map<Long, List<Pernoite.PernoitePessoa>> pessoasMap = buscarPessoasPorPernoites(ids);

    return bases.stream()
        .map(
            p -> {
              List<Pernoite.Diaria> diarias = diariasMap.getOrDefault(p.id(), List.of());
              int qtd = diarias.size();
              int atual =
                  diarias.stream()
                      .filter(d -> d.status() == Pernoite.Diaria.Status.ATIVO)
                      .mapToInt(Pernoite.Diaria::numero)
                      .max()
                      .orElse(qtd);
              return new Pernoite(
                  p.id(),
                  p.funcionario(),
                  p.data_hora_registro(),
                  p.check_in(),
                  p.check_out(),
                  p.hora_chegada(),
                  p.hora_saida(),
                  p.status(),
                  p.valor_total(),
                  qtd,
                  atual,
                  diarias,
                  pessoasMap.getOrDefault(p.id(), List.of()));
            })
        .toList();
  }

  private Map<Long, List<Pernoite.Diaria>> buscarDiariasPorPernoites(List<Long> pernoiteIds) {
    String in = String.join(",", Collections.nCopies(pernoiteIds.size(), "?"));
    String sql = SELECT_DIARIA_BASE + " WHERE d.fk_pernoite IN (" + in + ") ORDER BY d.fk_pernoite, d.numero ASC ";

    Map<Long, List<Pernoite.Diaria>> basesMap = new HashMap<>();
    jdbcTemplate.query(
        sql,
        rs -> {
          Long pid = rs.getLong("diaria_fk_pernoite");
          basesMap.computeIfAbsent(pid, k -> new ArrayList<>()).add(mapDiariaBase(rs));
        },
        pernoiteIds.toArray());

    if (basesMap.isEmpty()) return basesMap;

    List<Pernoite.Diaria> todasDiarias = basesMap.values().stream().flatMap(List::stream).toList();
    List<Pernoite.Diaria> enriquecidas = enriquecerDiarias(todasDiarias);

    Map<Long, Pernoite.Diaria> enriquecidaById = new HashMap<>();
    for (Pernoite.Diaria d : enriquecidas) {
      enriquecidaById.put(d.id(), d);
    }

    Map<Long, List<Pernoite.Diaria>> result = new HashMap<>();
    basesMap.forEach(
        (pid, list) ->
            result.put(
                pid,
                list.stream()
                    .map(d -> enriquecidaById.getOrDefault(d.id(), d))
                    .toList()));
    return result;
  }

  private List<Pernoite.Diaria> enriquecerDiarias(List<Pernoite.Diaria> bases) {
    if (bases.isEmpty()) return bases;
    List<Long> diariaIds = bases.stream().map(Pernoite.Diaria::id).toList();

    Map<Long, List<Pessoa.DadosPrincipais>> pessoasMap = buscarPessoasPorDiarias(diariaIds);
    Map<Long, List<Item.Consumo>> consumosMap = buscarConsumosPorDiarias(diariaIds);
    Map<Long, List<Pagamento>> pagamentosMap = buscarPagamentosPorDiarias(diariaIds);

    return bases.stream()
        .map(
            d ->
                new Pernoite.Diaria(
                    d.id(),
                    d.numero(),
                    d.quarto(),
                    d.data_hora_inicio(),
                    d.data_hora_fim(),
                    d.valor(),
                    d.status(),
                    d.observacao(),
                    pessoasMap.getOrDefault(d.id(), List.of()),
                    consumosMap.getOrDefault(d.id(), List.of()),
                    pagamentosMap.getOrDefault(d.id(), List.of())))
        .toList();
  }

  private Pernoite.Diaria mapDiariaBase(java.sql.ResultSet rs) {
    try {
      Long quartoId = rs.getObject("diaria_quarto_id", Long.class);
      String statusStr = rs.getString("diaria_status");
      return new Pernoite.Diaria(
          rs.getLong("diaria_id"),
          rs.getInt("diaria_numero"),
          quartoId == null ? null : new Quarto.Descricao(quartoId, rs.getString("diaria_quarto_descricao")),
          rs.getObject("diaria_data_hora_inicio", LocalDateTime.class),
          rs.getObject("diaria_data_hora_fim", LocalDateTime.class),
          rs.getFloat("diaria_valor"),
          statusStr != null ? Pernoite.Diaria.Status.valueOf(statusStr) : Pernoite.Diaria.Status.ATIVO,
          rs.getString("diaria_observacao"),
          null,
          null,
          null);
    } catch (java.sql.SQLException e) {
      throw new RuntimeException("Erro ao mapear diária", e);
    }
  }

  private Map<Long, List<Pessoa.DadosPrincipais>> buscarPessoasPorDiarias(List<Long> diariaIds) {
    String in = String.join(",", Collections.nCopies(diariaIds.size(), "?"));
    String sql =
        """
        SELECT
          dp.fk_diaria            AS diaria_id,
          p.id                    AS pessoa_id,
          p.nome                  AS pessoa_nome,
          p.data_nascimento       AS pessoa_data_nascimento,
          p.cpf                   AS pessoa_cpf,
          p.email                 AS pessoa_email,
          p.telefone              AS pessoa_telefone,
          p.status                AS pessoa_status,
          false                   AS pessoa_titular
        FROM public.diaria_pessoa dp
        JOIN public.pessoa p ON p.id = dp.fk_pessoa
        WHERE dp.fk_diaria IN (%s)
        ORDER BY dp.fk_diaria, p.nome ASC
        """.formatted(in);

    Map<Long, List<Pessoa.DadosPrincipais>> map = new HashMap<>();
    jdbcTemplate.query(
        sql,
        rs -> {
          Long diariaId = rs.getLong("diaria_id");
          map.computeIfAbsent(diariaId, k -> new ArrayList<>())
              .add(Pessoa.DadosPrincipais.ROW_MAPPER.mapRow(rs, 0));
        },
        diariaIds.toArray());
    return map;
  }

  private Map<Long, List<Item.Consumo>> buscarConsumosPorDiarias(List<Long> diariaIds) {
    String in = String.join(",", Collections.nCopies(diariaIds.size(), "?"));
    Map<Long, List<Item.Consumo>> map = new HashMap<>();
    jdbcTemplate.query(
        SELECT_CONSUMO_POR_DIARIA + " WHERE dc.fk_diaria IN (" + in + ") ORDER BY dc.fk_diaria, c.data_hora_registro ASC ",
        rs -> {
          Long diariaId = rs.getLong("consumo_fk_diaria");
          map.computeIfAbsent(diariaId, k -> new ArrayList<>())
              .add(Item.Consumo.ROW_MAPPER.mapRow(rs, 0));
        },
        diariaIds.toArray());
    return map;
  }

  private Map<Long, List<Pagamento>> buscarPagamentosPorDiarias(List<Long> diariaIds) {
    String in = String.join(",", Collections.nCopies(diariaIds.size(), "?"));
    Map<Long, List<Pagamento>> map = new HashMap<>();
    jdbcTemplate.query(
        SELECT_PAGAMENTO_POR_DIARIA + " WHERE dp.fk_diaria IN (" + in + ") ORDER BY dp.fk_diaria, p.data_hora_registro ASC ",
        rs -> {
          Long diariaId = rs.getLong("dp_fk_diaria");
          map.computeIfAbsent(diariaId, k -> new ArrayList<>())
              .add(Pagamento.ROW_MAPPER.mapRow(rs, 0));
        },
        diariaIds.toArray());
    return map;
  }

  private Map<Long, List<Pernoite.PernoitePessoa>> buscarPessoasPorPernoites(List<Long> pernoiteIds) {
    String in = String.join(",", Collections.nCopies(pernoiteIds.size(), "?"));
    String sql =
        """
        SELECT
          pp.id                   AS pp_id,
          pp.fk_pernoite          AS pp_fk_pernoite,
          p.id                    AS pessoa_id,
          p.nome                  AS pessoa_nome,
          p.data_nascimento       AS pessoa_data_nascimento,
          p.cpf                   AS pessoa_cpf,
          p.email                 AS pessoa_email,
          p.telefone              AS pessoa_telefone,
          p.status                AS pessoa_status,
          pp.representante        AS pessoa_titular
        FROM public.pernoite_pessoa pp
        JOIN public.pessoa p ON p.id = pp.fk_pessoa
        WHERE pp.fk_pernoite IN (%s)
        ORDER BY pp.fk_pernoite, pp.representante DESC, p.nome ASC
        """.formatted(in);

    Map<Long, List<Pernoite.PernoitePessoa>> map = new HashMap<>();
    jdbcTemplate.query(
        sql,
        rs -> {
          Long pernoiteId = rs.getLong("pp_fk_pernoite");
          Pessoa.DadosPrincipais pessoa = Pessoa.DadosPrincipais.ROW_MAPPER.mapRow(rs, 0);
          map.computeIfAbsent(pernoiteId, k -> new ArrayList<>())
              .add(new Pernoite.PernoitePessoa(rs.getLong("pp_id"), pessoa));
        },
        pernoiteIds.toArray());
    return map;
  }

  // ── Conflict check ──────────────────────────────────────────────────────────

  public boolean hasConflito(
      Long quartoId, LocalDateTime entrada, LocalDateTime saida, Long excludePernoiteId) {
    String sqlPernoite =
        """
        SELECT COUNT(*) > 0
        FROM public.pernoite
        WHERE fk_quarto = ?
          AND status IN ('ATIVO', 'PAGAMENTO_PENDENTE', 'FINALIZADO_PAGAMENTO_PENDENTE')
          AND data_hora_entrada < ?
          AND data_hora_saida > ?
        """
            + (excludePernoiteId != null ? " AND id != ? " : "");

    boolean pernoiteConflito;
    if (excludePernoiteId != null) {
      pernoiteConflito =
          Boolean.TRUE.equals(
              jdbcTemplate.queryForObject(
                  sqlPernoite, Boolean.class, quartoId, saida, entrada, excludePernoiteId));
    } else {
      pernoiteConflito =
          Boolean.TRUE.equals(
              jdbcTemplate.queryForObject(sqlPernoite, Boolean.class, quartoId, saida, entrada));
    }
    if (pernoiteConflito) return true;

    String sqlReserva =
        """
        SELECT COUNT(*) > 0
        FROM public.reserva
        WHERE fk_quarto = ?
          AND status NOT IN ('CANCELADO', 'FINALIZADO', 'HOSPEDADO', 'ORCAMENTO')
          AND data_hora_entrada < ?
          AND data_hora_saida > ?
        """;
    return Boolean.TRUE.equals(
        jdbcTemplate.queryForObject(sqlReserva, Boolean.class, quartoId, saida, entrada));
  }

  public boolean isQuartoDisponivel(Long quartoId) {
    try {
      String status =
          jdbcTemplate.queryForObject(
              "SELECT status FROM public.quarto WHERE id = ?", String.class, quartoId);
      return "DISPONIVEL".equals(status);
    } catch (EmptyResultDataAccessException e) {
      throw new NotFoundException("Quarto não encontrado: " + quartoId);
    }
  }

  // ── Insert pernoite ─────────────────────────────────────────────────────────

  @Transactional
  public Long insertPernoite(
      Long quartoId,
      LocalDateTime entrada,
      LocalDateTime saida,
      Float valorTotal,
      Long reservaId,
      String observacao) {
    return jdbcTemplate.queryForObject(
        """
        INSERT INTO public.pernoite
          (fk_quarto, fk_funcionario, fk_reserva, status,
           data_hora_entrada, data_hora_saida, valor_total, observacao)
        VALUES (?, ?, ?, 'ATIVO'::public.status_pernoite, ?, ?, ?, ?)
        RETURNING id
        """,
        Long.class,
        quartoId,
        getFuncionarioId(),
        reservaId,
        entrada,
        saida,
        valorTotal,
        observacao);
  }

  @Transactional
  public void addPessoasToPernoite(Long pernoiteId, List<Long> pessoaIds) {
    for (Long pessoaId : pessoaIds) {
      jdbcTemplate.update(
          """
          INSERT INTO public.pernoite_pessoa (fk_pernoite, fk_pessoa, fk_funcionario, representante)
          VALUES (?, ?, ?, ?)
          ON CONFLICT DO NOTHING
          """,
          pernoiteId,
          pessoaId,
          getFuncionarioId(),
          false);
    }
  }

  // ── Insert diária ───────────────────────────────────────────────────────────

  @Transactional
  public Long insertDiaria(
      Long pernoiteId,
      int numero,
      LocalDateTime inicio,
      LocalDateTime fim,
      float valor,
      Long quartoId) {
    return jdbcTemplate.queryForObject(
        """
        INSERT INTO public.diaria
          (fk_pernoite, fk_funcionario, numero, data_hora_inicio, data_hora_fim,
           valor, status, fk_quarto)
        VALUES (?, ?, ?, ?, ?, ?, 'ATIVO'::public.status_diaria, ?)
        RETURNING id
        """,
        Long.class,
        pernoiteId,
        getFuncionarioId(),
        numero,
        inicio,
        fim,
        valor,
        quartoId);
  }

  @Transactional
  public void deleteDiaria(Long diariaId) {
    jdbcTemplate.update("DELETE FROM public.diaria WHERE id = ?", diariaId);
  }

  public List<Pernoite.Diaria> findDiariasByPernoite(Long pernoiteId) {
    String sql = SELECT_DIARIA_BASE + " WHERE d.fk_pernoite = ? ORDER BY d.numero ASC ";
    List<Pernoite.Diaria> bases =
        jdbcTemplate.query(sql, (rs, rowNum) -> mapDiariaBase(rs), pernoiteId);
    return enriquecerDiarias(bases);
  }

  public int getMaxNumero(Long pernoiteId) {
    Integer max =
        jdbcTemplate.queryForObject(
            "SELECT COALESCE(MAX(numero), 0) FROM public.diaria WHERE fk_pernoite = ?",
            Integer.class,
            pernoiteId);
    return max != null ? max : 0;
  }

  @Transactional
  public void deleteDiariasAPartirDoNumero(Long pernoiteId, int fromNumero) {
    jdbcTemplate.update(
        "DELETE FROM public.diaria WHERE fk_pernoite = ? AND numero >= ?",
        pernoiteId,
        fromNumero);
  }

  // ── Update pernoite ─────────────────────────────────────────────────────────

  @Transactional
  public void updatePernoite(
      Long id,
      Long quartoId,
      LocalDateTime novaEntrada,
      LocalDateTime novaSaida,
      LocalTime horaChegada,
      LocalTime horaSaida,
      Float novoValorTotal) {
    int rows =
        jdbcTemplate.update(
            """
            UPDATE public.pernoite SET
              fk_quarto          = COALESCE(?, fk_quarto),
              data_hora_entrada  = COALESCE(?, data_hora_entrada),
              data_hora_saida    = COALESCE(?, data_hora_saida),
              hora_chegada       = COALESCE(?, hora_chegada),
              hora_saida         = COALESCE(?, hora_saida),
              valor_total        = COALESCE(?, valor_total),
              fk_funcionario     = ?
            WHERE id = ?
            """,
            quartoId,
            novaEntrada,
            novaSaida,
            horaChegada,
            horaSaida,
            novoValorTotal,
            getFuncionarioId(),
            id);
    if (rows == 0) throw new NotFoundException("Pernoite não encontrado: " + id);
  }

  @Transactional
  public void updateStatus(Long id, Pernoite.Status status) {
    int rows =
        jdbcTemplate.update(
            "UPDATE public.pernoite SET status = ?::public.status_pernoite WHERE id = ?",
            status.name(),
            id);
    if (rows == 0) throw new NotFoundException("Pernoite não encontrado: " + id);
  }

  @Transactional
  public void updateValorTotal(Long pernoiteId, Float valorTotal) {
    jdbcTemplate.update(
        "UPDATE public.pernoite SET valor_total = ? WHERE id = ?", valorTotal, pernoiteId);
  }

  // ── Diária – quarto, pessoas, pagamentos, consumos ──────────────────────────

  @Transactional
  public void updateDiariaQuarto(Long diariaId, Long quartoId) {
    int rows =
        jdbcTemplate.update(
            "UPDATE public.diaria SET fk_quarto = ? WHERE id = ?", quartoId, diariaId);
    if (rows == 0) throw new NotFoundException("Diária não encontrada: " + diariaId);
  }

  @Transactional
  public void updateDiariaStatus(Long diariaId, Pernoite.Diaria.Status status) {
    int rows =
        jdbcTemplate.update(
            "UPDATE public.diaria SET status = ?::public.status_diaria WHERE id = ?",
            status.name(),
            diariaId);
    if (rows == 0) throw new NotFoundException("Diária não encontrada: " + diariaId);
  }

  @Transactional
  public void addPessoasToDiaria(Long diariaId, List<Long> pessoaIds) {
    for (Long pessoaId : pessoaIds) {
      jdbcTemplate.update(
          """
          INSERT INTO public.diaria_pessoa (fk_diaria, fk_pessoa, fk_funcionario)
          VALUES (?, ?, ?)
          ON CONFLICT DO NOTHING
          """,
          diariaId,
          pessoaId,
          getFuncionarioId());
    }
  }

  @Transactional
  public void removePessoaFromDiaria(Long diariaId, Long pessoaId) {
    jdbcTemplate.update(
        "DELETE FROM public.diaria_pessoa WHERE fk_diaria = ? AND fk_pessoa = ?",
        diariaId,
        pessoaId);
  }

  @Transactional
  public void addPagamentoToDiaria(Long diariaId, UUID pagamentoId) {
    jdbcTemplate.update(
        """
        INSERT INTO public.diaria_pagamento (fk_diaria, fk_pagamento, fk_funcionario)
        VALUES (?, ?, ?)
        """,
        diariaId,
        pagamentoId,
        getFuncionarioId());
  }

  public UUID findPagamentoUuidByDiariaPagamentoId(Long diariaPagamentoId) {
    try {
      return jdbcTemplate.queryForObject(
          "SELECT fk_pagamento FROM public.diaria_pagamento WHERE id = ?",
          (rs, rowNum) -> rs.getObject("fk_pagamento", UUID.class),
          diariaPagamentoId);
    } catch (EmptyResultDataAccessException e) {
      throw new NotFoundException("Pagamento da diária não encontrado: " + diariaPagamentoId);
    }
  }

  @Transactional
  public void cancelDiariaPagamento(Long diariaPagamentoId, String motivo) {
    UUID pagamentoId = findPagamentoUuidByDiariaPagamentoId(diariaPagamentoId);
    pagamentoRepository.cancelarPagamento(pagamentoId, motivo);
  }

  @Transactional
  public Long insertConsumo(
      UUID pagamentoId, Long itemId, float quantidade, boolean despesaPessoal, Long quartoId) {
    return jdbcTemplate.queryForObject(
        """
        INSERT INTO public.consumo (fk_funcionario, fk_pagamento, fk_item, quantidade,
                                    data_hora_registro, fk_quarto, despesa_pessoal, cancelado)
        VALUES (?, ?, ?, ?, NOW(), ?, ?, false)
        RETURNING id
        """,
        Long.class,
        getFuncionarioId(),
        pagamentoId,
        itemId,
        quantidade,
        quartoId,
        despesaPessoal);
  }

  @Transactional
  public void addConsumoDiaria(Long diariaId, Long consumoId) {
    jdbcTemplate.update(
        "INSERT INTO public.diaria_consumo (fk_diaria, fk_consumo) VALUES (?, ?)",
        diariaId,
        consumoId);
  }

  @Transactional
  public void removeConsumoFromDiaria(Long diariaId, Long consumoId) {
    jdbcTemplate.update(
        "DELETE FROM public.diaria_consumo WHERE fk_diaria = ? AND fk_consumo = ?",
        diariaId,
        consumoId);
  }

  // ── Helpers ─────────────────────────────────────────────────────────────────

  public Long getPernoiteIdByDiaria(Long diariaId) {
    try {
      return jdbcTemplate.queryForObject(
          "SELECT fk_pernoite FROM public.diaria WHERE id = ?", Long.class, diariaId);
    } catch (EmptyResultDataAccessException e) {
      throw new NotFoundException("Diária não encontrada: " + diariaId);
    }
  }

  public Long getQuartoIdByDiaria(Long diariaId) {
    try {
      return jdbcTemplate.queryForObject(
          """
          SELECT COALESCE(d.fk_quarto, p.fk_quarto)
          FROM public.diaria d
          JOIN public.pernoite p ON p.id = d.fk_pernoite
          WHERE d.id = ?
          """,
          Long.class,
          diariaId);
    } catch (EmptyResultDataAccessException e) {
      throw new NotFoundException("Diária não encontrada: " + diariaId);
    }
  }

  @Transactional
  public void updateDiariaValor(Long diariaId, float valor) {
    jdbcTemplate.update("UPDATE public.diaria SET valor = ? WHERE id = ?", valor, diariaId);
  }

  public Long getQuartoIdByPernoite(Long pernoiteId) {
    try {
      return jdbcTemplate.queryForObject(
          "SELECT fk_quarto FROM public.pernoite WHERE id = ?", Long.class, pernoiteId);
    } catch (EmptyResultDataAccessException e) {
      throw new NotFoundException("Pernoite não encontrado: " + pernoiteId);
    }
  }

  public Long getFuncionarioId() {
    return pessoaRepository.getFuncionarioIdFromRequest();
  }
}
