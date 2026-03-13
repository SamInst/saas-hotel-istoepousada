package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.RowMapper;

public record Relatorio(
    @NotNull Long id,
    @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_registro,
    @NotNull String relatorio,
    @NotNull Double valor,
    @NotNull Funcionario.Nome funcionario,
    @NotNull Pagamento pagamento,
    Quarto.Descricao quarto,
    @NotNull Double valor_historico_dinheiro,
    @NotNull Boolean despesa_pessoal) {
  public record Request(
      @NotNull Funcionario.Id funcionario,
      @NotNull String relatorio,
      @NotNull Double valor,
      @NotNull Registro tipo_registro,
      @NotNull Boolean despesa_pessoal,
      @NotNull Pagamento.Request pagamento,
      Quarto.Id quarto) {}

  public record Update(
      @NotNull Long id,
      @NotNull String descricao,
      @NotNull Double valor,
      Quarto.Id quarto,
      @NotNull Boolean despesa_pessoal) {}

  public enum Registro {
    ENTRADA,
    SAIDA
  }

  public record Extrato(Map<String, Resumo> pagamentos, Page<Diaria> page) {
    public record Diaria(
        @NotNull @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data,
        @NotNull Float total_entrada_dia,
        @NotNull Float total_saida_dia,
        @NotNull Float lucro_total_dia,
        List<Relatorio> relatorios) {}

    public record Resumo(
        @NotNull Double receitas, @NotNull Double despesas, @NotNull Double lucro) {

      public static Resumo of(Double receitas, Double despesas) {
        double r = receitas != null ? receitas : 0d;
        double d = despesas != null ? despesas : 0d;
        return new Resumo(r, d, r - d);
      }
    }

    public static final RowMapper<Relatorio> ROW_MAPPER =
        (rs, row_num) -> {
          Long funcionarioId = rs.getObject("funcionario_id", Long.class);
          Long quartoId = rs.getObject("quarto_id", Long.class);
          Long tipoPagamentoId = rs.getObject("tipo_pagamento_id", Long.class);
          UUID pagamentoId = rs.getObject("pagamento_id", UUID.class);

          Funcionario.Nome funcionario =
              funcionarioId == null
                  ? null
                  : new Funcionario.Nome(funcionarioId, rs.getString("funcionario_nome"));

          Quarto.Descricao quarto =
              quartoId == null
                  ? null
                  : new Quarto.Descricao(quartoId, rs.getString("quarto_descricao"));

          UUID pagamentoDescontoId = rs.getObject("pagamento_desconto_id", UUID.class);

          Long pagamentoDescontoFuncionarioId =
              rs.getObject("pagamento_desconto_funcionario_id", Long.class);

          Pagamento.Desconto desconto =
              pagamentoDescontoId == null
                  ? null
                  : new Pagamento.Desconto(
                      pagamentoDescontoId,
                      pagamentoDescontoFuncionarioId == null
                          ? null
                          : new Funcionario.Nome(
                              pagamentoDescontoFuncionarioId,
                              rs.getString("pagamento_desconto_funcionario_nome")),
                      rs.getObject("pagamento_desconto_porcentagem", Integer.class),
                      rs.getObject("pagamento_desconto_valor", Double.class),
                      rs.getObject("pagamento_desconto_data_hora_registro", LocalDateTime.class));

          Pagamento pagamento =
              pagamentoId == null
                  ? null
                  : new Pagamento(
                      pagamentoId,
                      tipoPagamentoId == null
                          ? null
                          : new Pagamento.TipoPagamento(
                              tipoPagamentoId, rs.getString("tipo_pagamento_descricao")),
                      funcionario,
                      rs.getObject("pagamento_data_hora_registro", LocalDateTime.class),
                      rs.getString("pagamento_nome_pagador"),
                      rs.getString("pagamento_descricao"),
                      rs.getObject("pagamento_valor", Double.class) == null
                          ? 0d
                          : rs.getObject("pagamento_valor", Double.class),
                      rs.getBoolean("pagamento_cancelado"),
                      desconto,
                      rs.getString("pagamento_path_arquivo"));

          Double valorPagamento = rs.getObject("pagamento_valor", Double.class);

          return new Relatorio(
              rs.getLong("relatorio_id"),
              rs.getObject("relatorio_data_hora", LocalDateTime.class),
              rs.getString("relatorio_descricao"),
              valorPagamento == null ? 0d : valorPagamento,
              funcionario,
              pagamento,
              quarto,
              rs.getObject("relatorio_valor_historico_dinheiro", Double.class),
              rs.getObject("relatorio_despesa_pessoal", Boolean.class));
        };
  }
}
