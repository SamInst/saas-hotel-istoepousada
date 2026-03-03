package saas.hotel.istoepousada.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.HistoricoFuncionario;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;

@Repository
public class HistoricoFuncionarioRepository {

  private final JdbcTemplate jdbcTemplate;

  public HistoricoFuncionarioRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<HistoricoFuncionario> listarPorFuncionario(Long funcionarioId) {
    String sql =
        """
                SELECT
                  hf.id AS id,
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

    return jdbcTemplate.query(
        sql, (rs, rowNum) -> HistoricoFuncionario.mapHistoricoFuncionario(rs), funcionarioId);
  }

  public HistoricoFuncionario findById(Long id) {
    String sql =
        """
                SELECT
                  hf.id AS id,
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
      return jdbcTemplate.queryForObject(
          sql, (rs, rowNum) -> HistoricoFuncionario.mapHistoricoFuncionario(rs), id);
    } catch (EmptyResultDataAccessException ex) {
      throw new NotFoundException("Histórico não encontrado para o id: " + id);
    }
  }

  @Transactional
  public HistoricoFuncionario save(HistoricoFuncionario historico) {
    if (historico.id() == null) return insert(historico);
    update(historico);
    return findById(historico.id());
  }

  private HistoricoFuncionario insert(HistoricoFuncionario historico) {
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

    Long generatedId =
        keyHolder.getKeys() != null && keyHolder.getKeys().containsKey("id")
            ? ((Number) keyHolder.getKeys().get("id")).longValue()
            : null;

    return new HistoricoFuncionario(
        generatedId, historico.cargo(), historico.funcionario(), historico.salario());
  }

  @Transactional
  public void update(HistoricoFuncionario historico) {
    findById(historico.id());

    String sql =
        """
                UPDATE historico_funcionario
                SET cargo_id = ?, funcionario_id = ?, salario = ?
                WHERE id = ?
                """;

    jdbcTemplate.update(
        sql,
        historico.cargo().id(),
        historico.funcionario().id(),
        historico.salario(),
        historico.id());
  }
}
