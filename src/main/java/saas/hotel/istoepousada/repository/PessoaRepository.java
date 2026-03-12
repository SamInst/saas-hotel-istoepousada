package saas.hotel.istoepousada.repository;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import saas.hotel.istoepousada.dto.Empresa;
import saas.hotel.istoepousada.dto.Funcionario;
import saas.hotel.istoepousada.dto.Pessoa;
import saas.hotel.istoepousada.dto.Veiculo;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;

@Repository
public class PessoaRepository {

  private static final Logger log = LoggerFactory.getLogger(PessoaRepository.class);

  private final JdbcTemplate jdbcTemplate;

  public PessoaRepository(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  private static final RowMapper<Pessoa> PESSOA_ROW_MAPPER =
          (rs, rowNum) ->
                  new Pessoa(
                          rs.getLong("pessoa_id"),
                          rs.getObject("pessoa_data_hora_registro", LocalDateTime.class),
                          rs.getObject("pessoa_data_nascimento", LocalDate.class),
                          rs.getString("pessoa_nome"),
                          rs.getString("pessoa_cpf"),
                          rs.getString("pessoa_rg"),
                          rs.getString("pessoa_email"),
                          rs.getString("pessoa_telefone"),
                          rs.getString("pessoa_pais"),
                          rs.getString("pessoa_estado"),
                          rs.getString("pessoa_municipio"),
                          rs.getString("pessoa_endereco"),
                          rs.getString("pessoa_complemento"),
                          rs.getObject("pessoa_vezes_hospedado", Integer.class),
                          rs.getString("pessoa_cep"),
                          rs.getObject("pessoa_idade", Integer.class),
                          rs.getString("pessoa_bairro"),
                          rs.getObject("pessoa_sexo", Integer.class),
                          rs.getString("pessoa_numero"),
                          Pessoa.Status.map(rs.getString("pessoa_status")),
                          List.of(),
                          List.of(),
                          rs.getObject("pessoa_fk_funcionario", Long.class) == null
                                  ? null
                                  : new Funcionario.Nome(
                                  rs.getObject("pessoa_fk_funcionario", Long.class),
                                  rs.getString("pessoa_funcionario_nome")),
                          rs.getObject("pessoa_fk_titular", Long.class) == null
                                  ? null
                                  : new Pessoa.Nome(
                                  rs.getObject("pessoa_fk_titular", Long.class),
                                  rs.getString("pessoa_titular_nome")),
                          List.of());

  private static final RowMapper<Empresa> EMPRESA_ROW_MAPPER =
          (rs, rowNum) -> {
            Long empresaId = rs.getObject("empresa_id", Long.class);
            if (empresaId == null) return null;

            return new Empresa(
                    empresaId,
                    rs.getObject("empresa_data_hora_registro", LocalDateTime.class),
                    rs.getString("empresa_razao_social"),
                    rs.getString("empresa_nome_fantasia"),
                    rs.getString("empresa_cnpj"),
                    rs.getString("empresa_telefone"),
                    rs.getString("empresa_email"),
                    rs.getString("empresa_endereco"),
                    rs.getString("empresa_cep"),
                    rs.getString("empresa_numero"),
                    rs.getString("empresa_complemento"),
                    rs.getString("empresa_pais"),
                    rs.getString("empresa_estado"),
                    rs.getString("empresa_municipio"),
                    rs.getString("empresa_bairro"),
                    rs.getString("empresa_tipo_empresa"),
                    Empresa.Status.map(rs.getString("empresa_status")),
                    null,
                    List.of());
          };

  private static final RowMapper<Veiculo> VEICULO_ROW_MAPPER =
          (rs, rowNum) -> {
            Long veiculoId = rs.getObject("veiculo_id", Long.class);
            if (veiculoId == null) return null;

            return new Veiculo(
                    veiculoId,
                    rs.getString("veiculo_modelo"),
                    rs.getString("veiculo_marca"),
                    rs.getObject("veiculo_ano", Integer.class),
                    rs.getString("veiculo_placa"),
                    rs.getString("veiculo_cor"));
          };

  private static final ResultSetExtractor<List<Pessoa>> PESSOA_COM_EMPRESAS_E_VEICULOS_EXTRACTOR =
          rs -> {
            Map<Long, Pessoa> pessoaMap = new LinkedHashMap<>();
            Map<Long, Map<Long, Empresa>> empresasPorPessoa = new HashMap<>();
            Map<Long, Map<Long, Veiculo>> veiculosPorPessoa = new HashMap<>();

            int rowNum = 0;
            while (rs.next()) {
              Long pessoaId = rs.getLong("pessoa_id");

              if (!pessoaMap.containsKey(pessoaId)) {
                pessoaMap.put(pessoaId, PESSOA_ROW_MAPPER.mapRow(rs, rowNum));
                empresasPorPessoa.put(pessoaId, new LinkedHashMap<>());
                veiculosPorPessoa.put(pessoaId, new LinkedHashMap<>());
              }

              Empresa empresa = EMPRESA_ROW_MAPPER.mapRow(rs, rowNum);
              if (empresa != null) {
                empresasPorPessoa.get(pessoaId).putIfAbsent(empresa.id(), empresa);
              }

              Veiculo veiculo = VEICULO_ROW_MAPPER.mapRow(rs, rowNum);
              if (veiculo != null) {
                veiculosPorPessoa.get(pessoaId).putIfAbsent(veiculo.id(), veiculo);
              }

              rowNum++;
            }

            List<Pessoa> pessoas = new ArrayList<>();

            for (Map.Entry<Long, Pessoa> entry : pessoaMap.entrySet()) {
              Long pessoaId = entry.getKey();
              Pessoa base = entry.getValue();

              pessoas.add(
                      new Pessoa(
                              base.id(),
                              base.data_hora_registro(),
                              base.data_nascimento(),
                              base.nome(),
                              base.cpf(),
                              base.rg(),
                              base.email(),
                              base.telefone(),
                              base.pais(),
                              base.estado(),
                              base.municipio(),
                              base.endereco(),
                              base.complemento(),
                              base.vezes_hospedado(),
                              base.cep(),
                              base.idade(),
                              base.bairro(),
                              base.sexo(),
                              base.numero(),
                              base.status(),
                              new ArrayList<>(empresasPorPessoa.getOrDefault(pessoaId, Map.of()).values()),
                              new ArrayList<>(veiculosPorPessoa.getOrDefault(pessoaId, Map.of()).values()),
                              base.funcionario(),
                              base.titular(),
                              List.of()));
            }

            return pessoas;
          };

  public Page<Pessoa> buscar(
          Long id, String termo, String placaVeiculo, Pessoa.Status status, Pageable pageable) {

    boolean hasId = id != null;
    boolean hasTermo = termo != null && !termo.trim().isEmpty();
    boolean hasPlaca = placaVeiculo != null && !placaVeiculo.trim().isEmpty();

    String termoTrim = hasTermo ? termo.trim() : null;
    String search = hasTermo ? "%" + termoTrim + "%" : null;
    String placaTrim = hasPlaca ? placaVeiculo.trim().toUpperCase() : null;

    String baseSelect =
            """
            SELECT
                p.id                   AS pessoa_id,
                p.data_hora_registro   AS pessoa_data_hora_registro,
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
                p.fk_funcionario       AS pessoa_fk_funcionario,
                p.fk_titular           AS pessoa_fk_titular,
                func.nome              AS pessoa_funcionario_nome,
                titular.nome           AS pessoa_titular_nome,
                e.id                   AS empresa_id,
                e.data_hora_registro   AS empresa_data_hora_registro,
                e.razao_social         AS empresa_razao_social,
                e.nome_fantasia        AS empresa_nome_fantasia,
                e.cnpj                 AS empresa_cnpj,
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
                e.status               AS empresa_status,
                v.id                   AS veiculo_id,
                v.modelo               AS veiculo_modelo,
                v.marca                AS veiculo_marca,
                v.ano                  AS veiculo_ano,
                v.placa                AS veiculo_placa,
                v.cor                  AS veiculo_cor
            FROM pessoa p
            LEFT JOIN pessoa func ON func.id = p.fk_funcionario
            LEFT JOIN pessoa titular ON titular.id = p.fk_titular
            LEFT JOIN empresa_pessoa ep ON ep.fk_pessoa = p.id
            LEFT JOIN empresa e ON e.id = ep.fk_empresa
            LEFT JOIN pessoa_veiculo pv ON pv.pessoa_id = p.id AND pv.vinculo_ativo = true
            LEFT JOIN veiculo v ON v.id = pv.veiculo_id
            """;

    StringBuilder where = new StringBuilder(" WHERE 1 = 1 ");
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
      where.append(" AND p.status = ?::public.pessoa_status ");
      params.add(status.name());
    }

    if (hasPlaca) {
      where.append(
              """
              AND EXISTS (
                  SELECT 1
                  FROM pessoa_veiculo pv2
                  JOIN veiculo v2 ON v2.id = pv2.veiculo_id
                  WHERE pv2.pessoa_id = p.id
                    AND pv2.vinculo_ativo = true
                    AND UPPER(v2.placa) = ?
              )
              """);
      params.add(placaTrim);
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
    idsParams.add(pageable.getOffset());

    List<Long> ids =
            jdbcTemplate.query(idsSql, (rs, rowNum) -> rs.getLong("id"), idsParams.toArray());

    if (ids.isEmpty()) {
      return new PageImpl<>(List.of(), pageable, total);
    }

    String inPlaceholders = String.join(",", Collections.nCopies(ids.size(), "?"));

    String pageSql =
            baseSelect
                    + " WHERE p.id IN (" + inPlaceholders + ") "
                    + " ORDER BY p.nome ASC, e.razao_social ASC, v.placa ASC ";

    List<Pessoa> content =
            jdbcTemplate.query(pageSql, PESSOA_COM_EMPRESAS_E_VEICULOS_EXTRACTOR, ids.toArray());

    List<Pessoa> enriched = adicionarAcompanhantesParaTitulares(content);

    return new PageImpl<>(Objects.requireNonNull(enriched), pageable, total);
  }

  private List<Pessoa> adicionarAcompanhantesParaTitulares(List<Pessoa> pessoas) {
    if (pessoas == null || pessoas.isEmpty()) return pessoas;

    List<Long> titularIds =
            pessoas.stream()
                    .filter(p -> p.titular() == null)
                    .map(Pessoa::id)
                    .filter(Objects::nonNull)
                    .toList();

    if (titularIds.isEmpty()) return pessoas;

    Map<Long, List<Pessoa>> acompanhantesPorTitular = buscarAcompanhantesPorTitularIds(titularIds);

    return pessoas.stream()
            .map(
                    p ->
                            p.titular() != null
                                    ? p
                                    : new Pessoa(
                                    p.id(),
                                    p.data_hora_registro(),
                                    p.data_nascimento(),
                                    p.nome(),
                                    p.cpf(),
                                    p.rg(),
                                    p.email(),
                                    p.telefone(),
                                    p.pais(),
                                    p.estado(),
                                    p.municipio(),
                                    p.endereco(),
                                    p.complemento(),
                                    p.vezes_hospedado(),
                                    p.cep(),
                                    p.idade(),
                                    p.bairro(),
                                    p.sexo(),
                                    p.numero(),
                                    p.status(),
                                    p.empresas_vinculadas(),
                                    p.veiculos_vinculados(),
                                    p.funcionario(),
                                    p.titular(),
                                    acompanhantesPorTitular.getOrDefault(p.id(), List.of())))
            .toList();
  }

  public Pessoa findById(Long id) {
    Page<Pessoa> page = buscar(id, null, null, null, Pageable.ofSize(1));
    if (page.isEmpty()) {
      throw new NotFoundException("Pessoa não encontrada para o id: " + id);
    }
    return page.getContent().getFirst();
  }

  @Transactional
  public Pessoa save(Pessoa pessoa, Long funcionarioId) {
    if (pessoa.id() == null) {
      return insert(pessoa, funcionarioId);
    }
    update(pessoa, funcionarioId);
    return findById(pessoa.id());
  }

  private Pessoa insert(Pessoa pessoa, Long funcionarioId) {
    String sql =
            """
            INSERT INTO pessoa (
                data_hora_registro,
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
                numero,
                status,
                fk_funcionario,
                fk_titular
            ) VALUES (now(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?, ?, ?, ?::pessoa_status, ?, ?)
            """;

    Integer idade =
            pessoa.data_nascimento() != null
                    ? Period.between(pessoa.data_nascimento(), LocalDate.now()).getYears()
                    : null;

    KeyHolder keyHolder = new GeneratedKeyHolder();

    jdbcTemplate.update(
            connection -> {
              PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
              int idx = 1;

              ps.setString(idx++, pessoa.nome());
              ps.setDate(idx++, pessoa.data_nascimento() != null ? Date.valueOf(pessoa.data_nascimento()) : null);
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
              ps.setObject(idx++, idade);
              ps.setString(idx++, pessoa.bairro());
              ps.setObject(idx++, pessoa.sexo());
              ps.setString(idx++, pessoa.numero());
              ps.setString(idx++, pessoa.status() == null ? Pessoa.Status.ATIVO.name() : pessoa.status().name());

              if (funcionarioId != null) ps.setLong(idx++, funcionarioId);
              else ps.setNull(idx++, Types.BIGINT);

              if (pessoa.titular() != null && pessoa.titular().id() != null) ps.setLong(idx++, pessoa.titular().id());
              else ps.setNull(idx++, Types.BIGINT);

              return ps;
            },
            keyHolder);

    Number generated = keyHolder.getKey();
    if (generated == null) {
      throw new IllegalStateException("Não foi possível obter o id da pessoa inserida.");
    }

    return findById(generated.longValue());
  }

  @Transactional
  public void update(Pessoa pessoa, Long funcionarioId) {
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
                status = ?::pessoa_status,
                fk_funcionario = ?,
                fk_titular = ?
            WHERE id = ?
            """;

    Integer idade =
            pessoa.data_nascimento() != null
                    ? Period.between(pessoa.data_nascimento(), LocalDate.now()).getYears()
                    : null;

    jdbcTemplate.update(
            sql,
            pessoa.nome(),
            pessoa.data_nascimento() != null ? Date.valueOf(pessoa.data_nascimento()) : null,
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
            pessoa.status() == null ? Pessoa.Status.ATIVO.name() : pessoa.status().name(),
            funcionarioId,
            pessoa.titular() != null ? pessoa.titular().id() : null,
            pessoa.id());
  }

  @Transactional
  public void alterarStatus(Long id, Pessoa.Status status) {
    Pessoa pessoa = findById(id);
    jdbcTemplate.update("UPDATE pessoa SET status = ?::pessoa_status WHERE id = ?", status.name(), id);
    log.info("Status alterado da pessoa {}: {} -> {}", id, pessoa.status(), status);
  }

  @Transactional
  public void incrementarHospedagem(Long id) {
    jdbcTemplate.update(
            """
            UPDATE pessoa
            SET vezes_hospedado = COALESCE(vezes_hospedado, 0) + 1
            WHERE id = ?
            """,
            id);
  }

  private Funcionario.Authorization getFuncionarioAuthorizationFromRequest() {
    ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

    if (attributes == null) {
      log.warn("RequestAttributes não disponível, funcionário não será registrado");
      return null;
    }

    HttpServletRequest request = attributes.getRequest();
    Object attribute = request.getAttribute("funcionario");

    if (attribute == null) {
      log.warn("Funcionário não encontrado no request, operação sem registro de responsável");
      return null;
    }

    if (!(attribute instanceof Funcionario.Authorization funcionario)) {
      log.warn("Atributo 'funcionario' possui tipo inválido: {}", attribute.getClass().getName());
      return null;
    }

    return funcionario;
  }

  public Long getFuncionarioPessoaIdFromRequest() {
    Funcionario.Authorization funcionario = getFuncionarioAuthorizationFromRequest();
    return funcionario == null ? null : funcionario.pessoa().id();
  }

  public Long getFuncionarioIdFromRequest() {
    Funcionario.Authorization funcionario = getFuncionarioAuthorizationFromRequest();
    return funcionario == null ? null : funcionario.id();
  }

  private Map<Long, List<Pessoa>> buscarAcompanhantesPorTitularIds(List<Long> titularIds) {
    if (titularIds == null || titularIds.isEmpty()) return Map.of();

    String inPlaceholders = String.join(",", Collections.nCopies(titularIds.size(), "?"));

    String sql =
            """
            SELECT
                p.id                   AS pessoa_id,
                p.data_hora_registro   AS pessoa_data_hora_registro,
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
                p.fk_funcionario       AS pessoa_fk_funcionario,
                p.fk_titular           AS pessoa_fk_titular,
                func.nome              AS pessoa_funcionario_nome,
                titular.nome           AS pessoa_titular_nome
            FROM pessoa p
            LEFT JOIN pessoa func ON func.id = p.fk_funcionario
            LEFT JOIN pessoa titular ON titular.id = p.fk_titular
            WHERE p.fk_titular IN (""" + inPlaceholders + ") ORDER BY p.fk_titular, p.nome ";

return jdbcTemplate.query(
    sql,
    rs -> {
      Map<Long, List<Pessoa>> map = new LinkedHashMap<>();
      int rowNum = 0;
      while (rs.next()) {
        Pessoa acompanhante = PESSOA_ROW_MAPPER.mapRow(rs, rowNum++);
        Long titularId = acompanhante.titular() == null ? null : acompanhante.titular().id();
        if (titularId != null) {
          map.computeIfAbsent(titularId, k -> new ArrayList<>()).add(acompanhante);
        }
      }
      return map;
    },
    titularIds.toArray());
}
}
