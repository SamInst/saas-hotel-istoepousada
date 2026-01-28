package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

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
      @Schema(description = "Telas e permissões") List<TelaAuth> telas) {

    @Schema(description = "Tela do sistema")
    public record TelaAuth(
        @Schema(description = "ID da tela") Long id,
        @Schema(description = "Nome da tela") String nome,
        @Schema(description = "Permissões dentro da tela") List<PermissaoAuth> permissoes) {}

    @Schema(description = "Permissão granular dentro de uma tela")
    public record PermissaoAuth(
        @Schema(description = "ID da permissão") Long id,
        @Schema(description = "Código da permissão") String permissao,
        @Schema(description = "Descrição") String descricao) {}
  }

  public static FuncionarioAuth from(Funcionario funcionario) {
    if (funcionario == null) throw new IllegalArgumentException("Funcionário é obrigatório.");
    if (funcionario.usuario() == null)
      throw new IllegalStateException("Funcionário não possui usuário vinculado");

    List<CargoAuth.TelaAuth> telas =
        funcionario.cargo() == null || funcionario.cargo().telas() == null
            ? List.of()
            : funcionario.cargo().telas().stream()
                .filter(Objects::nonNull)
                .map(
                    t ->
                        new CargoAuth.TelaAuth(
                            t.id(),
                            t.nome(),
                            t.permissoes() == null
                                ? List.of()
                                : t.permissoes().stream()
                                    .filter(Objects::nonNull)
                                    .map(
                                        p ->
                                            new CargoAuth.PermissaoAuth(
                                                p.id(), p.permissao(), p.descricao()))
                                    .toList()))
                .toList();

    CargoAuth cargo =
        funcionario.cargo() == null
            ? new CargoAuth(null, null, telas)
            : new CargoAuth(funcionario.cargo().id(), funcionario.cargo().cargo(), telas);

    return new FuncionarioAuth(
        funcionario.id(),
        funcionario.usuario().id(),
        funcionario.usuario().username(),
        funcionario.pessoa() != null ? funcionario.pessoa().id() : null,
        funcionario.pessoa() != null ? funcionario.pessoa().nome() : null,
        funcionario.pessoa() != null ? funcionario.pessoa().email() : null,
        funcionario.dataAdmissao(),
        cargo);
  }
}
