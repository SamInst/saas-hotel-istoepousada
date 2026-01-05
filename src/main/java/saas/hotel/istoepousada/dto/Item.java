package saas.hotel.istoepousada.dto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

import static saas.hotel.istoepousada.dto.Categoria.mapCategoria;

public record Item(
        Long id,
        String descricao,
        Categoria categoria,
        LocalDateTime data_hora_registro
) {

    public static Item mapItem(ResultSet rs) throws SQLException {
        return mapItem(rs, "item_");
    }

    public static Item mapItem(ResultSet rs, String prefix) throws SQLException {
        Long id = rs.getObject(prefix + "id", Long.class);
        if (id == null) return null;

        String descricao = rs.getString(prefix + "descricao");

        LocalDateTime dh =
                rs.getTimestamp(prefix + "data_hora_registro") != null
                        ? rs.getTimestamp(prefix + "data_hora_registro").toLocalDateTime()
                        : null;

        Categoria categoria = mapCategoria(rs, "categoria_item_");

        return new Item(id, descricao, categoria, dh);
    }
}
