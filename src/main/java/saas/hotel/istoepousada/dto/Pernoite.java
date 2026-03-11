package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public record Pernoite(
        @NotNull Long id,
        @NotNull Funcionario.Nome funcionario,
        @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_registro,
        @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime check_in,
        @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime check_out,
        @NotNull Status status,
        @NotNull Integer quantidade_diarias,
        @NotNull Integer numero_diaria_atual,
        @NotNull List<Diaria> diarias,
        @NotNull List<PernoitePessoa> pessoas
        ){

  public record Id(Long id) {}

  public record PernoitePessoa(@NotNull Long id, @NotNull Pessoa.DadosPrincipais pessoa
  ){
    public record Request(@NotNull Pernoite.Id pernoite, @NotNull List<Pessoa.Id> pessoas){}
  }

  public record PernoitePagamento(@NotNull Long id, @NotNull Pagamento pagamento) {
    public record Request(@NotNull Long id, @NotNull Pagamento.Request pagamento
    ){}
  }
  public record PernoiteConsumo(Long id, Consumo consumo) {
    public record Request(
            Long id,
            Consumo.Request consumo
    ) {}
  }

  public record Request(
          Quarto.Id quarto,
          @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_inicio,
          @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_fim,
          List<PernoitePessoa.Request> pessoas,
          List<PernoiteConsumo.Request> diariaConsumos,
          List<PernoitePagamento.Request> diariaPagamentos
  ){}
  public record Update(
          Long id,
          Quarto.Id quarto,
          List<PernoitePessoa.Request> pessoas,
          List<PernoiteConsumo.Request> consumos,
          List<PernoitePagamento.Request> pagamentos
  ){}

  public record Diaria(
          @NotNull Long id,
          @NotNull Integer numero,
          @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_inicio,
          @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_fim,
          @NotNull Float valor,
          @NotNull Status status,
          String observacao) {
    public record Request(
            @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_inicio,
            @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_fim,
            @NotNull Float valor,
            String observacao){}
    public record Update(
            @NotNull Long id,
            @NotNull Float valor,
            String observacao
    ){}
    public enum Status {
      ATIVO,
      FINALIZADO
    }
  }
  public enum Status {
    ATIVO,
    CANCELADO,
    PAGAMENTO_PENDENTE,
    FINALIZADO,
    FINALIZADO_PAGAMENTO_PENDENTE,
    AUSENTE
  }
}
