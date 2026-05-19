package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record MotivoCancelamentoHospedagem(
    @NotNull Long id,
    @NotNull String motivo_cancelamento,
    Funcionario.Nome funcionario,
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_registro) {
  public record Request(
      @NotNull Long fk_hospedagem,
      @NotNull @NotNull Long id,
      @NotNull String motivo_cancelamento) {}
}
