package saas.hotel.istoepousada.dto.pagamento;

import com.fasterxml.jackson.annotation.JsonFormat;
import saas.hotel.istoepousada.dto.Funcionario;
import saas.hotel.istoepousada.dto.TipoPagamento;

import java.time.LocalDateTime;
import java.util.UUID;

public record Pagamento(
    UUID id,
    TipoPagamento tipo_pagamento,
    Funcionario.Nome funcionario,
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss") LocalDateTime data_hora_registro,
    String nome_pagador,
    String descricao,
    Float valor,
    Desconto desconto) {
  public record Request(
      Long tipo_pagamento_id,
      Long funcionario_id,
      String nome_pagador,
      String descricao,
      Float valor) {}

  public record Desconto(
          UUID id,
          Funcionario.Nome funcionario,
          Integer porcentagem,
          Float valor,
          @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss") LocalDateTime data_hora_registro
  ){}
}
