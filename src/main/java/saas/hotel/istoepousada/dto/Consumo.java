package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record Consumo(
    @NotNull Long id,
    @NotNull Funcionario.Nome funcionario,
    @NotNull Pagamento pagamento,
    @NotNull Item item,
    @NotNull Integer quantidade,
    @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_registro) {
  public record Request(
      Long fk_hospedagem,
      @NotNull Long id,
      Pagamento.Request pagamento,
      @NotNull Item.Id item,
      @NotNull Integer quantidade) {}
}
