package saas.hotel.istoepousada.repository;

import static saas.hotel.istoepousada.dto.Objeto.*;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestClient;
import saas.hotel.istoepousada.dto.Objeto;
import saas.hotel.istoepousada.dto.ViaCep;

@Repository
public class LocalidadeRepository {

  private final JdbcTemplate jdbcTemplate;
  private final RestClient client;

  public LocalidadeRepository(JdbcTemplate jdbcTemplate, RestClient client) {
    this.jdbcTemplate = jdbcTemplate;
    this.client = client;
  }

  public List<Objeto> listarPaises() {
    String sql =
            """
                SELECT id, descricao
                FROM public.pais
                ORDER BY descricao
            """;

    return jdbcTemplate.query(sql, mapObjeto);
  }

  public List<Objeto> listarEstadosPorPais(Long pais) {
    String sql =
            """
                SELECT id, descricao
                FROM public.estado
                WHERE fk_pais = ?
                ORDER BY descricao
            """;

    return jdbcTemplate.query(sql, mapObjeto, pais);
  }

  public List<Objeto> listarMunicipiosPorEstado(Long estado) {
    String sql =
            """
                SELECT id, descricao
                FROM public.municipio
                WHERE municipio.fk_estado = ?
                ORDER BY descricao
            """;

    return jdbcTemplate.query(sql, mapObjeto, estado);
  }

  public ViaCep buscarPorCep(String cep) {
    String cepLimpo = limparCep(cep);
    return client.get()
            .uri("/ws/{cep}/json/", cepLimpo)
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(ViaCep.class);
  }

  public Optional<Objeto> buscarPaisPorNome(String nomePais) {
    String sql =
            """
                SELECT id, descricao
                FROM public.pais
                WHERE LOWER(TRIM(descricao)) = LOWER(TRIM(?))
                LIMIT 1
            """;

    try {
      return Optional.ofNullable(jdbcTemplate.queryForObject(sql, mapObjeto, nomePais));
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  public Optional<Objeto> buscarEstadoPorNome(String nomeEstado) {
    String sql =
            """
                SELECT id, descricao
                FROM public.estado
                WHERE LOWER(TRIM(descricao)) = LOWER(TRIM(?))
                LIMIT 1
            """;

    try {
      return Optional.ofNullable(jdbcTemplate.queryForObject(sql, mapObjeto, nomeEstado));
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  public Optional<Objeto> buscarMunicipioPorNome(String nomeMunicipio, Long estadoId) {
    String sql =
            """
                SELECT id, descricao
                FROM public.municipio
                WHERE LOWER(TRIM(descricao)) = LOWER(TRIM(?))
                  AND fk_estado = ?
                LIMIT 1
            """;

    try {
      return Optional.ofNullable(jdbcTemplate.queryForObject(sql, mapObjeto, nomeMunicipio, estadoId));
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  private String limparCep(String cep) {
    if (cep == null) return "";
    return cep.replaceAll("\\D", "");
  }
}
