package saas.hotel.istoepousada.repository;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import saas.hotel.istoepousada.dto.Objeto;

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

  public List<Objeto> permissoes(Long telaId) {
    return jdbcTemplate.query(
        "select id, permissao.permissao as descricao from permissao where fk_tela = ?",
            Objeto.mapObjeto, telaId);
  }

  public Objeto findById(Long id) {
    return jdbcTemplate.queryForObject(
        "select id, descricao from tipo_pagamento where id = ?", Objeto.mapObjeto, id);
  }
}
