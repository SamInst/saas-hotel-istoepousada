package saas.hotel.istoepousada.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record HistoricoHospedagem(
    @NotNull Long id,
    @NotNull String tipo_hospedagem,
    @NotNull Integer quantidade_hospedagem,
    @NotNull Integer total_dias_hospedado,
    @NotNull Float valor_total,
    List<Pernoite> pernoites) {}
