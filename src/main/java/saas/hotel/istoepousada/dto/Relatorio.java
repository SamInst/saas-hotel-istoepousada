package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.RowMapper;

public record Relatorio(
    @NotNull Long id,
    @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_registro,
    @NotNull String relatorio,
    @NotNull Float valor,
    @NotNull Funcionario.Nome funcionario,
    @NotNull Pagamento pagamento,
    Quarto.Descricao quarto,
    @NotNull Float valor_historico_dinheiro,
    @NotNull Boolean despesa_pessoal) {
  public record Id(@NotNull Long id) {}

  public record Request(
      @NotNull String relatorio,
      @NotNull Registro tipo_registro,
      @NotNull Boolean despesa_pessoal,
      @NotNull Pagamento.Request pagamento,
      Quarto.Id quarto) {}

  public record Update(
      @NotNull Long id,
      @NotNull String descricao,
      @NotNull Pagamento.Update pagamento,
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

    public record Resumo(@NotNull Float receitas, @NotNull Float despesas, @NotNull Float lucro) {

      public static Resumo of(Float receitas, Float despesas) {
        Float r = receitas != null ? receitas : 0F;
        Float d = despesas != null ? despesas : 0F;
        return new Resumo(r, d, r - d);
      }
    }

    public static final RowMapper<Relatorio> ROW_MAPPER =
        (rs, row_num) -> {
          Long quartoId = rs.getObject("quarto_id", Long.class);

          Quarto.Descricao quarto =
              quartoId == null
                  ? null
                  : new Quarto.Descricao(quartoId, rs.getString("quarto_descricao"));

          Float valorPagamento = rs.getObject("pagamento_valor", Float.class);
          Float valorOriginal = valorPagamento == null ? 0F : valorPagamento;

          // Calcula o valor com desconto
          Integer descontoPorcentagem =
              rs.getObject("pagamento_desconto_porcentagem", Integer.class);
          Float descontoValor = rs.getObject("pagamento_desconto_valor", Float.class);

          Float valorFinal = valorOriginal;
          if (valorOriginal > 0) { // desconto só se aplica a valores positivos (entradas)
            if (descontoPorcentagem != null && descontoPorcentagem > 0) {
              valorFinal = valorOriginal - (valorOriginal * descontoPorcentagem / 100);
            } else if (descontoValor != null && descontoValor > 0) {
              valorFinal = valorOriginal - descontoValor;
            }
          }

          return new Relatorio(
              rs.getLong("relatorio_id"),
              rs.getObject("relatorio_data_hora", LocalDateTime.class),
              rs.getString("relatorio_descricao"),
              valorFinal,
              Funcionario.Nome.ROW_MAPPER_RELATORIO.mapRow(rs, row_num),
              Pagamento.ROW_MAPPER.mapRow(rs, row_num),
              quarto,
              rs.getFloat("relatorio_valor_historico_dinheiro"),
              rs.getObject("relatorio_despesa_pessoal", Boolean.class));
        };
  }
}
