package saas.hotel.istoepousada.repository;

import static saas.hotel.istoepousada.dto.Objeto.*;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import saas.hotel.istoepousada.dto.Objeto;

@Repository
public class LocalidadeRepository {

  private final JdbcTemplate jdbcTemplate;

  public LocalidadeRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<Objeto> listarPaises() {
    String sql =
        """
            SELECT id, descricao
            FROM public.paises
            ORDER BY descricao
        """;

    return jdbcTemplate.query(sql, mapObjeto);
  }

  public List<Objeto> listarEstadosPorPais(Long fkPais) {
    String sql =
        """
            SELECT id, descricao
            FROM public.estados
            WHERE fk_pais = ?
            ORDER BY descricao
        """;

    return jdbcTemplate.query(sql, mapObjeto, fkPais);
  }

  public List<Objeto> listarMunicipiosPorEstado(Long fkEstado) {
    String sql =
        """
            SELECT id, descricao
            FROM public.municipios
            WHERE fk_municipio = ?
            ORDER BY descricao
        """;

    return jdbcTemplate.query(sql, mapObjeto, fkEstado);
  }
}
