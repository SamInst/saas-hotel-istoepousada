package saas.hotel.istoepousada.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.ResultSet;
import java.sql.SQLException;

@Schema(description = "Permissão granular dentro de uma tela")
public record Permissao(
    @Schema(description = "ID da permissão") Long id,
    @Schema(description = "Código da permissão", example = "RELATORIO_EXTRATO_EXPORTAR")
        String permissao,
    @Schema(description = "Descrição da permissão") String descricao) {

  public static Permissao mapPermissao(ResultSet rs, String prefix) throws SQLException {
    Long id = rs.getObject(prefix + "id", Long.class);
    if (id == null) return null;
    return new Permissao(
        id, rs.getString(prefix + "permissao"), rs.getString(prefix + "descricao"));
  }
}
