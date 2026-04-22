package saas.hotel.istoepousada.repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import saas.hotel.istoepousada.dto.Objeto;
import saas.hotel.istoepousada.dto.Permissao;
import saas.hotel.istoepousada.dto.Tela;

@Repository
public class ObjectRepository {
  private final JdbcTemplate jdbcTemplate;

  public ObjectRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<Objeto> tipoPagamento() {
    return jdbcTemplate.query("select id, descricao from tipo_pagamento", Objeto.mapObjeto);
  }

  public List<Objeto> telas() {
    return jdbcTemplate.query("select id, nome as descricao from tela", Objeto.mapObjeto);
  }

  public List<Tela> telasComPermissoes() {
    String sql =
        """
        SELECT
            t.id          AS tela_id,
            t.nome        AS tela_nome,
            t.descricao   AS tela_descricao,
            p.id          AS permissao_id,
            p.permissao   AS permissao_permissao,
            p.descricao   AS permissao_descricao
        FROM tela t
        LEFT JOIN permissao p ON p.fk_tela = t.id
        ORDER BY t.nome, p.permissao
        """;

    return jdbcTemplate.query(
        sql,
        rs -> {
          Map<Long, Tela> telasMap = new LinkedHashMap<>();
          Map<Long, List<Permissao>> permissoesPorTela = new LinkedHashMap<>();

          while (rs.next()) {
            Long telaId = rs.getObject("tela_id", Long.class);
            if (telaId == null) continue;

            telasMap.putIfAbsent(
                telaId,
                new Tela(telaId, rs.getString("tela_nome"), rs.getString("tela_descricao"), null));
            permissoesPorTela.putIfAbsent(telaId, new ArrayList<>());

            Long permissaoId = rs.getObject("permissao_id", Long.class);
            if (permissaoId != null) {
              permissoesPorTela
                  .get(telaId)
                  .add(
                      new Permissao(
                          permissaoId,
                          rs.getString("permissao_permissao"),
                          rs.getString("permissao_descricao")));
            }
          }

          return telasMap.entrySet().stream()
              .map(
                  e -> {
                    Tela t = e.getValue();
                    return new Tela(
                        t.id(),
                        t.nome(),
                        t.descricao(),
                        permissoesPorTela.getOrDefault(e.getKey(), List.of()));
                  })
              .toList();
        });
  }

  public List<Objeto> permissoes(Long telaId) {
    return jdbcTemplate.query(
        "select id, permissao.permissao as descricao from permissao where fk_tela = ?",
        Objeto.mapObjeto,
        telaId);
  }

  public List<Objeto> cargos() {
    return jdbcTemplate.query("select id, cargo.cargo as descricao from cargo", Objeto.mapObjeto);
  }

  public Objeto findById(Long id) {
    return jdbcTemplate.queryForObject(
        "select id, descricao from tipo_pagamento where id = ?", Objeto.mapObjeto, id);
  }
}
