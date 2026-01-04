package saas.hotel.istoepousada.dto;

import java.time.LocalDateTime;

public record Item(
        Long id,
        String descricao,
        Categoria categoria,
        LocalDateTime data_hora_registro
) {
}
