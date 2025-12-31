package saas.hotel.istoepousada.dto;

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
    Long fkPais,
    Long fkEstado,
    Long fkMunicipio,
    String endereco,
    String complemento,
    Boolean hospedado,
    Integer vezesHospedado,
    Boolean clienteNovo,
    String cep,
    Integer idade,
    String bairro,
    Short sexo,
    String numero,
    Boolean bloqueado,
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
      Long fkPais,
      Long fkEstado,
      Long fkMunicipio,
      String endereco,
      String complemento,
      String cep,
      Integer idade,
      String bairro,
      Short sexo,
      String numero,
      Boolean bloqueado) {
    this(
        null,
        LocalDateTime.now(),
        nome,
        dataNascimento,
        cpf,
        rg,
        email,
        telefone,
        fkPais,
        fkEstado,
        fkMunicipio,
        endereco,
        complemento,
        false,
        0,
        true,
        cep,
        idade,
        bairro,
        sexo,
        numero,
        bloqueado,
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
        this.fkPais,
        this.fkEstado,
        this.fkMunicipio,
        this.endereco,
        this.complemento,
        this.hospedado,
        this.vezesHospedado,
        this.clienteNovo,
        this.cep,
        this.idade,
        this.bairro,
        this.sexo,
        this.numero,
        this.bloqueado,
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
        this.fkPais,
        this.fkEstado,
        this.fkMunicipio,
        this.endereco,
        this.complemento,
        this.hospedado,
        this.vezesHospedado,
        this.clienteNovo,
        this.cep,
        this.idade,
        this.bairro,
        this.sexo,
        this.numero,
        this.bloqueado,
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
        this.fkPais,
        this.fkEstado,
        this.fkMunicipio,
        this.endereco,
        this.complemento,
        this.hospedado,
        this.vezesHospedado,
        this.clienteNovo,
        this.cep,
        this.idade,
        this.bairro,
        this.sexo,
        this.numero,
        this.bloqueado,
        this.empresasVinculadas,
        veiculos);
  }

  public Pessoa comHospedado(Boolean hospedado) {
    return new Pessoa(
        this.id,
        this.dataHoraCadastro,
        this.nome,
        this.dataNascimento,
        this.cpf,
        this.rg,
        this.email,
        this.telefone,
        this.fkPais,
        this.fkEstado,
        this.fkMunicipio,
        this.endereco,
        this.complemento,
        hospedado,
        this.vezesHospedado,
        this.clienteNovo,
        this.cep,
        this.idade,
        this.bairro,
        this.sexo,
        this.numero,
        this.bloqueado,
        this.empresasVinculadas,
        this.veiculos);
  }

  public Pessoa incrementarHospedagem() {
    return new Pessoa(
        this.id,
        this.dataHoraCadastro,
        this.nome,
        this.dataNascimento,
        this.cpf,
        this.rg,
        this.email,
        this.telefone,
        this.fkPais,
        this.fkEstado,
        this.fkMunicipio,
        this.endereco,
        this.complemento,
        this.hospedado,
        this.vezesHospedado + 1,
        false,
        this.cep,
        this.idade,
        this.bairro,
        this.sexo,
        this.numero,
        this.bloqueado,
        this.empresasVinculadas,
        this.veiculos);
  }

  public static Pessoa mapPessoa(ResultSet rs) throws SQLException {
    return mapPessoa(rs, "");
  }

  public static Pessoa mapPessoa(ResultSet rs, String prefix) throws SQLException {
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
        rs.getObject(prefix + "fk_pais", Long.class),
        rs.getObject(prefix + "fk_estado", Long.class),
        rs.getObject(prefix + "fk_municipio", Long.class),
        rs.getString(prefix + "endereco"),
        rs.getString(prefix + "complemento"),
        rs.getObject(prefix + "hospedado", Boolean.class),
        rs.getObject(prefix + "vezes_hospedado", Integer.class),
        rs.getObject(prefix + "cliente_novo", Boolean.class),
        rs.getString(prefix + "cep"),
        rs.getObject(prefix + "idade", Integer.class),
        rs.getString(prefix + "bairro"),
        rs.getObject(prefix + "sexo", Short.class),
        rs.getString(prefix + "numero"),
        rs.getObject(prefix + "bloqueado", Boolean.class),
        List.of(),
        List.of());
  }

  public boolean possuiEmpresas() {
    return !empresasVinculadas.isEmpty();
  }

  public int totalEmpresas() {
    return empresasVinculadas.size();
  }
}
