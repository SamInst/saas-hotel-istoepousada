package saas.hotel.istoepousada.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Totais financeiros por tipo de pagamento")
public record RelatorioPagamentoResumo(
        @Schema(description = "Receitas (valores positivos)") Double receitas,
        @Schema(description = "Despesas (valores negativos em módulo)") Double despesas,
        @Schema(description = "Lucro = receitas - despesas") Double lucro) {

    public static RelatorioPagamentoResumo of(Double receitas, Double despesas) {
        double r = receitas != null ? receitas : 0d;
        double d = despesas != null ? despesas : 0d;
        return new RelatorioPagamentoResumo(r, d, r - d);
    }
}
