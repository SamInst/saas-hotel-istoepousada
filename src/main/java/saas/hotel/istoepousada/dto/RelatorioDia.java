package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Grupo de relatórios por dia")
public record RelatorioDia(
    @JsonFormat(pattern = "dd/MM/yyyy")
    @Schema(description = "Data do grupo") LocalDate data,
    @Schema(description = "Total do dia (somente valores positivos)") Float totalDia,
    @Schema(description = "Relatórios do dia") List<Relatorio> content) {}
