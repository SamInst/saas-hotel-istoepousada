package saas.hotel.istoepousada.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import saas.hotel.istoepousada.dto.Funcionario;
import saas.hotel.istoepousada.dto.RelatorioHistorico;

@Repository
public class RelatorioHistoricoRepository {
  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  public RelatorioHistoricoRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
  }

  /** Grava um evento de alteração do relatório com a lista de campos que mudaram. */
  public void registrar(
      Long relatorioId,
      Long funcionarioId,
      String acao,
      List<RelatorioHistorico.Alteracao> alteracoes) {
    if (alteracoes == null || alteracoes.isEmpty()) {
      return; // nada mudou — não registra
    }
    String alteracoesJson;
    try {
      alteracoesJson = objectMapper.writeValueAsString(alteracoes);
    } catch (Exception e) {
      throw new IllegalStateException("Falha ao serializar alterações do relatório.", e);
    }

    jdbcTemplate.update(
        """
            INSERT INTO relatorio_historico (fk_relatorio, fk_funcionario, acao, data_hora, alteracoes)
            VALUES (?, ?, ?, now(), ?::jsonb)
            """,
        relatorioId,
        funcionarioId,
        acao,
        alteracoesJson);
  }

  /** Lista o histórico de alterações de um relatório, do mais recente para o mais antigo. */
  public List<RelatorioHistorico> buscarPorRelatorio(Long relatorioId) {
    String sql =
        """
            SELECT
                h.id                     AS id,
                h.acao                   AS acao,
                h.data_hora              AS data_hora,
                h.alteracoes             AS alteracoes,
                f.id                     AS funcionario_id,
                p.nome                   AS funcionario_nome
            FROM relatorio_historico h
            LEFT JOIN funcionario f ON f.id = h.fk_funcionario
            LEFT JOIN pessoa p ON p.id = f.fk_pessoa
            WHERE h.fk_relatorio = ?
            ORDER BY h.data_hora DESC, h.id DESC
            """;

    return jdbcTemplate.query(
        sql,
        (rs, rowNum) -> {
          List<RelatorioHistorico.Alteracao> alteracoes;
          try {
            String json = rs.getString("alteracoes");
            alteracoes =
                json == null ? List.of() : objectMapper.readValue(json, new TypeReference<>() {});
          } catch (Exception e) {
            alteracoes = List.of();
          }

          Long funcId = rs.getObject("funcionario_id", Long.class);
          Funcionario.Nome funcionario =
              funcId == null
                  ? null
                  : new Funcionario.Nome(funcId, rs.getString("funcionario_nome"));

          return new RelatorioHistorico(
              rs.getLong("id"),
              rs.getString("acao"),
              rs.getObject("data_hora", LocalDateTime.class),
              funcionario,
              alteracoes);
        },
        relatorioId);
  }
}
