package saas.hotel.istoepousada.repository;

import jakarta.servlet.http.HttpServletRequest;
import java.sql.Date;
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

  private static final ResultSetExtractor<List<Pessoa>> PESSOA_EXTRACTOR =
      rs -> {
        Map<Long, Pessoa> pessoaMap = new LinkedHashMap<>();
        Map<Long, Map<Long, Empresa>> empresasPorPessoa = new HashMap<>();
        Map<Long, Map<Long, Veiculo>> veiculosPorPessoa = new HashMap<>();

        int rowNum = 0;
        while (rs.next()) {
          Long pessoaId = rs.getLong("pessoa_id");

          if (!pessoaMap.containsKey(pessoaId)) {
            pessoaMap.put(pessoaId, Pessoa.ROW_MAPPER.mapRow(rs, rowNum));
            empresasPorPessoa.put(pessoaId, new LinkedHashMap<>());
            veiculosPorPessoa.put(pessoaId, new LinkedHashMap<>());
          }

          Empresa empresa = Empresa.ROW_MAPPER.mapRow(rs, rowNum);
          if (empresa != null) {
            empresasPorPessoa.get(pessoaId).putIfAbsent(empresa.id(), empresa);
          }

          Veiculo veiculo = Veiculo.ROW_MAPPER.mapRow(rs, rowNum);
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
                  List.of(),
                  base.profissao()));
        }

        return pessoas;
      };

  public static String SELECT_PESSOA =
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
          p.fk_funcionario       AS pessoa_funcionario_id,
          p.fk_titular           AS pessoa_titular_id,
          pf.nome                AS pessoa_funcionario_nome,
          t.nome                 AS pessoa_titular_nome,
          p.profissao            AS pessoa_profissao
  """;

  public static String SELECT_PESSOA_COMPLETO =
      """
          SELECT
          p.id                    AS pessoa_id,
          p.data_hora_registro    AS pessoa_data_hora_registro,
          p.nome                  AS pessoa_nome,
          p.data_nascimento       AS pessoa_data_nascimento,
          p.cpf                   AS pessoa_cpf,
          p.rg                    AS pessoa_rg,
          p.email                 AS pessoa_email,
          p.telefone              AS pessoa_telefone,
          p.pais                  AS pessoa_pais,
          p.estado                AS pessoa_estado,
          p.municipio             AS pessoa_municipio,
          p.endereco              AS pessoa_endereco,
          p.complemento           AS pessoa_complemento,
          p.vezes_hospedado       AS pessoa_vezes_hospedado,
          p.cep                   AS pessoa_cep,
          p.idade                 AS pessoa_idade,
          p.bairro                AS pessoa_bairro,
          p.sexo                  AS pessoa_sexo,
          p.numero                AS pessoa_numero,
          p.status                AS pessoa_status,
          p.fk_funcionario        AS pessoa_funcionario_id,
          p.fk_titular            AS pessoa_titular_id,
          p.profissao             AS pessoa_profissao,
          pf.nome                 AS pessoa_funcionario_nome,
          t.nome                  AS pessoa_titular_nome,
          e.id                    AS empresa_id,
          e.data_hora_registro    AS empresa_data_hora_registro,
          e.razao_social          AS empresa_razao_social,
          e.nome_fantasia         AS empresa_nome_fantasia,
          e.cnpj                  AS empresa_cnpj,
          e.telefone              AS empresa_telefone,
          e.email                 AS empresa_email,
          e.endereco              AS empresa_endereco,
          e.cep                   AS empresa_cep,
          e.numero                AS empresa_numero,
          e.complemento           AS empresa_complemento,
          e.pais                  AS empresa_pais,
          e.estado                AS empresa_estado,
          e.municipio             AS empresa_municipio,
          e.bairro                AS empresa_bairro,
          e.tipo_empresa          AS empresa_tipo_empresa,
          e.status                AS empresa_status,
          fe.id                   AS empresa_funcionario_id,
          pfe.nome                AS empresa_funcionario_nome,
          v.id                    AS veiculo_id,
          v.modelo                AS veiculo_modelo,
          v.marca                 AS veiculo_marca,
          v.ano                   AS veiculo_ano,
          v.placa                 AS veiculo_placa,
          v.cor                   AS veiculo_cor
          """;

  public static String FROM_BASE =
      """
          FROM pessoa p

            -- funcionario que cadastrou
            LEFT JOIN funcionario f ON f.id = p.fk_funcionario
            LEFT JOIN pessoa pf ON pf.id = f.fk_pessoa

            -- titular
            LEFT JOIN pessoa t ON t.id = p.fk_titular
            LEFT JOIN empresa_pessoa ep ON ep.fk_pessoa = p.id
            LEFT JOIN empresa e ON e.id = ep.fk_empresa

            LEFT JOIN funcionario fe on fe.id = e.fk_funcionario
            LEFT JOIN pessoa pfe ON pfe.id = fe.fk_pessoa

            LEFT JOIN pessoa_veiculo pv ON pv.pessoa_id = p.id
            LEFT JOIN veiculo v ON v.id = pv.veiculo_id AND pv.vinculo_ativo = true
          """;

  public Page<Pessoa> buscar(
      Long id,
      String termo,
      String placa,
      Pessoa.Status status,
      Pessoa.Ordenacao ordenacao,
      Pessoa.Direcao direcao,
      Pageable pageable) {

    boolean hasId = id != null;
    boolean hasTermo = termo != null && !termo.isEmpty();
    boolean hasPlaca = placa != null && !placa.trim().isEmpty();

    String search = hasTermo ? "%" + termo + "%" : null;
    String placaTrim = hasPlaca ? placa.trim().toUpperCase() : null;

    String veiculoJoinCondition = hasPlaca ? " AND UPPER(v.placa) = ?" : "";

    String baseSelect = String.format(SELECT_PESSOA_COMPLETO + FROM_BASE, veiculoJoinCondition);

    StringBuilder where =
        new StringBuilder(
            " WHERE p.status NOT IN ('CONTRATADO'::public.pessoa_status, 'DEMITIDO'::public.pessoa_status) ");
    List<Object> params = new ArrayList<>();

    if (hasId) {
      where.append(" AND p.id = ? ");
      params.add(id);
    }

    if (hasTermo) {
      where.append(" AND (p.nome ILIKE ? OR p.cpf = ?) ");
      params.add(search);
      params.add(termo);
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

    Pessoa.Ordenacao ord = ordenacao != null ? ordenacao : Pessoa.Ordenacao.NOME;
    Pessoa.Direcao dir = direcao != null ? direcao : Pessoa.Direcao.ASC;
    String orderByColumn =
        ord == Pessoa.Ordenacao.DATA_CADASTRO ? "p.data_hora_registro" : "p.nome";
    String direction = dir.name();

    long total;
    try {
      String countSql = "SELECT COUNT(*) FROM pessoa p" + where;
      total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
    } catch (EmptyResultDataAccessException ex) {
      total = 0L;
    }

    if (total == 0) {
      return new PageImpl<>(List.of(), pageable, 0);
    }

    String idsSql =
        "SELECT p.id FROM pessoa p "
            + where
            + " ORDER BY "
            + orderByColumn
            + " "
            + direction
            + " LIMIT ? OFFSET ?";

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
            + " WHERE p.id IN ("
            + inPlaceholders
            + ") ORDER BY "
            + orderByColumn
            + " "
            + direction
            + ", e.razao_social, v.placa";

    List<Object> pageParams = new ArrayList<>(Arrays.asList(ids.toArray()));
    if (hasPlaca) {
      pageParams.addFirst(placaTrim);
    }

    List<Pessoa> content = jdbcTemplate.query(pageSql, PESSOA_EXTRACTOR, pageParams.toArray());

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
                        acompanhantesPorTitular.getOrDefault(p.id(), List.of()),
                        p.profissao()))
        .toList();
  }

  public Pessoa findById(Long id) {
    try {
      return jdbcTemplate.queryForObject(
          SELECT_PESSOA_COMPLETO + FROM_BASE + " WHERE p.id = ?", Pessoa.ROW_MAPPER, id);
    } catch (EmptyResultDataAccessException ex) {
      throw new NotFoundException("Pessoa não encontrada para o id: " + id);
    }
  }

  public Pessoa insert(Pessoa.Request pessoa) {
    var pessoa_id =
        jdbcTemplate.queryForObject(
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
                            fk_titular,
                            profissao
                        ) VALUES (now(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?, ?, ?, ?::pessoa_status, ?, ?, ?) returning id
                        """,
            Long.class,
            pessoa.nome(),
            pessoa.data_nascimento() != null ? Date.valueOf(pessoa.data_nascimento()) : null,
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
            pessoa.data_nascimento() != null
                ? Period.between(pessoa.data_nascimento(), LocalDate.now()).getYears()
                : null,
            pessoa.bairro(),
            pessoa.sexo(),
            pessoa.numero(),
            pessoa.status() == null ? Pessoa.Status.ATIVO.name() : pessoa.status().name(),
            getFuncionarioIdFromRequest(),
            pessoa.titular() != null ? pessoa.titular().id() : null,
            pessoa.profissao());
    return findById(pessoa_id);
  }

  @Transactional
  public Pessoa update(Pessoa.Update pessoa) {
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
                    fk_titular = ?,
                    profissao = ?
                WHERE id = ?
                RETURNING id
                """;

    Integer idade =
        pessoa.data_nascimento() != null
            ? Period.between(pessoa.data_nascimento(), LocalDate.now()).getYears()
            : null;

    var pessoa_id =
        jdbcTemplate.queryForObject(
            sql,
            Long.class,
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
            getFuncionarioIdFromRequest(),
            pessoa.titular() != null ? pessoa.titular().id() : null,
            pessoa.profissao(),
            pessoa.id());
    return findById(pessoa_id);
  }

  @Transactional
  public void alterarStatus(Long id, Pessoa.Status status) {
    Pessoa pessoa = findById(id);
    jdbcTemplate.update(
        "UPDATE pessoa SET status = ?::pessoa_status WHERE id = ?", status.name(), id);
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

  public Long getFuncionarioIdFromRequest() {
    Funcionario.Authorization funcionario = getFuncionarioAuthorizationFromRequest();
    return funcionario == null ? null : funcionario.id();
  }

  private Map<Long, List<Pessoa>> buscarAcompanhantesPorTitularIds(List<Long> titularIds) {
    if (titularIds == null || titularIds.isEmpty()) return Map.of();

    String inPlaceholders = String.join(",", Collections.nCopies(titularIds.size(), "?"));

    String sql =
        SELECT_PESSOA
            + """
        FROM pessoa p
        LEFT JOIN funcionario f ON f.id = p.fk_funcionario
        LEFT JOIN pessoa pf ON pf.id = f.fk_pessoa
        LEFT JOIN pessoa t ON t.id = p.fk_titular
        WHERE p.fk_titular IN ("""
            + inPlaceholders
            + ") ORDER BY p.fk_titular, p.nome ";

    return jdbcTemplate.query(
        sql,
        rs -> {
          Map<Long, List<Pessoa>> map = new LinkedHashMap<>();
          int rowNum = 0;
          while (rs.next()) {
            Pessoa acompanhante = Pessoa.ROW_MAPPER.mapRow(rs, rowNum++);
            Long titularId = acompanhante.titular() == null ? null : acompanhante.titular().id();
            if (titularId != null) {
              map.computeIfAbsent(titularId, k -> new ArrayList<>()).add(acompanhante);
            }
          }
          return map;
        },
        titularIds.toArray());
  }

  public List<Pessoa> findPessoasByEmpresaId(Long empresa_id) {
    String sql =
        SELECT_PESSOA
            + """
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

  public void vincularTitular(Pessoa.VinculoTitular pessoa) {
    if (pessoa.vinculo()) {
      jdbcTemplate.update(
          "UPDATE pessoa SET fk_titular = ? WHERE id = ?",
          pessoa.titular().id(),
          pessoa.acompanhante().id());
    } else {
      jdbcTemplate.update(
          "UPDATE pessoa SET fk_titular = NULL WHERE id = ?", pessoa.acompanhante().id());
    }
  }

  public Boolean cpfJaExiste(String cpf) {
    return jdbcTemplate.queryForObject(
        """
                    SELECT COUNT(*) > 0
                    FROM pessoa
                    WHERE cpf = ?
                    """,
        Boolean.class,
        cpf);
  }

  public Boolean emailJaExiste(String email) {
    return jdbcTemplate.queryForObject(
        """
                    SELECT COUNT(*) > 0
                    FROM pessoa
                    WHERE email = ?
                    """,
        Boolean.class,
        email);
  }

  public Boolean telefoneJaExiste(String telefone) {
    return jdbcTemplate.queryForObject(
        """
                    SELECT COUNT(*) > 0
                    FROM pessoa
                    WHERE telefone = ?
                    """,
        Boolean.class,
        telefone);
  }

  public Boolean placaJaExiste(String placa) {
    return jdbcTemplate.queryForObject(
        """
                    SELECT COUNT(*) > 0
                    FROM veiculo
                    WHERE placa = ?
                    """,
        Boolean.class,
        placa);
  }
}
