package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

@Schema(
    description =
        "Histórico de recebidos do funcionário (pagamentos vinculados ao histórico_funcionario)")
public record HistoricoRecebidosFuncionario(
    @Schema(description = "ID do registro") Long id,
    @Schema(description = "Histórico do funcionário vinculado")
        HistoricoFuncionario historicoFuncionario,
    @Schema(description = "Valor recebido") Float valorRecebido,
    @Schema(description = "Data/hora início") @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
        LocalDateTime dataHoraInicio,
    @Schema(description = "Data/hora fim") @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
        LocalDateTime dataHoraFim,
    @Schema(description = "Data/hora pagamento") @JsonFormat(pattern = "dd/MM/yyyy HH:mm")
        LocalDateTime dataHoraPagamento,
    @Schema(description = "Tipo de pagamento") TipoPagamento tipoPagamento,
    @Schema(description = "Descrição") String descricao,
    @Schema(description = "Path do arquivo de comprovante") String pathArquivoComprovante) {

  public record HistoricoFuncionario(Long id) {}

  public record TipoPagamento(Long id, String descricao) {}

  public HistoricoRecebidosFuncionario(
      HistoricoFuncionario historicoFuncionario,
      Float valorRecebido,
      LocalDateTime dataHoraInicio,
      LocalDateTime dataHoraFim,
      LocalDateTime dataHoraPagamento,
      TipoPagamento tipoPagamento,
      String descricao,
      String pathArquivoComprovante) {
    this(
        null,
        historicoFuncionario,
        valorRecebido,
        dataHoraInicio,
        dataHoraFim,
        dataHoraPagamento,
        tipoPagamento,
        descricao,
        pathArquivoComprovante);
  }

  public HistoricoRecebidosFuncionario withId(Long id) {
    return new HistoricoRecebidosFuncionario(
        id,
        this.historicoFuncionario,
        this.valorRecebido,
        this.dataHoraInicio,
        this.dataHoraFim,
        this.dataHoraPagamento,
        this.tipoPagamento,
        this.descricao,
        this.pathArquivoComprovante);
  }

  public static HistoricoRecebidosFuncionario mapHistoricoRecebidosFuncionario(ResultSet rs)
      throws SQLException {
    return new HistoricoRecebidosFuncionario(
        rs.getLong("id"),
        new HistoricoFuncionario(rs.getLong("historico_funcionario_id")),
        rs.getFloat("valor_recebido"),
        rs.getObject("data_hora_inicio", LocalDateTime.class),
        rs.getObject("data_hora_fim", LocalDateTime.class),
        rs.getObject("data_hora_pagamento", LocalDateTime.class),
        new TipoPagamento(
            rs.getLong("tipo_pagamento_id"), rs.getString("tipo_pagamento_descricao")),
        rs.getString("descricao"),
        rs.getString("path_arquivo_comprovante"));
  }
}
