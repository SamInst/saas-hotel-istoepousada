package saas.hotel.istoepousada.dto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

import static saas.hotel.istoepousada.dto.Item.mapItem;
import static saas.hotel.istoepousada.dto.TipoPagamento.mapTipoPagamento;

public record DiariaConsumo(
        Long id,
        LocalDateTime data_hora,
        Item item,
        Integer quantidade,
        TipoPagamento tipo_pagamento
) {

    public static DiariaConsumo mapDiariaConsumo(ResultSet rs) throws SQLException {
        return mapDiariaConsumo(rs, "diaria_consumo_");
    }

    public static DiariaConsumo mapDiariaConsumo(ResultSet rs, String prefix) throws SQLException {
        Long id = rs.getObject(prefix + "id", Long.class);
        if (id == null) return null;

        LocalDateTime dataHora =
                rs.getTimestamp(prefix + "data_hora") != null
                        ? rs.getTimestamp(prefix + "data_hora").toLocalDateTime()
                        : null;

        Integer quantidade = rs.getObject(prefix + "quantidade", Integer.class);

        TipoPagamento tipoPagamento = mapTipoPagamento(rs, "tipo_pagamento_cons_");

        Item item = mapItem(rs, "item_");

        return new DiariaConsumo(id, dataHora, item, quantidade, tipoPagamento);
    }
}
