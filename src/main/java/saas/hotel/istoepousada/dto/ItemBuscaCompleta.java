package saas.hotel.istoepousada.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record ItemBuscaCompleta(
    @Schema(description = "Informacoes para amostragem do dashboard") DashBoard dashBoard,
    @Schema(description = "Informacoes para amostragem das categorias")
        List<InfoCategorias> categorias) {
  public record DashBoard(
      @Schema(description = "Quantidade total de categorias ") Integer categorias,
      @Schema(description = "Quantidade total de itens cadastrados") Integer totalDeItens,
      @Schema(description = "Soma total do valor de compra dos itens") Float valorTotalInvestido,
      @Schema(description = "Soma total do valor de venda dos itens") Float valorTotalVenda,
      @Schema(description = "Valor de venda - Valor investido") Float lucro,
      @Schema(description = "Itens com quantidade menor que 10") Integer itensComAtencao) {}

  public record InfoCategorias(
      @Schema(description = "Id da categoria") Long id,
      @Schema(description = "Nome da categoria") String categoria,
      @Schema(description = "Descricao da utilidade da categoria") String descricao,
      @Schema(description = "Total de itens que a categoria possui") Integer totalDeItens,
      @Schema(description = "Soma total do valor de compra dos itens para esta categoria")
          Float valorTotalInvestido,
      @Schema(description = "Soma total do valor de venda dos itens para esta categoria")
          Float valorTotalVenda,
      @Schema(description = "Valor de venda - Valor investido para esta categoria") Float lucro,
      @Schema(description = "Lista de itens dessa categoria") List<ItemResponse> itens) {}
}
