package saas.hotel.istoepousada.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import saas.hotel.istoepousada.dto.*;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;

@Repository
public class ItemRepository {

  private final JdbcTemplate jdbcTemplate;

  public ItemRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  private final ResultSetExtractor<List<ItemResponse>> ITEM_EXTRACTOR =
      rs -> {
        List<ItemResponse> list = new ArrayList<>();
        while (rs.next()) list.add(ItemResponse.mapItem(rs));
        return list;
      };

  public Page<ItemResponse> buscar(
      Long id,
      String termo,
      Long categoriaId,
      LocalDate dataInicioCadastro,
      LocalDate dataFimCadastro,
      Pageable pageable) {

    boolean hasId = id != null;
    boolean hasTermo = termo != null && !termo.trim().isEmpty();
    boolean hasCategoriaId = categoriaId != null;

    String termoTrim = hasTermo ? termo.trim() : null;
    String search = hasTermo ? "%" + termoTrim.toLowerCase() + "%" : null;

    String baseFrom =
        """
                FROM item i
                INNER JOIN categoria_item c ON c.id = i.fk_categoria
                LEFT JOIN estoque e ON e.fk_item = i.id
                LEFT JOIN LATERAL (
                    SELECT hpi.data_hora_registro
                    FROM historico_preco_item hpi
                    WHERE hpi.fk_item = i.id
                    ORDER BY hpi.data_hora_registro DESC, hpi.id DESC
                    LIMIT 1
                ) h ON TRUE
                """;

    String baseSelect =
        """
                SELECT
                    i.id                    AS item_id,
                    i.descricao             AS item_descricao,
                    e.qtd_total_unidades    AS estoque_qtd_total_unidades,
                    e.valor_compra_unidade  AS estoque_valor_compra_unidade,
                    e.valor_venda_unidade   AS estoque_valor_venda_unidade,
                    h.data_hora_registro    AS data_hora_registro
                """
            + baseFrom;

    StringBuilder where = new StringBuilder(" WHERE 1=1 ");
    List<Object> params = new ArrayList<>();

    if (hasId) {
      where.append(" AND i.id = ? ");
      params.add(id);
    }

    if (hasTermo) {
      where.append(" AND (LOWER(i.descricao) ILIKE ? OR LOWER(c.categoria) ILIKE ?) ");
      params.add(search);
      params.add(search);
    }

    if (hasCategoriaId) {
      where.append(" AND i.fk_categoria = ? ");
      params.add(categoriaId);
    }

    if (dataInicioCadastro != null) {
      where.append(" AND i.data_hora_registro_item >= ? ");
      params.add(Timestamp.valueOf(dataInicioCadastro.atStartOfDay()));
    }

    if (dataFimCadastro != null) {
      where.append(" AND i.data_hora_registro_item < ? ");
      params.add(Timestamp.valueOf(dataFimCadastro.plusDays(1).atStartOfDay()));
    }

    String countSql = "SELECT COUNT(DISTINCT i.id) " + baseFrom + where;

    Long total;
    try {
      total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
    } catch (EmptyResultDataAccessException ex) {
      total = 0L;
    }

    if (total == null || total == 0) {
      return new PageImpl<>(List.of(), pageable, 0);
    }

    String idsSql =
        """
                SELECT i.id
                """
            + baseFrom
            + where
            + """
        ORDER BY i.descricao ASC
        LIMIT ? OFFSET ?
        """;

    List<Object> idsParams = new ArrayList<>(params);
    idsParams.add(pageable.getPageSize());
    idsParams.add((int) pageable.getOffset());

    List<Long> ids =
        jdbcTemplate.query(idsSql, (rs, rowNum) -> rs.getLong("id"), idsParams.toArray());

    if (ids.isEmpty()) {
      return new PageImpl<>(List.of(), pageable, total);
    }

    String inPlaceholders = String.join(",", Collections.nCopies(ids.size(), "?"));

    String pageSql =
        baseSelect + " WHERE i.id IN (" + inPlaceholders + ") ORDER BY i.descricao ASC";

    List<Object> pageParams = new ArrayList<>(ids);

    List<ItemResponse> content = jdbcTemplate.query(pageSql, ITEM_EXTRACTOR, pageParams.toArray());

    return new PageImpl<>(Objects.requireNonNull(content), pageable, total);
  }

