package saas.hotel.istoepousada.dto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;

public record Pernoite(
    Long id,
    LocalDate data_entrada,
    LocalDate data_saida,
    Status status,
    LocalTime hora_chegada,
    LocalTime hora_saida,
    Float valot_total,
    Boolean ativo) {

  public static Pernoite mapPernoite(ResultSet rs) throws SQLException {
    return mapPernoite(rs, "pernoite_");
  }

  public static Pernoite mapPernoite(ResultSet rs, String prefix) throws SQLException {
    Long id = rs.getObject(prefix + "id", Long.class);

    LocalDate dataEntrada =
        rs.getDate(prefix + "data_entrada") != null
            ? rs.getDate(prefix + "data_entrada").toLocalDate()
            : null;

    LocalDate dataSaida =
        rs.getDate(prefix + "data_saida") != null
            ? rs.getDate(prefix + "data_saida").toLocalDate()
            : null;

    String statusStr = rs.getString(prefix + "status");
    Pernoite.Status status = (statusStr == null) ? null : Pernoite.Status.valueOf(statusStr);

    LocalTime horaChegada =
        rs.getTime(prefix + "hora_chegada") != null
            ? rs.getTime(prefix + "hora_chegada").toLocalTime()
            : null;

    LocalTime horaSaida =
        rs.getTime(prefix + "hora_saida") != null
            ? rs.getTime(prefix + "hora_saida").toLocalTime()
            : null;

    Double valorTotalDb = rs.getObject(prefix + "valor_total", Double.class);
    Float valorTotal = valorTotalDb != null ? valorTotalDb.floatValue() : null;

    Boolean ativo = rs.getObject(prefix + "ativo", Boolean.class);

    return new Pernoite(
        id, dataEntrada, dataSaida, status, horaChegada, horaSaida, valorTotal, ativo);
  }

  @Getter
  public enum Status {
    ATIVO,
    DIARIA_ENCERRADA,
    FINALIZADO,
    CANCELADO,
    FINALIZADO_PAGAMENTO_PENDENTE
  }
}
