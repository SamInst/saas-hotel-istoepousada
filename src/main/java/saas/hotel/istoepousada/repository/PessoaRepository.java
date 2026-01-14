package saas.hotel.istoepousada.repository;

import static saas.hotel.istoepousada.dto.Empresa.mapEmpresa;
import static saas.hotel.istoepousada.dto.Pessoa.mapPessoa;
import static saas.hotel.istoepousada.dto.Veiculo.mapVeiculo;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import saas.hotel.istoepousada.dto.Veiculo;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;

@Repository
public class PessoaRepository {
  Logger log = LoggerFactory.getLogger(PessoaRepository.class);
  private final JdbcTemplate jdbcTemplate;

  public PessoaRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  private final ResultSetExtractor<List<Pessoa>> PESSOA_COM_EMPRESAS_EXTRACTOR =
      rs -> {
        Map<Long, Pessoa> pessoaMap = new LinkedHashMap<>();
        Map<Long, List<Empresa>> empresasPorPessoa = new HashMap<>();
        Map<Long, List<Veiculo>> veiculosPorPessoa = new HashMap<>();

        while (rs.next()) {
          Long pessoaId = rs.getLong("pessoa_id");

          if (!pessoaMap.containsKey(pessoaId)) {
            Pessoa pessoa = mapPessoa(rs);
            pessoaMap.put(pessoaId, pessoa);
            empresasPorPessoa.put(pessoaId, new ArrayList<>());
            veiculosPorPessoa.put(pessoaId, new ArrayList<>());
          }

          Long empresaId = rs.getObject("empresa_id", Long.class);
          if (empresaId != null) {
            Empresa empresa = mapEmpresa(rs);
            empresasPorPessoa.get(pessoaId).add(empresa);
          }

          Long veiculoId = rs.getObject("veiculo_id", Long.class);
          if (veiculoId != null) {
            Veiculo veiculo = mapVeiculo(rs, "veiculo_");
            veiculosPorPessoa.get(pessoaId).add(veiculo);
          }
        }

        return pessoaMap.entrySet().stream()
            .map(
                entry ->
                    entry
                        .getValue()
                        .withEmpresas(empresasPorPessoa.get(entry.getKey()))
                        .withVeiculos(veiculosPorPessoa.get(entry.getKey())))
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
      Long id, String termo, Pessoa.Status status, Pageable pageable) {
    boolean hasId = id != null;
    boolean hasTermo = termo != null && !termo.trim().isEmpty();

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
                p.pais                 AS pessoa_pais,
                p.estado               AS pessoa_estado,
                p.municipio            AS pessoa_municipio,
                p.endereco             AS pessoa_endereco,
                p.complemento          AS pessoa_complemento,
                p.vezes_hospedado      AS pessoa_vezes_hospedado,
                p.cep                  AS pessoa_cep,
                p.idade                AS pessoa_idade,
                p.bairro               AS pessoa_bairro,
                p.sexo                 AS pessoa_sexo,
                p.numero               AS pessoa_numero,
                p.status               AS pessoa_status,

                e.id                   AS empresa_id,
                e.razao_social         AS empresa_razao_social,
                e.nome_fantasia        AS empresa_nome_fantasia,
                e.cnpj                 AS empresa_cnpj,
                e.inscricao_estadual   AS empresa_inscricao_estadual,
                e.inscricao_municipal  AS empresa_inscricao_municipal,
                e.telefone             AS empresa_telefone,
                e.email                AS empresa_email,
                e.endereco             AS empresa_endereco,
                e.cep                  AS empresa_cep,
                e.numero               AS empresa_numero,
                e.complemento          AS empresa_complemento,
                e.pais                 AS empresa_pais,
                e.estado               AS empresa_estado,
                e.municipio            AS empresa_municipio,
                e.bairro               AS empresa_bairro,
                e.tipo_empresa         AS empresa_tipo_empresa,
                e.bloqueado            AS empresa_bloqueado,

                v.id                   AS veiculo_id,
                v.modelo               AS veiculo_modelo,
                v.marca                AS veiculo_marca,
                v.ano                  AS veiculo_ano,
                v.placa                AS veiculo_placa,
                v.cor                  AS veiculo_cor
            FROM pessoa p
            LEFT JOIN LATERAL (
                 SELECT e.*
                 FROM empresa_pessoa ep
                 JOIN empresa e ON e.id = ep.fk_empresa
                 WHERE ep.fk_pessoa = p.id
                 ORDER BY e.id DESC
                 LIMIT 1
             ) e ON true

             LEFT JOIN LATERAL (
                 SELECT v.*
                 FROM pessoa_veiculo pv
                 JOIN veiculo v ON v.id = pv.veiculo_id
                 WHERE pv.pessoa_id = p.id
                   AND pv.vinculo_ativo = true
                 ORDER BY v.id DESC
                 LIMIT 1
             ) v ON true
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

    if (status != null) {
      if (status.equals(Pessoa.Status.HOSPEDADO)) {
        where.append(" AND p.status = 'HOSPEDADO'::public.pessoa_status ");
      }

      if (status.equals(Pessoa.Status.BLOQUEADO)) {
        where.append(" AND p.status = 'BLOQUEADO'::public.pessoa_status ");
      }

      if (status.equals(Pessoa.Status.ATIVO)) {
        where.append(" AND p.status = 'ATIVO'::public.pessoa_status ");
      }
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

  public Pessoa findById(Long id) {
    Page<Pessoa> page = buscarPorIdNomeCpfOuHospedados(id, null, null, Pageable.ofSize(1));
    if (page.isEmpty()) {
      throw new NotFoundException("Pessoa não encontrada para o id: " + id);
    }
    return page.getContent().getFirst();
  }

  @Transactional
  public Pessoa save(Pessoa pessoa) {
    if (pessoa.id() == null) {
      return insert(pessoa);
    } else {
      update(pessoa);
      return findById(pessoa.id());
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
            pais,
            estado,
            municipio,
            endereco,
            complemento,
            vezes_hospedado,
            cep,
            idade,
            bairro,
            sexo,
            numero
        ) VALUES (now(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?, ?, ?);
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
          ps.setObject(idx++, pessoa.pais());
          ps.setObject(idx++, pessoa.estado());
          ps.setObject(idx++, pessoa.municipio());
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
                        pais = ?,
                        estado = ?,
                        municipio = ?,
                        endereco = ?,
                        complemento = ?,
                        cep = ?,
                        bairro = ?,
                        sexo = ?,
                        numero = ?,
                        status = ?::pessoa_status
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
        pessoa.pais(),
        pessoa.estado(),
        pessoa.municipio(),
        pessoa.endereco(),
        pessoa.complemento(),
        pessoa.cep(),
        pessoa.bairro(),
        pessoa.sexo(),
        pessoa.numero(),
        pessoa.status().toDb(),
        pessoa.id());
  }

  @Transactional
  public void alterarStatus(Long id, Pessoa.Status status) {
    var pessoa = findById(id);
    Pessoa.Status oldStatus = pessoa.status();
    String sql = "UPDATE pessoa SET status = ?::pessoa_status WHERE id = ?";
    jdbcTemplate.update(sql, status, id);
    log.info(
        "Usuário: [{}] alterou o status de: {} -> {} do cliente: [{}]",
        "usuario",
        oldStatus,
        status,
        pessoa);
  }

  @Transactional
  public void incrementarHospedagem(Long id) {
    var pessoa = findById(id);
    String sql =
        """
            UPDATE pessoa
            SET vezes_hospedado = COALESCE(vezes_hospedado, 0) + 1
            WHERE id = ?
        """;
    jdbcTemplate.update(sql, id);
    log.info(
        "Cliente: {}, Incrementado hospedagem (1), Total: {}", pessoa, pessoa.vezesHospedado());
  }
}
