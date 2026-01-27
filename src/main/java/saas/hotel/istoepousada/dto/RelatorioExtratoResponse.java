package saas.hotel.istoepousada.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

@Schema(
    description =
        "Resposta da consulta de relatórios com totais calculados pelos filtros informados")
public record RelatorioExtratoResponse(
    @Schema(description = "Balanço geral = totalEntradas + totalSaidas") Float balancoGeral,
    @Schema(description = "Soma de valores positivos") Float totalEntradas,
    @Schema(description = "Soma de valores negativos") Float totalSaidas,
    @Schema(description = "Soma de valores positivos com fk_tipo_pagamento = 1 (Dinheiro)")
        Float totalDinheiro,
    @Schema(description = "Soma de valores negativos com fk_tipo_pagamento = 1 (Dinheiro)")
        Float totalDinheiroSaida,
    @Schema(description = "Balanço dinheiro = totalDinheiro + totalDinheiroSaida")
        Float balancoDinheiro,
    @Schema(description = "Página de relatórios") Page<Relatorio> page) {}
