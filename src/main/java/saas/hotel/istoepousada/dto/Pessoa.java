package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Pessoa (hóspede/cliente)")
public record Pessoa(
    @Schema(description = "ID da pessoa") Long id,
    @Schema(description = "Data e hora de cadastro") @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
        LocalDateTime dataHoraCadastro,
    @Schema(description = "Nome completo") String nome,
    @Schema(description = "Data de nascimento") LocalDate dataNascimento,
    @Schema(description = "CPF") String cpf,
    @Schema(description = "RG") String rg,
    @Schema(description = "Email") String email,
    @Schema(description = "Telefone") String telefone,
    @Schema(description = "País") String pais,
    @Schema(description = "Estado") String estado,
    @Schema(description = "Município") String municipio,
    @Schema(description = "Endereço") String endereco,
    @Schema(description = "Complemento") String complemento,
    @Schema(description = "Vezes hospedado") Integer vezesHospedado,
    @Schema(description = "CEP") String cep,
    @Schema(description = "Idade") Integer idade,
    @Schema(description = "Bairro") String bairro,
    @Schema(description = "Sexo (0=Feminino, 1=Masculino)") Integer sexo,
    @Schema(description = "Número") String numero,
    @Schema(description = "Status da pessoa") Status status,
    @Schema(description = "Empresas vinculadas") List<Empresa> empresasVinculadas,
    @Schema(description = "Veículos vinculados") List<Veiculo> veiculos,
    @Schema(description = "ID do funcionário responsável") Long funcionarioId,
    @Schema(description = "Nome do funcionário responsável") String funcionarioNome,
    @Schema(description = "ID do titular") Long titularId,
    @Schema(description = "Nome do titular") String titularNome) {

  public Pessoa(
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
      Integer sexo,
      String numero,
      Status status) {
    this(
        id,
        dataHoraCadastro,
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
        vezesHospedado,
        cep,
        idade,
        bairro,
        sexo,
        numero,
        status,
        List.of(),
        List.of(),
        null,
        null,
        null,
        null);
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
        this.veiculos,
        this.funcionarioId,
        this.funcionarioNome,
        this.titularId,
        this.titularNome);
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
        this.veiculos,
        this.funcionarioId,
        this.funcionarioNome,
        this.titularId,
        this.titularNome);
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
        veiculos,
        this.funcionarioId,
        this.funcionarioNome,
        this.titularId,
        this.titularNome);
  }

  public Pessoa withFuncionario(Long funcionarioId, String funcionarioNome) {
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
        this.veiculos,
        funcionarioId,
        funcionarioNome,
        this.titularId,
        this.titularNome);
  }

  public Pessoa withTitular(Long titularId) {
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
        this.veiculos,
        this.funcionarioId,
        this.funcionarioNome,
        titularId,
        this.titularNome);
  }

  public static Pessoa mapPessoa(ResultSet rs) throws SQLException {
    return mapPessoa(rs, "pessoa_");
  }

  public static Pessoa mapPessoa(ResultSet rs, String prefix) throws SQLException {
    Long id = rs.getLong(prefix + "id");
    LocalDateTime dataHoraCadastro =
        rs.getTimestamp(prefix + "data_hora_cadastro") != null
            ? rs.getTimestamp(prefix + "data_hora_cadastro").toLocalDateTime()
            : null;
    LocalDate dataNascimento = rs.getObject(prefix + "data_nascimento", LocalDate.class);
    String statusDb = rs.getString(prefix + "status");
    Status status = Status.fromDb(statusDb);
    Long funcionarioId = rs.getObject(prefix + "fk_funcionario", Long.class);
    String funcionarioNome = rs.getString(prefix + "funcionario_nome");
    Long titularId = rs.getObject(prefix + "fk_titular", Long.class);
    String titularNome = rs.getString(prefix + "titular_nome");
    return new Pessoa(
        id,
        dataHoraCadastro,
        rs.getString(prefix + "nome"),
        dataNascimento,
        rs.getString(prefix + "cpf"),
        rs.getString(prefix + "rg"),
        rs.getString(prefix + "email"),
        rs.getString(prefix + "telefone"),
        rs.getString(prefix + "pais"),
        rs.getString(prefix + "estado"),
        rs.getString(prefix + "municipio"),
        rs.getString(prefix + "endereco"),
        rs.getString(prefix + "complemento"),
        rs.getInt(prefix + "vezes_hospedado"),
        rs.getString(prefix + "cep"),
        rs.getObject(prefix + "idade", Integer.class),
        rs.getString(prefix + "bairro"),
        rs.getObject(prefix + "sexo", Integer.class),
        rs.getString(prefix + "numero"),
        status,
        List.of(),
        List.of(),
        funcionarioId,
        funcionarioNome,
        titularId,
        titularNome);
  }

  public enum Status {
    ATIVO("ATIVO"),
    BLOQUEADO("BLOQUEADO"),
    HOSPEDADO("HOSPEDADO"),
    CONTRATADO("CONTRATADO"),
    DEMITIDO("DEMITIDO");

    private final String dbValue;

    Status(String dbValue) {
      this.dbValue = dbValue;
    }

    public String toDb() {
      return dbValue;
    }

    public static Status fromDb(String dbValue) {
      if (dbValue == null) return ATIVO;
      for (Status s : values()) {
        if (s.dbValue.equalsIgnoreCase(dbValue)) {
          return s;
        }
      }
      return ATIVO;
    }
  }
}
