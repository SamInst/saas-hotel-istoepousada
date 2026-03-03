package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public record Funcionario(
        Long id,
        Pessoa pessoa,
        @JsonFormat(pattern = "dd/MM/yyyy") LocalDate dataAdmissao,
        Float salario,
        Cargo cargo,
        Usuario.UsuarioResponse usuario) {

  public Funcionario(Pessoa pessoa, LocalDate dataAdmissao, Float salario, Cargo cargo) {
    this(null, pessoa, dataAdmissao, salario, cargo, null);
  }

  public Funcionario withId(Long id) {
    return new Funcionario(id, this.pessoa, this.dataAdmissao, this.salario, this.cargo, this.usuario);
  }

  public Funcionario withUsuario(Usuario.UsuarioResponse usuario) {
    return new Funcionario(this.id, this.pessoa, this.dataAdmissao, this.salario, this.cargo, usuario);
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
      usuario = new Usuario.UsuarioResponse(
              usuarioId,
              rs.getString(prefix + "usuario_username"),
              rs.getBoolean(prefix + "usuario_bloqueado"));
    }

    Float salario = rs.getFloat(prefix + "salario");

    return new Funcionario(
            rs.getLong(prefix + "id"),
            pessoa,
            rs.getObject(prefix + "data_admissao", LocalDate.class),
            salario,
            cargo,
            usuario);
  }

  public record FuncionarioRequest(
          Long pessoaId,
          LocalDate dataAdmissao,
          Long cargoId,
          UsuarioData usuario,
          Float salario) {

    public record UsuarioData(String username, String senha) {}
  }
  public record Nome(Long id, String nome) {}
}
