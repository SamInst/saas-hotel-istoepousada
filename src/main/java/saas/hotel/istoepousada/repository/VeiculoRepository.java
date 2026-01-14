package saas.hotel.istoepousada.repository;

import static saas.hotel.istoepousada.dto.Veiculo.mapVeiculo;

import java.util.List;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.Veiculo;

@Repository
public class VeiculoRepository {
  private final JdbcTemplate jdbcTemplate;

  public VeiculoRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public Optional<Veiculo> findById(Long id) {
    try {
      String sql =
          """
                    SELECT id, modelo, marca, ano, placa, cor
                    FROM veiculo
                    WHERE id = ?
                    """;
      return Optional.ofNullable(
          jdbcTemplate.queryForObject(sql, (rs, rowNum) -> mapVeiculo(rs), id));
    } catch (EmptyResultDataAccessException ex) {
      return Optional.empty();
    }
  }

  public List<Veiculo> findAll() {
    String sql =
        """
                SELECT id, modelo, marca, ano, placa, cor
                FROM veiculo
                ORDER BY marca, modelo, placa
                """;
    return jdbcTemplate.query(sql, (rs, rowNum) -> mapVeiculo(rs));
  }

  public List<Veiculo> findAllByPessoaId(Long pessoaId) {
    String sql =
        """
                SELECT v.id, v.modelo, v.marca, v.ano, v.placa, v.cor
                FROM pessoa_veiculo pv
                JOIN veiculo v ON v.id = pv.veiculo_id
                WHERE pv.pessoa_id = ?
                ORDER BY pv.vinculo_ativo DESC NULLS LAST, v.marca, v.modelo, v.placa
                """;
    return jdbcTemplate.query(sql, (rs, rowNum) -> mapVeiculo(rs), pessoaId);
  }

  @Transactional
  public Veiculo save(Long pessoa_id, Veiculo veiculo) {
    if (veiculo.id() == null) {
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

      return veiculo.withId(id);
    } else {
      System.out.println(veiculo.id());
      jdbcTemplate.update(
          """
                      UPDATE veiculo SET
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
    }
    vincularPessoa(pessoa_id, veiculo.id());

    return veiculo;
  }

  /**
   * Vincula um veículo a uma pessoa.
   *
   * <p>Regras: - UNIQUE(veiculo_id) => um veículo só pode estar ligado a uma pessoa por vez - Ao
   * vincular, setamos vinculo_ativo=true - Se o veículo já estiver vinculado a outra pessoa, o
   * vínculo será "movido" para a nova pessoa
   */
  @Transactional
  public void vincularPessoa(Long pessoaId, Long veiculoId) {
    String sql =
        """
                INSERT INTO pessoa_veiculo (pessoa_id, veiculo_id, vinculo_ativo)
                VALUES (?, ?, true)
                ON CONFLICT (veiculo_id)
                DO UPDATE SET
                  pessoa_id = EXCLUDED.pessoa_id,
                  vinculo_ativo = true
                """;
    jdbcTemplate.update(sql, pessoaId, veiculoId);
  }

  @Transactional
  public void setVinculoAtivo(Long pessoaId, Long veiculoId, boolean ativo) {
    String sql =
        """
                INSERT INTO pessoa_veiculo (pessoa_id, veiculo_id, vinculo_ativo)
                VALUES (?, ?, ?)
                ON CONFLICT (veiculo_id)
                DO UPDATE SET
                  pessoa_id = EXCLUDED.pessoa_id,
                  vinculo_ativo = EXCLUDED.vinculo_ativo
                """;

    jdbcTemplate.update(sql, pessoaId, veiculoId, ativo);
  }
}