  public ItemResponse findById(Long id) {
    Page<ItemResponse> page = buscar(id, null, null, null, null, Pageable.ofSize(1));
    if (page.isEmpty()) {
      throw new NotFoundException("Item não encontrado para o id: " + id);
    }
    return page.getContent().getFirst();
  }

  public ItemResponse insert(ItemResponse.ItemRequest request, Long funcionarioId) {
    String sqlItem =
        """
                INSERT INTO item (descricao, fk_categoria, data_hora_registro_item)
                VALUES (?, ?, now())
                """;

    KeyHolder keyHolder = new GeneratedKeyHolder();
    jdbcTemplate.update(
        connection -> {
          PreparedStatement ps =
              connection.prepareStatement(sqlItem, Statement.RETURN_GENERATED_KEYS);
          ps.setString(1, request.descricao().trim());
          ps.setLong(2, request.categoriaId());
          return ps;
        },
        keyHolder);

    Long itemId =
        keyHolder.getKeys() != null && keyHolder.getKeys().containsKey("id")
            ? ((Number) keyHolder.getKeys().get("id")).longValue()
            : null;

    if (itemId == null) {
      throw new IllegalStateException("Falha ao obter ID gerado do item");
    }

    String sqlEstoque =
        """
                INSERT INTO estoque (
                    qtd_total_unidades,
                    data_hora_ultima_reposicao,
                    fk_item,
                    valor_compra_unidade,
                    valor_venda_unidade,
                    fornecedor
                ) VALUES (?, now(), ?, ?, ?, ?)
                """;

    int qtdInicial = request.quantidadeTotal() != null ? request.quantidadeTotal() : 0;
    double valorCompra = request.valorCompraUnidade() != null ? request.valorCompraUnidade() : 0d;
    double valorVenda = request.valorVendaUnidade();

    jdbcTemplate.update(
        sqlEstoque, qtdInicial, itemId, valorCompra, valorVenda, request.fornecedor());

    Long estoqueId =
        jdbcTemplate.queryForObject("SELECT id FROM estoque WHERE fk_item = ?", Long.class, itemId);

    String sqlHistReposicao =
        """
                INSERT INTO historico_estoque (
                    fk_estoque,
                    data_hora_reposicao,
                    qtd_total_unidades,
                    valor_compra_unidade,
                    valor_venda_unidade,
                    fk_funcionario
                ) VALUES (?, now(), ?, ?, ?, ?)
                """;

    jdbcTemplate.update(
        sqlHistReposicao, estoqueId, qtdInicial, valorCompra, valorVenda, funcionarioId);

    String sqlHistoricoPreco =
        """
                INSERT INTO historico_preco_item (
                    data_hora_registro,
                    fk_item,
                    valor_compra_unidade,
                    valor_venda_unidade,
                    fk_funcionario
                ) VALUES (now(), ?, ?, ?, ?)
                """;

    jdbcTemplate.update(sqlHistoricoPreco, itemId, valorCompra, valorVenda, funcionarioId);

    return findById(itemId);
  }

