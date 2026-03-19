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
import saas.hotel.istoepousada.dto.Funcionario;
import saas.hotel.istoepousada.dto.Pessoa;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;

@Repository
public class EmpresaRepository {
  private final JdbcTemplate jdbcTemplate;
  private final PessoaRepository pessoaRepository;

  public EmpresaRepository(JdbcTemplate jdbcTemplate, PessoaRepository pessoaRepository) {
    this.jdbcTemplate = jdbcTemplate;
    this.pessoaRepository = pessoaRepository;
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
                            e.id,
                            e.data_hora_registro,
                            e.razao_social,
                            e.nome_fantasia,
                            e.cnpj,
                            e.telefone,
                            e.email,
                            e.endereco,
                            e.cep,
                            e.numero,
                            e.complemento,
                            e.pais,
                            e.estado,
                            e.municipio,
                            e.bairro,
                            e.tipo_empresa,
                            e.status,
                            f.id    AS funcionario_id,
                            pf.nome AS funcionario_nome
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

    List<Empresa> content =
        empresas.stream()
            .map(
                empresa -> {
                  List<Pessoa> pessoas_vinculadas = findPessoasByEmpresaId(empresa.id());
                  return new Empresa(
                      empresa.id(),
                      empresa.data_hora_registro(),
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
                      empresa.status(),
                      new Funcionario.Nome(
                          empresa.funcionario().id(), empresa.funcionario().nome()),
                      pessoas_vinculadas);
                })
            .toList();

    return new PageImpl<>(content, pageable, total);
  }

  public Optional<Empresa> findById(Long id) {
    Page<Empresa> page = findByIdNomeOuCnpj(id, null, Pageable.ofSize(1));
    if (page.isEmpty()) throw new NotFoundException("Empresa não cadastrada para o uuid: " + id);

    return Optional.of(page.getContent().getFirst());
  }

  public Empresa create(Empresa.Request empresa) {
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
            getFuncionarioIdLogado());

    return findByIdNomeOuCnpj(empresa_id, null, Pageable.ofSize(1)).getContent().getFirst();
  }

  public Empresa update(Empresa.Update empresa) {
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
        getFuncionarioIdLogado(),
        empresa.id());

    return findByIdNomeOuCnpj(empresa.id(), null, Pageable.ofSize(1)).getContent().getFirst();
  }

  public List<Pessoa> findPessoasByEmpresaId(Long empresa_id) {
    String sql =
        """
                SELECT
                    p.id,
                    p.data_hora_registro AS data_hora_registro,
                    p.data_nascimento,
                    p.nome,
                    p.cpf,
                    p.rg,
                    p.email,
                    p.telefone,
                    p.pais,
                    p.estado,
                    p.municipio,
                    p.endereco,
                    p.complemento,
                    p.vezes_hospedado,
                    p.cep,
                    p.idade,
                    p.bairro,
                    p.sexo,
                    p.numero,
                    p.status,
                    f.id AS funcionario_id,
                    pf.nome AS funcionario_nome,
                    t.id AS titular_id,
                    t.nome AS titular_nome
                FROM empresa_pessoa ep
                JOIN pessoa p ON p.id = ep.fk_pessoa
                LEFT JOIN funcionario f ON f.id = p.fk_funcionario
                LEFT JOIN pessoa pf ON pf.id = f.fk_pessoa
                LEFT JOIN pessoa t ON t.id = p.fk_titular
                WHERE ep.fk_empresa = ?
                ORDER BY p.nome
                """;

    return jdbcTemplate.query(sql, Pessoa.ROW_MAPPER, empresa_id);
  }

  public void vincularPessoa(Empresa.Vincular vinculo) {
    String sql =
        Boolean.TRUE.equals(vinculo.ativo())
            ? "INSERT INTO empresa_pessoa (fk_empresa, fk_pessoa) VALUES (?, ?) ON CONFLICT DO NOTHING"
            : "DELETE FROM empresa_pessoa WHERE fk_empresa = ? AND fk_pessoa = ?";

    jdbcTemplate.update(sql, vinculo.empresa().id(), vinculo.pessoa().id());
  }

  public Long getFuncionarioIdLogado() {
    return pessoaRepository.getFuncionarioIdFromRequest();
  }
}
