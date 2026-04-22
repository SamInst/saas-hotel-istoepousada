package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.jdbc.core.RowMapper;
import saas.hotel.istoepousada.dto.enums.ModeloMenorIdade;

public record Categoria(
    @NotNull Long id,
    @NotNull String nome,
    String descricao,
    @JsonFormat(pattern = "HH:mm") LocalTime hora_checkin,
    @JsonFormat(pattern = "HH:mm") LocalTime hora_checkout,
    Funcionario.Nome funcionario,
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_cadastro,
    List<ModeloOcupacao> modelos_ocupacao,
    List<ModeloFixo> modelos_fixo,
    List<DayUseOperacao> day_use,
    List<Quarto.Descricao> quartos,
    List<Sazonalidade.Nome> sazonalidades,
    List<MenorIdade> menores_idade) {

  public record Id(@NotNull Long id) {}

  public record Nome(@NotNull Long id, @NotNull String nome) {

    public static final RowMapper<Categoria.Nome> ROW_MAPPER =
        (rs, rowNum) -> {
          Long categoriaId = rs.getObject("categoria_id", Long.class);
          return categoriaId == null
              ? null
              : new Categoria.Nome(categoriaId, rs.getString("categoria_nome"));
        };
  }

  // ── Request / Update completos (todas as abas) ────────────────────────────

  public record Request(
      @NotNull String nome,
      String descricao,
      @JsonFormat(pattern = "HH:mm") LocalTime hora_checkin,
      @JsonFormat(pattern = "HH:mm") LocalTime hora_checkout,
      List<ModeloOcupacao.Input> modelos_ocupacao,
      List<ModeloFixo.Input> modelos_fixo,
      List<DayUseOperacao.Input> day_use,
      List<Long> fk_quartos,
      List<Long> fk_sazonalidades,
      List<MenorIdade.Input> menores_idade) {}

  public record Update(
      @NotNull Long id,
      @NotNull String nome,
      String descricao,
      @JsonFormat(pattern = "HH:mm") LocalTime hora_checkin,
      @JsonFormat(pattern = "HH:mm") LocalTime hora_checkout,
      List<ModeloOcupacao.Input> modelos_ocupacao,
      List<ModeloFixo.Input> modelos_fixo,
      List<DayUseOperacao.Input> day_use,
      List<Long> fk_quartos,
      List<Long> fk_sazonalidades,
      List<MenorIdade.Input> menores_idade) {}

  // ── Aba Preços ────────────────────────────────────────────────────────────

  public record ModeloOcupacao(
      @NotNull Long id,
      Sazonalidade.Nome sazonalidade,
      @NotNull Integer quantidade,
      @NotNull Double valor) {

    /**
     * Usado dentro do Request/Update da Categoria (sem fk_categoria, fk_sazonalidade sempre null).
     */
    public record Input(@NotNull Integer quantidade, @NotNull Double valor) {}

    public static final RowMapper<ModeloOcupacao> ROW_MAPPER =
        (rs, rowNum) -> {
          Long sazonId = rs.getObject("mo_sazonalidade_id", Long.class);
          Sazonalidade.Nome sazon =
              sazonId == null
                  ? null
                  : new Sazonalidade.Nome(sazonId, rs.getString("mo_sazonalidade_descricao"));
          return new ModeloOcupacao(
              rs.getLong("mo_id"), sazon, rs.getInt("mo_quantidade"), rs.getDouble("mo_valor"));
        };
  }

  public record ModeloFixo(
      @NotNull Long id, Sazonalidade.Nome sazonalidade, @NotNull Double valor) {

    /**
     * Usado dentro do Request/Update da Categoria (sem fk_categoria, fk_sazonalidade sempre null).
     */
    public record Input(@NotNull Double valor) {}

    public static final RowMapper<ModeloFixo> ROW_MAPPER =
        (rs, rowNum) -> {
          Long sazonId = rs.getObject("mf_sazonalidade_id", Long.class);
          Sazonalidade.Nome sazon =
              sazonId == null
                  ? null
                  : new Sazonalidade.Nome(sazonId, rs.getString("mf_sazonalidade_descricao"));
          return new ModeloFixo(rs.getLong("mf_id"), sazon, rs.getDouble("mf_valor"));
        };
  }

  // ── Aba Day Use ───────────────────────────────────────────────────────────

  public record DayUseOperacao(
      @NotNull Long id,
      Sazonalidade.Nome sazonalidade,
      @NotNull Boolean ativo,
      DayUsePadrao padrao,
      List<DayUseOcupacao> ocupacoes) {

    /**
     * Usado dentro do Request/Update da Categoria. Inclui padrao ou ocupacoes (mutuamente
     * exclusivos pelo modelo escolhido).
     */
    public record Input(
        @NotNull Boolean ativo, DayUsePadrao.Input padrao, List<DayUseOcupacao.Input> ocupacoes) {}
  }

  public record DayUsePadrao(
      @NotNull Long id,
      @NotNull Double preco_base,
      @NotNull Integer hora_preco_base,
      Double valor_hora_adicional) {

    /** Usado dentro de DayUseOperacao.Input. */
    public record Input(
        @NotNull Double preco_base,
        @NotNull Integer hora_preco_base,
        Double valor_hora_adicional) {}

    public static final RowMapper<DayUsePadrao> ROW_MAPPER =
        (rs, rowNum) ->
            new DayUsePadrao(
                rs.getLong("dup_id"),
                rs.getDouble("dup_preco_base"),
                rs.getInt("dup_hora_preco_base"),
                rs.getObject("dup_valor_hora_adicional", Double.class));
  }

  public record DayUseOcupacao(
      @NotNull Long id,
      @NotNull Integer quantidade_pessoa,
      List<DayUseOcupacaoPessoa> quantidades) {

    /** Usado dentro de DayUseOperacao.Input. */
    public record Input(
        @NotNull Integer quantidade_pessoa,
        @NotNull List<DayUseOcupacaoPessoa.Input> quantidades) {}
  }

  public record DayUseOcupacaoPessoa(
      @NotNull Long id,
      @NotNull Integer quantidade,
      @NotNull Integer valor,
      Integer valor_hora_adicional_por_pessoa) {

    /** Usado dentro de DayUseOcupacao.Input. */
    public record Input(
        @NotNull Integer quantidade,
        @NotNull Integer valor,
        Integer valor_hora_adicional_por_pessoa) {}

    public static final RowMapper<DayUseOcupacaoPessoa> ROW_MAPPER =
        (rs, rowNum) ->
            new DayUseOcupacaoPessoa(
                rs.getLong("duop_id"),
                rs.getInt("duop_quantidade"),
                rs.getInt("duop_valor"),
                rs.getObject("duop_valor_hora_adicional_por_pessoa", Integer.class));
  }

  // ── Aba Menores de Idade ──────────────────────────────────────────────────

  public record MenorIdade(
      @NotNull Long id,
      Sazonalidade.Nome sazonalidade,
      Integer idade_gratuidade,
      ModeloMenorIdade modelo,
      List<MenorTaxaFixa> taxas_fixas,
      List<MenorTaxaPorQuantidade> taxas_por_quantidade,
      List<MenorFaixaEtaria> faixas_etarias,
      List<MenorPorcentagemPorQuantidade> porcentagens_por_quantidade) {

    /** Usado dentro do Request/Update da Categoria. Inclui o modelo e os dados correspondentes. */
    public record Input(
        Integer idade_gratuidade,
        @NotNull ModeloMenorIdade modelo,
        List<MenorTaxaFixa.Input> taxas_fixas,
        List<MenorTaxaPorQuantidade.Input> taxas_por_quantidade,
        List<MenorFaixaEtaria.Input> faixas_etarias,
        List<MenorPorcentagemPorQuantidade.Input> porcentagens_por_quantidade) {}
  }

  public record MenorTaxaFixa(
      @NotNull Long id, @NotNull Integer idade_maxima, @NotNull Double valor_por_crianca) {

    public record Input(@NotNull Integer idade_maxima, @NotNull Double valor_por_crianca) {}

    public static final RowMapper<MenorTaxaFixa> ROW_MAPPER =
        (rs, rowNum) ->
            new MenorTaxaFixa(
                rs.getLong("mtf_id"),
                rs.getInt("mtf_idade_maxima"),
                rs.getDouble("mtf_valor_por_crianca"));
  }

  public record MenorTaxaPorQuantidade(
      @NotNull Long id, @NotNull Integer quantidade_crianca, @NotNull Double valor) {

    public record Input(@NotNull Integer quantidade_crianca, @NotNull Double valor) {}

    public static final RowMapper<MenorTaxaPorQuantidade> ROW_MAPPER =
        (rs, rowNum) ->
            new MenorTaxaPorQuantidade(
                rs.getLong("mtq_id"),
                rs.getInt("mtq_quantidade_crianca"),
                rs.getDouble("mtq_valor"));
  }

  public record MenorFaixaEtaria(
      @NotNull Long id, @NotNull List<Integer> faixa_etaria, @NotNull Double valor) {

    public record Input(@NotNull List<Integer> faixa_etaria, @NotNull Double valor) {}

    public static final RowMapper<MenorFaixaEtaria> ROW_MAPPER =
        (rs, rowNum) -> {
          java.sql.Array arr = rs.getArray("mfe_faixa_etaria");
          List<Integer> faixa = List.of();
          if (arr != null) {
            try {
              Integer[] boxed = (Integer[]) arr.getArray();
              faixa = List.of(boxed);
            } catch (SQLException ignored) {
            }
          }
          return new MenorFaixaEtaria(rs.getLong("mfe_id"), faixa, rs.getDouble("mfe_valor"));
        };
  }

  public record MenorPorcentagemPorQuantidade(
      @NotNull Long id, @NotNull Integer quantidade, @NotNull Integer porcentagem) {

    public record Input(@NotNull Integer quantidade, @NotNull Integer porcentagem) {}

    public static final RowMapper<MenorPorcentagemPorQuantidade> ROW_MAPPER =
        (rs, rowNum) ->
            new MenorPorcentagemPorQuantidade(
                rs.getLong("mpq_id"), rs.getInt("mpq_quantidade"), rs.getInt("mpq_porcentagem"));
  }

  public static final RowMapper<Categoria> ROW_MAPPER =
      (rs, rowNum) -> {
        Long funcId = rs.getObject("funcionario_id", Long.class);
        Funcionario.Nome funcionario =
            funcId == null ? null : new Funcionario.Nome(funcId, rs.getString("funcionario_nome"));
        return new Categoria(
            rs.getLong("categoria_id"),
            rs.getString("categoria_nome"),
            rs.getString("categoria_descricao"),
            rs.getObject("categoria_hora_checkin", LocalTime.class),
            rs.getObject("categoria_hora_checkout", LocalTime.class),
            funcionario,
            rs.getObject("categoria_data_hora_cadastro", LocalDateTime.class),
            null,
            null,
            null,
            null,
            null,
            null);
      };
}
