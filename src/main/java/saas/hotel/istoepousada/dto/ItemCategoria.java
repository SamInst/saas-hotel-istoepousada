package saas.hotel.istoepousada.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public record ItemCategoria(
        Long id,
        String categoria,
        String descricao,
        @JsonFormat(pattern = "dd/MM/yyyy HH:mm") LocalDateTime dataRegistroCategoria
) {

  public static ItemCategoria mapItemCategoria(ResultSet rs) throws SQLException {
    return mapItemCategoria(rs, "categoria_");
  }

  public static ItemCategoria mapItemCategoria(ResultSet rs, String prefix) throws SQLException {
    Long id = rs.getObject(prefix + "id", Long.class);
    String categoria = rs.getString(prefix + "categoria");
    String descricao = rs.getString(prefix + "descricao");
    LocalDateTime dataRegistro =
            rs.getTimestamp(prefix + "data_registro_categoria") != null
                    ? rs.getTimestamp(prefix + "data_registro_categoria").toLocalDateTime()
                    : null;

    return new ItemCategoria(id, categoria, descricao, dataRegistro);
  }
}
