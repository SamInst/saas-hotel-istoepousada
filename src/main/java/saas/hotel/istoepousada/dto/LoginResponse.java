package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Response de autenticação com token JWT")
public record LoginResponse(
    @Schema(description = "Token JWT") String token,
    @Schema(description = "Tipo do token", example = "Bearer") String type,
    @Schema(description = "Dados do funcionário autenticado") FuncionarioAuth funcionario) {

  public LoginResponse(String token, FuncionarioAuth funcionario) {
    this(token, "Bearer", funcionario);
  }

  @Schema(description = "Dados do funcionário para autenticação")
  public record FuncionarioAuth(
      @Schema(description = "ID do funcionário") Long id,
      @Schema(description = "ID do usuário") Long usuarioId,
      @Schema(description = "Username") String username,
      @Schema(description = "ID da pessoa") Long pessoaId,
      @Schema(description = "Nome da pessoa") String nome,
      @Schema(description = "Email") String email,
      @Schema(description = "Data de admissão") @JsonFormat(pattern = "dd/MM/yyyy")
          LocalDate dataAdmissao,
      @Schema(description = "Cargo") CargoAuth cargo) {

    @Schema(description = "Cargo do funcionário")
    public record CargoAuth(
        @Schema(description = "ID do cargo") Long id,
        @Schema(description = "Nome do cargo") String nome,
        @Schema(description = "Telas permitidas") List<TelaAuth> telas) {

      @Schema(description = "Tela do sistema")
      public record TelaAuth(
          @Schema(description = "ID da tela") Long id,
          @Schema(description = "Nome da tela") String nome,
          @Schema(description = "Rota da tela") String rota) {}
    }

    public static FuncionarioAuth from(Funcionario funcionario) {
      if (funcionario.usuario() == null) {
        throw new IllegalStateException("Funcionário não possui usuário vinculado");
      }

      List<CargoAuth.TelaAuth> telas =
          funcionario.cargo().telas().stream()
              .map(t -> new CargoAuth.TelaAuth(t.id(), t.nome(), t.rota()))
              .toList();

      CargoAuth cargo = new CargoAuth(funcionario.cargo().id(), funcionario.cargo().cargo(), telas);

      return new FuncionarioAuth(
          funcionario.id(),
          funcionario.usuario().id(),
          funcionario.usuario().username(),
          funcionario.pessoa().id(),
          funcionario.pessoa().nome(),
          funcionario.pessoa().email(),
          funcionario.dataAdmissao(),
          cargo);
    }
  }
}
