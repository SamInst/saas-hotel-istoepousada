package saas.hotel.istoepousada.dto;

import jakarta.validation.constraints.NotNull;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;

public record Tela(
    @NotNull Long id,
    @NotNull String nome,
    @NotNull String descricao,
    List<Permissao> permissoes) {
  public record Id(@NotNull Long id) {}
  public record Request(@NotNull String nome, @NotNull String descricao) {}
  public static final RowMapper<Tela> ROW_MAPPER =
          (rs, rowNum) -> {
            Long telaId = rs.getObject("tela_id", Long.class);
            if (telaId == null) return null;
            return new Tela(
                    telaId,
                    rs.getString("tela_nome"),
                    rs.getString("tela_descricao"),
                    List.of());
          };
}
