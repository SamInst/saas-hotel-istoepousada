package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.RowMapper;

public record Orcamento(
    @NotNull Long id,
    @NotNull String nome_solicitante,
    @NotNull Funcionario.Nome funcionario,
    @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime data_hora_registro,
    @NotNull List<Hospedagem> hospedagens) {

  public record Request(
      Long id,
      @NotNull String nome_solicitante,
      @NotNull @NotBlank List<Hospedagem.Request> hospedagens) {}

  public static final RowMapper<Orcamento> MAPPER =
      (rs, rowNum) ->
          new Orcamento(
              rs.getLong("orcamento_id"),
              rs.getString("orcamento_nome_solicitante"),
              new Funcionario.Nome(
                  rs.getLong("orcamento_funcionario_id"),
                  rs.getString("orcamento_funcionario_nome")),
              rs.getTimestamp("orcamento_data_hora_registro").toLocalDateTime(),
              null);
}
