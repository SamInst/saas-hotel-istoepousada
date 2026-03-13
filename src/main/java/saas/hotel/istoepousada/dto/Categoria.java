package saas.hotel.istoepousada.dto;

import jakarta.validation.constraints.NotNull;
import saas.hotel.istoepousada.dto.enums.ModeloCobranca;
import saas.hotel.istoepousada.dto.enums.ModeloOperacao;

public record Categoria(
    @NotNull Long id,
    @NotNull String nome,
    @NotNull String descricao,
    @NotNull ModeloCobranca modelo_cobranca,
    @NotNull ModeloOperacao modelo_operacao) {
  public record Id(@NotNull Long id) {}

  public record Request(
      @NotNull String categoria,
      @NotNull String descricao,
      @NotNull ModeloCobranca modelo_cobranca,
      @NotNull ModeloOperacao modelo_operacao) {}

  public record Update(
      @NotNull Long id,
      @NotNull String categoria,
      String descricao,
      @NotNull ModeloCobranca modelo_cobranca,
      @NotNull ModeloOperacao modelo_operacao) {}
}
