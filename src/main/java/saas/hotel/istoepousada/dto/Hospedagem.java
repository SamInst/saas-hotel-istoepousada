package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import org.springframework.jdbc.core.RowMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record Hospedagem(
        @NotNull Long id,
        @NotNull Funcionario.Nome funcionario,
        @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_registro,
        @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_checkin,
        @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_checkout,
        @NotNull Hospedagem.Status status,
        @NotNull Float valor_total,
        @NotNull Integer quantidade_diarias,
        @NotNull Integer numero_diaria_atual,
        List<Diaria> diarias,
        List<Pagamento> pagamentos,
        String observacao
) {

    public record HospedagemRequest(
            @NotNull Long quarto_id,
            @NotNull Hospedagem.Status status,
            @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_checkin,
            @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_checkout,
            List<Long> pessoas,
            List<Pagamento.Request> pagamentos) {}

    public record HospedagemUpdate(
            @NotNull Long id,
            @NotNull Long quarto_id,
            @NotNull Hospedagem.Status status,
            @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_checkin,
            @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_checkout,
            List<Long> pessoas
    ){}

    public record Diaria(
            @NotNull Long id,
            @NotNull Integer numero,
            @NotNull Quarto.Descricao quarto,
            @JsonFormat(pattern = "dd/MM/yyyy HH:mm") @NotNull LocalDateTime checkin,
            @JsonFormat(pattern = "dd/MM/yyyy HH:mm") @NotNull LocalDateTime checkout,
            @NotNull Float valor,
            List<Pessoa.DadosPrincipais> pessoas) {

        public record Request(
                @NotNull Long quarto_id,
                @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime checkin,
                @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime checkout,
                List<Long> pessoas,
                List<Pagamento.Request> pagamentos) {}

        public record Update(
                @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime checkin,
                @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime checkout) {}
    }
    public record Calcular(
            Long categoria_id,
            List<LocalDate> datas_nascimento,
            LocalDate checkin,
            LocalDate checkout
    ){}

    public record Valores(
            Float valor_pago,
            Float valor_pendente,
            Float valor_total
    ){}
    public enum Status {
        ORCAMENTO,
        ORCAMENTO_CANCELADO,
        RESERVA_SOLICITADA,
        RESERVA_ATIVA,
        RESERVA_CANCELADA,
        RESERVA_AUSENTE,

        PERNOITE_ATIVA,
        PERNOITE_CANCELADA,
        PERNOITE_FINALIZADA,
        PERNOITE_FINALIZADA_PAGAMENTO_PENDENTE,

        DAY_USE_SOLICITADA,
        DAY_USE_ATIVA,
        DAY_USE_CANCELADA,
        DAY_USE_FINALIZADA,
        DAY_USE_FINALIZADA_PAGAMENTO_PENDENTE,
        DAY_USE_AUSENTE,
    }
    public static final RowMapper<Hospedagem> MAPPER = (rs, rowNum) -> new Hospedagem(
            rs.getLong("hospedagem_id"),
            new Funcionario.Nome(
                    rs.getLong("hospedagem_funcionario_id"),
                    rs.getString("hospedagem_funcionario_nome")
            ),
            rs.getTimestamp("hospedagem_data_hora_registro").toLocalDateTime(),
            rs.getTimestamp("hospedagem_data_hora_checkin").toLocalDateTime(),
            rs.getTimestamp("hospedagem_data_hora_checout").toLocalDateTime(),
            Hospedagem.Status.valueOf(rs.getString("hospedagem_status")),
            rs.getFloat("hospedagem_valor_total"),
            null, // quantidade_diarias — não está no SELECT
            null, // numero_diaria_atual — não está no SELECT
            null, // diarias
            null, // pagamentos
            null  // observacao
    );
}
