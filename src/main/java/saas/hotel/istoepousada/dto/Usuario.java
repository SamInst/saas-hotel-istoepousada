package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.ResultSet;
import java.sql.SQLException;

@Schema(description = "Usuário do sistema")
public record Usuario(
    @Schema(description = "ID do usuário") Long id,
    @Schema(description = "Nome de usuário (login)") String username,
    @Schema(description = "Senha criptografada em MD5", accessMode = Schema.AccessMode.WRITE_ONLY)
        @JsonIgnore
        String senha,
    @Schema(description = "Indica se o usuário está bloqueado") Boolean bloqueado) {

  public Usuario {
    if (bloqueado == null) {
      bloqueado = false;
    }
  }

  public Usuario(String username, String senha) {
    this(null, username, senha, false);
  }

  public Usuario withId(Long id) {
    return new Usuario(id, this.username, this.senha, this.bloqueado);
  }

  public Usuario withSenha(String senha) {
    return new Usuario(this.id, this.username, senha, this.bloqueado);
  }

  public Usuario withBloqueado(Boolean bloqueado) {
    return new Usuario(this.id, this.username, this.senha, bloqueado);
  }

  public static Usuario mapUsuario(ResultSet rs) throws SQLException {
    return mapUsuario(rs, "");
  }

  public static Usuario mapUsuario(ResultSet rs, String prefix) throws SQLException {
    return new Usuario(
        rs.getLong(prefix + "id"),
        rs.getString(prefix + "username"),
        rs.getString(prefix + "senha"),
        rs.getBoolean(prefix + "bloqueado"));
  }

  @Schema(description = "Resposta sem a senha (para segurança)")
  public record UsuarioResponse(Long id, String username, Boolean bloqueado) {
    public static UsuarioResponse from(Usuario usuario) {
      return new UsuarioResponse(usuario.id(), usuario.username(), usuario.bloqueado());
    }
  }
}
