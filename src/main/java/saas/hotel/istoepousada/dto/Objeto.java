package saas.hotel.istoepousada.dto;

import org.springframework.jdbc.core.RowMapper;

public record Objeto(Long id, String descricao) {
  public static final RowMapper<Objeto> mapObjeto =
      (rs, rowNum) -> new Objeto(rs.getLong("id"), rs.getString("descricao"));
}
