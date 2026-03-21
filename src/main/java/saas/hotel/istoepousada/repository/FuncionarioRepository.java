package saas.hotel.istoepousada.repository;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Repository;
import saas.hotel.istoepousada.dto.*;
import saas.hotel.istoepousada.handler.exceptions.NotFoundException;

@Repository
public class FuncionarioRepository {

  private final JdbcTemplate jdbcTemplate;
  private final UsuarioRepository usuarioRepository;

  public FuncionarioRepository(JdbcTemplate jdbcTemplate, UsuarioRepository usuarioRepository) {
    this.jdbcTemplate = jdbcTemplate;
    this.usuarioRepository = usuarioRepository;
  }

  private static Funcionario mapFuncionarioBase(ResultSet rs, int rowNum) throws SQLException {
    BigDecimal salario = rs.getBigDecimal("salario");

    return new Funcionario(
        rs.getLong("id"),
        Pessoa.ROW_MAPPER.mapRow(rs, rowNum),
        rs.getObject("data_admissao", LocalDate.class),
        salario == null ? null : salario.floatValue(),
        Cargo.ROW_MAPPER.mapRow(rs, rowNum),
        Usuario.ROW_MAPPER.mapRow(rs, rowNum));
  }

  private static final ResultSetExtractor<List<Funcionario>> FUNCIONARIO_EXTRACTOR =
      rs -> {
        Map<Long, Funcionario> funcionarios = new LinkedHashMap<>();
        Map<Long, LinkedHashMap<Long, Tela>> telasPorFuncionario = new HashMap<>();
        Map<Long, Map<Long, List<Permissao>>> permissoesPorFuncionarioTela = new HashMap<>();
        Map<Long, Map<Long, Set<Long>>> permissaoIdsPorFuncionarioTela = new HashMap<>();

        int rowNum = 0;
        while (rs.next()) {
          Long funcionarioId = rs.getLong("id");

          if (!funcionarios.containsKey(funcionarioId)) {
            Funcionario funcionario = mapFuncionarioBase(rs, rowNum);

            funcionarios.put(funcionarioId, funcionario);
            telasPorFuncionario.put(funcionarioId, new LinkedHashMap<>());
            permissoesPorFuncionarioTela.put(funcionarioId, new HashMap<>());
            permissaoIdsPorFuncionarioTela.put(funcionarioId, new HashMap<>());
          }

          Tela tela = Tela.ROW_MAPPER.mapRow(rs, rowNum);
          if (tela != null) {
            LinkedHashMap<Long, Tela> telasMap = telasPorFuncionario.get(funcionarioId);
            telasMap.putIfAbsent(tela.id(), tela);

            Permissao permissao = Permissao.ROW_MAPPER.mapRow(rs, rowNum);
            if (permissao != null) {
              permissoesPorFuncionarioTela
                  .get(funcionarioId)
                  .computeIfAbsent(tela.id(), k -> new ArrayList<>());

              permissaoIdsPorFuncionarioTela
                  .get(funcionarioId)
                  .computeIfAbsent(tela.id(), k -> new HashSet<>());

              if (permissaoIdsPorFuncionarioTela
                  .get(funcionarioId)
                  .get(tela.id())
                  .add(permissao.id())) {
                permissoesPorFuncionarioTela.get(funcionarioId).get(tela.id()).add(permissao);
              }
            }
          }

          rowNum++;
        }

        List<Funcionario> resultado = new ArrayList<>();

        for (Map.Entry<Long, Funcionario> entry : funcionarios.entrySet()) {
          Long funcionarioId = entry.getKey();
          Funcionario funcionarioBase = entry.getValue();

          List<Tela> telas =
              telasPorFuncionario.get(funcionarioId).values().stream()
                  .map(
                      tela ->
                          new Tela(
                              tela.id(),
                              tela.nome(),
                              tela.descricao(),
                              permissoesPorFuncionarioTela
                                  .getOrDefault(funcionarioId, Map.of())
                                  .getOrDefault(tela.id(), List.of())))
                  .toList();

          Cargo cargo =
              funcionarioBase.cargo() == null
                  ? null
                  : new Cargo(
                      funcionarioBase.cargo().id(), funcionarioBase.cargo().descricao(), telas);

          resultado.add(
              new Funcionario(
                  funcionarioBase.id(),
                  funcionarioBase.pessoa(),
                  funcionarioBase.data_admissao(),
                  funcionarioBase.salario(),
                  cargo,
                  funcionarioBase.usuario()));
        }

        return resultado;
      };

