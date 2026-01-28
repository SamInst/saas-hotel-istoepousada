package saas.hotel.istoepousada.repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.Permissao;
import saas.hotel.istoepousada.dto.Tela;

@Repository
public class PermissaoRepository {
  private final JdbcTemplate jdbcTemplate;

  public PermissaoRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<Tela> buscarTelasComPermissoesPorPessoaId(Long pessoaId) {
    String sql =
        """
                SELECT
                  t.id           AS tela_id,
                  t.nome         AS tela_nome,
                  t.descricao    AS tela_descricao,
                  pe.id          AS permissao_id,
                  pe.permissao   AS permissao_permissao,
                  pe.descricao   AS permissao_descricao
                FROM pessoa_permissao pp
                JOIN permissao pe ON pe.id = pp.fk_permissao
                JOIN tela t       ON t.id  = pe.fk_tela
                WHERE pp.fk_pessoa = ?
                ORDER BY t.nome, pe.permissao
                """;

    return jdbcTemplate.query(
        sql,
        rs -> {
          Map<Long, Tela> telas = new LinkedHashMap<>();
          Map<Long, List<Permissao>> perms = new HashMap<>();

          while (rs.next()) {
            Long telaId = rs.getLong("tela_id");
            telas.putIfAbsent(
                telaId,
                new Tela(telaId, rs.getString("tela_nome"), rs.getString("tela_descricao")));

            Permissao p =
                new Permissao(
                    rs.getObject("permissao_id", Long.class),
                    rs.getString("permissao_permissao"),
                    rs.getString("permissao_descricao"));

            perms.computeIfAbsent(telaId, k -> new ArrayList<>()).add(p);
          }
          return telas.values().stream()
              .map(t -> t.withPermissoes(perms.getOrDefault(t.id(), List.of())))
              .toList();
        },
        pessoaId);
  }

  @Transactional
  public int adicionarPermissoes(Long pessoaId, List<Long> permissoesIds) {
    if (pessoaId == null) throw new IllegalArgumentException("pessoaId é obrigatório.");
    if (permissoesIds == null || permissoesIds.isEmpty()) return 0;
    Set<Long> jaExistentes = buscarPermissoesIdsDaPessoa(pessoaId);

    List<Long> novas =
        permissoesIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .filter(id -> !jaExistentes.contains(id))
            .toList();

    if (novas.isEmpty()) return 0;
    String sql =
        """

                        INSERT INTO pessoa_permissao (fk_permissao, fk_pessoa)
                VALUES (?, ?)
                """;

    int[] batch =
        jdbcTemplate.batchUpdate(
            sql,
            new BatchPreparedStatementSetter() {
              @Override
              public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setLong(1, novas.get(i));
                ps.setLong(2, pessoaId);
              }

              @Override
              public int getBatchSize() {
                return novas.size();
              }
            });

    int total = 0;
    for (int v : batch) {
      if (v > 0) total += v;
      else if (v == Statement.SUCCESS_NO_INFO) total += 1;
    }
    return total;
  }

  @Transactional
  public int removerPermissoes(Long pessoaId, List<Long> permissoesIds) {
    if (pessoaId == null) throw new IllegalArgumentException("pessoaId é obrigatório.");
    if (permissoesIds == null || permissoesIds.isEmpty()) return 0;

    List<Long> ids = permissoesIds.stream().filter(Objects::nonNull).distinct().toList();
    if (ids.isEmpty()) return 0;

    String in = String.join(",", Collections.nCopies(ids.size(), "?"));

    String sql =
        "DELETE FROM pessoa_permissao WHERE fk_pessoa = ? AND fk_permissao IN (" + in + ")";

    List<Object> params = new ArrayList<>();
    params.add(pessoaId);
    params.addAll(ids);

    return jdbcTemplate.update(sql, params.toArray());
  }

  private Set<Long> buscarPermissoesIdsDaPessoa(Long pessoaId) {
    String sql =
        """
                SELECT pp.fk_permissao
                FROM pessoa_permissao pp
                WHERE pp.fk_pessoa = ?
                """;
    List<Long> ids = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("fk_permissao"), pessoaId);
    return new HashSet<>(ids);
  }
}
