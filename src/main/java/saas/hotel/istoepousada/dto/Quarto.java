package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
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
    @NotNull Integer quantidade_beliche) {
  public record Request(
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

  public record QuartoDetalhe(
      @NotNull Long id, @NotNull Integer quantidade, @NotNull String descricao) {
    public record Request(@NotNull Integer quantidade, @NotNull String descricao) {}

    public record Update(
        @NotNull Long id, @NotNull Integer quantidade, @NotNull String descricao) {}
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
        @NotNull Funcionario.Id funcionario,
        @NotNull String descricao,
        String nome_responsavel,
        @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_inicio,
        @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_fim) {}

    public record Update(
        @NotNull Long id,
        @NotNull Quarto.Id quarto,
        @NotNull Funcionario.Id funcionario,
        @NotNull String descricao,
        @NotNull String nome_responsavel,
        @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_inicio,
        @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_fim,
        @NotNull Boolean ativo) {}
  }

  public record QuartoLimpeza(
      @NotNull Long id,
      @NotNull Quarto.Id quarto,
      @NotNull Funcionario.Nome funcionario,
      @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_registro,
      @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_inicio,
      @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_fim,
      Boolean ativo) {
    public record Request(
        @NotNull Quarto.Id quarto,
        @NotNull Funcionario.Id funcionario,
        @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_inicio,
        @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_fim) {}

    public record Update(
        @NotNull Long id,
        @NotNull Funcionario.Id funcionario,
        @NotNull String descricao,
        @NotNull String nome_responsavel,
        @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_inicio,
        @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_fim,
        @NotNull Boolean ativo) {}
  }

  public record Id(Long id) {}

  public record Descricao(Long id, String descricao) {}

  @Getter
  public enum Status {
    DISPONIVEL,
    OCUPADO,
    RESERVADO,
    EM_LIMPEZA,
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
              rs.getObject("quarto_quantidade_beliche", Integer.class));
}