  public Page<Funcionario> buscar(
      Long id, String termo, Long cargoId, Long pessoaId, Long usuarioId, Pageable pageable) {

    boolean hasId = id != null;
    boolean hasTermo = termo != null && !termo.trim().isEmpty();
    boolean hasCargoId = cargoId != null;
    boolean hasPessoaId = pessoaId != null;
    boolean hasUsuarioId = usuarioId != null;

    String termoTrim = hasTermo ? termo.trim() : null;
    String search = hasTermo ? "%" + termoTrim + "%" : null;

    String baseSelect =
        """
                        SELECT
                            f.id                                AS id,
                            f.data_admissao                     AS data_admissao,
                            f.salario                           AS salario,

                            p.id                                AS pessoa_id,
                            p.nome                              AS pessoa_nome,
                            p.data_nascimento                   AS pessoa_data_nascimento,
                            p.data_hora_registro                AS pessoa_data_hora_registro,
                            p.cpf                               AS pessoa_cpf,
                            p.rg                                AS pessoa_rg,
                            p.email                             AS pessoa_email,
                            p.telefone                          AS pessoa_telefone,
                            p.pais                              AS pessoa_pais,
                            p.estado                            AS pessoa_estado,
                            p.municipio                         AS pessoa_municipio,
                            p.endereco                          AS pessoa_endereco,
                            p.complemento                       AS pessoa_complemento,
                            p.vezes_hospedado                   AS pessoa_vezes_hospedado,
                            p.cep                               AS pessoa_cep,
                            p.idade                             AS pessoa_idade,
                            p.bairro                            AS pessoa_bairro,
                            p.sexo                              AS pessoa_sexo,
                            p.numero                            AS pessoa_numero,
                            p.status                            AS pessoa_status,
                            p.fk_titular                        AS pessoa_titular_id,
                            p.fk_funcionario                    AS pessoa_funcionario_id,
                            p.nome                              AS pessoa_funcionario_nome,
                            CASE WHEN p.fk_titular IS NOT NULL THEN TRUE ELSE FALSE END AS pessoa_titular,

                            c.id                                AS cargo_id,
                            c.cargo                             AS cargo_cargo,

                            u.id                                AS usuario_id,
                            u.username                          AS usuario_username,
                            u.bloqueado                         AS usuario_bloqueado,

                            t.id                                AS tela_id,
                            t.nome                              AS tela_nome,
                            t.descricao                         AS tela_descricao,

                            perm.id                             AS permissao_id,
                            perm.permissao                      AS permissao_permissao,
                            perm.descricao                      AS permissao_descricao

                        FROM funcionario f
                        INNER JOIN pessoa p ON p.id = f.fk_pessoa
                        INNER JOIN cargo c ON c.id = f.fk_cargo
                        LEFT JOIN usuario u ON u.id = f.fk_usuario
                        LEFT JOIN cargo_tela ct ON ct.cargo_id = c.id
                        LEFT JOIN tela t ON t.id = ct.tela_id
                        LEFT JOIN cargo_permissao cp ON cp.fk_cargo = c.id
                        LEFT JOIN permissao perm
                               ON perm.id = cp.fk_permissao
                              AND perm.fk_tela = t.id
                        """;

    StringBuilder where = new StringBuilder(" WHERE 1 = 1 ");
    List<Object> params = new ArrayList<>();

    if (hasId) {
      where.append(" AND f.id = ? ");
      params.add(id);
    }

    if (hasTermo) {
      where.append(" AND (p.nome ILIKE ? OR p.cpf = ?) ");
      params.add(search);
      params.add(termoTrim);
    }

    if (hasCargoId) {
      where.append(" AND c.id = ? ");
      params.add(cargoId);
    }

    if (hasPessoaId) {
      where.append(" AND p.id = ? ");
      params.add(pessoaId);
    }

    if (hasUsuarioId) {
      where.append(" AND u.id = ? ");
      params.add(usuarioId);
    }

    String countSql =
        """
                        SELECT COUNT(DISTINCT f.id)
                        FROM funcionario f
                        INNER JOIN pessoa p ON p.id = f.fk_pessoa
                        INNER JOIN cargo c ON c.id = f.fk_cargo
                        LEFT JOIN usuario u ON u.id = f.fk_usuario
                        """
            + where;

    Long total;
    try {
      total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
    } catch (EmptyResultDataAccessException ex) {
      total = 0L;
    }

    if (total == null || total == 0) {
      return new PageImpl<>(List.of(), pageable, 0);
    }

    String idsSql =
        """
                        SELECT f.id
                        FROM funcionario f
                        INNER JOIN pessoa p ON p.id = f.fk_pessoa
                        INNER JOIN cargo c ON c.id = f.fk_cargo
                        LEFT JOIN usuario u ON u.id = f.fk_usuario
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
            + " WHERE f.id IN ("
            + inPlaceholders
            + ") "
            + " ORDER BY p.nome, t.nome, perm.permissao ";

    List<Funcionario> content = jdbcTemplate.query(pageSql, FUNCIONARIO_EXTRACTOR, ids.toArray());

    return new PageImpl<>(Objects.requireNonNull(content), pageable, total);
  }

  public Funcionario findById(Long id) {
    Page<Funcionario> page = buscar(id, null, null, null, null, Pageable.ofSize(1));
    if (page.isEmpty()) {
      throw new NotFoundException("Funcionário não encontrado para o id: " + id);
    }
    return page.getContent().getFirst();
  }

  public Funcionario findByUsuarioId(Long usuarioId) {
    Page<Funcionario> page = buscar(null, null, null, null, usuarioId, Pageable.ofSize(1));
    if (page.isEmpty()) {
      throw new NotFoundException("Funcionário não encontrado para o usuario id: " + usuarioId);
    }
    return page.getContent().getFirst();
  }

  public Funcionario insert(Funcionario.Request request) {
    var usuario = usuarioRepository.create(request.usuario().username(), request.usuario().senha());
    var id =
        jdbcTemplate.queryForObject(
            """
                INSERT INTO funcionario (
                 fk_pessoa,
                 fk_cargo,
                 fk_usuario,
                 data_admissao,
                 salario)
                VALUES (?, ?, ?, ?, ?)
                RETURNING id
                """,
            Long.class,
            request.pessoa().id(),
            request.cargo().id(),
            usuario.id(),
            Date.valueOf(request.data_admissao()),
            request.salario());
    return findById(id);
  }

  public Funcionario update(Funcionario.Update funcionario) {
    jdbcTemplate.queryForObject(
        """
        UPDATE funcionario
          SET fk_cargo = ?,
               data_admissao = ?,
               salario = ?
         WHERE id = ?
         RETURNING id
        """,
        Long.class,
        funcionario.cargo().id(),
        Date.valueOf(funcionario.data_admissao()),
        funcionario.salario(),
        funcionario.id());
    return findById(funcionario.id());
  }

  public Funcionario.Authorization funcionarioLogin(Usuario.Id usuario) {
    String sql =
        """
                SELECT
                    f.id                              AS funcionario_id,
                    f.data_admissao                   AS funcionario_data_admissao,

                    u.id                              AS usuario_id,
                    u.username                        AS usuario_username,
                    u.bloqueado                       AS usuario_bloqueado,

                    p.id                              AS pessoa_id,
                    p.nome                            AS pessoa_nome,
                    p.data_nascimento                 AS pessoa_data_nascimento,
                    p.cpf                             AS pessoa_cpf,
                    p.email                           AS pessoa_email,
                    p.telefone                        AS pessoa_telefone,
                    p.status                          AS pessoa_status,
                    (p.fk_titular IS NULL)            AS pessoa_titular,

                    c.id                              AS cargo_id,
                    c.cargo                           AS cargo_cargo,

                    t.id                              AS tela_id,
                    t.nome                            AS tela_nome,
                    t.descricao                       AS tela_descricao,

                    pm.id                             AS permissao_id,
                    pm.permissao                      AS permissao_permissao,
                    pm.descricao                      AS permissao_descricao
                FROM funcionario f
                JOIN usuario u
                    ON u.id = f.fk_usuario
                JOIN pessoa p
                    ON p.id = f.fk_pessoa
                JOIN cargo c
                    ON c.id = f.fk_cargo
                LEFT JOIN cargo_tela ct
                    ON ct.cargo_id = c.id
                LEFT JOIN tela t
                    ON t.id = ct.tela_id
                LEFT JOIN cargo_permissao cp
                    ON cp.fk_cargo = c.id
                LEFT JOIN permissao pm
                    ON pm.id = cp.fk_permissao
                WHERE u.id = ?
                ORDER BY c.id, t.id, pm.id
                """;

    try {
      return jdbcTemplate.query(
          sql,
          rs -> {
            Funcionario.Authorization authorization = null;

            Long cargoId;
            String cargoDescricao;

            Map<Long, Tela> telasMap = new LinkedHashMap<>();
            Map<Long, Permissao> permissoesMap = new LinkedHashMap<>();

            while (rs.next()) {
              if (authorization == null) {
                cargoId = rs.getObject("cargo_id", Long.class);
                cargoDescricao = rs.getString("cargo_cargo");

                authorization =
                    new Funcionario.Authorization(
                        rs.getLong("funcionario_id"),
                        Usuario.ROW_MAPPER.mapRow(rs, rs.getRow()),
                        Pessoa.DadosPrincipais.ROW_MAPPER.mapRow(rs, rs.getRow()),
                        rs.getObject("funcionario_data_admissao", LocalDate.class),
                        new Cargo(cargoId, cargoDescricao, List.of()));
              }

              Tela tela = Tela.ROW_MAPPER.mapRow(rs, rs.getRow());
              if (tela != null) {
                telasMap.putIfAbsent(
                    tela.id(),
                    new Tela(tela.id(), tela.nome(), tela.descricao(), new ArrayList<>()));
              }

              Permissao permissao = Permissao.ROW_MAPPER.mapRow(rs, rs.getRow());
              if (permissao != null) {
                permissoesMap.putIfAbsent(permissao.id(), permissao);
              }
            }

            if (authorization == null) {
              return null;
            }

            List<Permissao> permissoes = new ArrayList<>(permissoesMap.values());

            List<Tela> telas =
                telasMap.values().stream()
                    .map(tela -> new Tela(tela.id(), tela.nome(), tela.descricao(), permissoes))
                    .toList();

            Cargo cargo =
                new Cargo(authorization.cargo().id(), authorization.cargo().descricao(), telas);

            return new Funcionario.Authorization(
                authorization.id(),
                authorization.usuario(),
                authorization.pessoa(),
                authorization.data_admissao(),
                cargo);
          },
          usuario.id());
    } catch (EmptyResultDataAccessException ex) {
      return null;
    }
  }
}
