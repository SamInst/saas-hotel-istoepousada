package saas.hotel.istoepousada.dto;

import static saas.hotel.istoepousada.dto.TipoPagamento.mapTipoPagamento;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public record DiariaPagamento(
    Long id, String descricao, Float valor, LocalDateTime data_hora, TipoPagamento tipo_pagamento) {

  public static DiariaPagamento mapDiariaPagamento(ResultSet rs) throws SQLException {
    return mapDiariaPagamento(rs, "diaria_pagamento_");
  }

  public static DiariaPagamento mapDiariaPagamento(ResultSet rs, String prefix)
      throws SQLException {
    Long id = rs.getObject(prefix + "id", Long.class);
    if (id == null) return null;

    String descricao = rs.getString(prefix + "descricao");

    Float valor = null;
    Object v = rs.getObject(prefix + "valor");
    if (v instanceof Number n) valor = n.floatValue();

    LocalDateTime dataHora =
        rs.getTimestamp(prefix + "data_hora") != null
            ? rs.getTimestamp(prefix + "data_hora").toLocalDateTime()
            : null;

    // >>> AQUI: usar o mesmo prefixo do pagamento para o tipo de pagamento <<<
    TipoPagamento tipoPagamento = mapTipoPagamento(rs, prefix + "tipo_pagamento_");

    return new DiariaPagamento(id, descricao, valor, dataHora, tipoPagamento);
  }
}
