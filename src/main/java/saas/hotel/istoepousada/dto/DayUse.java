package saas.hotel.istoepousada.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record DayUse(
    @NotNull Long id,
    @NotNull Quarto quarto,
    @NotNull Funcionario funcionario,
    @NotNull LocalDateTime data_hora_registro,
    @NotNull LocalTime hora_inicio,
    @NotNull LocalTime hora_fim) {
  public record Request(@NotNull Quarto.Id quarto, @NotNull Funcionario.Id funcionario) {}

  public record Update(@NotNull Long id, @NotNull Quarto.Id quarto, @NotNull LocalTime hora_fim) {}

  public enum Status {
    ATIVO,
    CANCELADO,
    PAGAMENTO_PENDENTE,
    FINALIZADO,
    FINALIZADO_PAGAMENTO_PENDENTE
  }
}
