package saas.hotel.istoepousada.dto;

import java.time.LocalDateTime;

public record DiariaPagamento(
        Long id,
        Float valor,
        Diaria diaria,
        LocalDateTime data_hora,
        TipoPagamento tipo_pagamento
) {
}
