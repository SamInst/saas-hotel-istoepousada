package saas.hotel.istoepousada.dto;

import java.time.LocalDateTime;

public record DiariaConsumo(
        Long id,
        LocalDateTime data_hora,
        Diaria diaria,
        Item item,
        Integer quantidade,
        TipoPagamento tipo_pagamento
) {
}
