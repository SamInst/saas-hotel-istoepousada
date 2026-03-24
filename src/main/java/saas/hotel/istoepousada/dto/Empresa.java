package saas.hotel.istoepousada.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.RowMapper;

public record Empresa(
    @NotNull Long id,
    @NotNull LocalDateTime data_hora_registro,
    @NotNull String razao_social,
    String nome_fantasia,
    @NotNull String cnpj,
    @NotNull String telefone,
    @NotNull String email,
    String endereco,
    @NotNull String cep,
    String numero,
    String complemento,
    String pais,
    String estado,
    String municipio,
    String bairro,
    String tipo_empresa,
    Status status,
    Funcionario.Nome funcionario,
    List<Pessoa> pessoas_vinculadas) {
  public record Id(@NotNull Long id) {}

  public record Request(
      @NotNull String razao_social,
      String tipo_empresa,
      String nome_fantasia,
      @NotNull String cnpj,
      @NotNull String telefone,
      @NotNull String email,
      String endereco,
      @NotNull String cep,
      String numero,
      String complemento,
      String pais,
      String estado,
      String municipio,
      String bairro) {}

  public record Update(
      @NotNull Long id,
      @NotNull String cnpj,
      @NotNull String telefone,
      @NotNull String email,
      String endereco,
      @NotNull String cep,
      String numero,
      String complemento,
      String pais,
      String estado,
      String municipio,
      String bairro,
      @NotNull Status status,
      @NotNull String razao_social,
      String nome_fantasia,
      String tipo_empresa) {}

  public record Vincular(@NotNull Empresa.Id empresa, @NotNull Pessoa.Id pessoa, Boolean ativo) {}

  public enum Status {
    ATIVO,
    HOSPEDADO,
    BLOQUEADO;

    public static Status map(String status) {
      if (status == null || status.isBlank()) return ATIVO;
      try {
        return Status.valueOf(status.trim().toUpperCase());
      } catch (IllegalArgumentException ex) {
        return ATIVO;
      }
    }
  }

  public static final RowMapper<Empresa> ROW_MAPPER =
          (rs, rowNum) -> {
            Long empresaId = rs.getObject("empresa_id", Long.class);
            if (empresaId == null) return null;

            return new Empresa(
                    empresaId,
                    rs.getObject("empresa_data_hora_registro", LocalDateTime.class),
                    rs.getString("empresa_razao_social"),
                    rs.getString("empresa_nome_fantasia"),
                    rs.getString("empresa_cnpj"),
                    rs.getString("empresa_telefone"),
                    rs.getString("empresa_email"),
                    rs.getString("empresa_endereco"),
                    rs.getString("empresa_cep"),
                    rs.getString("empresa_numero"),
                    rs.getString("empresa_complemento"),
                    rs.getString("empresa_pais"),
                    rs.getString("empresa_estado"),
                    rs.getString("empresa_municipio"),
                    rs.getString("empresa_bairro"),
                    rs.getString("empresa_tipo_empresa"),
                    Empresa.Status.map(rs.getString("empresa_status")),
                    new Funcionario.Nome(
                            rs.getLong("empresa_funcionario_id"),
                            rs.getString("empresa_funcionario_nome")
                    ),
                    List.of());
          };

  public record EmpresaResponse(
      String cnpj,
      String razaoSocial,
      String nomeFantasia,
      String tipoEmpresa,
      Empresa.Status status,
      String dataAbertura,
      Endereco endereco,
      String telefone,
      String email) {}
}
