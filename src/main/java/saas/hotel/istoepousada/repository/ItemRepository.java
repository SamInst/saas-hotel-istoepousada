package saas.hotel.istoepousada.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import saas.hotel.istoepousada.dto.CategoriaItem;
import saas.hotel.istoepousada.dto.Item;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;

@Repository
public class ItemRepository {

  private final JdbcTemplate jdbcTemplate;

  public ItemRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public record ConsumoParaCancelamento(UUID pagamentoId, Long itemId, Long quartoId, Float quantidade) {}

  public Page<Item> buscar(Long id, String termo, Long categoriaId, Pageable pageable) {

    boolean hasId = id != null;
    boolean hasTermo = termo != null && !termo.trim().isEmpty();
    boolean hasCategoriaId = categoriaId != null;

    String termoTrim = hasTermo ? termo.trim() : null;
    String search = hasTermo ? "%" + termoTrim + "%" : null;

    String baseFrom =
        """
                        FROM item i
                        INNER JOIN categoria_item c ON c.id = i.fk_categoria
                        """;

    String baseSelect =
        """
                        SELECT
                            i.id          AS item_id,
                            i.descricao   AS item_descricao
                        """
            + baseFrom;

    StringBuilder where = new StringBuilder(" WHERE 1 = 1 ");
    List<Object> params = new java.util.ArrayList<>();

    if (hasId) {
      where.append(" AND i.id = ? ");
      params.add(id);
    }

    if (hasTermo) {
      where.append(" AND (i.descricao ILIKE ? OR c.categoria ILIKE ?) ");
      params.add(search);
      params.add(search);
    }

    if (hasCategoriaId) {
      where.append(" AND i.fk_categoria = ? ");
      params.add(categoriaId);
    }

    String countSql = "SELECT COUNT(*) " + baseFrom + where;

    long total;
    try {
      total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
    } catch (EmptyResultDataAccessException ex) {
      total = 0L;
    }

    if (total == 0) {
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

    List<Object> idsParams = new java.util.ArrayList<>(params);
    idsParams.add(pageable.getPageSize());
    idsParams.add(pageable.getOffset());

    List<Long> ids =
        jdbcTemplate.query(idsSql, (rs, rowNum) -> rs.getLong("id"), idsParams.toArray());

    if (ids.isEmpty()) {
      return new PageImpl<>(List.of(), pageable, total);
    }

    String inPlaceholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));

    String pageSql = baseSelect + " WHERE i.id IN (" + inPlaceholders + ") ORDER BY i.descricao ";

    List<Item> content = jdbcTemplate.query(pageSql, Item.ROW_MAPPER, ids.toArray());

