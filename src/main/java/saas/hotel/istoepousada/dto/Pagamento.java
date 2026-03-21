package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.multipart.MultipartFile;

public record Pagamento(
    @NotNull UUID uuid,
    @NotNull TipoPagamento tipo_pagamento,
    @NotNull Funcionario.Nome funcionario,
    @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss") LocalDateTime data_hora_registro,
    String nome_pagador,
    String descricao,
    @NotNull Float valor,
    Boolean cancelado,
    Desconto desconto,
    String path_arquivo) {
  public record Uuid(@NotNull UUID uuid) {}

  public record Request(
      @NotNull TipoPagamento.Id tipo_pagamento,
      @NotNull String nome_pagador,
      String descricao,
      @NotNull Float valor,
      Desconto.Request desconto,
      MultipartFile arquivo) {}

  public record Update(
      @NotNull UUID uuid,
      @NotNull TipoPagamento.Id tipo_pagamento,
      @NotNull String nome_pagador,
      String descricao,
      @NotNull Float valor,
      Desconto.Update desconto,
      MultipartFile arquivo) {}

  public record Desconto(
      @NotNull UUID uuid,
      @NotNull Funcionario.Nome funcionario,
      Integer porcentagem,
      Float valor,
      @NotNull @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss") LocalDateTime data_hora_registro) {
    public record Request(@NotNull Pagamento.Uuid pagamento, Integer porcentagem, Float valor) {}

    public record Update(@NotNull UUID uuid, Integer porcentagem, Float valor) {}

    public static final RowMapper<Desconto> ROW_MAPPER =
        (rs, row_num) -> {
          UUID desconto_id = rs.getObject("pagamento_desconto_id", UUID.class);
          return desconto_id == null
              ? null
              : new Desconto(
                  rs.getObject("pagamento_desconto_id", UUID.class),
                  Funcionario.Nome.ROW_MAPPER_PAGAMENTO_DESCONTO.mapRow(rs, row_num),
                  rs.getObject("pagamento_desconto_porcentagem", Integer.class),
                  rs.getObject("pagamento_desconto_valor", Float.class),
                  rs.getObject("pagamento_desconto_data_hora_registro", LocalDateTime.class));
        };
  }

  public record TipoPagamento(Long id, String descricao) {
    public record Id(@NotNull Long id) {}

    public record Request(@NotNull String descricao) {}

    public record Update(@NotNull Long id, @NotNull String descricao) {}

    public static final RowMapper<TipoPagamento> ROW_MAPPER =
        (rs, row_num) -> {
          Long tipoPagamentoId = rs.getObject("tipo_pagamento_id", Long.class);
          return tipoPagamentoId == null
              ? null
              : new TipoPagamento(tipoPagamentoId, rs.getString("tipo_pagamento_descricao"));
        };
  }

  public static final RowMapper<Pagamento> ROW_MAPPER =
      (rs, row_num) -> {
        UUID pagamentoId = rs.getObject("pagamento_id", UUID.class);
        if (pagamentoId == null) {
          return null;
        }

        return new Pagamento(
            pagamentoId,
            TipoPagamento.ROW_MAPPER.mapRow(rs, row_num),
            Funcionario.Nome.ROW_MAPPER_PAGAMENTO.mapRow(rs, row_num),
            rs.getObject("pagamento_data_hora_registro", LocalDateTime.class),
            rs.getString("pagamento_nome_pagador"),
            rs.getString("pagamento_descricao"),
            rs.getObject("pagamento_valor", Float.class) == null
                ? 0F
                : rs.getObject("pagamento_valor", Float.class),
            rs.getBoolean("pagamento_cancelado"),
            Pagamento.Desconto.ROW_MAPPER.mapRow(rs, row_num),
            rs.getString("pagamento_path_arquivo"));
      };
}
