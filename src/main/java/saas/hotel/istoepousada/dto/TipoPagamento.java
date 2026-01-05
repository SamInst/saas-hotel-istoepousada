package saas.hotel.istoepousada.dto;

import java.sql.ResultSet;
import java.sql.SQLException;

public record TipoPagamento(Long id, String descricao) {

  public static TipoPagamento mapTipoPagamento(ResultSet rs) throws SQLException {
    return mapTipoPagamento(rs, "tipo_pagamento_");
  }

  public static TipoPagamento mapTipoPagamento(ResultSet rs, String prefix) throws SQLException {
    Long id = rs.getObject(prefix + "id", Long.class);
    if (id == null) return null;
    String descricao = rs.getString(prefix + "descricao");
    return new TipoPagamento(id, descricao);
  }

  public enum StatusPagamento {
    PENDENTE,
    CONCLUIDO
  }
}
