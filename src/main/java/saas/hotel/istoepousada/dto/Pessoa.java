package saas.hotel.istoepousada.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record Pessoa(
    Long id,
    LocalDateTime dataHoraCadastro,
    String nome,
    LocalDate dataNascimento,
    String cpf,
    String rg,
    String email,
    String telefone,
    String pais,
    String estado,
    String municipio,
    String endereco,
    String complemento,
    Integer vezesHospedado,
    String cep,
    Integer idade,
    String bairro,
    Short sexo,
    String numero,
    Status status,
    List<Empresa> empresasVinculadas,
    List<Veiculo> veiculos) {
  public Pessoa {
    empresasVinculadas = empresasVinculadas != null ? List.copyOf(empresasVinculadas) : List.of();
  }

  public Pessoa(
      String nome,
      LocalDate dataNascimento,
      String cpf,
      String rg,
      String email,
      String telefone,
      String pais,
      String estado,
      String municipio,
      String endereco,
      String complemento,
      String cep,
      Integer idade,
      String bairro,
      Short sexo,
      String numero,
      Status status) {
    this(
        null,
        LocalDateTime.now(),
        nome,
        dataNascimento,
        cpf,
        rg,
        email,
        telefone,
        pais,
        estado,
        municipio,
        endereco,
        complemento,
        0,
        cep,
        idade,
        bairro,
        sexo,
        numero,
        status,
        List.of(),
        List.of());
  }

  public Pessoa withId(Long id) {
    return new Pessoa(
        id,
        this.dataHoraCadastro,
        this.nome,
        this.dataNascimento,
        this.cpf,
        this.rg,
        this.email,
        this.telefone,
        this.pais,
        this.estado,
        this.municipio,
        this.endereco,
        this.complemento,
        this.vezesHospedado,
        this.cep,
        this.idade,
        this.bairro,
        this.sexo,
        this.numero,
        this.status,
        this.empresasVinculadas,
        this.veiculos);
  }

  public Pessoa withEmpresas(List<Empresa> empresas) {
    return new Pessoa(
        this.id,
        this.dataHoraCadastro,
        this.nome,
        this.dataNascimento,
        this.cpf,
        this.rg,
        this.email,
        this.telefone,
        this.pais,
        this.estado,
        this.municipio,
        this.endereco,
        this.complemento,
        this.vezesHospedado,
        this.cep,
        this.idade,
        this.bairro,
        this.sexo,
        this.numero,
        this.status,
        empresas,
        this.veiculos);
  }

  public Pessoa withVeiculos(List<Veiculo> veiculos) {
    return new Pessoa(
        this.id,
        this.dataHoraCadastro,
        this.nome,
        this.dataNascimento,
        this.cpf,
        this.rg,
        this.email,
        this.telefone,
        this.pais,
        this.estado,
        this.municipio,
        this.endereco,
        this.complemento,
        this.vezesHospedado,
        this.cep,
        this.idade,
        this.bairro,
        this.sexo,
        this.numero,
        this.status,
        this.empresasVinculadas,
        veiculos);
  }

  public static Pessoa mapPessoa(ResultSet rs) throws SQLException {
    return mapPessoa(rs, "pessoa_");
  }

  public static Pessoa mapPessoa(ResultSet rs, String prefix) throws SQLException {
    String statusDb = rs.getString(prefix + "status");
    Status status = statusDb != null ? Status.valueOf(statusDb) : null;
    return new Pessoa(
        rs.getLong(prefix + "id"),
        rs.getTimestamp(prefix + "data_hora_cadastro") != null
            ? rs.getTimestamp(prefix + "data_hora_cadastro").toLocalDateTime()
            : null,
        rs.getString(prefix + "nome"),
        rs.getDate(prefix + "data_nascimento") != null
            ? rs.getDate(prefix + "data_nascimento").toLocalDate()
            : null,
        rs.getString(prefix + "cpf"),
        rs.getString(prefix + "rg"),
        rs.getString(prefix + "email"),
        rs.getString(prefix + "telefone"),
        rs.getString(prefix + "pais"),
        rs.getString(prefix + "estado"),
        rs.getString(prefix + "municipio"),
        rs.getString(prefix + "endereco"),
        rs.getString(prefix + "complemento"),
        rs.getObject(prefix + "vezes_hospedado", Integer.class),
        rs.getString(prefix + "cep"),
        rs.getObject(prefix + "idade", Integer.class),
        rs.getString(prefix + "bairro"),
        rs.getObject(prefix + "sexo", Short.class),
        rs.getString(prefix + "numero"),
        status,
        List.of(),
        List.of());
  }

  public boolean possuiEmpresas() {
    return !empresasVinculadas.isEmpty();
  }

  public int totalEmpresas() {
    return empresasVinculadas.size();
  }

  @Schema(description = "Status da pessoa no sistema")
  public enum Status {
    @Schema(description = "Pessoa ativa (padrão)")
    ATIVO,

    @Schema(description = "Pessoa atualmente hospedada")
    HOSPEDADO,

    @Schema(description = "Funcionario atualmente contratado")
    CONTRATADO,

    @Schema(description = "Funcionario atualmente demitido")
    DEMITIDO,

    @Schema(description = "Pessoa bloqueada para novas hospedagens")
    BLOQUEADO;

    public static Status fromDb(String value) {
      if (value == null || value.isBlank()) return null;
      return Status.valueOf(value.trim().toUpperCase());
    }

    public String toDb() {
      return name();
    }
  }
}
