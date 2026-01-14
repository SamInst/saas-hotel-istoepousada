package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public record Empresa(
    Long id,
    @JsonProperty("razao_social") String razaoSocial,
    @JsonProperty("nome_fantasia") String nomeFantasia,
    String cnpj,
    @JsonProperty("inscricao_estadual") String inscricaoEstadual,
    @JsonProperty("inscricao_municipal") String inscricaoMunicipal,
    String telefone,
    String email,
    String endereco,
    String cep,
    String numero,
    String complemento,
    String pais,
    String estado,
    String municipio,
    String bairro,
    @JsonProperty("tipo_empresa") String tipoEmpresa,
    Status status,
    @JsonProperty("pessoasVinculadas") List<Pessoa> pessoas) {
  public Empresa withId(Long newId) {
    return new Empresa(
        newId,
        razaoSocial,
        nomeFantasia,
        cnpj,
        inscricaoEstadual,
        inscricaoMunicipal,
        telefone,
        email,
        endereco,
        cep,
        numero,
        complemento,
        pais,
        estado,
        municipio,
        bairro,
        tipoEmpresa,
        status,
        pessoas);
  }

  public Empresa withPessoas(List<Pessoa> newPessoas) {
    return new Empresa(
        id,
        razaoSocial,
        nomeFantasia,
        cnpj,
        inscricaoEstadual,
        inscricaoMunicipal,
        telefone,
        email,
        endereco,
        cep,
        numero,
        complemento,
        pais,
        estado,
        municipio,
        bairro,
        tipoEmpresa,
        status,
        newPessoas);
  }

  public static Empresa mapEmpresa(ResultSet rs) throws SQLException {
    return mapEmpresa(rs, "empresa_");
  }

  public static Empresa mapEmpresa(ResultSet rs, String prefix) throws SQLException {
    String statusDb = rs.getString(prefix + "status");
    Empresa.Status status = statusDb != null ? Empresa.Status.valueOf(statusDb) : null;
    return new Empresa(
        rs.getLong(prefix + "id"),
        rs.getString(prefix + "razao_social"),
        rs.getString(prefix + "nome_fantasia"),
        rs.getString(prefix + "cnpj"),
        rs.getString(prefix + "inscricao_estadual"),
        rs.getString(prefix + "inscricao_municipal"),
        rs.getString(prefix + "telefone"),
        rs.getString(prefix + "email"),
        rs.getString(prefix + "endereco"),
        rs.getString(prefix + "cep"),
        rs.getString(prefix + "numero"),
        rs.getString(prefix + "complemento"),
        rs.getString(prefix + "pais"),
        rs.getString(prefix + "estado"),
        rs.getString(prefix + "municipio"),
        rs.getString(prefix + "bairro"),
        rs.getString(prefix + "tipo_empresa"),
        status,
        List.of());
  }

  @Schema(description = "Status da Empresa no sistema")
  public enum Status {
    @Schema(description = "Empresa ativa (padrão)")
    ATIVO,

    @Schema(description = "Empresa atualmente hospedada")
    HOSPEDADO,

    @Schema(description = "Empresa bloqueada para novas hospedagens")
    BLOQUEADO;

    public static Pessoa.Status fromDb(String value) {
      if (value == null || value.isBlank()) return null;
      return Pessoa.Status.valueOf(value.trim().toUpperCase());
    }

    public String toDb() {
      return name();
    }
  }
}
