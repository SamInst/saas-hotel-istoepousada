package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record Pernoite(
    @NotNull Long id,
    @NotNull Funcionario.Nome funcionario,
    @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_registro,
    @NotNull @JsonFormat(pattern = "dd/MM/yyyy") LocalDate check_in,
    @NotNull @JsonFormat(pattern = "dd/MM/yyyy") LocalDate check_out,
    @JsonFormat(pattern = "HH:mm") LocalTime hora_chegada,
    @JsonFormat(pattern = "HH:mm") LocalTime hora_saida,
    @NotNull Status status,
    Float valor_total,
    @NotNull Integer quantidade_diarias,
    @NotNull Integer numero_diaria_atual,
    @NotNull List<Diaria> diarias,
    @NotNull List<Pessoa.DadosPrincipais> pessoas) {

  public record Id(Long id) {}

  public record PernoitePessoa(@NotNull Long id, @NotNull Pessoa.DadosPrincipais pessoa) {
    public record Request(@NotNull Pernoite.Id pernoite, @NotNull List<Pessoa.Id> pessoas) {}
  }

  public record PernoitePagamento(@NotNull Long id, @NotNull Pagamento pagamento) {
    public record Request(@NotNull Long id, @NotNull Pagamento.Request pagamento) {}
  }

  public record PernoiteConsumo(Long id, Consumo consumo) {
    public record Request(Long id, Consumo.Request consumo) {}
  }

  // ── Criação de pernoite ─────────────────────────────────────────────────────
  public record CreateRequest(
      Long quarto_id,
      @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data_entrada,
      @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data_saida,
      List<Long> pessoas,
      List<Pagamento.Request> pagamentos,
      Long reserva_id) {}

  // ── Edição de pernoite ──────────────────────────────────────────────────────
  public record EditRequest(
      @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data_entrada,
      @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data_saida,
      @JsonFormat(pattern = "HH:mm") LocalTime hora_chegada,
      @JsonFormat(pattern = "HH:mm") LocalTime hora_saida,
      Long quarto_id) {}

  // ── Requests legados mantidos ──────────────────────────────────────────────
  public record Request(
      Quarto.Id quarto,
      @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_inicio,
      @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_fim,
      List<PernoitePessoa.Request> pessoas,
      List<PernoiteConsumo.Request> diariaConsumos,
      List<PernoitePagamento.Request> diariaPagamentos) {}

  public record Update(
      Long id,
      Quarto.Id quarto,
      List<PernoitePessoa.Request> pessoas,
      List<PernoiteConsumo.Request> consumos,
      List<PernoitePagamento.Request> pagamentos) {}

  public record AlterarQuartoRequest(@NotNull Long quarto_id) {}

  public record AdicionarPessoasRequest(@NotNull List<Long> pessoa_ids) {}

  public record CancelamentoPagamentoRequest(@NotNull String motivo_cancelamento) {}

  // ── Diária ──────────────────────────────────────────────────────────────────
  public record Diaria(
      @NotNull Long id,
      @NotNull Integer numero,
      Quarto.Descricao quarto,
      @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_inicio,
      @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_fim,
      @NotNull Float valor,
      @NotNull Status status,
      String observacao,
      List<Pessoa.DadosPrincipais> pessoas,
      List<Item.Consumo> consumos,
      List<Pagamento> pagamentos) {

    public record Request(
        @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_inicio,
        @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_fim,
        @NotNull Float valor,
        String observacao) {}

    public record Update(@NotNull Long id, @NotNull Float valor, String observacao) {}

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
