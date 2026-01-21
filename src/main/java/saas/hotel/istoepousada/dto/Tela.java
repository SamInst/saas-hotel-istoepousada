package saas.hotel.istoepousada.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.ResultSet;
import java.sql.SQLException;

@Schema(description = "Tela do sistema")
public record Tela(
    @Schema(description = "ID da tela") Long id,
    @Schema(description = "Nome da tela", example = "DASHBOARD") String nome,
    @Schema(description = "Descrição da tela") String descricao,
    @Schema(description = "Rota da tela", example = "/dashboard") String rota) {

  public static Tela mapTela(ResultSet rs, String prefix) throws SQLException {
    return new Tela(
        rs.getLong(prefix + "id"),
        rs.getString(prefix + "nome"),
        rs.getString(prefix + "descricao"),
        rs.getString(prefix + "rota"));
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Tela other)) return false;
    return java.util.Objects.equals(id, other.id);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(id);
  }
}
