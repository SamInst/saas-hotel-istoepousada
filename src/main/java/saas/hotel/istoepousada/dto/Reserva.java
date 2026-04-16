package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.RowMapper;

public record Reserva(
    @NotNull Long id,
    Quarto.Descricao quarto,
    Categoria.Nome categoria,
    Funcionario.Nome funcionario,
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_entrada,
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_saida,
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_registro,
    Boolean ativa,
    Boolean hospedado,
    Boolean cancelado,
    Float valor_total,
    String observacao,
    List<ReservaPessoa> pessoas,
    List<ReservaPagamento> pagamentos) {

  public enum Status {
    SOLICITADA,
    ATIVO,
    HOSPEDADO,
    FINALIZADO,
    CANCELADO;

    public static Status map(String status) {
      if (status == null || status.isBlank()) return ATIVO;
      try {
        return Status.valueOf(status.trim().toUpperCase());
      } catch (IllegalArgumentException ex) {
        return ATIVO;
      }
    }
  }

  // ── Output: pessoa vinculada à reserva ────────────────────────────────────

  public record ReservaPessoa(
      @NotNull Long id,
      Pessoa.DadosPrincipais pessoa,
      Boolean representante,
      Funcionario.Nome funcionario,
      @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_registro) {

    public static final RowMapper<ReservaPessoa> ROW_MAPPER =
        (rs, rowNum) -> {
          Long funcId = rs.getObject("rp_funcionario_id", Long.class);
          return new ReservaPessoa(
              rs.getLong("rp_id"),
              Pessoa.DadosPrincipais.ROW_MAPPER.mapRow(rs, rowNum),
              rs.getBoolean("rp_representante"),
              funcId == null
                  ? null
                  : new Funcionario.Nome(funcId, rs.getString("rp_funcionario_nome")),
              rs.getObject("rp_data_hora_registro", LocalDateTime.class));
        };
  }

  // ── Output: pagamento vinculado à reserva ─────────────────────────────────

  public record ReservaPagamento(
      @NotNull Long id,
      Pagamento pagamento,
      Funcionario.Nome funcionario,
      @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_registro) {

    public static final RowMapper<ReservaPagamento> ROW_MAPPER =
        (rs, rowNum) -> {
          Long funcId = rs.getObject("rpag_funcionario_id", Long.class);
          return new ReservaPagamento(
              rs.getLong("rpag_id"),
              Pagamento.ROW_MAPPER.mapRow(rs, rowNum),
              funcId == null
                  ? null
                  : new Funcionario.Nome(funcId, rs.getString("rpag_funcionario_nome")),
              rs.getObject("rpag_data_hora_registro", LocalDateTime.class));
        };
  }

  // ── Agrupamento por dia (visão mensal) ────────────────────────────────────

  public record PorDia(
      @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data, List<Reserva> reservas) {}

  // ── Requests ──────────────────────────────────────────────────────────────

  public record PessoaRequest(@JsonAlias("id") @NotNull Long fk_pessoa, Boolean representante) {}

  public record PagamentoReservaRequest(
      @NotNull Pagamento.TipoPagamento.Id tipo_pagamento,
      @NotNull String nome_pagador,
      String descricao,
      @NotNull Float valor) {}

  /** Dados de uma única reserva a ser inserida (usada dentro do BatchRequest). */
  public record Request(
      @NotNull Long fk_quarto,
      @NotNull @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data_entrada,
      @NotNull @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data_saida,
      Integer quantidade_adultos,
      List<Integer> idades_criancas,
      @JsonFormat(pattern = "dd/MM/yyyy") List<LocalDate> datas_nascimento,
      Double valor_total,
      String observacao,
      List<PessoaRequest> pessoas,
      List<PagamentoReservaRequest> pagamentos) {}

  /** Inserção de múltiplas reservas de uma vez. */
  public record BatchRequest(@NotNull List<Request> reservas) {}

  /** Edição de uma reserva (quarto, datas e observação). */
  public record Update(
      @NotNull Long id,
      Long fk_quarto,
      @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data_entrada,
      @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data_saida,
      Double valor_total,
      String observacao) {}

  // ── Cálculo de preços ─────────────────────────────────────────────────────

  public record CalculoPrecosRequest(
      @NotNull Long fk_quarto,
      @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data_entrada,
      @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data_saida,
      @NotNull Integer quantidade_adultos,
      List<Integer> idades_criancas,
      @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime hora_inicio,
      @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime hora_fim) {}

  public record ItemPreco(
      String descricao,
      Double valor_base,
      Double acrescimo_sazonalidade,
      Double valor_criancas,
      Double valor_final) {}

  public record ResultadoPreco(
      Quarto.Descricao quarto,
      Categoria.Nome categoria,
      @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data_entrada,
      @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data_saida,
      Integer diarias,
      Double valor_total,
      List<Sazonalidade.Nome> sazonalidades_aplicadas,
      List<ItemPreco> detalhes) {}

  // ── RowMapper ──────────────────────────────────────────────────────────────

  public static final RowMapper<Reserva> ROW_MAPPER =
      (rs, rowNum) -> {
        Long quartoId = rs.getObject("reserva_quarto_id", Long.class);
        Long categoriaId = rs.getObject("reserva_categoria_id", Long.class);
        Long funcId = rs.getObject("reserva_funcionario_id", Long.class);
        return new Reserva(
            rs.getLong("reserva_id"),
            quartoId == null
                ? null
                : new Quarto.Descricao(quartoId, rs.getString("reserva_quarto_descricao")),
            categoriaId == null
                ? null
                : new Categoria.Nome(categoriaId, rs.getString("reserva_categoria_nome")),
            funcId == null
                ? null
                : new Funcionario.Nome(funcId, rs.getString("reserva_funcionario_nome")),
            rs.getObject("reserva_data_hora_entrada", LocalDateTime.class),
            rs.getObject("reserva_data_hora_saida", LocalDateTime.class),
            rs.getObject("reserva_data_hora_registro", LocalDateTime.class),
            rs.getBoolean("reserva_ativa"),
            rs.getBoolean("reserva_hospedado"),
            rs.getBoolean("reserva_cancelado"),
            rs.getObject("reserva_valor_total", Float.class),
            rs.getString("reserva_observacao"),
            null,
            null);
      };
}