  public ItemResponse update(Long id, ItemResponse.ItemRequest request, Long funcionarioId) {
    if (id == null) throw new IllegalArgumentException("id é obrigatório.");

    ItemResponse atual = findById(id);

    String sqlItem =
        """
                UPDATE item
                SET descricao = ?, fk_categoria = ?
                WHERE id = ?
                """;

    jdbcTemplate.update(sqlItem, request.descricao().trim(), request.categoriaId(), id);

    String sqlEstoqueSelect =
        """
                SELECT id, qtd_total_unidades, valor_compra_unidade, valor_venda_unidade
                FROM estoque
                WHERE fk_item = ?
                """;

    Map<String, Object> estoqueRow = jdbcTemplate.queryForMap(sqlEstoqueSelect, id);

    Long estoqueId = ((Number) estoqueRow.get("id")).longValue();
    int qtdAtual =
        estoqueRow.get("qtd_total_unidades") != null
            ? ((Number) estoqueRow.get("qtd_total_unidades")).intValue()
            : 0;
    Double valorCompraAtual =
        estoqueRow.get("valor_compra_unidade") != null
            ? ((Number) estoqueRow.get("valor_compra_unidade")).doubleValue()
            : 0d;
    Double valorVendaAtual =
        estoqueRow.get("valor_venda_unidade") != null
            ? ((Number) estoqueRow.get("valor_venda_unidade")).doubleValue()
            : 0d;

    String sqlEstoqueUpdate =
        """
                UPDATE estoque
                SET
                  qtd_total_unidades = ?,
                  valor_compra_unidade = ?,
                  valor_venda_unidade = ?,
                  data_hora_ultima_reposicao = now()
                WHERE id = ?
                """;

    jdbcTemplate.update(
        sqlEstoqueUpdate,
        request.quantidadeTotal() != null ? request.quantidadeTotal() : qtdAtual,
        request.valorCompraUnidade() != null ? request.valorCompraUnidade() : valorCompraAtual,
        request.valorVendaUnidade() != null ? request.valorVendaUnidade() : valorVendaAtual,
        estoqueId);

    boolean mudouPreco =
        (request.valorCompraUnidade() != null
                && !Objects.equals(request.valorCompraUnidade(), valorCompraAtual))
            || (request.valorVendaUnidade() != null
                && !Objects.equals(request.valorVendaUnidade(), valorVendaAtual));

    if (mudouPreco) {
      String sqlHistorico =
          """
                    INSERT INTO historico_preco_item (
                        data_hora_registro,
                        fk_item,
                        valor_compra_unidade,
                        valor_venda_unidade,
                        fk_funcionario
                    ) VALUES (now(), ?, ?, ?, ?)
                    """;

      jdbcTemplate.update(
          sqlHistorico,
          id,
          request.valorCompraUnidade() != null ? request.valorCompraUnidade() : valorCompraAtual,
          request.valorVendaUnidade() != null ? request.valorVendaUnidade() : valorVendaAtual,
          funcionarioId);
    }

    return findById(id);
  }

  public HistoricoReposicaoItem listarHistoricoReposicaoPorItemId(Long itemId) {
    String sql =
        """
                SELECT
                  he.id                   AS id,
                  he.data_hora_reposicao  AS data_hora_reposicao,
                  he.valor_compra_unidade AS valor_compra_unidade,
                  he.valor_venda_unidade  AS valor_venda_unidade,
                  he.qtd_total_unidades   AS qtd_total_unidades,
                  e.fornecedor            AS fornecedor,
                  f.id                    AS funcionario_id,
                  p.nome                  AS funcionario_nome
                FROM historico_estoque he
                JOIN estoque e ON e.id = he.fk_estoque
                LEFT JOIN funcionario f ON f.id = he.fk_funcionario
                LEFT JOIN pessoa p ON p.id = f.fk_pessoa
                WHERE e.fk_item = ?
                ORDER BY he.data_hora_reposicao DESC, he.id DESC
                """;

    List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, itemId);

    double totalInvestidoGeral = 0d;
    double totalVendaGeral = 0d;

    List<HistoricoReposicaoItem.ItemReposicao> itens = new ArrayList<>();

    for (Map<String, Object> r : rows) {
      Double vc =
          r.get("valor_compra_unidade") != null
              ? ((Number) r.get("valor_compra_unidade")).doubleValue()
              : 0d;
      Double vv =
          r.get("valor_venda_unidade") != null
              ? ((Number) r.get("valor_venda_unidade")).doubleValue()
              : 0d;

      Integer qtd =
          r.get("qtd_total_unidades") != null
              ? ((Number) r.get("qtd_total_unidades")).intValue()
              : 0;

      double investido = vc * qtd;
      double venda = vv * qtd;
      double lucro = venda - investido;

      totalInvestidoGeral += investido;
      totalVendaGeral += venda;

      itens.add(
          new HistoricoReposicaoItem.ItemReposicao(
              ((Number) r.get("id")).longValue(),
              ((Timestamp) r.get("data_hora_reposicao")).toLocalDateTime(),
              vc,
              vv,
              (String) r.get("fornecedor"),
              r.get("funcionario_id") != null
                  ? ((Number) r.get("funcionario_id")).longValue()
                  : null,
              (String) r.get("funcionario_nome"),
              qtd,
              (float) investido,
              (float) venda,
              (float) lucro));
    }

