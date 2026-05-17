package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record Orcamento(
        @NotNull Long id,
        @NotNull String nome_solicitante,
        @NotNull Funcionario.Nome funcionario,
        @NotNull Categoria.Nome categoria,
        String observacao,
        @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime checkin,
        @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime checkout,
        @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_registro,
        @NotNull List<Orcamento.Pessoa> pessoas) {

    public record Pessoa(
            @NotNull Long id,
            @NotNull String nome,
            @NotNull @JsonFormat(pattern = "dd/MM/yyyy") LocalDateTime data_nascimento
    ){
        public record Request(
                Long id,
                @NotNull String nome,
                @NotNull @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data_nascimento
        ){}
    }
    public record Request(
            @NotNull Long id,
            @NotNull String nome_solicitante,
            @NotNull Categoria.Id categoria,
            String observacao,
            @NotNull LocalDateTime checkin,
            @NotNull LocalDateTime checkout,
            @NotNull List<Pessoa.Request> pessoas
    ){}
}
