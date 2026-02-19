package saas.hotel.istoepousada.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import org.springframework.data.domain.Page;

@Schema(
    description =
        "Resposta da consulta de relatórios com totais por tipo de pagamento e extrato por dia")
public record RelatorioExtratoResponse(
    @Schema(
            description =
                "Mapa de totais por tipo de pagamento.\n"
                    + "Chave 'TOTAL' representa o agregado geral.\n"
                    + "Demais chaves são as descrições de tipo_pagamento do banco.")
        Map<String, RelatorioPagamentoResumo> pagamentos,
    @Schema(description = "Página de relatórios agrupados por dia") Page<RelatorioDia> page) {}
