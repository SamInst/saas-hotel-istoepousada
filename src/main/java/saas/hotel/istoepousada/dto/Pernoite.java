package saas.hotel.istoepousada.dto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;

public record Pernoite(
        Long id,
        LocalDate data_entrada,
        LocalDate data_saida,
        String status_pernoite_enum,
        LocalTime hora_chegada,
        LocalTime hora_saida,
        Float valot_total,
        Boolean ativo
) {

    public static Pernoite mapPernoite(ResultSet rs) throws SQLException {
        return mapPernoite(rs, "");
    }

    /**
     * Espera colunas com os aliases:
     * - {prefix}id
     * - {prefix}data_entrada
     * - {prefix}data_saida
     * - {prefix}status_pernoite_enum
     * - {prefix}hora_chegada
     * - {prefix}hora_saida
     * - {prefix}valot_total
     * - {prefix}ativo
     */
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

        Short status = rs.getObject(prefix + "status_pernoite_enum", Short.class);
        String statusStr = status != null ? String.valueOf(status) : null;

        LocalTime horaChegada = rs.getTime(prefix + "hora_chegada") != null
                ? rs.getTime(prefix + "hora_chegada").toLocalTime()
                : null;

        LocalTime horaSaida = rs.getTime(prefix + "hora_saida") != null
                ? rs.getTime(prefix + "hora_saida").toLocalTime()
                : null;

        Double valorTotalDb = rs.getObject(prefix + "valot_total", Double.class);
        Float valorTotal = valorTotalDb != null ? valorTotalDb.floatValue() : null;

        Boolean ativo = rs.getObject(prefix + "ativo", Boolean.class);

        return new Pernoite(
                id,
                dataEntrada,
                dataSaida,
                statusStr,
                horaChegada,
                horaSaida,
                valorTotal,
                ativo
        );
    }
}
