package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.jdbc.core.RowMapper;

public record Hospedagem(
    @NotNull Long id,
    @NotNull Funcionario.Nome funcionario,
    Quarto quarto,
    @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_registro,
    @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_checkin,
    @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_checkout,
    @NotNull Hospedagem.Status status,
    @NotNull Float valor_total,
    @NotNull Integer quantidade_diarias,
    Integer numero_diaria_atual,
    String observacao,
    List<Diaria> diarias,
    List<Item.Consumo> consumos,
    List<Pagamento> pagamentos,
    List<Pessoa.DadosPrincipais> pessoas,
    List<Hospedagem.PessoaHospedagemOrcamento> pessoas_orcamento,
    MotivoCancelamentoHospedagem motivo_cancelamento,
    Long grupo_id,
    HospedagemNovoPreco novo_preco) {

  public record Request(
      Long hospedagem_id,
      @NotNull Long quarto_id,
      @NotNull Hospedagem.Status status,
      @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_checkin,
      @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_checkout,
      List<Long> pessoas,
      List<Pagamento.Request> pagamentos,
      String observacao,
      @NotNull Double valor_total,
      List<Hospedagem.PessoaHospedagemOrcamento.Request> pessoas_orcamento,
      List<Consumo.Request> consumos,
      MotivoCancelamentoHospedagem.Request motivo_cancelamento,
      /* ajuste manual de preço aplicado na criação ("Gerenciar Preços"); opcional */
      HospedagemNovoPreco.Request novo_preco) {}

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
        List<Long> pessoas) {}

    public record Update(
        @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime checkin,
        @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime checkout) {}
  }

  public record PessoaHospedagemOrcamento(
      @NotNull Long id,
      @NotNull String nome,
      @NotNull @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data_nascimento) {
    public record Request(
        Long id,
        @NotNull String nome,
        @NotNull @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data_nascimento) {}

    public static final RowMapper<PessoaHospedagemOrcamento> MAPPER =
        (rs, rowNum) ->
            new PessoaHospedagemOrcamento(
                rs.getLong("id"),
                rs.getString("nome_pessoa"),
                rs.getDate("data_nascimento").toLocalDate());
  }

  public enum Status {
    ORCAMENTO,
    ORCAMENTO_CANCELADO,

    RESERVA_SOLICITADA,
    RESERVA_ATIVA,
    RESERVA_CANCELADA,
    RESERVA_AUSENTE,

    PERNOITE_ATIVO,
    PERNOITE_CANCELADO,
    PERNOITE_FINALIZADO,
    PERNOITE_FINALIZADO_PAGAMENTO_PENDENTE,

    DAY_USE_SOLICITADO,
    DAY_USE_ATIVO,
    DAY_USE_CANCELADO,
    DAY_USE_FINALIZADO,
    DAY_USE_FINALIZADO_PAGAMENTO_PENDENTE,
    DAY_USE_AUSENTE,
  }

  public static final RowMapper<Hospedagem> MAPPER =
      (rs, rowNum) -> {
        LocalDateTime checkin = rs.getTimestamp("hospedagem_data_hora_checkin").toLocalDateTime();
        LocalDateTime checkout = rs.getTimestamp("hospedagem_data_hora_checout").toLocalDateTime();
        int quantidadeDiarias =
            (int) ChronoUnit.DAYS.between(checkin.toLocalDate(), checkout.toLocalDate());
        int numeroDiariaAtual =
            Math.clamp(
                ChronoUnit.DAYS.between(checkin, LocalDateTime.now()) + 1, 1, quantidadeDiarias);

        return new Hospedagem(
            rs.getLong("hospedagem_id"),
            new Funcionario.Nome(
                rs.getLong("hospedagem_funcionario_id"),
                rs.getString("hospedagem_funcionario_nome")),
            null, // quarto
            rs.getTimestamp("hospedagem_data_hora_registro").toLocalDateTime(),
            checkin,
            checkout,
            Status.valueOf(rs.getString("hospedagem_status")),
            rs.getFloat("hospedagem_valor_total"),
            quantidadeDiarias,
            numeroDiariaAtual,
            rs.getString("hospedagem_observacao"),
            null, // diarias
            null, // consumos
            null, // pagamentos
            null, // pessoas
            null, // pessoas_orcamento
            null, // motivo_cancelamento
            rs.getObject("hospedagem_grupo_id", Long.class),
            null); // novo_preco
      };
}
