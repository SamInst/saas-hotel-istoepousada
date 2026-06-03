package saas.hotel.istoepousada.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import saas.hotel.istoepousada.dto.Empresa;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;

@Repository
public class EmpresaRepository {
  private final JdbcTemplate jdbcTemplate;

  public EmpresaRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public Page<Empresa> findByIdNomeOuCnpj(Long id, String termo, Pageable pageable) {
    boolean has_id = id != null;
    boolean has_termo = termo != null && !termo.trim().isEmpty();
    String termo_trim = has_termo ? termo.trim() : null;
    String search = has_termo ? "%" + termo_trim + "%" : null;

    StringBuilder where = new StringBuilder(" WHERE 1=1 ");
    List<Object> params = new ArrayList<>();

    if (has_id) {
      where.append(" AND e.id = ? ");
      params.add(id);
    }

    if (has_termo) {
      where.append(" AND (e.razao_social ILIKE ? OR e.nome_fantasia ILIKE ? OR e.cnpj = ?) ");
      params.add(search);
      params.add(search);
      params.add(termo_trim);
    }

    long total =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM empresa e" + where, Long.class, params.toArray());

    if (total == 0) {
      return new PageImpl<>(List.of(), pageable, 0);
    }

    List<Object> query_params = new ArrayList<>(params);
    query_params.add(pageable.getPageSize());
    query_params.add(pageable.getOffset());

    String sql =
        """
                        SELECT
                            e.id as empresa_id,
                            e.data_hora_registro as empresa_data_hora_registro,
                            e.razao_social AS empresa_razao_social,
                            e.nome_fantasia AS empresa_nome_fantasia,
                            e.cnpj AS empresa_cnpj,
                            e.telefone AS empresa_telefone,
                            e.email AS empresa_email,
                            e.endereco AS empresa_endereco,
                            e.cep AS empresa_cep,
                            e.numero AS empresa_numero,
                            e.complemento AS empresa_complemento,
                            e.pais AS empresa_pais,
                            e.estado AS empresa_estado,
                            e.municipio AS empresa_municipio,
                            e.bairro AS empresa_bairro,
                            e.tipo_empresa AS empresa_tipo_empresa,
                            e.status AS empresa_status,
                            f.id    AS empresa_funcionario_id,
                            pf.nome AS empresa_funcionario_nome
                        FROM empresa e
                        LEFT JOIN funcionario f ON f.fk_pessoa = e.fk_funcionario
                        LEFT JOIN pessoa pf ON pf.id = f.fk_pessoa

                        """
            + where
            + """
                        ORDER BY e.razao_social
                        LIMIT ? OFFSET ?
                        """;

    List<Empresa> empresas = jdbcTemplate.query(sql, Empresa.ROW_MAPPER, query_params.toArray());

    return new PageImpl<>(empresas, pageable, total);
  }

  public Optional<Empresa> findById(Long id) {
    Page<Empresa> page = findByIdNomeOuCnpj(id, null, Pageable.ofSize(1));
    if (page.isEmpty()) throw new NotFoundException("Empresa não cadastrada para o id: " + id);

    return Optional.of(page.getContent().getFirst());
  }

  public Empresa create(Empresa.Request empresa, Long funcionarioId) {
    var empresa_id =
        jdbcTemplate.queryForObject(
            """
            INSERT INTO empresa (
                cnpj,
                telefone,
                email,
                endereco,
                cep,
                numero,
                complemento,
                pais,
                estado,
                municipio,
                bairro,
                razao_social,
                nome_fantasia,
                tipo_empresa,
                status,
                fk_funcionario,
                data_hora_registro
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?,?,?,?, 'ATIVO'::empresa_status, ?, now())
              returning id
            """,
            Long.class,
            empresa.cnpj(),
            empresa.telefone(),
            empresa.email(),
            empresa.endereco(),
            empresa.cep(),
            empresa.numero(),
            empresa.complemento(),
            empresa.pais(),
            empresa.estado(),
            empresa.municipio(),
            empresa.bairro(),
            empresa.razao_social(),
            empresa.nome_fantasia(),
            empresa.tipo_empresa(),
            funcionarioId);

    return findByIdNomeOuCnpj(empresa_id, null, Pageable.ofSize(1)).getContent().getFirst();
  }

  public Empresa update(Empresa.Update empresa, Long funcionarioId) {
    String sql =
        """
        UPDATE empresa SET
            razao_social = ?,
            nome_fantasia = ?,
            cnpj = ?,
            telefone = ?,
            email = ?,
            endereco = ?,
            cep = ?,
            numero = ?,
            complemento = ?,
            pais = ?,
            estado = ?,
            municipio = ?,
            bairro = ?,
            tipo_empresa = ?,
            status = ?::empresa_status,
            fk_funcionario = ?
        WHERE id = ?
        """;

    String status =
        empresa.status() == null ? Empresa.Status.ATIVO.name() : empresa.status().name();

    jdbcTemplate.update(
        sql,
        empresa.razao_social(),
        empresa.nome_fantasia(),
        empresa.cnpj(),
        empresa.telefone(),
        empresa.email(),
        empresa.endereco(),
        empresa.cep(),
        empresa.numero(),
        empresa.complemento(),
        empresa.pais(),
        empresa.estado(),
        empresa.municipio(),
        empresa.bairro(),
        empresa.tipo_empresa(),
        status,
        funcionarioId,
        empresa.id());

    return findByIdNomeOuCnpj(empresa.id(), null, Pageable.ofSize(1)).getContent().getFirst();
  }

  public void vincularPessoa(Empresa.Vincular vinculo) {
    String sql =
        Boolean.TRUE.equals(vinculo.ativo())
            ? "INSERT INTO empresa_pessoa (fk_empresa, fk_pessoa) VALUES (?, ?) ON CONFLICT DO NOTHING"
            : "DELETE FROM empresa_pessoa WHERE fk_empresa = ? AND fk_pessoa = ?";

    jdbcTemplate.update(sql, vinculo.empresa().id(), vinculo.pessoa().id());
  }
}
