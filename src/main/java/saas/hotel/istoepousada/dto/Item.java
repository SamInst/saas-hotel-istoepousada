package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import org.springframework.jdbc.core.RowMapper;

import java.time.LocalDateTime;

public record Item(
        @NotNull Long id,
        String descricao) {

  public record Id(@NotNull Long id){}

  public record Request(
          CategoriaItem.Id categoria_item,
          String descricao){
  }

  public record Update(
          @NotNull Long id,
          @NotNull CategoriaItem.Id categoria_item,
          @NotNull String descricao
  ){}

  public record HistoricoPreco(
          @NotNull Long id,
          @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_registro,
          @NotNull Double valor_compra_unidade,
          @NotNull Double valor_venda_unidade,
          @NotNull Funcionario funcionario
  ) {
    public record Request(
            @NotNull Item.Id item,
            @NotNull Double valor_compra_unidade,
            @NotNull Double valor_venda_unidade,
            @NotNull Funcionario.Id funcionario
    ) {}

    public static final RowMapper<Item.HistoricoPreco> ROW_MAPPER =
            (rs, rowNum) ->
                    new Item.HistoricoPreco(
                            rs.getLong("historico_preco_id"),
                            rs.getObject("historico_preco_data_hora_registro", LocalDateTime.class),
                            rs.getObject("historico_preco_valor_compra_unidade", Double.class),
                            rs.getObject("historico_preco_valor_venda_unidade", Double.class),
                            new Funcionario(
                                    rs.getObject("funcionario_id", Long.class),
                                    null,
                                    null,
                                    null,
                                    null,
                                    null));
  }

  public record HistoricoReposicao(
          @NotNull Long id,
          @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_registro,
          String fornecedor,
          @NotNull Funcionario.Nome funcionario,
          Integer quantidade_unidades) {
    public record Request(
            @NotNull Item.Id item,
            @NotNull Funcionario.Id funcionario,
            String fornecedor,
            @NotNull Integer quantidade_unidades
    ) {}
    public record Update(
            @NotNull Long id,
            @NotNull Integer quantidade_unidades,
            @NotNull Funcionario.Id funcionario,
            String fornecedor
    ){}

    public static final RowMapper<Item.HistoricoReposicao> ROW_MAPPER =
            (rs, rowNum) ->
                    new Item.HistoricoReposicao(
                            rs.getLong("historico_reposicao_id"),
                            rs.getObject("historico_reposicao_data_hora_registro", LocalDateTime.class),
                            rs.getString("historico_reposicao_fornecedor"),
                            rs.getObject("funcionario_id", Long.class) == null
                                    ? null
                                    : new Funcionario.Nome(
                                    rs.getObject("funcionario_id", Long.class),
                                    rs.getString("funcionario_nome")),
                            rs.getObject("historico_reposicao_quantidade_unidades", Integer.class));
  }

  public static final RowMapper<Item> ROW_MAPPER =
          (rs, rowNum) ->
                  new Item(
                          rs.getLong("item_id"),
                          rs.getString("item_descricao"));
}
