package saas.hotel.istoepousada.repository;

import java.util.List;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import saas.hotel.istoepousada.dto.Veiculo;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;

@Repository
public class VeiculoRepository {

  private final JdbcTemplate jdbcTemplate;

  public VeiculoRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public Veiculo findById(Long id) {
    String sql =
        """
            SELECT
                id     as veiculo_id,
                modelo as veiculo_modelo,
                marca  as veiculo_marca,
                ano    as veiculo_ano,
                placa  as veiculo_placa,
                cor    as veiculo_cor
            FROM veiculo
            WHERE id = ?
            """;

    try {
      return jdbcTemplate.queryForObject(sql, Veiculo.ROW_MAPPER, id);
    } catch (EmptyResultDataAccessException ex) {
      throw new NotFoundException("Veiculo nao encontrado para o uuid: " + id);
    }
  }

  public Veiculo findByPlaca(String placa) {
    String sql =
        """
                SELECT
                    id     as veiculo_id,
                    modelo as veiculo_modelo,
                    marca  as veiculo_marca,
                    ano    as veiculo_ano,
                    placa  as veiculo_placa,
                    cor    as veiculo_cor
                FROM veiculo
                WHERE placa = ?
                """;
    try {

      return jdbcTemplate.queryForObject(sql, Veiculo.ROW_MAPPER, placa);
    } catch (EmptyResultDataAccessException ex) {
      return null;
    }
  }

  public List<Veiculo> findAllByPessoaId(Long pessoa_id) {
    String sql =
        """
            SELECT
                v.id     as veiculo_id,
                v.modelo as veiculo_modelo,
                v.marca  as veiculo_marca,
                v.ano    as veiculo_ano,
                v.placa  as veiculo_placa,
                v.cor    as veiculo_cor
            FROM pessoa_veiculo pv
            JOIN veiculo v ON v.id = pv.veiculo_id
            WHERE pv.pessoa_id = ?
            ORDER BY pv.vinculo_ativo DESC NULLS LAST, v.marca, v.modelo, v.placa
            """;

    return jdbcTemplate.query(sql, Veiculo.ROW_MAPPER, pessoa_id);
  }

  public Veiculo create(Veiculo.Request veiculo) {
    Long id =
        jdbcTemplate.queryForObject(
            """
                    INSERT INTO veiculo (modelo, marca, ano, placa, cor)
                    VALUES (?, ?, ?, ?, ?)
                    RETURNING id
                    """,
            Long.class,
            veiculo.modelo(),
            veiculo.marca(),
            veiculo.ano(),
            veiculo.placa(),
            veiculo.cor());
    return findById(id);
  }

  public Veiculo update(Veiculo.Update veiculo) {
    jdbcTemplate.update(
        """
            UPDATE veiculo
            SET
              modelo = ?,
              marca = ?,
              ano = ?,
              placa = ?,
              cor = ?
            WHERE id = ?
            """,
        veiculo.modelo(),
        veiculo.marca(),
        veiculo.ano(),
        veiculo.placa(),
        veiculo.cor(),
        veiculo.id());

    return findById(veiculo.id());
  }

  public void setVinculo(Veiculo.Vincular vinculo) {
    String sql =
        """
            INSERT INTO pessoa_veiculo (pessoa_id, veiculo_id, vinculo_ativo)
            VALUES (?, ?, ?)
            ON CONFLICT (veiculo_id)
            DO UPDATE SET
              pessoa_id = EXCLUDED.pessoa_id,
              vinculo_ativo = EXCLUDED.vinculo_ativo
            """;

    jdbcTemplate.update(sql, vinculo.pessoa().id(), vinculo.veiculo().id(), vinculo.ativo());
  }
}
