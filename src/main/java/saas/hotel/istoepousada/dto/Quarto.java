package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import org.springframework.jdbc.core.RowMapper;

public record Quarto(
    @NotNull Long id,
    @NotNull String descricao,
    @NotNull Integer quantidade_pessoas,
    @NotNull Status status,
    @NotNull Integer quantidade_cama_casal,
    @NotNull Integer quantidade_cama_solteiro,
    @NotNull Integer quantidade_rede,
    @NotNull Integer quantidade_beliche,
    List<ItemQuarto> quarto_itens,
    QuartoManutencao quarto_manutencao,
    QuartoLimpeza quarto_limpeza) {
  public record Request(
      @NotNull Long numero,
      @NotNull String descricao,
      @NotNull Categoria.Id categoria,
      @NotNull Integer quantidade_pessoas,
      @NotNull Integer quantidade_cama_casal,
      @NotNull Integer quantidade_cama_solteiro,
      @NotNull Integer quantidade_rede,
      @NotNull Integer quantidade_beliche) {}

  public record Update(
      @NotNull Long id,
      @NotNull String descricao,
      @NotNull Categoria.Id categoria,
      @NotNull Integer quantidade_pessoas,
      @NotNull Integer quantidade_cama_casal,
      @NotNull Integer quantidade_cama_solteiro,
      @NotNull Integer quantidade_rede,
      @NotNull Integer quantidade_beliche,
      @NotNull Status status) {}

  public record QuartoItem(
      @NotNull Long id,
      @NotNull Item item,
      @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_reposicao,
      @NotNull Funcionario.Nome funcionario,
      @NotNull Integer quantidade_atual,
      @NotNull Integer quantidade_padrao) {
    public record Request(
        @NotNull Item.Id item,
        @NotNull Integer quantidade_padrao,
        @NotNull Integer quantidade_atual) {}

    public record Update(
        @NotNull Long id, @NotNull Item.Id item, @NotNull Integer quantidade_padrao) {}

    public record Consumir(@NotNull Long id, @NotNull Integer quantidade) {}

    public record Repor(@NotNull Long id, @NotNull Integer quantidade) {}
  }

  public record QuartoManutencao(
      @NotNull Long id,
      @NotNull Quarto.Id quarto,
      @NotNull Funcionario.Nome funcionario,
      @NotNull String descricao,
      @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_registro,
      @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_inicio,
      @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_fim,
      String nome_responsavel,
      @NotNull Boolean ativo) {
    public record Request(
        @NotNull Quarto.Id quarto,
        @NotNull String descricao,
        String nome_responsavel,
        @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_inicio,
        @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_fim) {}

    public record Update(
        @NotNull Long id,
        @NotNull Quarto.Id quarto,
        @NotNull String descricao,
        @NotNull String nome_responsavel,
        @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_inicio,
        @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_fim,
        @NotNull Boolean ativo) {}

    public static final RowMapper<QuartoManutencao> ROW_MAPPER =
        (rs, rowNum) ->
            new QuartoManutencao(
                rs.getLong("manutencao_id"),
                new Quarto.Id(rs.getLong("quarto_id")),
                new Funcionario.Nome(
                    rs.getLong("manutencao_funcionario_id"),
                    rs.getString("manutencao_funcionario_nome")),
                rs.getString("manutencao_descricao"),
                rs.getObject("manutencao_data_hora_registro", LocalDateTime.class),
                rs.getObject("manutencao_data_hora_inicio", LocalDateTime.class),
                rs.getObject("manutencao_data_hora_fim", LocalDateTime.class),
                rs.getString("manutencao_nome_responsavel"),
                rs.getBoolean("manutencao_ativo"));
  }

  public record QuartoLimpeza(
      @NotNull Long id,
      @NotNull Quarto.Id quarto,
      @NotNull Funcionario.Nome funcionario,
      @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_registro,
      @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_inicio,
      @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_fim,
      Boolean ativo) {
    public record Request(Funcionario.Id funcionario) {}

    public record Update(
        @NotNull Long id,
        @NotNull String descricao,
        @NotNull String nome_responsavel,
        @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_inicio,
        @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_fim,
        @NotNull Boolean ativo) {}

    public static final RowMapper<QuartoLimpeza> ROW_MAPPER =
        (rs, rowNum) ->
            new QuartoLimpeza(
                rs.getLong("limpeza_id"),
                new Quarto.Id(rs.getLong("quarto_id")),
                new Funcionario.Nome(
                    rs.getLong("limpeza_funcionario_id"), rs.getString("limpeza_funcionario_nome")),
                rs.getObject("limpeza_data_hora_registro", LocalDateTime.class),
                rs.getObject("limpeza_data_hora_inicio", LocalDateTime.class),
                rs.getObject("limpeza_data_hora_fim", LocalDateTime.class),
                rs.getObject("limpeza_ativo", Boolean.class));
  }

  public record Id(Long id) {}

  public record Descricao(Long id, String descricao) {}

  /**
   * Resposta da consulta de disponibilidade em lote.
   *
   * <p>O {@code status} reflete a situação do quarto <b>para o período solicitado</b>:
   *
   * <ul>
   *   <li>{@link Status#DISPONIVEL} – livre para reserva no período
   *   <li>{@link Status#RESERVADO} – já possui hospedagem ativa no período
   *   <li>{@link Status#OCUPADO} – fisicamente ocupado no momento
   *   <li>{@link Status#MANUTENCAO} – em manutenção
   *   <li>{@link Status#LIMPEZA} – em limpeza
   *   <li>{@link Status#FORA_DE_SERVICO} – fora de serviço
   * </ul>
   *
   * @param quarto_id id do quarto
   * @param descricao descrição / nome do quarto
   * @param status situação efetiva do quarto para o período consultado
   * @param disponivel true se o quarto estiver disponível no período solicitado
   */
  public record Disponibilidade(
      Long quarto_id, String descricao, Status status, boolean disponivel) {}

  public record ItemQuarto(
      @NotNull Long id,
      @NotNull Item item,
      @NotNull Integer quantidade_atual,
      @NotNull Integer quantidade_padrao,
      Double preco,
      Integer qtd_total_unidades) {

    public static final RowMapper<ItemQuarto> ROW_MAPPER =
        (rs, rowNum) -> {
          double precoRaw = rs.getDouble("preco");
          Double preco = rs.wasNull() ? null : precoRaw;
          return new ItemQuarto(
              rs.getLong("quarto_item_id"),
              new Item(rs.getLong("item_id"), rs.getString("item_descricao")),
              rs.getInt("quarto_item_quantidade_atual"),
              rs.getInt("quarto_item_quantidade_padrao"),
              preco,
              rs.getInt("qtd_total_unidades"));
        };
  }

  @Getter
  public enum Status {
    DISPONIVEL,
    OCUPADO,
    RESERVADO,
    LIMPEZA,
    MANUTENCAO,
    FORA_DE_SERVICO
  }

  public static final RowMapper<Quarto> ROW_MAPPER =
      (rs, rowNum) ->
          new Quarto(
              rs.getLong("quarto_id"),
              rs.getString("quarto_descricao"),
              rs.getObject("quarto_quantidade_pessoas", Integer.class),
              Quarto.Status.valueOf(rs.getString("quarto_status")),
              rs.getObject("quarto_quantidade_cama_casal", Integer.class),
              rs.getObject("quarto_quantidade_cama_solteiro", Integer.class),
              rs.getObject("quarto_quantidade_rede", Integer.class),
              rs.getObject("quarto_quantidade_beliche", Integer.class),
              List.of(),
              null,
              null);
}
