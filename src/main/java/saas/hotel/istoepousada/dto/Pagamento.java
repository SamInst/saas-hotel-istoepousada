package saas.hotel.istoepousada.dto.pagamento;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import saas.hotel.istoepousada.dto.Funcionario;
import saas.hotel.istoepousada.dto.TipoPagamento;

import java.time.LocalDateTime;
import java.util.UUID;

public record Pagamento(
    @NotNull UUID id,
    @NotNull TipoPagamento tipo_pagamento,
    @NotNull Funcionario.Nome funcionario,
    @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss") LocalDateTime data_hora_registro,
    String nome_pagador,
    String descricao,
    @NotNull Float valor,
    Desconto desconto) {
  public record Request(
      TipoPagamento.Request.Id tipo_pagamento_id,
      Funcionario funcionario_id,
      String nome_pagador,
      String descricao,
      Float valor) {
    public record Id(UUID id){}
  }

  public record Desconto(
      @NotNull UUID id,
      @NotNull Funcionario.Nome funcionario,
      Integer porcentagem,
      Float valor,
      @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss") LocalDateTime data_hora_registro) {
    public record Request(
            @NotNull Pagamento.Request.Id pagamento_id,
            @NotNull Funcionario.Request.Id funcionario_id,
            Integer porcentagem,
            Float valor
    ){}
  }
}
