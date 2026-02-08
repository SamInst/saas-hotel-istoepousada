package saas.hotel.istoepousada.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

@Schema(description = "Tela do sistema")
public record Tela(
    @Schema(description = "ID da tela") Long id,
    @Schema(description = "Nome da tela", example = "DASHBOARD") String nome,
    @Schema(description = "Descrição da tela") String descricao,
    @Schema(description = "Permissões granulares dentro da tela") List<Permissao> permissoes) {

  public Tela(Long id, String nome, String descricao) {
    this(id, nome, descricao, List.of());
  }

  public Tela withPermissoes(List<Permissao> permissoes) {
    return new Tela(
        this.id, this.nome, this.descricao, permissoes == null ? List.of() : permissoes);
  }

  public static Tela mapTela(ResultSet rs, String prefix) throws SQLException {
    return new Tela(
        rs.getLong(prefix + "id"),
        rs.getString(prefix + "nome"),
        rs.getString(prefix + "descricao"));
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!(obj instanceof Tela other)) return false;
    return Objects.equals(id, other.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  public record Request(Long id) {}
}
