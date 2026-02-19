package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Histórico de preço por item")
public record HistoricoPrecoItem(
    Long id,
    @Schema(description = "Data/hora do registro") @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
        LocalDateTime dataHoraRegistro,
    @Schema(description = "Valor de compra por unidade") Double valorCompraUnidade,
    @Schema(description = "Valor de venda por unidade") Double valorVendaUnidade,
    @Schema(description = "ID do funcionário responsável") Long funcionarioId,
    @Schema(description = "Nome do funcionário responsável") String funcionarioNome) {}
