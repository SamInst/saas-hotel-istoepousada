package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.jdbc.core.RowMapper;

public record Sazonalidade(
    @NotNull Long id,
    @NotNull String descricao,
    @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data_inicio,
    @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data_fim,
    @JsonFormat(pattern = "HH:mm") LocalTime diario_hora_inicio_ciclo,
    @JsonFormat(pattern = "HH:mm") LocalTime diario_hora_fim_ciclo,
    List<Integer> semanal,
    List<Integer> mensal,
    List<Integer> anual,
    @JsonFormat(pattern = "HH:mm") LocalTime hora_checkin,
    @JsonFormat(pattern = "HH:mm") LocalTime hora_checkout,
    List<CategoriaVinculo> categorias,
    List<Categoria.ModeloOcupacao> modelos_ocupacao,
    List<Categoria.ModeloFixo> modelos_fixo,
    List<Categoria.DayUseOperacao> day_use,
    List<Categoria.MenorIdade> menores_idade) {

  public record Id(@NotNull Long id) {}

  public record Nome(@NotNull Long id, @NotNull String descricao) {

    public static final RowMapper<Sazonalidade.Nome> ROW_MAPPER =
        (rs, rowNum) -> {
          Long sazonId = rs.getObject("sazonalidade_id", Long.class);
          return sazonId == null
              ? null
              : new Sazonalidade.Nome(sazonId, rs.getString("sazonalidade_descricao"));
        };
  }

  // ── Vínculo categoria ─────────────────────────────────────────────────────

  public record CategoriaVinculo(
      @NotNull Long id,
      @NotNull Categoria.Nome categoria,
      @NotNull Boolean ativo,
      Funcionario.Nome funcionario,
      @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_cadastro) {}

  // ── Request / Update ──────────────────────────────────────────────────────

  public record Request(
      @NotNull String descricao,
      @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data_inicio,
      @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data_fim,
      @JsonFormat(pattern = "HH:mm") LocalTime diario_hora_inicio_ciclo,
      @JsonFormat(pattern = "HH:mm") LocalTime diario_hora_fim_ciclo,
      List<Integer> semanal,
      List<Integer> mensal,
      List<Integer> anual,
      @JsonFormat(pattern = "HH:mm") LocalTime hora_checkin,
      @JsonFormat(pattern = "HH:mm") LocalTime hora_checkout,
      List<Categoria.ModeloOcupacao.Input> modelos_ocupacao,
      List<Categoria.ModeloFixo.Input> modelos_fixo,
      List<Categoria.DayUseOperacao.Input> day_use,
      List<Long> fk_categorias,
      List<Categoria.MenorIdade.Input> menores_idade) {}

  public record Update(
      @NotNull Long id,
      @NotNull String descricao,
      @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data_inicio,
      @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data_fim,
      @JsonFormat(pattern = "HH:mm") LocalTime diario_hora_inicio_ciclo,
      @JsonFormat(pattern = "HH:mm") LocalTime diario_hora_fim_ciclo,
      List<Integer> semanal,
      List<Integer> mensal,
      List<Integer> anual,
      @JsonFormat(pattern = "HH:mm") LocalTime hora_checkin,
      @JsonFormat(pattern = "HH:mm") LocalTime hora_checkout,
      List<Categoria.ModeloOcupacao.Input> modelos_ocupacao,
      List<Categoria.ModeloFixo.Input> modelos_fixo,
      List<Categoria.DayUseOperacao.Input> day_use,
      List<Long> fk_categorias,
      List<Categoria.MenorIdade.Input> menores_idade) {}

  // ── Vínculo requests ──────────────────────────────────────────────────────

  public record VinculoCategoriaRequest(
      @NotNull Long fk_sazonalidade, @NotNull Long fk_categoria) {}

  public record VinculoCategoriaToggle(@NotNull Long id, @NotNull Boolean ativo) {}

  // ── RowMapper ─────────────────────────────────────────────────────────────

  static List<Integer> parseIntArray(java.sql.Array arr) {
    if (arr == null) return null;
    try {
      Integer[] boxed = (Integer[]) arr.getArray();
      return (boxed == null || boxed.length == 0) ? null : List.of(boxed);
    } catch (SQLException e) {
      return null;
    }
  }

  public static final RowMapper<Sazonalidade> ROW_MAPPER =
      (rs, rowNum) ->
          new Sazonalidade(
              rs.getLong("sazonalidade_id"),
              rs.getString("sazonalidade_descricao"),
              rs.getObject("sazonalidade_data_inicio", LocalDate.class),
              rs.getObject("sazonalidade_data_fim", LocalDate.class),
              rs.getObject("sazonalidade_diario_hora_inicio", LocalTime.class),
              rs.getObject("sazonalidade_diario_hora_fim", LocalTime.class),
              parseIntArray(rs.getArray("sazonalidade_semanal")),
              parseIntArray(rs.getArray("sazonalidade_mensal")),
              parseIntArray(rs.getArray("sazonalidade_anual")),
              rs.getObject("sazonalidade_hora_checkin", LocalTime.class),
              rs.getObject("sazonalidade_hora_checkout", LocalTime.class),
              null,
              null,
              null,
              null,
              null);
}
