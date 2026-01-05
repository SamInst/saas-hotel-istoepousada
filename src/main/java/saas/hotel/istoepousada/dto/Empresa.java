package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("fk_pais") Long fkPais,
    @JsonProperty("fk_estado") Long fkEstado,
    @JsonProperty("fk_municipio") Long fkMunicipio,
    String bairro,
    @JsonProperty("tipo_empresa") String tipoEmpresa,
    Boolean bloqueado,
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
        fkPais,
        fkEstado,
        fkMunicipio,
        bairro,
        tipoEmpresa,
        bloqueado,
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
        fkPais,
        fkEstado,
        fkMunicipio,
        bairro,
        tipoEmpresa,
        bloqueado,
        newPessoas);
  }

  public static Empresa mapEmpresa(ResultSet rs) throws SQLException {
    return mapEmpresa(rs, "empresa_");
  }

  public static Empresa mapEmpresa(ResultSet rs, String prefix) throws SQLException {
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
        rs.getObject(prefix + "fk_pais", Long.class),
        rs.getObject(prefix + "fk_estado", Long.class),
        rs.getObject(prefix + "fk_municipio", Long.class),
        rs.getString(prefix + "bairro"),
        rs.getString(prefix + "tipo_empresa"),
        rs.getBoolean(prefix + "bloqueado"),
        List.of());
  }
}
