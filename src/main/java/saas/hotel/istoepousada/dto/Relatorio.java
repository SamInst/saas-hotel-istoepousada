package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

@Schema(description = "Relatório financeiro / operacional")
public record Relatorio(
    @Schema(description = "ID do relatório") Long id,
    @Schema(description = "Data e hora do lançamento") @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
        LocalDateTime dataHora,
    @Schema(description = "Descrição do relatório") String relatorio,
    @Schema(description = "Valor") Double valor,
    @Schema(description = "Pessoa responsável (funcionário)") Pessoa funcionario,
    @Schema(description = "ID do tipo de pagamento") Long tipoPagamentoId,
    @Schema(description = "Descrição do tipo de pagamento") String tipoPagamentoDescricao,
    @Schema(description = "ID do quarto (opcional)") Long quartoId,
    @Schema(description = "Descrição do quarto") String quartoDescricao) {

  public Relatorio withId(Long id) {
    return new Relatorio(
        id,
        this.dataHora,
        this.relatorio,
        this.valor,
        this.funcionario,
        this.tipoPagamentoId,
        this.tipoPagamentoDescricao,
        this.quartoId,
        this.quartoDescricao);
  }

  public static Relatorio mapRelatorio(ResultSet rs) throws SQLException {
    Pessoa funcionario = Pessoa.mapPessoa(rs, "funcionario_");

    LocalDateTime dataHora =
        rs.getTimestamp("data_hora") != null
            ? rs.getTimestamp("data_hora").toLocalDateTime()
            : null;

    return new Relatorio(
        rs.getLong("relatorio_id"),
        dataHora,
        rs.getString("relatorio"),
        rs.getObject("valor", Double.class),
        funcionario,
        rs.getObject("tipo_pagamento_id", Long.class),
        rs.getString("tipo_pagamento_descricao"),
        rs.getObject("quarto_id", Long.class),
        rs.getString("quarto_descricao"));
  }

  @Schema(description = "Request para criar/atualizar relatório")
  public record RelatorioRequest(
      @Schema(description = "Data/hora do lançamento", example = "2026-01-27T14:30:00")
          LocalDateTime dataHora,
      @Schema(description = "Descrição do relatório", example = "Pagamento de diária")
          String relatorio,
      @Schema(description = "Valor", example = "250.00") Double valor,
      @Schema(description = "ID do tipo de pagamento", example = "1") Long tipoPagamentoId,
      @Schema(description = "ID do quarto (opcional)", example = "5") Long quartoId) {}
}
