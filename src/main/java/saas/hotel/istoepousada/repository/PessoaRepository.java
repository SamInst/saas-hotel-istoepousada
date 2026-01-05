package saas.hotel.istoepousada.repository;

import static saas.hotel.istoepousada.dto.Empresa.mapEmpresa;
import static saas.hotel.istoepousada.dto.Pessoa.mapPessoa;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.Period;
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
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;

@Repository
public class PessoaRepository {

  private final JdbcTemplate jdbcTemplate;

  public PessoaRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  private final ResultSetExtractor<List<Pessoa>> PESSOA_COM_EMPRESAS_EXTRACTOR =
      rs -> {
        Map<Long, Pessoa> pessoaMap = new LinkedHashMap<>();
        Map<Long, List<Empresa>> empresasPorPessoa = new HashMap<>();

        while (rs.next()) {
          Long pessoaId = rs.getLong("pessoa_id");

          if (!pessoaMap.containsKey(pessoaId)) {
            Pessoa pessoa = mapPessoa(rs);
            pessoaMap.put(pessoaId, pessoa);
            empresasPorPessoa.put(pessoaId, new ArrayList<>());
          }

          Long empresaId = rs.getObject("empresa_id", Long.class);
          if (empresaId != null) {
            Empresa empresa = mapEmpresa(rs);
            empresasPorPessoa.get(pessoaId).add(empresa);
          }
        }

        return pessoaMap.entrySet().stream()
            .map(entry -> entry.getValue().withEmpresas(empresasPorPessoa.get(entry.getKey())))
            .toList();
      };

