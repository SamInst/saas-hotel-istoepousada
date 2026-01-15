package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public record Notificacao(
    Long id,
    Long fkPessoa,
    String nome,
    String descricao,
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime dataHora) {
  public static Notificacao mapNotificacao(ResultSet rs) throws SQLException {
    Timestamp ts = rs.getTimestamp("data_hora");
    LocalDateTime dataHora = ts != null ? ts.toLocalDateTime() : null;

    return new Notificacao(
        rs.getLong("id"),
        rs.getLong("fk_pessoa"),
        rs.getString("nome_pessoa"),
        rs.getString("descricao"),
        dataHora);
  }
}
