package saas.hotel.istoepousada.dto;

import jakarta.validation.constraints.NotNull;
import org.springframework.jdbc.core.RowMapper;

public record Sazonalidade(@NotNull Long id, @NotNull String descricao) {

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
}
