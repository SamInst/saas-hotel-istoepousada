package saas.hotel.istoepousada.repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.Pernoite;
import saas.hotel.istoepousada.dto.Quarto;
import saas.hotel.istoepousada.dto.Recepcao;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;

@Repository
public class QuartoRepository {

  private final JdbcTemplate jdbcTemplate;
  private final PessoaRepository pessoaRepository;
  private final PernoiteRepository pernoiteRepository;

  public QuartoRepository(
      JdbcTemplate jdbcTemplate,
      PessoaRepository pessoaRepository,
      PernoiteRepository pernoiteRepository) {
    this.jdbcTemplate = jdbcTemplate;
    this.pessoaRepository = pessoaRepository;
    this.pernoiteRepository = pernoiteRepository;
  }

  private Long getFuncionarioId() {
    return pessoaRepository.getFuncionarioIdFromRequest();
  }

  // ── SELECT base ─────────────────────────────────────────────────────────────

  private static final String SELECT_QUARTO_BASE =
      """
      SELECT
        quarto.id                         AS quarto_id,
        quarto.descricao                  AS quarto_descricao,
        quarto.quantidade_pessoa          AS quarto_quantidade_pessoas,
        quarto.status                     AS quarto_status,
        quarto.quantidade_cama_casal      AS quarto_quantidade_cama_casal,
        quarto.quantidade_cama_solteiro   AS quarto_quantidade_cama_solteiro,
        quarto.quantidade_rede            AS quarto_quantidade_rede,
        quarto.quantidade_beliche         AS quarto_quantidade_beliche
      FROM public.quarto quarto
      """;

  private static final ResultSetExtractor<List<Quarto>> QUARTO_EXTRACTOR =
      rs -> {
        List<Quarto> list = new ArrayList<>();
        int rowNum = 0;
        while (rs.next()) {
          list.add(Quarto.ROW_MAPPER.mapRow(rs, rowNum++));
        }
        return list;
      };

  // ── Buscar paginado ─────────────────────────────────────────────────────────

  public Page<Quarto> buscar(Long id, String termo, Quarto.Status status, Pageable pageable) {
    String baseFrom = " FROM public.quarto quarto ";

    StringBuilder where = new StringBuilder(" WHERE 1 = 1 ");
    List<Object> params = new ArrayList<>();

    if (id != null) {
      where.append(" AND quarto.id = ? ");
      params.add(id);
    }

    if (termo != null && !termo.isBlank()) {
      String t = termo.trim();
      boolean isNumeric = t.matches("\\d+");
      where.append(" AND (quarto.descricao ILIKE ? ");
      params.add("%" + t + "%");
      if (isNumeric) {
        where.append(" OR quarto.id = ? ");
        params.add(Long.parseLong(t));
      }
      where.append(") ");
    }

    if (status != null) {
      where.append(" AND quarto.status = CAST(? AS public.status_quarto_enum) ");
      params.add(status.name());
    }

    Long total;
    try {
      total =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*)" + baseFrom + where, Long.class, params.toArray());
    } catch (EmptyResultDataAccessException ex) {
      total = 0L;
    }

    if (total == null || total == 0) {
      return new PageImpl<>(List.of(), pageable, 0);
    }

    String idsSql =
        "SELECT quarto.id AS id"
            + baseFrom
            + where
            + " ORDER BY quarto.id ASC LIMIT ? OFFSET ? ";

    List<Object> idsParams = new ArrayList<>(params);
    idsParams.add(pageable.getPageSize());
    idsParams.add(pageable.getOffset());

    List<Long> ids =
        jdbcTemplate.query(idsSql, (rs, rowNum) -> rs.getLong("id"), idsParams.toArray());

    if (ids.isEmpty()) {
      return new PageImpl<>(List.of(), pageable, total);
    }

