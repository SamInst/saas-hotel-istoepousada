package saas.hotel.istoepousada.dto;

import jakarta.validation.constraints.NotNull;
import org.springframework.jdbc.core.RowMapper;

public record Permissao(@NotNull Long id, @NotNull String permissao, @NotNull String descricao) {
  public record Id(@NotNull Long id) {}

  public record Request(@NotNull String permissao, @NotNull String descricao) {}

  public static final RowMapper<Permissao> ROW_MAPPER =
      (rs, rowNum) -> {
        Long permissaoId = rs.getObject("permissao_id", Long.class);
        if (permissaoId == null) return null;
        return new Permissao(
            permissaoId, rs.getString("permissao_permissao"), rs.getString("permissao_descricao"));
      };
}
