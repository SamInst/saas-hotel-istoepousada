package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public record Pagamento(
    @NotNull UUID uuid,
    @NotNull TipoPagamento tipo_pagamento,
    @NotNull Funcionario.Nome funcionario,
    @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss") LocalDateTime data_hora_registro,
    String nome_pagador,
    String descricao,
    @NotNull Double valor,
    Boolean cancelado,
    Desconto desconto,
    String path_arquivo) {
  public record Id(@NotNull UUID id) {}

  public record Request(
      @NotNull TipoPagamento.Id tipo_pagamento,
      @NotNull Funcionario.Id funcionario,
      @NotNull String nome_pagador,
      String descricao,
      @NotNull Double valor,
      Desconto.Request desconto,
      MultipartFile arquivo) {}

  public record Update(
      @NotNull UUID uuid,
      @NotNull TipoPagamento.Id tipo_pagamento,
      @NotNull Funcionario.Id funcionario,
      @NotNull String nome_pagador,
      String descricao,
      @NotNull Double valor,
      Desconto.Update desconto,
      MultipartFile arquivo) {}

  public record Desconto(
      @NotNull UUID uuid,
      @NotNull Funcionario.Nome funcionario,
      Integer porcentagem,
      Double valor,
      @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss") LocalDateTime data_hora_registro) {
    public record Request(
        @NotNull Pagamento.Id pagamento,
        @NotNull Funcionario.Id funcionario,
        Integer porcentagem,
        Double valor) {}

    public record Update(
        @NotNull UUID id, @NotNull Funcionario.Id funcionario, Integer porcentagem, Double valor) {}
  }

  public record TipoPagamento(Long id, String descricao) {
    public record Id(@NotNull Long id) {}

    public record Request(@NotNull String descricao) {}

    public record Update(@NotNull Long id, @NotNull String descricao) {}
  }
}
