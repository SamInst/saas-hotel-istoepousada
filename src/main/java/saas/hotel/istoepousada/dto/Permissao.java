package saas.hotel.istoepousada.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.ResultSet;
import java.sql.SQLException;

@Schema(description = "Permissão do sistema")
public record Permissao(
    @Schema(description = "ID da permissão") Long id,
    @Schema(description = "Nome da permissão") String permissao,
    @Schema(description = "Descrição da permissão") String descricao,
    @Schema(description = "ID da tela") Long telaId) {

  public static Permissao mapPermissao(ResultSet rs, String prefix) throws SQLException {
    return new Permissao(
        rs.getLong(prefix + "id"),
        rs.getString(prefix + "permissao"),
        rs.getString(prefix + "descricao"),
        rs.getLong(prefix + "fk_tela"));
  }
}
