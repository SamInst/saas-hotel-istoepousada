package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Histórico de preço por item")
public record HistoricoReposicaoItem(
    @Schema(description = "Soma total do valor de compra do item") Float valorTotalInvestido,
    @Schema(description = "Soma total do valor de venda do item") Float valorTotalVenda,
    @Schema(description = "Valor de venda - Valor investido total") Float lucro,
    List<ItemReposicao> itemReposicaoList) {
  public record ItemReposicao(
      Long id,
      @Schema(description = "Data/hora do registro") @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
          LocalDateTime dataHoraRegistro,
      @Schema(description = "Valor de compra por unidade") Double valorCompraUnidade,
      @Schema(description = "Valor de venda por unidade") Double valorVendaUnidade,
      @Schema(description = "Fornecedor") String fornecedor,
      @Schema(description = "ID do funcionário responsável") Long funcionarioId,
      @Schema(description = "Nome do funcionário responsável") String funcionarioNome,
      @Schema(description = "Quantidade de unidades do item") Integer qtdUnidades,
      @Schema(description = "Soma total do valor de compra do item desta reposição")
          Float valorTotalInvestido,
      @Schema(description = "Soma total do valor de venda do item desta reposição")
          Float valorTotalVenda,
      @Schema(description = "Valor de venda - Valor investido desta reposição") Float lucro) {}
}
