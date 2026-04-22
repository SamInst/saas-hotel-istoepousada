package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import org.springframework.jdbc.core.RowMapper;

public record CategoriaItem(
    @NotNull Long id,
    @NotNull Funcionario.Nome funcionario,
    @NotNull String nome,
    String descricao,
    @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_registro) {
  public record Id(@NotNull Long id) {}

  public record Request(@NotNull String nome, String descricao) {}

  public record Update(@NotNull Long id, String nome, String descricao) {}

  public static final RowMapper<CategoriaItem> ROW_MAPPER =
      (rs, rowNum) ->
          new CategoriaItem(
              rs.getLong("categoria_id"),
              new Funcionario.Nome(
                  rs.getLong("categoria_funcionario_id"),
                  rs.getString("categoria_funcionario_nome")),
              rs.getString("categoria_nome"),
              rs.getString("categoria_descricao"),
              rs.getObject("categoria_data_hora_registro", LocalDateTime.class));
}
