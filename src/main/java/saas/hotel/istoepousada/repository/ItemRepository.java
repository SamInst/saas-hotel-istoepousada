package saas.hotel.istoepousada.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.CategoriaItem;
import saas.hotel.istoepousada.dto.Item;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;

@Repository
public class ItemRepository {

  private final JdbcTemplate jdbcTemplate;

  public ItemRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public Page<Item> buscar(
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

    if (dataInicioCadastro != null) {
      where.append(" AND i.data_hora_registro_item >= ? ");
      params.add(Timestamp.valueOf(dataInicioCadastro.atStartOfDay()));
    }

    if (dataFimCadastro != null) {
      where.append(" AND i.data_hora_registro_item < ? ");
      params.add(Timestamp.valueOf(dataFimCadastro.plusDays(1).atStartOfDay()));
    }

    String countSql = "SELECT COUNT(*) " + baseFrom + where;

    Long total;
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

    String pageSql =
        baseSelect + " WHERE i.id IN (" + inPlaceholders + ") ORDER BY i.descricao ASC";

    List<Item> content = jdbcTemplate.query(pageSql, Item.ROW_MAPPER, ids.toArray());

    return new PageImpl<>(content, pageable, total);
  }

  public Item findById(Long id) {
    Page<Item> page = buscar(id, null, null, null, null, Pageable.ofSize(1));
    if (page.isEmpty()) {
      throw new NotFoundException("Item não encontrado para o id: " + id);
    }
    return page.getContent().getFirst();
  }

  @Transactional
  public Item insert(Item.Request request) {
    String sql =
        """
            INSERT INTO item (descricao, fk_categoria, data_hora_registro)
            VALUES (?, ?, now())
            """;

    KeyHolder keyHolder = new GeneratedKeyHolder();

    jdbcTemplate.update(
        connection -> {
          PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
          ps.setString(1, request.descricao().trim());
          ps.setLong(2, request.categoria_item().id());
          return ps;
        },
        keyHolder);

    Number generated = keyHolder.getKey();
    if (generated == null) {
      throw new IllegalStateException("Falha ao obter ID gerado do item");
    }

    return findById(generated.longValue());
  }

  @Transactional
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

  public List<Item.HistoricoPreco> listarHistoricoPrecoPorItemId(Long itemId) {
    String sql =
        """
            SELECT
              hpi.id                   AS historico_preco_id,
              hpi.data_hora_registro   AS historico_preco_data_hora_registro,
              hpi.valor_compra_unidade AS historico_preco_valor_compra_unidade,
              hpi.valor_venda_unidade  AS historico_preco_valor_venda_unidade,
              f.id                     AS funcionario_id
            FROM historico_preco_item hpi
            LEFT JOIN funcionario f ON f.id = hpi.fk_funcionario
            WHERE hpi.fk_item = ?
            ORDER BY hpi.data_hora_registro DESC, hpi.id DESC
            """;

    return jdbcTemplate.query(sql, Item.HistoricoPreco.ROW_MAPPER, itemId);
  }

  public List<Item.HistoricoReposicao> listarHistoricoReposicaoPorItemId(Long itemId) {
    String sql =
        """
            SELECT
              he.id                   AS historico_reposicao_id,
              he.data_hora_reposicao  AS historico_reposicao_data_hora_registro,
              e.fornecedor            AS historico_reposicao_fornecedor,
              he.qtd_total_unidades   AS historico_reposicao_quantidade_unidades,
              f.id                    AS funcionario_id,
              p.nome                  AS funcionario_nome
            FROM historico_estoque he
            JOIN estoque e ON e.id = he.fk_estoque
            LEFT JOIN funcionario f ON f.id = he.fk_funcionario
            LEFT JOIN pessoa p ON p.id = f.fk_pessoa
            WHERE e.fk_item = ?
            ORDER BY he.data_hora_reposicao DESC, he.id DESC
            """;

    return jdbcTemplate.query(sql, Item.HistoricoReposicao.ROW_MAPPER, itemId);
  }

  @Transactional
  public void registrarHistoricoPreco(Item.HistoricoPreco.Request request) {
    jdbcTemplate.update(
        """
            INSERT INTO historico_preco_item (
                data_hora_registro,
                fk_item,
                valor_compra_unidade,
                valor_venda_unidade,
                fk_funcionario
            ) VALUES (now(), ?, ?, ?, ?)
            """,
        request.item().id(),
        request.valor_compra_unidade(),
        request.valor_venda_unidade(),
        request.funcionario().id());
  }

  @Transactional
  public void registrarHistoricoReposicao(Item.HistoricoReposicao.Request request) {
    Long estoqueId =
        jdbcTemplate.queryForObject(
            "SELECT id FROM estoque WHERE fk_item = ?", Long.class, request.item().id());

    if (estoqueId == null) {
      throw new NotFoundException("Estoque não encontrado para o item: " + request.item().id());
    }

    jdbcTemplate.update(
        """
            INSERT INTO historico_estoque (
              fk_estoque,
              data_hora_reposicao,
              qtd_total_unidades,
              fk_funcionario
            ) VALUES (?, now(), ?, ?)
            """,
        estoqueId,
        request.quantidade_unidades(),
        request.funcionario().id());

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

  @Transactional
  public void atualizarHistoricoReposicao(Item.HistoricoReposicao.Update request) {
    String buscarSql =
        """
            SELECT
              he.id AS historico_id,
              he.fk_estoque AS estoque_id,
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
        request.funcionario().id(),
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

  @Transactional
  public Long criarCategoria(CategoriaItem.Request request) {
    String sql =
        """
            INSERT INTO categoria_item (categoria, descricao, data_registro_categoria)
            VALUES (?, ?, now())
            RETURNING id
            """;

    return jdbcTemplate.queryForObject(sql, Long.class, request.nome().trim(), request.descricao());
  }

  @Transactional
  public void atualizarCategoria(Long id, CategoriaItem.Request request) {
    int rows =
        jdbcTemplate.update(
            """
                    UPDATE categoria_item
                    SET categoria = ?, descricao = ?
                    WHERE id = ?
                    """,
            request.nome().trim(),
            request.descricao(),
            id);

    if (rows == 0) {
      throw new NotFoundException("Categoria não encontrada para o id: " + id);
    }
  }

  public List<CategoriaItem> listarCategorias() {
    String sql =
        """
            SELECT
              c.id                       AS categoria_id,
              NULL                       AS categoria_funcionario_id,
              NULL                       AS categoria_funcionario_nome,
              c.categoria                AS categoria_nome,
              c.descricao                AS categoria_descricao,
              c.data_registro_categoria  AS categoria_data_hora_registro
            FROM categoria_item c
            ORDER BY c.categoria
            """;

    return jdbcTemplate.query(sql, CategoriaItem.ROW_MAPPER);
  }

  public CategoriaItem findCategoriaById(Long id) {
    String sql =
        """
            SELECT
              c.id                       AS categoria_id,
              NULL                       AS categoria_funcionario_id,
              NULL                       AS categoria_funcionario_nome,
              c.categoria                AS categoria_nome,
              c.descricao                AS categoria_descricao,
              c.data_registro_categoria  AS categoria_data_hora_registro
            FROM categoria_item c
            WHERE c.id = ?
            """;

    try {
      return jdbcTemplate.queryForObject(sql, CategoriaItem.ROW_MAPPER, id);
    } catch (EmptyResultDataAccessException ex) {
      throw new NotFoundException("Categoria não encontrada para o id: " + id);
    }
  }
}
