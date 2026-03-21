package saas.hotel.istoepousada.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.bind.annotation.RequestParam;

public record Cargo(@NotNull Long id, @NotNull String descricao, List<Tela> telas) {
  public record Id(@NotNull Long id) {}

  public record Request(
      @NotNull String descricao, List<Tela.Id> telas, List<Permissao.Id> permissoes) {}

  public record Update(
      @NotNull Long id,
      @NotNull String descricao,
      List<Tela.Id> telas,
      List<Permissao.Id> permissoes) {}

  public record Descricao(Long id, String descricao) {}

  public static final RowMapper<Cargo> ROW_MAPPER =
      (rs, rowNum) -> {
        Long cargoId = rs.getObject("cargo_id", Long.class);
        if (cargoId == null) return null;
        return new Cargo(cargoId, rs.getString("cargo_cargo"), List.of());
      };

  public record Buscar(
      Long id,
      String termo,
      Pessoa.Id pessoa,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {}

  public record Vincular(Cargo.Id cargo, List<Tela.Id> telas, Boolean ativo) {}
}
