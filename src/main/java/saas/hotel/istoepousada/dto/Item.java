package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.RowMapper;

public record Item(@NotNull Long id, String descricao) {

  public record Id(@NotNull Long id) {}

  public record Request(CategoriaItem.Id categoria_item, String descricao) {}

  public record Update(
      @NotNull Long id, @NotNull CategoriaItem.Id categoria_item, @NotNull String descricao) {}

  public record HistoricoEstoque(
      @NotNull Long id,
      @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_registro,
      String fornecedor,
      @NotNull Funcionario.Nome funcionario,
      Integer quantidade_unidades,
      Float valor_compra_unidade,
      Float valor_venda_unidade) {
    public record Request(
        @NotNull Item.Id item,
        String fornecedor,
        @NotNull Integer quantidade_unidades,
        @NotNull Float valor_compra_unidade,
        @NotNull Float valor_venda_unidade) {}

    public record Update(
        @NotNull Long id, @NotNull Integer quantidade_unidades, String fornecedor) {}

    public record Estoque(List<CategoriaItem> categorias) {
      public record CategoriaItem(
          Long id, String nome, String descricao, Dashboard dashboard, Object itens) {
        public record Dashboard(
            Integer quantidade_itens, Float valor_investido, Float lucro_potencial) {}

        public record ItemEstoque(
            Long id, String descricao, Integer quantidade, Float valor_venda) {}
      }
    }

    public static final RowMapper<HistoricoEstoque> ROW_MAPPER =
        (rs, rowNum) ->
            new HistoricoEstoque(
                rs.getLong("historico_reposicao_id"),
                rs.getObject("historico_reposicao_data_hora_registro", LocalDateTime.class),
                rs.getString("historico_reposicao_fornecedor"),
                new Funcionario.Nome(
                    rs.getLong("historico_reposicao_funcionario_id"),
                    rs.getString("historico_reposicao_funcionario_nome")),
                rs.getInt("historico_reposicao_quantidade_unidades"),
                rs.getFloat("historico_reposicao_valor_compra_unidade"),
                rs.getFloat("historico_reposicao_valor_venda_unidade"));
  }

  public record ReporEstoque(
      Item.Id item,
      Integer quantidade_total_unidades,
      Float valor_compra_unidade,
      Float valor_venda_unidade,
      String fornecedor) {}

  public record Consumo(
      @NotNull Long id,
      @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_registro,
      Pagamento pagamento,
      @NotNull Item item,
      @NotNull Funcionario.Nome funcionario,
      @NotNull Float quantidade,
      Quarto.Descricao quarto,
      Boolean despesa_pessoal,
      Boolean cancelado) {

    public record Request(
        Pagamento.Request pagamento,
        @NotNull Item.Id item,
        @NotNull Float quantidade,
        Boolean despesa_pessoal,
        Quarto.Id quarto) {}

    public record Update(@NotNull Long id, @NotNull Float quantidade) {}

    public static final RowMapper<Consumo> ROW_MAPPER =
        (rs, rowNum) -> {
          Long quartoId = rs.getObject("consumo_quarto_id", Long.class);
          Quarto.Descricao quarto =
              quartoId == null
                  ? null
                  : new Quarto.Descricao(quartoId, rs.getString("consumo_quarto_descricao"));
          return new Consumo(
              rs.getLong("consumo_id"),
              rs.getObject("consumo_data_hora_registro", LocalDateTime.class),
              Pagamento.ROW_MAPPER.mapRow(rs, rowNum),
              new Item(rs.getLong("consumo_item_id"), rs.getString("consumo_item_descricao")),
              new Funcionario.Nome(
                  rs.getLong("consumo_funcionario_id"), rs.getString("consumo_funcionario_nome")),
              rs.getFloat("consumo_quantidade"),
              quarto,
              rs.getObject("consumo_despesa_pessoal", Boolean.class),
              rs.getObject("consumo_cancelado", Boolean.class));
        };
  }

  public static final RowMapper<Item> ROW_MAPPER =
      (rs, rowNum) -> new Item(rs.getLong("item_id"), rs.getString("item_descricao"));
}
