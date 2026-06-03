package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import saas.hotel.istoepousada.config.BrLocalDateDeserializer;

public record CalcularPreco(
    @NotNull Long fk_quarto,
    @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data_entrada,
    @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data_saida,
    @NotNull Integer quantidade_adultos,
    List<Integer> idades_criancas,
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime hora_inicio,
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime hora_fim) {
  public record Request(
      /* O cálculo e feito com vários quartos, então não pode ser categoria */
      @NotNull Long fk_quarto,

      /* Calcular por range de datas */
      @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data_entrada,
      @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data_saida,

      /* datas de nascimento para calcular preço por adultos ou menores de idade */
      @NotNull @JsonDeserialize(contentUsing = BrLocalDateDeserializer.class)
          List<LocalDate> datas_nascimento,

      // Calcular para dayUse
      @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime hora_inicio,
      @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime hora_fim) {}

  public record Resultado(
      Quarto.Descricao quarto,
      Categoria.Nome categoria,
      @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data_entrada,
      @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data_saida,
      Integer diarias,
      Double valor_total,
      List<Sazonalidade.Nome> sazonalidades_aplicadas,
      List<ItemPreco> detalhes) {}

  public record ItemPreco(
      String descricao,
      Sazonalidade.Nome sazonalidade,
      Double valor_base,
      Double acrescimo_sazonalidade,
      Double valor_criancas,
      Double valor_final) {}
}
