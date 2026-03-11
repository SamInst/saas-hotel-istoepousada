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
            Funcionario.Id funcionario,
            Pagamento.Request pagamento,
            Item.Id item,
            Integer quantidade) {}
    public record Update(
            @NotNull Long id,
            @NotNull Funcionario.Id funcionario,
            @NotNull Item.Id item,
            @NotNull Integer quantidade) {}
}