    String in = String.join(",", Collections.nCopies(ids.size(), "?"));
    String pageSql = SELECT_QUARTO_BASE + " WHERE quarto.id IN (" + in + ") ORDER BY quarto.id";
    List<Quarto> content = jdbcTemplate.query(pageSql, QUARTO_EXTRACTOR, ids.toArray());
    return new PageImpl<>(Objects.requireNonNull(content), pageable, total);
  }

  // ── findById ────────────────────────────────────────────────────────────────

  public Quarto findByIdOrThrow(Long id) {
    if (id == null) throw new IllegalArgumentException("Id é obrigatório.");
    try {
      return jdbcTemplate.queryForObject(
          SELECT_QUARTO_BASE + " WHERE quarto.id = ? ", Quarto.ROW_MAPPER, id);
    } catch (EmptyResultDataAccessException ex) {
      throw new NotFoundException("Quarto não encontrado: " + id);
    }
  }

  // ── Insert / Update quarto ──────────────────────────────────────────────────

  @Transactional
  public Quarto insert(Quarto.Request quarto) {
    Long quartoId =
        jdbcTemplate.queryForObject(
            """
            INSERT INTO public.quarto (
              descricao, quantidade_pessoa, status,
              quantidade_cama_casal, quantidade_cama_solteiro,
              quantidade_rede, quantidade_beliche
            ) VALUES (?, ?, 'DISPONIVEL'::status_quarto_enum, ?, ?, ?, ?)
            RETURNING id
            """,
            Long.class,
            quarto.descricao(),
            quarto.quantidade_pessoas(),
            quarto.quantidade_cama_casal(),
            quarto.quantidade_cama_solteiro(),
            quarto.quantidade_rede(),
            quarto.quantidade_beliche());
    vincularCategoriaAtiva(quartoId, quarto.categoria().id());
    return findByIdOrThrow(quartoId);
  }

  @Transactional
  public Quarto update(Quarto.Update request) {
    findByIdOrThrow(request.id());
    int rows =
        jdbcTemplate.update(
            """
            UPDATE public.quarto SET
              descricao = ?,
              quantidade_pessoa = ?,
              status = CAST(? AS public.status_quarto_enum),
              quantidade_cama_casal = ?,
              quantidade_cama_solteiro = ?,
              quantidade_rede = ?,
              quantidade_beliche = ?
            WHERE id = ?
            """,
            request.descricao().trim(),
            request.quantidade_pessoas(),
            request.status().name(),
            request.quantidade_cama_casal(),
            request.quantidade_cama_solteiro(),
            request.quantidade_rede(),
            request.quantidade_beliche(),
            request.id());
    if (rows == 0) throw new NotFoundException("Quarto não encontrado: " + request.id());
    atualizarCategoriaAtiva(request.id(), request.categoria().id());
    return findByIdOrThrow(request.id());
  }

  @Transactional
  public void updateStatus(Long id, Quarto.Status status) {
    jdbcTemplate.update(
        "UPDATE public.quarto SET status = CAST(? AS public.status_quarto_enum) WHERE id = ?",
        status.name(),
        id);
  }

  // ── Itens ────────────────────────────────────────────────────────────────────

  public List<Quarto.ItemQuarto> listarItens(Long quartoId) {
    String sql =
        """
        SELECT
          qi.id                 AS quarto_item_id,
          i.id                  AS item_id,
          i.descricao           AS item_descricao,
          qi.quantidade_atual   AS quarto_item_quantidade_atual,
          qi.quantidade_padrao  AS quarto_item_quantidade_padrao
        FROM quarto_item qi
        JOIN item i ON i.id = qi.fk_item
        WHERE qi.fk_quarto = ?
        ORDER BY i.descricao
        """;
    return jdbcTemplate.query(sql, Quarto.ItemQuarto.ROW_MAPPER, quartoId);
  }

  @Transactional
  public Quarto.ItemQuarto adicionarItem(Long quartoId, Quarto.QuartoItem.Request req) {
    findByIdOrThrow(quartoId);
    Long id =
        jdbcTemplate.queryForObject(
            """
            INSERT INTO public.quarto_item
              (fk_quarto, fk_item, quantidade_padrao, quantidade_atual, data_hora_reposicao, fk_funcionario)
            VALUES (?, ?, ?, ?, NOW(), ?)
            RETURNING id
            """,
            Long.class,
            quartoId,
            req.item().id(),
            req.quantidade_padrao(),
            req.quantidade_atual(),
            getFuncionarioId());
    return findItemById(id);
  }

  @Transactional
  public Quarto.ItemQuarto atualizarItem(Quarto.QuartoItem.Update req) {
    jdbcTemplate.update(
        "UPDATE public.quarto_item SET fk_item = ?, quantidade_padrao = ? WHERE id = ?",
        req.item().id(),
        req.quantidade_padrao(),
        req.id());
    return findItemById(req.id());
  }

  @Transactional
  public Quarto.ItemQuarto consumirItem(Quarto.QuartoItem.Consumir req) {
    jdbcTemplate.update(
        "UPDATE public.quarto_item SET quantidade_atual = GREATEST(0, quantidade_atual - ?) WHERE id = ?",
        req.quantidade(),
        req.id());
    return findItemById(req.id());
  }

  @Transactional
  public Quarto.ItemQuarto reporItem(Quarto.QuartoItem.Repor req) {
    jdbcTemplate.update(
        """
        UPDATE public.quarto_item
        SET quantidade_atual = LEAST(quantidade_atual + ?, quantidade_padrao),
            data_hora_reposicao = NOW(),
            fk_funcionario = ?
        WHERE id = ?
        """,
        req.quantidade(),
        getFuncionarioId(),
        req.id());
    return findItemById(req.id());
  }

  private Quarto.ItemQuarto findItemById(Long id) {
    return jdbcTemplate.queryForObject(
        """
        SELECT
          qi.id                 AS quarto_item_id,
          i.id                  AS item_id,
          i.descricao           AS item_descricao,
          qi.quantidade_atual   AS quarto_item_quantidade_atual,
          qi.quantidade_padrao  AS quarto_item_quantidade_padrao
        FROM public.quarto_item qi
        JOIN public.item i ON i.id = qi.fk_item
        WHERE qi.id = ?
        """,
        Quarto.ItemQuarto.ROW_MAPPER,
        id);
  }

  // ── Manutenção ───────────────────────────────────────────────────────────────

  @Transactional
  public Quarto.QuartoManutencao inserirManutencao(Quarto.QuartoManutencao.Request req) {
    findByIdOrThrow(req.quarto().id());
    Long id =
        jdbcTemplate.queryForObject(
            """
            INSERT INTO public.quarto_manutencao
              (fk_quarto, fk_funcionario, descricao, nome_responsavel,
               data_hora_registro, data_hora_inicio, data_hora_fim, ativo)
            VALUES (?, ?, ?, ?, NOW(), ?, ?, true)
            RETURNING id
            """,
            Long.class,
            req.quarto().id(),
            getFuncionarioId(),
            req.descricao(),
            req.nome_responsavel(),
            req.data_hora_inicio(),
            req.data_hora_fim());
    updateStatus(req.quarto().id(), Quarto.Status.MANUTENCAO);
    return findManutencaoById(id);
  }

  @Transactional
  public Quarto.QuartoManutencao atualizarManutencao(Quarto.QuartoManutencao.Update req) {
    jdbcTemplate.update(
        """
        UPDATE public.quarto_manutencao SET
          descricao = ?,
          nome_responsavel = ?,
          data_hora_inicio = ?,
          data_hora_fim = ?,
          ativo = ?
        WHERE id = ?
        """,
        req.descricao(),
        req.nome_responsavel(),
        req.data_hora_inicio(),
        req.data_hora_fim(),
        req.ativo(),
        req.id());
    return findManutencaoById(req.id());
  }

  @Transactional
  public void finalizarManutencao(Long id) {
    Long quartoId =
        jdbcTemplate.queryForObject(
            "UPDATE public.quarto_manutencao SET ativo = false WHERE id = ? RETURNING fk_quarto",
            Long.class,
            id);
    if (quartoId != null) updateStatus(quartoId, Quarto.Status.DISPONIVEL);
  }

  public Quarto.QuartoManutencao findManutencaoById(Long id) {
    try {
      return jdbcTemplate.queryForObject(
          """
          SELECT
            qm.id                    AS manutencao_id,
            qm.fk_quarto             AS quarto_id,
            f.id                     AS manutencao_funcionario_id,
            pf.nome                  AS manutencao_funcionario_nome,
            qm.descricao             AS manutencao_descricao,
            qm.data_hora_registro    AS manutencao_data_hora_registro,
            qm.data_hora_inicio      AS manutencao_data_hora_inicio,
            qm.data_hora_fim         AS manutencao_data_hora_fim,
            qm.nome_responsavel      AS manutencao_nome_responsavel,
            qm.ativo                 AS manutencao_ativo
          FROM public.quarto_manutencao qm
          LEFT JOIN public.funcionario f ON f.id = qm.fk_funcionario
          LEFT JOIN public.pessoa pf ON pf.id = f.fk_pessoa
          WHERE qm.id = ?
          """,
          Quarto.QuartoManutencao.ROW_MAPPER,
          id);
    } catch (EmptyResultDataAccessException e) {
      throw new NotFoundException("Manutenção não encontrada: " + id);
    }
  }

  // ── Limpeza ──────────────────────────────────────────────────────────────────

  @Transactional
  public Quarto.QuartoLimpeza acionarLimpeza(Long quartoId) {
    findByIdOrThrow(quartoId);
    Long id =
        jdbcTemplate.queryForObject(
            """
            INSERT INTO public.quarto_limpeza (fk_quarto, fk_funcionario, data_hora_registro, ativo)
            VALUES (?, ?, NOW(), true)
            RETURNING id
            """,
            Long.class,
            quartoId,
            getFuncionarioId());
    updateStatus(quartoId, Quarto.Status.EM_LIMPEZA);
    return findLimpezaById(id);
  }

  @Transactional
  public void finalizarLimpeza(Long id) {
    Long quartoId =
        jdbcTemplate.queryForObject(
            "UPDATE public.quarto_limpeza SET ativo = false WHERE id = ? RETURNING fk_quarto",
            Long.class,
            id);
    if (quartoId != null) updateStatus(quartoId, Quarto.Status.DISPONIVEL);
  }

  public Quarto.QuartoLimpeza findLimpezaById(Long id) {
    try {
      return jdbcTemplate.queryForObject(
          """
          SELECT
            ql.id                    AS limpeza_id,
            ql.fk_quarto             AS quarto_id,
            f.id                     AS limpeza_funcionario_id,
            pf.nome                  AS limpeza_funcionario_nome,
            ql.data_hora_registro    AS limpeza_data_hora_registro,
            ql.data_hora_inicio      AS limpeza_data_hora_inicio,
            ql.data_hora_fim         AS limpeza_data_hora_fim,
            ql.ativo                 AS limpeza_ativo
          FROM public.quarto_limpeza ql
          LEFT JOIN public.funcionario f ON f.id = ql.fk_funcionario
          LEFT JOIN public.pessoa pf ON pf.id = f.fk_pessoa
          WHERE ql.id = ?
          """,
          Quarto.QuartoLimpeza.ROW_MAPPER,
          id);
    } catch (EmptyResultDataAccessException e) {
      throw new NotFoundException("Limpeza não encontrada: " + id);
    }
  }

  // ── Recepção ─────────────────────────────────────────────────────────────────

  public Recepcao buscarRecepcao(LocalDate dataInicio, LocalDate dataFim) {
    record QuartoRow(
        long quartoId,
        String descricao,
        int qtdPessoas,
        Quarto.Status status,
        int camaCasal,
        int camaSolteiro,
        int rede,
        int beliche,
        long categoriaId,
        String categoriaNome,
        String categoriaDescricao) {}

    record PernoiteRangeInfo(long quartoId, long pernoiteId, LocalDate entrada, LocalDate saida) {}

    List<QuartoRow> quartoRows =
        jdbcTemplate.query(
            """
            SELECT
              q.id AS quarto_id,
              q.descricao AS quarto_descricao,
              q.quantidade_pessoa AS quarto_quantidade_pessoas,
              q.status AS quarto_status,
              q.quantidade_cama_casal AS quarto_quantidade_cama_casal,
              q.quantidade_cama_solteiro AS quarto_quantidade_cama_solteiro,
              q.quantidade_rede AS quarto_quantidade_rede,
              q.quantidade_beliche AS quarto_quantidade_beliche,
              c.id AS categoria_id,
              c.nome AS categoria_nome,
              c.descricao AS categoria_descricao
            FROM public.quarto q
            JOIN public.quarto_categoria qc ON qc.fk_quarto = q.id AND qc.ativo = true
            JOIN public.categoria c ON c.id = qc.fk_categoria
            ORDER BY c.nome, q.descricao
            """,
            (rs, rowNum) ->
                new QuartoRow(
                    rs.getLong("quarto_id"),
                    rs.getString("quarto_descricao"),
                    rs.getInt("quarto_quantidade_pessoas"),
                    Quarto.Status.valueOf(rs.getString("quarto_status")),
                    rs.getInt("quarto_quantidade_cama_casal"),
                    rs.getInt("quarto_quantidade_cama_solteiro"),
                    rs.getInt("quarto_quantidade_rede"),
                    rs.getInt("quarto_quantidade_beliche"),
                    rs.getLong("categoria_id"),
                    rs.getString("categoria_nome"),
                    rs.getString("categoria_descricao")));

    Map<Long, Quarto.QuartoManutencao> manutencaoPorQuarto = new HashMap<>();
    jdbcTemplate.query(
        """
        SELECT DISTINCT ON (qm.fk_quarto)
          qm.fk_quarto             AS quarto_id,
          qm.id                    AS manutencao_id,
          f.id                     AS manutencao_funcionario_id,
          pf.nome                  AS manutencao_funcionario_nome,
          qm.descricao             AS manutencao_descricao,
          qm.data_hora_registro    AS manutencao_data_hora_registro,
          qm.data_hora_inicio      AS manutencao_data_hora_inicio,
          qm.data_hora_fim         AS manutencao_data_hora_fim,
          qm.nome_responsavel      AS manutencao_nome_responsavel,
          qm.ativo                 AS manutencao_ativo
        FROM public.quarto_manutencao qm
        LEFT JOIN public.funcionario f ON f.id = qm.fk_funcionario
        LEFT JOIN public.pessoa pf ON pf.id = f.fk_pessoa
        WHERE qm.ativo = true
        ORDER BY qm.fk_quarto, qm.data_hora_registro DESC
        """,
        rs -> {
          long qid = rs.getLong("quarto_id");
          manutencaoPorQuarto.put(qid, Quarto.QuartoManutencao.ROW_MAPPER.mapRow(rs, 0));
        });

    Map<Long, Quarto.QuartoLimpeza> limpezaPorQuarto = new HashMap<>();
    jdbcTemplate.query(
        """
        SELECT DISTINCT ON (ql.fk_quarto)
          ql.fk_quarto             AS quarto_id,
          ql.id                    AS limpeza_id,
          f.id                     AS limpeza_funcionario_id,
          pf.nome                  AS limpeza_funcionario_nome,
          ql.data_hora_registro    AS limpeza_data_hora_registro,
          ql.data_hora_inicio      AS limpeza_data_hora_inicio,
          ql.data_hora_fim         AS limpeza_data_hora_fim,
          ql.ativo                 AS limpeza_ativo
        FROM public.quarto_limpeza ql
        LEFT JOIN public.funcionario f ON f.id = ql.fk_funcionario
        LEFT JOIN public.pessoa pf ON pf.id = f.fk_pessoa
        WHERE ql.ativo = true
        ORDER BY ql.fk_quarto, ql.data_hora_registro DESC
        """,
        rs -> {
          long qid = rs.getLong("quarto_id");
          limpezaPorQuarto.put(qid, Quarto.QuartoLimpeza.ROW_MAPPER.mapRow(rs, 0));
        });

    List<PernoiteRangeInfo> pernoiteRows =
        jdbcTemplate.query(
            """
            SELECT
              last_d.fk_quarto   AS quarto_id,
              p.id               AS pernoite_id,
              p.data_entrada,
              p.data_saida
            FROM public.pernoite p
            JOIN (
              SELECT DISTINCT ON (d.fk_pernoite) d.fk_pernoite, d.fk_quarto
              FROM public.diaria d
              ORDER BY d.fk_pernoite, d.numero DESC
            ) last_d ON last_d.fk_pernoite = p.id
            WHERE p.status IN ('ATIVO','PAGAMENTO_PENDENTE','FINALIZADO_PAGAMENTO_PENDENTE')
              AND p.data_entrada <= ?
              AND p.data_saida > ?
            """,
            (rs, rowNum) ->
                new PernoiteRangeInfo(
                    rs.getLong("quarto_id"),
                    rs.getLong("pernoite_id"),
                    rs.getObject("data_entrada", LocalDate.class),
                    rs.getObject("data_saida", LocalDate.class)),
            dataFim,
            dataInicio);

    List<Long> pernoiteIds =
        pernoiteRows.stream().map(PernoiteRangeInfo::pernoiteId).distinct().toList();
    Map<Long, Pernoite> pernoiteById =
        pernoiteRepository.findByIds(pernoiteIds).stream()
            .collect(Collectors.toMap(Pernoite::id, p -> p));

    List<Recepcao.QuartoData> datas = new ArrayList<>();
    LocalDate current = dataInicio;
    while (!current.isAfter(dataFim)) {
      final LocalDate data = current;

      Map<Long, Pernoite> pernoitePorQuarto = new HashMap<>();
      for (PernoiteRangeInfo row : pernoiteRows) {
        if (!data.isBefore(row.entrada()) && data.isBefore(row.saida())) {
          Pernoite p = pernoiteById.get(row.pernoiteId());
          if (p != null) pernoitePorQuarto.put(row.quartoId(), p);
        }
      }

      int totalPessoas =
          pernoitePorQuarto.values().stream().mapToInt(p -> p.pessoas().size()).sum();

      Map<Long, List<Recepcao.QuartoData.Categoria.Hospedagem>> hospedagensPorCat =
          new LinkedHashMap<>();
      Map<Long, String[]> catNames = new LinkedHashMap<>();

      for (QuartoRow qr : quartoRows) {
        Quarto quarto =
            new Quarto(
                qr.quartoId(),
                qr.descricao(),
                qr.qtdPessoas(),
                qr.status(),
                qr.camaCasal(),
                qr.camaSolteiro(),
                qr.rede(),
                qr.beliche(),
                null,
                manutencaoPorQuarto.get(qr.quartoId()),
                limpezaPorQuarto.get(qr.quartoId()));

        Recepcao.QuartoData.Categoria.Hospedagem hospedagem =
            new Recepcao.QuartoData.Categoria.Hospedagem(
                quarto, pernoitePorQuarto.get(qr.quartoId()), null);

        hospedagensPorCat.computeIfAbsent(qr.categoriaId(), k -> new ArrayList<>()).add(hospedagem);
        catNames.putIfAbsent(
            qr.categoriaId(), new String[] {qr.categoriaNome(), qr.categoriaDescricao()});
      }

      List<Recepcao.QuartoData.Categoria> categorias = new ArrayList<>();
      for (Map.Entry<Long, List<Recepcao.QuartoData.Categoria.Hospedagem>> entry :
          hospedagensPorCat.entrySet()) {
        String[] names = catNames.get(entry.getKey());
        categorias.add(
            new Recepcao.QuartoData.Categoria(
                entry.getKey(), names[0], names[1], entry.getValue()));
      }

      datas.add(new Recepcao.QuartoData(data, totalPessoas, categorias));
      current = current.plusDays(1);
    }

    return new Recepcao(datas);
  }

  // ── Categoria helpers ────────────────────────────────────────────────────────

  private void vincularCategoriaAtiva(Long quartoId, Long categoriaId) {
    jdbcTemplate.update(
        "INSERT INTO public.quarto_categoria (fk_quarto, fk_categoria, ativo) VALUES (?, ?, true)",
        quartoId,
        categoriaId);
  }

  private void atualizarCategoriaAtiva(Long quartoId, Long categoriaId) {
    jdbcTemplate.update(
        "UPDATE public.quarto_categoria SET ativo = false WHERE fk_quarto = ?", quartoId);

    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM public.quarto_categoria WHERE fk_quarto = ? AND fk_categoria = ?",
            Integer.class,
            quartoId,
            categoriaId);

    if (count != null && count > 0) {
      jdbcTemplate.update(
          "UPDATE public.quarto_categoria SET ativo = true WHERE fk_quarto = ? AND fk_categoria = ?",
          quartoId,
          categoriaId);
    } else {
      jdbcTemplate.update(
          "INSERT INTO public.quarto_categoria (fk_quarto, fk_categoria, ativo) VALUES (?, ?, true)",
          quartoId,
          categoriaId);
    }
  }

  private void setIntOrNull(PreparedStatement ps, int idx, Integer value) throws SQLException {
    if (value == null) ps.setNull(idx, Types.INTEGER);
    else ps.setInt(idx, value);
  }
}
