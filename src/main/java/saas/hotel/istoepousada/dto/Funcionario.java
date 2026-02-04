package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Funcionário do sistema")
public record Funcionario(
    @Schema(description = "ID do funcionário") Long id,
    @Schema(description = "Dados da pessoa vinculada") Pessoa pessoa,
    @Schema(description = "Data de admissão") @JsonFormat(pattern = "dd/MM/yyyy")
        LocalDate dataAdmissao,
    @Schema(description = "Cargo do funcionário") Cargo cargo,
    @Schema(description = "Usuário do sistema vinculado") Usuario.UsuarioResponse usuario) {

  public Funcionario(Pessoa pessoa, LocalDate dataAdmissao, Cargo cargo) {
    this(null, pessoa, dataAdmissao, cargo, null);
  }

  public Funcionario withId(Long id) {
    return new Funcionario(id, this.pessoa, this.dataAdmissao, this.cargo, this.usuario);
  }

  public Funcionario withUsuario(Usuario.UsuarioResponse usuario) {
    return new Funcionario(this.id, this.pessoa, this.dataAdmissao, this.cargo, usuario);
  }

  public static Funcionario mapFuncionario(ResultSet rs) throws SQLException {
    return mapFuncionario(rs, "");
  }

  public static Funcionario mapFuncionario(ResultSet rs, String prefix) throws SQLException {
    Pessoa pessoa = Pessoa.mapPessoa(rs, prefix + "pessoa_");
    Cargo cargo = Cargo.mapCargo(rs, prefix + "cargo_");

    Usuario.UsuarioResponse usuario = null;
    Long usuarioId = rs.getObject(prefix + "usuario_id", Long.class);
    if (usuarioId != null && usuarioId > 0) {
      usuario =
          new Usuario.UsuarioResponse(
              usuarioId,
              rs.getString(prefix + "usuario_username"),
              rs.getBoolean(prefix + "usuario_bloqueado"));
    }

    return new Funcionario(
        rs.getLong(prefix + "id"),
        pessoa,
        rs.getObject(prefix + "data_admissao", LocalDate.class),
        cargo,
        usuario);
  }

  @Schema(description = "Request para criar funcionário")
  public record FuncionarioRequest(
      @Schema(description = "Dados da pessoa") Pessoa pessoa,
      @Schema(description = "Data de admissão", example = "2026-01-19") LocalDate dataAdmissao,
      @Schema(description = "ID do cargo", example = "1") Long cargoId,
      @Schema(description = "Dados do usuário (opcional)") UsuarioData usuario,
      @Schema(description = "Id das Telas do funcionario") List<Long> telas,
      @Schema(description = "Id das permissoes do funcionario") List<Long> permissoes) {

    public record UsuarioData(
        @Schema(description = "Username", example = "joao.silva") String username,
        @Schema(description = "Senha", example = "senha123") String senha) {}
  }
}
