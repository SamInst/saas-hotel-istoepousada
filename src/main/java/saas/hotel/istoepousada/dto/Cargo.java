package saas.hotel.istoepousada.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Schema(description = "Cargo do funcionário")
public record Cargo(
    @Schema(description = "ID do cargo") Long id,
    @Schema(description = "Nome do cargo") String cargo,
    @Schema(description = "Telas/permissões associadas") List<Tela> telas) {

  public Cargo(Long id, String cargo) {
    this(id, cargo, List.of());
  }

  public Cargo withTelas(List<Tela> telas) {
    return new Cargo(this.id, this.cargo, telas);
  }

  public static Cargo mapCargo(ResultSet rs) throws SQLException {
    return mapCargo(rs, "");
  }

  public static Cargo mapCargo(ResultSet rs, String prefix) throws SQLException {
    return new Cargo(rs.getLong(prefix + "id"), rs.getString(prefix + "cargo"));
  }

  public record Request(Long id, String descricao, List<Tela.Request> telasIds) {}
}
