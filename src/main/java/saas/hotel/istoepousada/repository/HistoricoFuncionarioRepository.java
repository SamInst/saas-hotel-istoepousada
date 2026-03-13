package saas.hotel.istoepousada.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.Cargo;
import saas.hotel.istoepousada.dto.Funcionario;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;

@Repository
public class HistoricoFuncionarioRepository {

  private final JdbcTemplate jdbcTemplate;

  public HistoricoFuncionarioRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  private static final RowMapper<Funcionario.Historico> HISTORICO_ROW_MAPPER =
      (rs, rowNum) ->
          new Funcionario.Historico(
              rs.getLong("id"),
              new Cargo.Descricao(
                  rs.getObject("cargo_id", Long.class), rs.getString("cargo_descricao")),
              new Funcionario.Nome(
                  rs.getObject("funcionario_id", Long.class),
                  rs.getString("funcionario_descricao")),
              rs.getObject("data_admissao", java.time.LocalDate.class),
              rs.getObject("salario", Float.class));

  public List<Funcionario.Historico> listarPorFuncionario(Long funcionarioId) {
    String sql =
        """
            SELECT
              hf.id AS id,
              f.data_admissao AS data_admissao,
              hf.salario AS salario,
              c.id AS cargo_id,
              c.cargo AS cargo_descricao,
              f.id AS funcionario_id,
              p.nome AS funcionario_descricao
            FROM historico_funcionario hf
            JOIN cargo c ON c.id = hf.cargo_id
            JOIN funcionario f ON f.id = hf.funcionario_id
            JOIN pessoa p ON p.id = f.fk_pessoa
            WHERE hf.funcionario_id = ?
            ORDER BY hf.id DESC
            """;

    return jdbcTemplate.query(sql, HISTORICO_ROW_MAPPER, funcionarioId);
  }

  public Funcionario.Historico findById(Long id) {
    String sql =
        """
            SELECT
              hf.id AS id,
              f.data_admissao AS data_admissao,
              hf.salario AS salario,
              c.id AS cargo_id,
              c.cargo AS cargo_descricao,
              f.id AS funcionario_id,
              p.nome AS funcionario_descricao
            FROM historico_funcionario hf
            JOIN cargo c ON c.id = hf.cargo_id
            JOIN funcionario f ON f.id = hf.funcionario_id
            JOIN pessoa p ON p.id = f.fk_pessoa
            WHERE hf.id = ?
            """;

    try {
      return jdbcTemplate.queryForObject(sql, HISTORICO_ROW_MAPPER, id);
    } catch (EmptyResultDataAccessException ex) {
      throw new NotFoundException("Histórico não encontrado para o id: " + id);
    }
  }

  public Funcionario.Historico insert(Funcionario.Historico.Request historico) {
    String sql =
        """
            INSERT INTO historico_funcionario (cargo_id, funcionario_id, salario)
            VALUES (?, ?, ?)
            """;

    KeyHolder keyHolder = new GeneratedKeyHolder();

    jdbcTemplate.update(
        connection -> {
          PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
          ps.setLong(1, historico.cargo().id());
          ps.setLong(2, historico.funcionario().id());
          ps.setFloat(3, historico.salario());
          return ps;
        },
        keyHolder);

    Number generated = keyHolder.getKey();
    if (generated == null) {
      throw new IllegalStateException("Não foi possível obter o id do histórico inserido.");
    }

    return findById(generated.longValue());
  }

  @Transactional
  public Funcionario.Historico update(Funcionario.Historico.Update historico) {

    String sql =
        """
            UPDATE historico_funcionario
            SET cargo_id = ?,
                salario = ?
            WHERE id = ?
            """;

    jdbcTemplate.update(sql, historico.cargo().id(), historico.salario(), historico.id());
    return findById(historico.id());
  }
}