    float valorTotalInvestido = (float) totalInvestidoGeral;
    float valorTotalVenda = (float) totalVendaGeral;
    float lucroTotal = (float) (totalVendaGeral - totalInvestidoGeral);

    return new HistoricoReposicaoItem(valorTotalInvestido, valorTotalVenda, lucroTotal, itens);
  }

  public void reporEstoque(
      Long itemId,
      int qtdMovimentada,
      Double valorCompra,
      Double valorVenda,
      String fornecedor,
      Long funcionarioId) {
    String selectEstoque =
        """
                SELECT id, qtd_total_unidades, valor_compra_unidade, valor_venda_unidade, fornecedor
                FROM estoque
                WHERE fk_item = ?
                """;

    Map<String, Object> row = jdbcTemplate.queryForMap(selectEstoque, itemId);

    Long estoqueId = ((Number) row.get("id")).longValue();
    int qtdAtual = ((Number) row.get("qtd_total_unidades")).intValue();
    double vcAtual =
        row.get("valor_compra_unidade") != null
            ? ((Number) row.get("valor_compra_unidade")).doubleValue()
            : 0d;
    double vvAtual =
        row.get("valor_venda_unidade") != null
            ? ((Number) row.get("valor_venda_unidade")).doubleValue()
            : 0d;

    double vcNovo = valorCompra != null ? valorCompra : vcAtual;
    double vvNovo = valorVenda != null ? valorVenda : vvAtual;

    int novaQtd = qtdAtual + qtdMovimentada;

    String updateEstoque =
        """
                UPDATE estoque
                SET
                  qtd_total_unidades = ?,
                  valor_compra_unidade = ?,
                  valor_venda_unidade = ?,
                  fornecedor = COALESCE(?, fornecedor),
                  data_hora_ultima_reposicao = now()
                WHERE id = ?
                """;

    jdbcTemplate.update(updateEstoque, novaQtd, vcNovo, vvNovo, fornecedor, estoqueId);

    String insertHistEstoque =
        """
                INSERT INTO historico_estoque (
                  fk_estoque,
                  data_hora_reposicao,
                  qtd_total_unidades,
                  valor_compra_unidade,
                  valor_venda_unidade,
                  fk_funcionario
                ) VALUES (?, now(), ?, ?, ?, ?)
                """;

    jdbcTemplate.update(
        insertHistEstoque, estoqueId, qtdMovimentada, vcNovo, vvNovo, funcionarioId);
  }

  public void consumirEstoque(Long itemId, int qtdConsumir, Long funcionarioId) {
    String selectEstoque =
        """
                SELECT id, qtd_total_unidades, valor_compra_unidade, valor_venda_unidade
                FROM estoque
                WHERE fk_item = ?
                """;

    Map<String, Object> row = jdbcTemplate.queryForMap(selectEstoque, itemId);

    Long estoqueId = ((Number) row.get("id")).longValue();
    int qtdAtual = ((Number) row.get("qtd_total_unidades")).intValue();
    double vcAtual =
        row.get("valor_compra_unidade") != null
            ? ((Number) row.get("valor_compra_unidade")).doubleValue()
            : 0d;
    double vvAtual =
        row.get("valor_venda_unidade") != null
            ? ((Number) row.get("valor_venda_unidade")).doubleValue()
            : 0d;

    if (qtdConsumir > qtdAtual) {
      throw new IllegalArgumentException("Quantidade em estoque insuficiente.");
    }

    int novaQtd = qtdAtual - qtdConsumir;

    String updateEstoque =
        """
                UPDATE estoque
                SET qtd_total_unidades = ?, data_hora_ultima_reposicao = now()
                WHERE id = ?
                """;

    jdbcTemplate.update(updateEstoque, novaQtd, estoqueId);

    String insertHistEstoque =
        """
                INSERT INTO historico_estoque (
                  fk_estoque,
                  data_hora_reposicao,
                  qtd_total_unidades,
                  valor_compra_unidade,
                  valor_venda_unidade,
                  fk_funcionario
                ) VALUES (?, now(), ?, ?, ?, ?)
                """;

    jdbcTemplate.update(
        insertHistEstoque, estoqueId, -qtdConsumir, vcAtual, vvAtual, funcionarioId);
  }

  public Long criarCategoria(String categoria, String descricao) {
    String sql =
        """
                INSERT INTO categoria_item (categoria, descricao, data_registro_categoria)
                VALUES (?, ?, now())
                RETURNING id
                """;
    return jdbcTemplate.queryForObject(sql, Long.class, categoria.trim(), descricao);
  }

  public void atualizarCategoria(Long id, String categoria, String descricao) {
    String sql =
        """
                UPDATE categoria_item
                SET categoria = ?, descricao = ?
                WHERE id = ?
                """;
    jdbcTemplate.update(sql, categoria.trim(), descricao, id);
  }

  public List<HistoricoPrecoItem> listarHistoricoPrecoPorItemId(Long itemId) {
    String sql =
        """
                SELECT
                  hpi.id                   AS id,
                  hpi.data_hora_registro   AS data_hora_registro,
                  hpi.valor_compra_unidade AS valor_compra_unidade,
                  hpi.valor_venda_unidade  AS valor_venda_unidade,
                  f.id                     AS funcionario_id,
                  p.nome                   AS funcionario_nome
                FROM historico_preco_item hpi
                LEFT JOIN funcionario f ON f.id = hpi.fk_funcionario
                LEFT JOIN pessoa p ON p.id = f.fk_pessoa
                WHERE hpi.fk_item = ?
                ORDER BY hpi.data_hora_registro DESC, hpi.id DESC
                """;

    return jdbcTemplate.query(
        sql,
        (rs, rowNum) ->
            new HistoricoPrecoItem(
                rs.getLong("id"),
                rs.getTimestamp("data_hora_registro").toLocalDateTime(),
                rs.getObject("valor_compra_unidade") != null
                    ? rs.getDouble("valor_compra_unidade")
                    : null,
                rs.getObject("valor_venda_unidade") != null
                    ? rs.getDouble("valor_venda_unidade")
                    : null,
                rs.getObject("funcionario_id") != null ? rs.getLong("funcionario_id") : null,
                rs.getString("funcionario_nome")),
        itemId);
  }

  public ItemBuscaCompleta buscarCompleto(LocalDate dataInicioCadastro, LocalDate dataFimCadastro) {
    String whereItens = " WHERE 1=1 ";
    List<Object> paramsItens = new ArrayList<>();

    if (dataInicioCadastro != null) {
      whereItens += " AND i.data_hora_registro_item >= ? ";
      paramsItens.add(Timestamp.valueOf(dataInicioCadastro.atStartOfDay()));
    }
    if (dataFimCadastro != null) {
      whereItens += " AND i.data_hora_registro_item < ? ";
      paramsItens.add(Timestamp.valueOf(dataFimCadastro.plusDays(1).atStartOfDay()));
    }

    String sqlItensCategorias =
        """
                SELECT
                  c.id                  AS categoria_id,
                  c.categoria           AS categoria,
                  c.descricao           AS categoria_descricao,
                  i.id                  AS item_id,
                  i.descricao           AS item_descricao,
                  e.qtd_total_unidades  AS estoque_qtd_total_unidades,
                  e.valor_compra_unidade AS estoque_valor_compra_unidade,
                  e.valor_venda_unidade  AS estoque_valor_venda_unidade,
                  h.data_hora_registro  AS data_hora_registro
                FROM categoria_item c
                LEFT JOIN item i ON i.fk_categoria = c.id
                LEFT JOIN estoque e ON e.fk_item = i.id
                LEFT JOIN LATERAL (
                    SELECT  hpi.data_hora_registro
                    FROM historico_preco_item hpi
                    WHERE hpi.fk_item = i.id
                    ORDER BY hpi.data_hora_registro DESC, hpi.id DESC
                    LIMIT 1
                ) h ON TRUE
                """
            + whereItens
            + """
        ORDER BY c.categoria, i.descricao
        """;

    Map<Long, List<ItemResponse>> itensPorCategoria = new LinkedHashMap<>();
    Map<Long, String> categoriaNome = new HashMap<>();
    Map<Long, String> categoriaDescricao = new HashMap<>();

    jdbcTemplate.query(
        sqlItensCategorias,
        rs -> {
          Long catId = rs.getObject("categoria_id", Long.class);
          if (catId == null) return;

          categoriaNome.put(catId, rs.getString("categoria"));
          categoriaDescricao.put(catId, rs.getString("categoria_descricao"));

          Long itemId = rs.getObject("item_id", Long.class);
          if (itemId != null) {
            ItemResponse item = ItemResponse.mapItem(rs);
            itensPorCategoria.computeIfAbsent(catId, k -> new ArrayList<>()).add(item);
          } else {
            itensPorCategoria.computeIfAbsent(catId, k -> new ArrayList<>());
          }
        },
        paramsItens.toArray());

    int totalCategorias = categoriaNome.size();
    int totalItens = 0;
    double valorTotalInvestido = 0d;
    double valorTotalVenda = 0d;
    int itensComAtencao = 0;

    List<ItemBuscaCompleta.InfoCategorias> categoriasInfo = new ArrayList<>();

    for (Map.Entry<Long, List<ItemResponse>> entry : itensPorCategoria.entrySet()) {
      Long catId = entry.getKey();
      List<ItemResponse> itens = entry.getValue();

      int totalItensCategoria = itens.size();
      double totalInvestidoCategoria = 0d;
      double totalVendaCategoria = 0d;

      for (ItemResponse item : itens) {
        int qtd = item.quantidadeTotal() != null ? item.quantidadeTotal() : 0;
        double vc = item.valorCompraUnidade() != null ? item.valorCompraUnidade() : 0d;
        double vv = item.valorVendaUnidade() != null ? item.valorVendaUnidade() : 0d;

        totalInvestidoCategoria += qtd * vc;
        totalVendaCategoria += qtd * vv;

        totalItens++;
        if (qtd < 10) itensComAtencao++;
      }

      valorTotalInvestido += totalInvestidoCategoria;
      valorTotalVenda += totalVendaCategoria;

      float lucroCategoria = (float) (totalVendaCategoria - totalInvestidoCategoria);

      categoriasInfo.add(
          new ItemBuscaCompleta.InfoCategorias(
              catId,
              categoriaNome.get(catId),
              categoriaDescricao.get(catId),
              totalItensCategoria,
              (float) totalInvestidoCategoria,
              (float) totalVendaCategoria,
              lucroCategoria,
              itens));
    }

    float lucroTotal = (float) (valorTotalVenda - valorTotalInvestido);

    ItemBuscaCompleta.DashBoard dash =
        new ItemBuscaCompleta.DashBoard(
            totalCategorias,
            totalItens,
            (float) valorTotalInvestido,
            (float) valorTotalVenda,
            lucroTotal,
            itensComAtencao);
    return new ItemBuscaCompleta(dash, categoriasInfo);
  }

  public List<ItemCategoria> listarCategorias() {
    String sql =
        """
            SELECT
              c.id                       AS categoria_id,
              c.categoria                AS categoria_categoria,
              c.descricao                AS categoria_descricao,
              c.data_registro_categoria  AS categoria_data_registro_categoria
            FROM categoria_item c
            ORDER BY c.categoria
            """;

    return jdbcTemplate.query(
        sql, (rs, rowNum) -> ItemCategoria.mapItemCategoria(rs, "categoria_"));
  }
}
