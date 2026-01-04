package saas.hotel.istoepousada.dto;

import java.sql.ResultSet;
import java.sql.SQLException;

public record Categoria(
        Long id,
        String categoria
) {

    public static Categoria mapCategoria(ResultSet rs) throws SQLException {
        return mapCategoria(rs, "");
    }

    /**
     * Espera colunas com os aliases:
     * - {prefix}id
     * - {prefix}categoria
     */
    public static Categoria mapCategoria(ResultSet rs, String prefix) throws SQLException {
        Long id = rs.getObject(prefix + "id", Long.class);
        String categoria = rs.getString(prefix + "categoria");
        return new Categoria(id, categoria);
    }
}
