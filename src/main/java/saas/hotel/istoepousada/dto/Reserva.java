package saas.hotel.istoepousada.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record Reserva(
        @NotNull Long id,
        Quarto quarto,
        Funcionario.Nome funcionario,
        LocalDateTime check_in,
        LocalDateTime check_out,
        LocalDateTime data_hora_registro,
        Status status,
        List<Pessoa> pessoas,
        List<Pagamento> pagamentos
) {
    public enum Status {
        SOLICITADA,
        ATIVO,
        HOSPEDADO,
        FINALIZADO,
        CANCELADO;
        public static Status map(String status) {
            if (status == null || status.isBlank()) return ATIVO;
            try {return Status.valueOf(status.trim().toUpperCase());}
            catch (IllegalArgumentException ex) {return ATIVO;}
        }
    }
}
