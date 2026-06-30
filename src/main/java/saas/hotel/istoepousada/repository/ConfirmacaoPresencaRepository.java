package saas.hotel.istoepousada.repository;

import jakarta.annotation.PostConstruct;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.ConfirmacaoPresenca;

@Repository
public class ConfirmacaoPresencaRepository {

  private final JdbcTemplate jdbcTemplate;

  public ConfirmacaoPresencaRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  /** Cria a tabela automaticamente caso ainda não exista (idempotente). */
  @PostConstruct
  void init() {
    jdbcTemplate.execute(
        """
        CREATE TABLE IF NOT EXISTS confirmacao_presenca (
            id            BIGSERIAL PRIMARY KEY,
            nome          TEXT NOT NULL,
            confirmado_em TIMESTAMPTZ NOT NULL DEFAULT now()
        )
        """);
  }

  @Transactional
  public ConfirmacaoPresenca insert(String nome) {
    Long id =
        jdbcTemplate.queryForObject(
            "INSERT INTO confirmacao_presenca (nome) VALUES (?) RETURNING id", Long.class, nome);
    return findById(id);
  }

  public ConfirmacaoPresenca findById(Long id) {
    return jdbcTemplate.queryForObject(
        "SELECT id, nome, confirmado_em FROM confirmacao_presenca WHERE id = ?",
        ConfirmacaoPresenca.ROW_MAPPER,
        id);
  }

  public List<ConfirmacaoPresenca> listar() {
    return jdbcTemplate.query(
        "SELECT id, nome, confirmado_em FROM confirmacao_presenca ORDER BY confirmado_em DESC",
        ConfirmacaoPresenca.ROW_MAPPER);
  }

  public long total() {
    Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM confirmacao_presenca", Long.class);
    return total == null ? 0L : total;
  }
}
