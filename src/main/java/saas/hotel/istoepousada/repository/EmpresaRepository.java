package saas.hotel.istoepousada.repository;

import static saas.hotel.istoepousada.dto.Empresa.mapEmpresa;
import static saas.hotel.istoepousada.dto.Pessoa.mapPessoa;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import saas.hotel.istoepousada.dto.Empresa;
import saas.hotel.istoepousada.dto.Pessoa;

@Repository
public class EmpresaRepository {
  private final JdbcTemplate jdbcTemplate;

  public EmpresaRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  private final ResultSetExtractor<List<Empresa>> EMPRESA_COM_PESSOAS_EXTRACTOR =
      rs -> {
        Map<Long, Empresa> empresaMap = new LinkedHashMap<>();
        Map<Long, List<Pessoa>> pessoasPorEmpresa = new HashMap<>();

        while (rs.next()) {
          Long empresaId = rs.getLong("id");

          if (!empresaMap.containsKey(empresaId)) {
            Empresa empresa = mapEmpresa(rs);
            empresaMap.put(empresaId, empresa);
            pessoasPorEmpresa.put(empresaId, new ArrayList<>());
          }

          Long pessoaId = rs.getObject("pessoa_id", Long.class);
          if (pessoaId != null) {
            Pessoa pessoa = mapPessoa(rs, "pessoa_");
            pessoasPorEmpresa.get(empresaId).add(pessoa);
          }
        }

        return empresaMap.entrySet().stream()
            .map(entry -> entry.getValue().withPessoas(pessoasPorEmpresa.get(entry.getKey())))
            .toList();
      };

  /**
   * Busca unificada paginada: - Se 'id' for informado, filtra por e.id - Se 'termo' for informado,
   * filtra por razão social/nome fantasia (ILIKE) ou CNPJ exato - Se ambos vierem, aplica os dois
   * filtros (AND) - Se nenhum vier (id=null e termo vazio/nulo), faz o findAll paginado
   *
   * <p>Paginação é feita em duas etapas: 1) busca os IDs da página (para não duplicar por causa do
   * JOIN) 2) busca os dados completos (empresa + pessoas) via IN (ids)
   */
  public Page<Empresa> buscarPorIdNomeOuCnpj(Long id, String termo, Pageable pageable) {
    boolean hasId = id != null;
    boolean hasTermo = termo != null && !termo.trim().isEmpty();

    String termoTrim = hasTermo ? termo.trim() : null;
    String search = hasTermo ? "%" + termoTrim + "%" : null;

    String baseSelect =
        """
                SELECT
                    e.*,
                    p.id                 as pessoa_id,
                    p.data_hora_cadastro as pessoa_data_hora_cadastro,
                    p.nome               as pessoa_nome,
                    p.data_nascimento    as pessoa_data_nascimento,
                    p.cpf                as pessoa_cpf,
                    p.rg                 as pessoa_rg,
                    p.email              as pessoa_email,
                    p.telefone           as pessoa_telefone,
                    p.fk_pais            as pessoa_fk_pais,
                    p.fk_estado          as pessoa_fk_estado,
                    p.fk_municipio       as pessoa_fk_municipio,
                    p.endereco           as pessoa_endereco,
                    p.complemento        as pessoa_complemento,
                    p.hospedado          as pessoa_hospedado,
                    p.vezes_hospedado    as pessoa_vezes_hospedado,
                    p.cliente_novo       as pessoa_cliente_novo,
                    p.cep                as pessoa_cep,
                    p.idade              as pessoa_idade,
                    p.bairro             as pessoa_bairro,
                    p.sexo               as pessoa_sexo,
                    p.numero             as pessoa_numero
                FROM empresa e
                LEFT JOIN empresa_pessoa ep ON e.id = ep.fk_empresa
                LEFT JOIN pessoa p ON ep.fk_pessoa = p.id
                """;

    StringBuilder where = new StringBuilder(" WHERE 1=1 ");
    List<Object> params = new ArrayList<>();

    if (hasId) {
      where.append(" AND e.id = ? ");
      params.add(id);
    }

    if (hasTermo) {
      where.append(" AND (e.razao_social ILIKE ? OR e.nome_fantasia ILIKE ? OR e.cnpj = ?) ");
      params.add(search);
      params.add(search);
      params.add(termoTrim);
    }

    Long total;
    try {
      String countSql = "SELECT COUNT(*) FROM empresa e" + where;
      total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
    } catch (EmptyResultDataAccessException ex) {
      total = 0L;
    }

    if (total == null || total == 0) {
      return new PageImpl<>(List.of(), pageable, 0);
    }

    String idsSql =
        """
                SELECT e.id
                FROM empresa e
                """
            + where
            + """
                ORDER BY e.razao_social
                LIMIT ? OFFSET ?
                """;

    List<Object> idsParams = new ArrayList<>(params);
    idsParams.add(pageable.getPageSize());
    idsParams.add((int) pageable.getOffset());

    List<Long> ids =
        jdbcTemplate.query(idsSql, (rs, rowNum) -> rs.getLong("id"), idsParams.toArray());

    if (ids.isEmpty()) {
      return new PageImpl<>(List.of(), pageable, total);
    }

    String inPlaceholders = String.join(",", Collections.nCopies(ids.size(), "?"));

    String pageSql =
        (baseSelect
                + """
        WHERE e.id IN (%s)
        ORDER BY e.razao_social, p.nome
        """)
            .formatted(inPlaceholders);

    List<Empresa> content =
        jdbcTemplate.query(pageSql, EMPRESA_COM_PESSOAS_EXTRACTOR, ids.toArray());
    return new PageImpl<>(Objects.requireNonNull(content), pageable, total);
  }

