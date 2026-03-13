package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.RowMapper;

public record Pessoa(
    @NotNull Long id,
    @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_registro,
    @NotNull @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data_nascimento,
    @NotNull String nome,
    @NotNull String cpf,
    String rg,
    @NotNull String email,
    @NotNull String telefone,
    String pais,
    String estado,
    String municipio,
    String endereco,
    String complemento,
    Integer vezes_hospedado,
    @NotNull String cep,
    Integer idade,
    String bairro,
    Integer sexo,
    String numero,
    @NotNull Status status,
    List<Empresa> empresas_vinculadas,
    List<Veiculo> veiculos_vinculados,
    @NotNull Funcionario.Nome funcionario,
    Pessoa.Nome titular,
    List<Pessoa> acompanhantes) {
  public record Id(Long id) {}

  public record Request(
      @NotNull String nome,
      @NotNull @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data_nascimento,
      @NotNull String cpf,
      @NotNull String email,
      @NotNull String telefone,
      @NotNull String cep,
      String rg,
      String pais,
      String estado,
      String municipio,
      String endereco,
      String complemento,
      String bairro,
      Integer sexo,
      String numero,
      @NotNull Status status,
      List<Veiculo> veiculos,
      @NotNull Funcionario.Id funcionario,
      Pessoa.Id titular,
      List<Pessoa.Id> acompanhantes) {}

  public record Update(
      @NotNull Long id,
      @NotNull @JsonFormat(pattern = "dd/MM/yyyy") LocalDate data_nascimento,
      @NotNull String nome,
      @NotNull String cpf,
      String rg,
      @NotNull String email,
      @NotNull String telefone,
      String pais,
      String estado,
      String municipio,
      String endereco,
      String complemento,
      @NotNull String cep,
      String bairro,
      Integer sexo,
      String numero,
      @NotNull Status status,
      List<Veiculo.Update> veiculos_vinculados,
      @NotNull Funcionario.Id funcionario,
      Pessoa.Id titular,
      List<Empresa.Id> empresas) {}

  public record Nome(@NotNull Long id, @NotNull String nome) {}

  public record DadosPrincipais(
      @NotNull Long id,
      @NotNull String nome,
      @NotNull LocalDate data_nascimento,
      @NotNull String cpf,
      @NotNull String email,
      @NotNull String telefone,
      @NotNull Status status,
      @NotNull Boolean titular) {
    public static final RowMapper<Pessoa.DadosPrincipais> ROW_MAPPER =
        (rs, rowNum) ->
            new Pessoa.DadosPrincipais(
                rs.getLong("pessoa_id"),
                rs.getString("pessoa_nome"),
                rs.getObject("pessoa_data_nascimento", LocalDate.class),
                rs.getString("pessoa_cpf"),
                rs.getString("pessoa_email"),
                rs.getString("pessoa_telefone"),
                Pessoa.Status.map(rs.getString("pessoa_status")),
                rs.getObject("pessoa_titular", Boolean.class));
  }

  public enum Status {
    ATIVO,
    BLOQUEADO,
    HOSPEDADO,
    CONTRATO_ATIVO,
    CONTRATO_ENCERRADO;

    public static Status map(String status) {
      if (status == null || status.isBlank()) return ATIVO;
      try {
        return Status.valueOf(status.trim().toUpperCase());
      } catch (IllegalArgumentException ex) {
        return ATIVO;
      }
    }
  }

  public static final RowMapper<Pessoa> ROW_MAPPER =
      (rs, row_num) ->
          new Pessoa(
              rs.getLong("id"),
              rs.getObject("data_hora_registro", LocalDateTime.class),
              rs.getObject("data_nascimento", LocalDate.class),
              rs.getString("nome"),
              rs.getString("cpf"),
              rs.getString("rg"),
              rs.getString("email"),
              rs.getString("telefone"),
              rs.getString("pais"),
              rs.getString("estado"),
              rs.getString("municipio"),
              rs.getString("endereco"),
              rs.getString("complemento"),
              rs.getObject("vezes_hospedado", Integer.class),
              rs.getString("cep"),
              rs.getObject("idade", Integer.class),
              rs.getString("bairro"),
              rs.getObject("sexo", Integer.class),
              rs.getString("numero"),
              Status.map(rs.getString("status")),
              List.of(),
              List.of(),
              new Funcionario.Nome(
                  rs.getObject("funcionario_id", Long.class), rs.getString("funcionario_nome")),
              rs.getObject("titular_id", Long.class) == null
                  ? null
                  : new Pessoa.Nome(
                      rs.getObject("titular_id", Long.class), rs.getString("titular_nome")),
              List.of());

  public record BatchRequest(List<Pessoa.Request> pessoas, List<Empresa.Id> empresas) {}

  public record VinculoVeiculo(Pessoa.Id pessoa, Veiculo.Id veiculo, Boolean vinculo) {}
}
