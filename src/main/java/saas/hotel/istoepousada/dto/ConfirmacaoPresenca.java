package saas.hotel.istoepousada.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;
import org.springframework.jdbc.core.RowMapper;

/** Confirmação de presença (RSVP) do Chá de Bebê do Vicente. */
public record ConfirmacaoPresenca(Long id, String nome, OffsetDateTime confirmadoEm) {

  public record Request(@NotBlank String nome) {}

  public static final RowMapper<ConfirmacaoPresenca> ROW_MAPPER =
      (rs, rowNum) ->
          new ConfirmacaoPresenca(
              rs.getLong("id"),
              rs.getString("nome"),
              rs.getObject("confirmado_em", OffsetDateTime.class));
}