  public Optional<Empresa> findById(Long id) {
    Page<Empresa> page = buscarPorIdNomeOuCnpj(id, null, Pageable.ofSize(1));
    return page.getContent().isEmpty() ? Optional.empty() : Optional.of(page.getContent().get(0));
  }

  @Transactional
  public Empresa save(Empresa empresa) {
    if (empresa.id() == null) {
      return insert(empresa);
    } else {
      update(empresa);
      return findById(empresa.id()).orElse(empresa);
    }
  }

  private Empresa insert(Empresa empresa) {
    String sql =
        """
                INSERT INTO empresa (
                    razao_social,
                    nome_fantasia,
                    cnpj,
                    inscricao_estadual,
                    inscricao_municipal,
                    telefone,
                    email,
                    endereco,
                    cep,
                    numero,
                    complemento,
                    fk_pais,
                    fk_estado,
                    fk_municipio,
                    bairro,
                    tipo_empresa,
                    bloqueado
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
                """;

    KeyHolder keyHolder = new GeneratedKeyHolder();

    jdbcTemplate.update(
        connection -> {
          PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
          int idx = 1;
          ps.setString(idx++, empresa.razaoSocial());
          ps.setString(idx++, empresa.nomeFantasia());
          ps.setString(idx++, empresa.cnpj());
          ps.setString(idx++, empresa.inscricaoEstadual());
          ps.setString(idx++, empresa.inscricaoMunicipal());
          ps.setString(idx++, empresa.telefone());
          ps.setString(idx++, empresa.email());
          ps.setString(idx++, empresa.endereco());
          ps.setString(idx++, empresa.cep());
          ps.setString(idx++, empresa.numero());
          ps.setString(idx++, empresa.complemento());
          ps.setObject(idx++, empresa.fkPais());
          ps.setObject(idx++, empresa.fkEstado());
          ps.setObject(idx++, empresa.fkMunicipio());
          ps.setString(idx++, empresa.bairro());
          ps.setString(idx++, empresa.tipoEmpresa());
          ps.setBoolean(idx++, empresa.bloqueado());
          return ps;
        },
        keyHolder);

    Long generatedId = Objects.requireNonNull(keyHolder.getKey()).longValue();

    return findById(generatedId).orElse(empresa.withId(generatedId));
  }

  @Transactional
  public void update(Empresa empresa) {
    String sql =
        """
                UPDATE empresa SET
                    razao_social = ?,
                    nome_fantasia = ?,
                    cnpj = ?,
                    inscricao_estadual = ?,
                    inscricao_municipal = ?,
                    telefone = ?,
                    email = ?,
                    endereco = ?,
                    cep = ?,
                    numero = ?,
                    complemento = ?,
                    fk_pais = ?,
                    fk_estado = ?,
                    fk_municipio = ?,
                    bairro = ?,
                    tipo_empresa = ?,
                    bloqueado = ?
                WHERE id = ?
                """;

    jdbcTemplate.update(
        sql,
        empresa.razaoSocial(),
        empresa.nomeFantasia(),
        empresa.cnpj(),
        empresa.inscricaoEstadual(),
        empresa.inscricaoMunicipal(),
        empresa.telefone(),
        empresa.email(),
        empresa.endereco(),
        empresa.cep(),
        empresa.numero(),
        empresa.complemento(),
        empresa.fkPais(),
        empresa.fkEstado(),
        empresa.fkMunicipio(),
        empresa.bairro(),
        empresa.tipoEmpresa(),
        empresa.bloqueado(),
        empresa.id());
  }

  @Transactional
  public void vincularPessoas(Long empresaId, List<Long> pessoaIds, Boolean vinculo) {
    String sql =
        Boolean.TRUE.equals(vinculo)
            ? "INSERT INTO empresa_pessoa (fk_empresa, fk_pessoa) VALUES (?, ?) ON CONFLICT DO NOTHING"
            : "DELETE FROM empresa_pessoa WHERE fk_empresa = ? AND fk_pessoa = ?";

    List<Object[]> batchArgs =
        pessoaIds.stream().map(pessoaId -> new Object[] {empresaId, pessoaId}).toList();

    jdbcTemplate.batchUpdate(sql, batchArgs);
  }
}
