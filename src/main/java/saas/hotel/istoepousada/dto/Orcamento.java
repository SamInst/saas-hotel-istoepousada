package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record Orcamento(
    @NotNull Long id,
    @NotNull String nome_solicitante,
    @NotNull Funcionario.Nome funcionario,
    @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_registro,
    @NotNull List<Hospedagem> hospedagens) {
  public record Request(
          Long id,
          @NotNull String nome_solicitante,
          @NotNull @NotBlank List<Hospedagem.Request> hospedagens) {}
}
