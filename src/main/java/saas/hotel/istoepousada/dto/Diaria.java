package saas.hotel.istoepousada.dto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public record Diaria(
        LocalDate dataInicio,
        LocalDate dataFim,
        Float valorDiaria,
        Float total,
        Integer numeroDiaria,
        Integer quantidadePessoas,
        String observacao
) {

    public static Diaria mapDiaria(ResultSet rs) throws SQLException {
        return mapDiaria(rs, "");
    }

    /**
     * Espera colunas com os aliases:
     * - {prefix}data_inicio
     * - {prefix}data_fim
     * - {prefix}valor_diaria
     * - {prefix}total
     * - {prefix}numero_diaria
     * - {prefix}quantidade_pessoa
     * - {prefix}observaca}
     */
    public static Diaria mapDiaria(ResultSet rs, String prefix) throws SQLException {
        LocalDate dataInicio =
                rs.getDate(prefix + "data_inicio") != null
                        ? rs.getDate(prefix + "data_inicio").toLocalDate()
                        : null;

        LocalDate dataFim =
                rs.getDate(prefix + "data_fim") != null
                        ? rs.getDate(prefix + "data_fim").toLocalDate()
                        : null;

        Float valorDiaria = rs.getObject(prefix + "valor_diaria", Double.class) != null
                ? rs.getObject(prefix + "valor_diaria", Double.class).floatValue()
                : null;

        Float total = rs.getObject(prefix + "total", Double.class) != null
                ? rs.getObject(prefix + "total", Double.class).floatValue()
                : null;

        Integer numeroDiaria = rs.getObject(prefix + "numero_diaria", Integer.class);

        Integer quantidadePessoas = rs.getObject(prefix + "quantidade_pessoa", Integer.class);

        String observacao = rs.getString(prefix + "observacao");

        return new Diaria(
                dataInicio,
                dataFim,
                valorDiaria,
                total,
                numeroDiaria,
                quantidadePessoas,
                observacao
        );
    }
}