    return new PageImpl<>(content, pageable, total);
  }

  public Item findById(Long id) {
    Page<Item> page = buscar(id, null, null, Pageable.ofSize(1));
    if (page.isEmpty()) {
      throw new NotFoundException("Item não encontrado para o id: " + id);
    }
    return page.getContent().getFirst();
  }

  public String buscarDescricaoItem(Long itemId) {
    return jdbcTemplate.queryForObject(
        "SELECT descricao FROM item WHERE id = ?", String.class, itemId);
  }

  public void inserirConsumo(
      UUID pagamentoId,
      Long itemId,
      Long funcionarioId,
      Integer quantidade,
      boolean despesaPessoal,
      Long quartoId) {
    jdbcTemplate.update(
        """
                INSERT INTO consumo (fk_pagamento, fk_item, fk_funcionario, quantidade, despesa_pessoal, fk_quarto)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
        pagamentoId,
        itemId,
        funcionarioId,
        quantidade,
        despesaPessoal,
        quartoId);
  }

  public Integer buscarQuantidadeQuartoItem(Long quartoId, Long itemId) {
    try {
      return jdbcTemplate.queryForObject(
          """
                  SELECT quantidade_atual FROM quarto_item
                  WHERE fk_quarto = ? AND fk_item = ?
                  """,
          Integer.class,
          quartoId,
          itemId);
    } catch (EmptyResultDataAccessException ignored) {
      return null;
    }
  }

  public void descontarQuartoItem(Long quartoId, Long itemId, Integer quantidade) {
    jdbcTemplate.update(
        """
                UPDATE quarto_item
                SET quantidade_atual = quantidade_atual - ?
                WHERE fk_quarto = ? AND fk_item = ?
                """,
        quantidade,
        quartoId,
        itemId);
  }

  public void descontarEstoquePorItem(Long itemId, Integer quantidade) {
    jdbcTemplate.update(
        """
                UPDATE estoque
                SET qtd_total_unidades = qtd_total_unidades - ?
                WHERE fk_item = ?
                """,
        quantidade,
        itemId);
  }

  public ConsumoParaCancelamento buscarConsumoParaCancelamento(Long consumoId) {
    var row =
        jdbcTemplate.queryForMap(
            """
                SELECT fk_pagamento, fk_item, fk_quarto, quantidade
                FROM consumo
                WHERE id = ?
                """,
            consumoId);
    return new ConsumoParaCancelamento(
        (UUID) row.get("fk_pagamento"),
        ((Number) row.get("fk_item")).longValue(),
        row.get("fk_quarto") != null ? ((Number) row.get("fk_quarto")).longValue() : null,
        ((Number) row.get("quantidade")).floatValue());
  }

  public int incrementarQuartoItem(Long quartoId, Long itemId, Float quantidade) {
    return jdbcTemplate.update(
        """
                UPDATE quarto_item
                SET quantidade_atual = quantidade_atual + ?
                WHERE fk_quarto = ? AND fk_item = ?
                """,
        quantidade,
        quartoId,
        itemId);
  }

  public void incrementarEstoquePorItem(Long itemId, Float quantidade) {
    jdbcTemplate.update(
        """
                UPDATE estoque
                SET qtd_total_unidades = qtd_total_unidades + ?
                WHERE fk_item = ?
                """,
        quantidade,
        itemId);
  }

  public void marcarConsumoCancelado(Long consumoId) {
    jdbcTemplate.update("UPDATE consumo SET cancelado = true WHERE id = ?", consumoId);
  }

  public List<Item.Consumo> listarConsumos(Pageable pageable) {
    String sql =
        """
                SELECT
                  c.id                    AS consumo_id,
                  c.data_hora_registro    AS consumo_data_hora_registro,
                  c.quantidade            AS consumo_quantidade,
                  c.despesa_pessoal       AS consumo_despesa_pessoal,
                  c.cancelado             AS consumo_cancelado,
                  i.id                    AS consumo_item_id,
                  i.descricao             AS consumo_item_descricao,
                  fc.id                   AS consumo_funcionario_id,
                  pc.nome                 AS consumo_funcionario_nome,
                  q.id                    AS consumo_quarto_id,
                  q.descricao             AS consumo_quarto_descricao,
                  p.id                    AS pagamento_id,
                  p.data_hora_registro    AS pagamento_data_hora_registro,
                  p.nome_pagador          AS pagamento_nome_pagador,
                  p.descricao             AS pagamento_descricao,
                  p.valor                 AS pagamento_valor,
                  p.cancelado             AS pagamento_cancelado,
                  p.path_arquivo          AS pagamento_path_arquivo,
                  tp.id                   AS tipo_pagamento_id,
                  tp.descricao            AS tipo_pagamento_descricao,
                  fp.id                   AS pagamento_funcionario_id,
                  pp.nome                 AS pagamento_funcionario_nome,
                  d.id                    AS pagamento_desconto_id,
                  d.fk_funcionario        AS pagamento_desconto_funcionario_id,
                  dpd.nome                AS pagamento_desconto_funcionario_nome,
                  d.porcentagem           AS pagamento_desconto_porcentagem,
                  d.valor                 AS pagamento_desconto_valor,
                  d.data_hora_registro    AS pagamento_desconto_data_hora_registro
                FROM consumo c
                JOIN item i ON i.id = c.fk_item
                JOIN funcionario fc ON fc.id = c.fk_funcionario
                JOIN pessoa pc ON pc.id = fc.fk_pessoa
                LEFT JOIN quarto q ON q.id = c.fk_quarto
                LEFT JOIN pagamento p ON p.id = c.fk_pagamento
                LEFT JOIN tipo_pagamento tp ON tp.id = p.fk_tipo_pagamento
                LEFT JOIN funcionario fp ON fp.id = p.fk_funcionario
                LEFT JOIN pessoa pp ON pp.id = fp.fk_pessoa
                LEFT JOIN LATERAL (
                  SELECT *
                  FROM pagamento_desconto d
                  WHERE d.fk_pagamento = p.id
                  ORDER BY d.data_hora_registro DESC
                  LIMIT 1
                ) d ON true
                LEFT JOIN funcionario df ON df.id = d.fk_funcionario
                LEFT JOIN pessoa dpd ON dpd.id = df.fk_pessoa
                ORDER BY c.data_hora_registro DESC
                LIMIT ? OFFSET ?
                """;

    return jdbcTemplate.query(
        sql, Item.Consumo.ROW_MAPPER, pageable.getPageSize(), pageable.getOffset());
  }

  public Item.HistoricoEstoque.Estoque listarEstoque() {
    String sql =
        """
                        SELECT
                          c.id                       AS categoria_id,
                          c.categoria                AS categoria_nome,
                          c.descricao                AS categoria_descricao,
                          i.id                       AS item_id,
                          i.descricao                AS item_descricao,
                          e.qtd_total_unidades        AS item_quantidade,
                          ultimo.qtd_total_unidades   AS ultimo_quantidade,
                          ultimo.valor_venda_unidade  AS item_valor_venda,
                          COALESCE(ultimo.qtd_total_unidades * ultimo.valor_compra_unidade, 0) AS valor_investido,
                          COALESCE(ultimo.qtd_total_unidades * (ultimo.valor_venda_unidade - ultimo.valor_compra_unidade), 0) AS lucro_potencial
                        FROM categoria_item c
                        LEFT JOIN item i ON i.fk_categoria = c.id
                        LEFT JOIN estoque e ON e.fk_item = i.id
                        LEFT JOIN LATERAL (
                          SELECT qtd_total_unidades, valor_compra_unidade, valor_venda_unidade
                          FROM historico_estoque
                          WHERE fk_estoque = e.id
                          ORDER BY data_hora_reposicao DESC, id DESC
                          LIMIT 1
                        ) ultimo ON true
                        ORDER BY c.categoria, i.descricao
                        """;

    List<java.util.Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

    var categorias =
        rows.stream()
            .collect(
                java.util.stream.Collectors.groupingBy(
                    r -> ((Number) r.get("categoria_id")).longValue(),
                    java.util.LinkedHashMap::new,
                    java.util.stream.Collectors.toList()))
            .values()
            .stream()
            .map(
                maps -> {
                  var first = maps.getFirst();

                  var itens =
                      maps.stream()
                          .filter(row -> row.get("item_id") != null)
                          .map(
                              row ->
                                  new Item.HistoricoEstoque.Estoque.CategoriaItem.ItemEstoque(
                                      ((Number) row.get("item_id")).longValue(),
                                      (String) row.get("item_descricao"),
                                      row.get("item_quantidade") != null
                                          ? ((Number) row.get("item_quantidade")).intValue()
                                          : 0,
                                      row.get("item_valor_venda") != null
                                          ? ((Number) row.get("item_valor_venda")).floatValue()
                                          : 0f))
                          .toList();

                  var itensComEstoque =
                      maps.stream().filter(r -> r.get("item_id") != null).toList();
                  var dashboard =
                      new Item.HistoricoEstoque.Estoque.CategoriaItem.Dashboard(
                          itensComEstoque.stream()
                              .mapToInt(
                                  r ->
                                      r.get("item_quantidade") != null
                                          ? ((Number) r.get("item_quantidade")).intValue()
                                          : 0)
                              .sum(),
                          (float)
                              itensComEstoque.stream()
                                  .mapToDouble(
                                      r -> ((Number) r.get("valor_investido")).doubleValue())
                                  .sum(),
                          (float)
                              itensComEstoque.stream()
                                  .mapToDouble(
                                      r -> ((Number) r.get("lucro_potencial")).doubleValue())
                                  .sum());

                  return new Item.HistoricoEstoque.Estoque.CategoriaItem(
                      ((Number) first.get("categoria_id")).longValue(),
                      (String) first.get("categoria_nome"),
                      (String) first.get("categoria_descricao"),
                      dashboard,
                      itens);
                })
            .toList();

    return new Item.HistoricoEstoque.Estoque(categorias);
  }

  public Boolean estoqueExisteParaItem(Long itemId) {
    return jdbcTemplate.queryForObject(
        "SELECT EXISTS(SELECT 1 FROM estoque WHERE fk_item = ?)", Boolean.class, itemId);
  }

  public Item insert(Item.Request request) {
    var id =
        jdbcTemplate.queryForObject(
            """
                        INSERT INTO item (descricao, fk_categoria, data_hora_registro)
                        VALUES (?, ?, now())
                        returning id
                        """,
            Long.class,
            request.descricao().trim(),
            request.categoria_item().id());

    return findById(id);
  }

  public Item update(Item.Update request) {
    int rows =
        jdbcTemplate.update(
            """
                                UPDATE item
                                SET descricao = ?, fk_categoria = ?
                                WHERE id = ?
                                """,
            request.descricao().trim(),
            request.categoria_item().id(),
            request.id());

    if (rows == 0) {
      throw new NotFoundException("Item não encontrado para o id: " + request.id());
    }

    return findById(request.id());
  }

  public List<Item.HistoricoEstoque> listarHistoricoReposicaoPorItemId(Long itemId) {
    String sql =
        """
                        SELECT
                          he.id                   AS historico_reposicao_id,
                          he.data_hora_reposicao  AS historico_reposicao_data_hora_registro,
                          e.fornecedor            AS historico_reposicao_fornecedor,
                          he.qtd_total_unidades   AS historico_reposicao_quantidade_unidades,
                          f.id                    AS historico_reposicao_funcionario_id,
                          p.nome                  AS historico_reposicao_funcionario_nome,
                          he.valor_compra_unidade AS historico_reposicao_valor_compra_unidade,
                          he.valor_venda_unidade  AS historico_reposicao_valor_venda_unidade
                        FROM historico_estoque he
                        JOIN estoque e ON e.id = he.fk_estoque
                        LEFT JOIN funcionario f ON f.id = he.fk_funcionario
                        LEFT JOIN pessoa p ON p.id = f.fk_pessoa
                        WHERE e.fk_item = ?
                        ORDER BY he.data_hora_reposicao DESC, he.id DESC
                        """;

    return jdbcTemplate.query(sql, Item.HistoricoEstoque.ROW_MAPPER, itemId);
  }

  public Long insertEstoque(Item.ReporEstoque estoque) {
    return jdbcTemplate.queryForObject(
        """
                        insert into estoque (
                        qtd_total_unidades,
                        data_hora_ultima_reposicao,
                        fk_item,
                        valor_compra_unidade,
                        valor_venda_unidade,
                        fornecedor)
                        VALUES (?,now(), ?, ?, ?, ?)
                        RETURNING id
                        """,
        Long.class,
        estoque.quantidade_total_unidades(),
        estoque.item().id(),
        estoque.valor_compra_unidade(),
        estoque.valor_venda_unidade(),
        estoque.fornecedor());
  }

  public void retirarDoEstoque(Long item_id, Integer quantidade) {
    Long estoque_id =
        jdbcTemplate.queryForObject(
            "select id from estoque where fk_item = ?;", Long.class, item_id);
    if (estoque_id == null) {
      throw new NotFoundException("Estoque nao encontrado para o item: " + item_id);
    }
    jdbcTemplate.update(
        """
            update estoque
            set qtd_total_unidades = qtd_total_unidades - ?
            where id = ?
            """,
        quantidade,
        estoque_id);
  }

  public void registrarHistoricoReposicao(Item.HistoricoEstoque.Request request, Long funcionarioId) {
    Long estoqueId;
    try {
      estoqueId =
          jdbcTemplate.queryForObject(
              "SELECT id FROM estoque WHERE fk_item = ?", Long.class, request.item().id());
    } catch (EmptyResultDataAccessException e) {
      estoqueId =
          insertEstoque(
              new Item.ReporEstoque(
                  request.item(),
                  0,
                  request.valor_compra_unidade(),
                  request.valor_venda_unidade(),
                  request.fornecedor()));
    }

    jdbcTemplate.update(
        """
                        INSERT INTO historico_estoque (
                        fk_estoque,
                        data_hora_reposicao,
                        qtd_total_unidades,
                        valor_compra_unidade,
                        valor_venda_unidade,
                        fk_funcionario)
                        VALUES (?, now(), ?, ?, ?, ?);
                        """,
        estoqueId,
        request.quantidade_unidades(),
        request.valor_compra_unidade(),
        request.valor_venda_unidade(),
        funcionarioId);

    jdbcTemplate.update(
        """
                        UPDATE estoque
                        SET
                          qtd_total_unidades = COALESCE(qtd_total_unidades, 0) + ?,
                          fornecedor = COALESCE(?, fornecedor),
                          data_hora_ultima_reposicao = now()
                        WHERE id = ?
                        """,
        request.quantidade_unidades(),
        request.fornecedor(),
        estoqueId);
  }

  public void atualizarHistoricoEstoque(Item.HistoricoEstoque.Update request, Long funcionarioId) {
    String buscarSql =
        """
                        SELECT
                          he.id                 AS historico_id,
                          he.fk_estoque         AS estoque_id,
                          he.qtd_total_unidades AS quantidade_anterior
                        FROM historico_estoque he
                        WHERE he.id = ?
                        """;

    var row = jdbcTemplate.queryForMap(buscarSql, request.id());

    Long estoqueId = ((Number) row.get("estoque_id")).longValue();
    Integer quantidadeAnterior =
        row.get("quantidade_anterior") != null
            ? ((Number) row.get("quantidade_anterior")).intValue()
            : 0;

    jdbcTemplate.update(
        """
                        UPDATE historico_estoque
                        SET
                          qtd_total_unidades = ?,
                          fk_funcionario = ?
                        WHERE id = ?
                        """,
        request.quantidade_unidades(),
        funcionarioId,
        request.id());

    int diferenca = request.quantidade_unidades() - quantidadeAnterior;

    jdbcTemplate.update(
        """
                        UPDATE estoque
                        SET
                          qtd_total_unidades = COALESCE(qtd_total_unidades, 0) + ?,
                          fornecedor = COALESCE(?, fornecedor),
                          data_hora_ultima_reposicao = now()
                        WHERE id = ?
                        """,
        diferenca,
        request.fornecedor(),
        estoqueId);
  }

  public Long criarCategoria(CategoriaItem.Request request, Long funcionarioId) {
    return jdbcTemplate.queryForObject(
        """
                        INSERT INTO categoria_item (categoria, descricao, data_registro_categoria, fk_funcionario)
                        VALUES (?, ?, now(), ?)
                        RETURNING id
                        """,
        Long.class,
        request.nome().trim(),
        request.descricao(),
        funcionarioId);
  }

  public void atualizarCategoria(CategoriaItem.Update categoria, Long funcionarioId) {
    int rows =
        jdbcTemplate.update(
            """
                                UPDATE categoria_item
                                SET categoria = ?, descricao = ?, fk_funcionario = ?
                                WHERE id = ?
                                """,
            categoria.nome().trim(),
            categoria.descricao(),
            funcionarioId,
            categoria.id());

    if (rows == 0) {
      throw new NotFoundException("Categoria não encontrada para o id: " + categoria.id());
    }
  }

  public List<CategoriaItem> listarCategorias() {
    String sql =
        """
                        SELECT
                          c.id                       AS categoria_id,
                          f.id                       AS categoria_funcionario_id,
                          p.nome                     AS categoria_funcionario_nome,
                          c.categoria                AS categoria_nome,
                          c.descricao                AS categoria_descricao,
                          c.data_registro_categoria  AS categoria_data_hora_registro
                        FROM categoria_item c
                        LEFT JOIN funcionario f ON f.id = c.fk_funcionario
                        LEFT JOIN pessoa p ON p.id = f.fk_pessoa
                        ORDER BY c.categoria
                        """;

    return jdbcTemplate.query(sql, CategoriaItem.ROW_MAPPER);
  }

  public CategoriaItem findCategoriaById(Long id) {
    String sql =
        """
                        SELECT
                          c.id                       AS categoria_id,
                          f.id                       AS categoria_funcionario_id,
                          p.nome                     AS categoria_funcionario_nome,
                          c.categoria                AS categoria_nome,
                          c.descricao                AS categoria_descricao,
                          c.data_registro_categoria  AS categoria_data_hora_registro
                        FROM categoria_item c
                        LEFT JOIN funcionario f ON f.id = c.fk_funcionario
                        LEFT JOIN pessoa p ON p.id = f.fk_pessoa
                        WHERE c.id = ?
                        """;

    try {
      return jdbcTemplate.queryForObject(sql, CategoriaItem.ROW_MAPPER, id);
    } catch (EmptyResultDataAccessException ex) {
      throw new NotFoundException("Categoria não encontrada para o id: " + id);
    }
  }
}
