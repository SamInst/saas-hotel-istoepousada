package saas.hotel.istoepousada.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.ResultSet;
import java.sql.SQLException;

@Schema(description = "Histórico de cargo e salário do funcionário")
public record HistoricoFuncionario(
    @Schema(description = "ID do histórico") Long id,
    @Schema(description = "Cargo no período") Cargo cargo,
    @Schema(description = "Funcionário") Funcionario funcionario,
    @Schema(description = "Salário no período") Float salario) {

  public record Cargo(Long id, String descricao) {}

  public record Funcionario(Long id, String descricao) {}

  public static HistoricoFuncionario mapHistoricoFuncionario(ResultSet rs) throws SQLException {
    return new HistoricoFuncionario(
        rs.getLong("id"),
        new Cargo(rs.getLong("cargo_id"), rs.getString("cargo_descricao")),
        new Funcionario(rs.getLong("funcionario_id"), rs.getString("funcionario_descricao")),
        rs.getFloat("salario"));
  }
}
