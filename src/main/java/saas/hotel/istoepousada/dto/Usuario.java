package saas.hotel.istoepousada.dto;

import jakarta.validation.constraints.NotNull;
import org.springframework.jdbc.core.RowMapper;

public record Usuario(
     @NotNull Long id,
     @NotNull String username,
     @NotNull Boolean bloqueado) {
  public record Id(@NotNull Long id){}
  public record Request(
          @NotNull String username,
          @NotNull String senha
  ){}
  public record Update(
          @NotNull Long id,
          @NotNull String username,
          @NotNull String senha,
          @NotNull Boolean bloqueado
  ){}
  public static final RowMapper<Usuario> ROW_MAPPER =
          (rs, rowNum) -> {
            Long usuarioId = rs.getObject("usuario_id", Long.class);
            if (usuarioId == null) return null;
            return new Usuario(
                    usuarioId,
                    rs.getString("usuario_username"),
                    rs.getObject("usuario_bloqueado", Boolean.class));
          };
}