  /**
   * Busca unificada paginada: - id != null => filtra por p.id - termo preenchido => filtra por
   * p.nome ILIKE %termo% OU p.cpf = termo - hospedados == true => filtra por p.hospedado = true -
   * se todos forem nulos/vazios => faz findAll paginado
   *
   * <p>Ordenação: p.nome ASC (sempre)
   *
   * <p>Paginação é feita em duas etapas para não duplicar por JOIN: 1) busca IDs da pessoa da
   * página 2) carrega dados completos (pessoa + empresas) via IN (ids)
   */
  public Page<Pessoa> buscarPorIdNomeCpfOuHospedados(
      Long id, String termo, Boolean hospedados, Pageable pageable) {
    boolean hasId = id != null;
    boolean hasTermo = termo != null && !termo.trim().isEmpty();
    boolean onlyHospedados = Boolean.TRUE.equals(hospedados);

    String termoTrim = hasTermo ? termo.trim() : null;
    String search = hasTermo ? "%" + termoTrim + "%" : null;

    String baseSelect =
        """
            SELECT
                p.id                   AS pessoa_id,
                p.data_hora_cadastro   AS pessoa_data_hora_cadastro,
                p.nome                 AS pessoa_nome,
                p.data_nascimento      AS pessoa_data_nascimento,
                p.cpf                  AS pessoa_cpf,
                p.rg                   AS pessoa_rg,
                p.email                AS pessoa_email,
                p.telefone             AS pessoa_telefone,
                p.fk_pais              AS pessoa_fk_pais,
                p.fk_estado            AS pessoa_fk_estado,
                p.fk_municipio         AS pessoa_fk_municipio,
                p.endereco             AS pessoa_endereco,
                p.complemento          AS pessoa_complemento,
                p.vezes_hospedado      AS pessoa_vezes_hospedado,
                p.cep                  AS pessoa_cep,
                p.idade                AS pessoa_idade,
                p.bairro               AS pessoa_bairro,
                p.sexo                 AS pessoa_sexo,
                p.numero               AS pessoa_numero,
                p.status               AS pessoa_status,
                e.id                   as empresa_id,
                e.razao_social         as empresa_razao_social,
                e.nome_fantasia        as empresa_nome_fantasia,
                e.cnpj                 as empresa_cnpj,
                e.inscricao_estadual   as empresa_inscricao_estadual,
                e.inscricao_municipal  as empresa_inscricao_municipal,
                e.telefone             as empresa_telefone,
                e.email                as empresa_email,
                e.endereco             as empresa_endereco,
                e.cep                  as empresa_cep,
                e.numero               as empresa_numero,
                e.complemento          as empresa_complemento,
                e.fk_pais              as empresa_fk_pais,
                e.fk_estado            as empresa_fk_estado,
                e.fk_municipio         as empresa_fk_municipio,
                e.bairro               as empresa_bairro,
                e.tipo_empresa         as empresa_tipo_empresa,
                e.bloqueado            as empresa_bloqueado
            FROM pessoa p
            LEFT JOIN empresa_pessoa ep ON p.id = ep.fk_pessoa
            LEFT JOIN empresa e ON ep.fk_empresa = e.id
        """;

    // WHERE dinâmico: se nada vier, fica WHERE 1=1 => findAll paginado
    StringBuilder where = new StringBuilder(" WHERE 1=1 ");
    List<Object> params = new ArrayList<>();

    if (hasId) {
      where.append(" AND p.id = ? ");
      params.add(id);
    }

    if (hasTermo) {
      where.append(" AND (p.nome ILIKE ? OR p.cpf = ?) ");
      params.add(search);
      params.add(termoTrim);
    }

    if (onlyHospedados) {
      where.append(" AND p.hospedado = true ");
    }

    Long total;
    try {
      String countSql = "SELECT COUNT(*) FROM pessoa p" + where;
      total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
    } catch (EmptyResultDataAccessException ex) {
      total = 0L;
    }

    if (total == null || total == 0) {
      return new PageImpl<>(List.of(), pageable, 0);
    }

    String idsSql =
        """
            SELECT p.id
            FROM pessoa p
        """
            + where
            + """
            ORDER BY p.nome ASC
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
        baseSelect
            + " WHERE p.id IN ("
            + inPlaceholders
            + ") "
            + " ORDER BY p.nome, e.razao_social";

    List<Pessoa> content =
        jdbcTemplate.query(pageSql, PESSOA_COM_EMPRESAS_EXTRACTOR, ids.toArray());

    return new PageImpl<>(Objects.requireNonNull(content), pageable, total);
  }

  public Optional<Pessoa> findById(Long id) {
    Page<Pessoa> page = buscarPorIdNomeCpfOuHospedados(id, null, null, Pageable.ofSize(1));
    return page.getContent().isEmpty()
        ? Optional.empty()
        : Optional.of(page.getContent().getFirst());
  }

  @Transactional
  public Pessoa save(Pessoa pessoa) {
    if (pessoa.id() == null) {
      return insert(pessoa);
    } else {
      update(pessoa);
      return findById(pessoa.id())
          .orElseThrow(
              () -> new NotFoundException("Pessoa não encontrada com o id: " + pessoa.id()));
    }
  }

  private Pessoa insert(Pessoa pessoa) {
    String sql =
        """
        INSERT INTO pessoa (
            data_hora_cadastro,
            nome,
            data_nascimento,
            cpf,
            rg,
            email,
            telefone,
            fk_pais,
            fk_estado,
            fk_municipio,
            endereco,
            complemento,
            vezes_hospedado,
            cep,
            idade,
            bairro,
            sexo,
            numero
        ) VALUES (now(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?, ?, ?)
        """;

    KeyHolder keyHolder = new GeneratedKeyHolder();

    jdbcTemplate.update(
        connection -> {
          PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
          int idx = 1;
          ps.setString(idx++, pessoa.nome());
          ps.setDate(
              idx++,
              pessoa.dataNascimento() != null ? Date.valueOf(pessoa.dataNascimento()) : null);
          ps.setString(idx++, pessoa.cpf());
          ps.setString(idx++, pessoa.rg());
          ps.setString(idx++, pessoa.email());
          ps.setString(idx++, pessoa.telefone());
          ps.setObject(idx++, pessoa.fkPais());
          ps.setObject(idx++, pessoa.fkEstado());
          ps.setObject(idx++, pessoa.fkMunicipio());
          ps.setString(idx++, pessoa.endereco());
          ps.setString(idx++, pessoa.complemento());
          ps.setString(idx++, pessoa.cep());
          ps.setObject(idx++, pessoa.idade());
          ps.setString(idx++, pessoa.bairro());
          ps.setObject(idx++, pessoa.sexo());
          ps.setString(idx++, pessoa.numero());
          return ps;
        },
        keyHolder);

    Map<String, Object> keys = keyHolder.getKeys();
    Long generatedId =
        keys != null && keys.containsKey("id") ? ((Number) keys.get("id")).longValue() : null;

    return pessoa.withId(generatedId);
  }

  @Transactional
  public void update(Pessoa pessoa) {
    findById(pessoa.id());
    String sql =
        """
                    UPDATE pessoa SET
                        nome = ?,
                        data_nascimento = ?,
                        idade = ?,
                        cpf = ?,
                        rg = ?,
                        email = ?,
                        telefone = ?,
                        fk_pais = ?,
                        fk_estado = ?,
                        fk_municipio = ?,
                        endereco = ?,
                        complemento = ?,
                        cep = ?,
                        bairro = ?,
                        sexo = ?,
                        numero = ?,
                        status = ?
                    WHERE id = ?
                """;

    Date dataNascimentoSql =
        pessoa.dataNascimento() != null ? Date.valueOf(pessoa.dataNascimento()) : null;

    Integer idade =
        pessoa.dataNascimento() != null
            ? Period.between(pessoa.dataNascimento(), LocalDate.now()).getYears()
            : null;

    jdbcTemplate.update(
        sql,
        pessoa.nome(),
        dataNascimentoSql,
        idade,
        pessoa.cpf(),
        pessoa.rg(),
        pessoa.email(),
        pessoa.telefone(),
        pessoa.fkPais(),
        pessoa.fkEstado(),
        pessoa.fkMunicipio(),
        pessoa.endereco(),
        pessoa.complemento(),
        pessoa.cep(),
        pessoa.bairro(),
        pessoa.sexo(),
        pessoa.numero(),
        pessoa.status(),
        pessoa.id());
  }

  @Transactional
  public void alterarStatus(Long id, Pessoa.Status status) {
    String sql = "UPDATE pessoa SET status = ?::pessoa_status WHERE id = ?";
    jdbcTemplate.update(sql, status, id);
  }

  @Transactional
  public void incrementarHospedagem(Long id) {
    String sql =
        """
            UPDATE pessoa
            SET vezes_hospedado = COALESCE(vezes_hospedado, 0) + 1
            WHERE id = ?
        """;
    jdbcTemplate.update(sql, id);
  }
}
