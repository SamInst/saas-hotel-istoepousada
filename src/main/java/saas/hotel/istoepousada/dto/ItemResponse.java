package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

@Schema(description = "Item de estoque")
public record ItemResponse(
    Long id,
    String descricao,
    Integer quantidadeTotal,
    Double valorCompraUnidade,
    Double valorVendaUnidade,
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime dataHoraRegistro) {

  public static ItemResponse mapItem(ResultSet rs) throws SQLException {
    LocalDateTime ultimaReposicao =
        rs.getTimestamp("data_hora_registro") != null
            ? rs.getTimestamp("data_hora_registro").toLocalDateTime()
            : null;

    return new ItemResponse(
        rs.getLong("item_id"),
        rs.getString("item_descricao"),
        rs.getObject("estoque_qtd_total_unidades", Integer.class),
        rs.getObject("estoque_valor_compra_unidade", Double.class),
        rs.getObject("estoque_valor_venda_unidade", Double.class),
        ultimaReposicao);
  }

  @Schema(description = "Request para criar/atualizar item")
  public record ItemRequest(
      String descricao,
      Long categoriaId,
      Integer quantidadeTotal,
      Double valorCompraUnidade,
      Double valorVendaUnidade,
      String fornecedor) {}
}
