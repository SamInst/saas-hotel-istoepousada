package saas.hotel.istoepousada.dto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

import static saas.hotel.istoepousada.dto.Quarto.*;

public record Diaria(
        LocalDate dataInicio,
        LocalDate dataFim,
        Float valorDiaria,
        Float total,
        Integer numeroDiaria,
        Integer quantidadePessoas,
        Quarto quarto,
        String observacao
) {

    public static Diaria mapDiaria(ResultSet rs) throws SQLException {
        return mapDiaria(rs, "diaria_");
    }

    public static Diaria mapDiaria(ResultSet rs, String prefix) throws SQLException {
        LocalDate dataInicio =
                rs.getDate(prefix + "data_inicio") != null
                        ? rs.getDate(prefix + "data_inicio").toLocalDate()
                        : null;

        LocalDate dataFim =
                rs.getDate(prefix + "data_fim") != null
                        ? rs.getDate(prefix + "data_fim").toLocalDate()
                        : null;

        Float valorDiaria = rs.getObject(prefix + "valor", Double.class) != null
                ? rs.getObject(prefix + "valor", Double.class).floatValue()
                : null;

        Float total = rs.getObject(prefix + "total", Double.class) != null
                ? rs.getObject(prefix + "total", Double.class).floatValue()
                : null;

        Integer numeroDiaria = rs.getObject(prefix + "numero", Integer.class);

        Integer quantidadePessoas = rs.getObject(prefix + "quantidade_pessoa", Integer.class);

        String observacao = rs.getString(prefix + "observacao");

        Quarto quarto = mapQuarto(rs);

        return new Diaria(
                dataInicio,
                dataFim,
                valorDiaria,
                total,
                numeroDiaria,
                quantidadePessoas,
                quarto,
                observacao
        );
    }
}
