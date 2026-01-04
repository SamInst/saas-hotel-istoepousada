package saas.hotel.istoepousada.repository;

import static saas.hotel.istoepousada.dto.Notificacao.mapNotificacao;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import saas.hotel.istoepousada.dto.Notificacao;

@Repository
public class NotificacaoRepository {
  private final JdbcTemplate jdbcTemplate;

  public NotificacaoRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<Notificacao> listarPorQuantidade(Integer quantidade) {
    String sql =
        """
                SELECT id, fk_pessoa, nome_pessoa, descricao, data_hora
                  FROM notificacao
                 ORDER BY data_hora DESC
                 LIMIT ?
                """;

    return jdbcTemplate.query(sql, (rs, rowNum) -> mapNotificacao(rs), quantidade);
  }

  public List<Notificacao> listarPorPessoa(Long pessoaId, Integer quantidade) {
    String sql =
        """
                SELECT id, fk_pessoa, nome_pessoa, descricao, data_hora
                  FROM notificacao
                 WHERE fk_pessoa = ?
                 ORDER BY data_hora DESC
                 LIMIT ?
                """;

    return jdbcTemplate.query(sql, (rs, rowNum) -> mapNotificacao(rs), pessoaId, quantidade);
  }

  public Notificacao criar(Long fkPessoa, String nome, String descricao) {
    String sql =
        """
                INSERT INTO notificacao (fk_pessoa, nome_pessoa, descricao, data_hora)
                VALUES (?, ?, ?, NOW())
                RETURNING id, fk_pessoa, nome_pessoa, descricao, data_hora
                """;

    try {
      return jdbcTemplate.queryForObject(
          sql, (rs, rowNum) -> mapNotificacao(rs), fkPessoa, nome, descricao);
    } catch (EmptyResultDataAccessException ex) {
      return new Notificacao(null, fkPessoa, nome, descricao, LocalDateTime.now());
    }
  }
}
